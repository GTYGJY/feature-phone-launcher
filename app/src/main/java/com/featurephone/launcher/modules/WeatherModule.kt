package com.featurephone.launcher.modules

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.featurephone.launcher.util.UnitSettingsManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 天气模块
 * 使用Open-Meteo免费API，无需API Key
 * 优先使用当前位置，无权限或获取失败时回退到北京
 */
class WeatherModule : InfoModule {

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var cachedWeather: String = "--°"
    private var lastFetchTime: Long = 0
    private val cacheDuration = 30 * 60 * 1000L // 30分钟缓存
    // 【修改】取位置失败/未授权后的重试间隔
    private val LOCATION_RETRY_INTERVAL_MS = 60 * 1000L
    // 【修改】单次定位请求的超时时间：超时后释放"在途"标记，允许下一次重试
    private val LOCATION_TIMEOUT_MS = 30 * 1000L
    private var updateCallback: (() -> Unit)? = null
    private var appContext: Context? = null

    // 当前位置
    private var currentLat: Double? = null
    private var currentLon: Double? = null
    // 【修改】原来用 locationRequested 一次性标记，导致“先开天气、后授权定位”时永远不再尝试取位置；
    // 改为记录上次尝试时间并做节流重试，直到拿到坐标为止。
    private var lastLocationAttempt = 0L
    // 【修改】避免重复注册定位监听：一次定位请求未结束（或超时未回调）前不再发起新的请求，
    // 否则常驻运行时每 60 秒就会堆积一个 LocationListener。
    private var locationRequestPending = false
    // 【修改】保存正在使用的定位监听器与 LocationManager：超时/停止时必须 removeUpdates，
    // 否则每次超时都会留下一个再也收不到回调的监听器，常驻运行会持续累积。
    private var pendingLocationListener: LocationListener? = null
    private var pendingLocationManager: LocationManager? = null
    private val locationTimeoutRunnable = Runnable { releasePendingLocation() }
    private var hasLocationPermission = false
    private var cityName: String = ""

    override fun getId(): String = "weather"

    override fun getDisplayName(): String = "天气"

    override fun getContent(context: Context): String {
        // 检查联网总开关
        val prefs = context.getSharedPreferences("network_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("network_enabled", false)) {
            cachedWeather = "--°"
            return "--°"
        }
        // 检查位置权限
        // 【修改】清单里已移除 ACCESS_FINE_LOCATION，只判断粗略定位
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            cachedWeather = "--°"
            return "--°"
        }
        // 尝试获取当前位置（未拿到坐标时按 60 秒节流重试，权限后来被授予也能自动生效）
        if ((currentLat == null || currentLon == null) &&
            System.currentTimeMillis() - lastLocationAttempt > LOCATION_RETRY_INTERVAL_MS
        ) {
            lastLocationAttempt = System.currentTimeMillis()
            getCurrentLocation(context.applicationContext)
        }
        // 有缓存且未过期，直接返回
        if (System.currentTimeMillis() - lastFetchTime < cacheDuration && cachedWeather != "--°") {
            return cachedWeather
        }
        // 异步获取天气
        fetchWeather()
        return cachedWeather
    }

    override fun getContentSize(): Float = 20f

    override fun getSubTitle(context: Context): String? {
        return if (cityName.isNotBlank()) cityName else null
    }

    override fun needsLiveUpdate(): Boolean = true

    override fun startListening(context: Context, onUpdate: () -> Unit) {
        appContext = context.applicationContext
        updateCallback = onUpdate
        // 立即尝试获取一次
        if (cachedWeather == "--°") {
            getContent(context.applicationContext)
        }
    }

    override fun stopListening(context: Context) {
        updateCallback = null
        // 【修改】模块停止时移除在途定位监听并清掉未执行的回调，避免常驻进程里一直持有定位监听器
        releasePendingLocation()
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 【修改】释放当前定位请求：移除超时回调 + removeUpdates，并复位"在途"标记。
     * 成功回调、超时、停止监听、注册失败四条路径都走这里，确保监听器一定被注销。
     */
    private fun releasePendingLocation() {
        handler.removeCallbacks(locationTimeoutRunnable)
        pendingLocationListener?.let { listener ->
            try {
                pendingLocationManager?.removeUpdates(listener)
            } catch (_: Exception) {}
        }
        pendingLocationListener = null
        pendingLocationManager = null
        locationRequestPending = false
    }

    /**
     * 获取当前位置
     */
    private fun getCurrentLocation(context: Context) {
        // 【修改】已有请求在途时直接返回，避免重复注册监听器
        if (locationRequestPending) return
        try {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                // 无权限，使用默认位置
                return
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // 优先使用最后已知位置（快速）
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (lastLocation != null) {
                currentLat = lastLocation.latitude
                currentLon = lastLocation.longitude
                // 位置更新后重新获取天气
                if (cachedWeather == "--°") {
                    fetchWeather()
                }
                return
            }

            // 没有最后已知位置，请求单次位置更新
            locationRequestPending = true
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // 【修改】回调后同样走释放逻辑，确保监听器被注销、超时回调被移除
                    releasePendingLocation()
                    currentLat = location.latitude
                    currentLon = location.longitude
                    fetchWeather() // 获取到位置后重新获取天气
                }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            pendingLocationManager = locationManager
            pendingLocationListener = listener
            try {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null)
                // 【修改】30 秒超时保护：始终没有回调时 removeUpdates 注销监听器（原来只清标记，监听器泄漏）
                handler.postDelayed(locationTimeoutRunnable, LOCATION_TIMEOUT_MS)
            } catch (_: Exception) {
                releasePendingLocation()
            }
        } catch (_: Exception) {
            // 忽略，使用默认位置
        }
    }

    private fun fetchWeather() {
        if (!hasLocationPermission) return // 无位置权限不请求
        if (currentLat == null || currentLon == null) return // 未获取到位置不请求
        if (System.currentTimeMillis() - lastFetchTime < 5000) return // 5秒内不重复请求
        lastFetchTime = System.currentTimeMillis()

        val lat = currentLat!!
        val lon = currentLon!!

        executor.execute {
            try {
                // 先用Geocoder获取城市名
                try {
                    val geocoder = Geocoder(appContext ?: return@execute, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val addr = addresses[0]
                        cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                    }
                } catch (_: Exception) {
                    cityName = ""
                }

                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$lat" +
                            "&longitude=$lon" +
                            "&current=temperature_2m,weather_code"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "FeaturePhoneLauncher/1.0")

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val weatherCode = current.getInt("weather_code")
                val weatherText = weatherCodeToText(weatherCode)

                // 根据设置格式化温度
                val tempText = if (appContext != null) {
                    UnitSettingsManager.formatTemperature(appContext!!, temp)
                } else {
                    "${temp}°C"
                }

                cachedWeather = "$tempText $weatherText"
                // 通知UI更新
                handler.post {
                    updateCallback?.invoke()
                }
            } catch (_: Exception) {
                lastFetchTime = 0 // 允许下次重试
            }
        }
    }

    private fun weatherCodeToText(code: Int): String {
        return when (code) {
            0 -> "晴"
            1, 2, 3 -> "多云"
            45, 48 -> "雾"
            51, 53, 55, 56, 57 -> "毛毛雨"
            61, 63, 65 -> "雨"
            66, 67 -> "冻雨"
            71, 73, 75 -> "雪"
            77 -> "米雪"
            80, 81, 82 -> "阵雨"
            85, 86 -> "阵雪"
            95, 96, 99 -> "雷暴"
            else -> "未知"
        }
    }
}
