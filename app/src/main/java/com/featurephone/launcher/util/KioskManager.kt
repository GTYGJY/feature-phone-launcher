package com.featurephone.launcher.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import com.featurephone.launcher.DeviceAdminReceiver

/**
 * Kiosk锁定模式管理器
 * 负责Kiosk模式的启用、关闭和状态管理
 *
 * 注意：Kiosk模式需要应用被设为设备所有者（Device Owner）才能完全生效
 * 设置方法：adb shell dpm set-device-owner com.featurephone.launcher/.DeviceAdminReceiver
 */
object KioskManager {

    private const val PREFS_NAME = "kiosk_settings"
    private const val KEY_KIOSK_ENABLED = "kiosk_enabled"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 检查应用是否为设备所有者
     */
    fun isDeviceOwner(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查Kiosk模式是否已启用
     */
    fun isKioskEnabled(): Boolean {
        return prefs.getBoolean(KEY_KIOSK_ENABLED, false)
    }

    /**
     * 启用Kiosk模式
     * @return true表示启用成功，false表示不是设备所有者无法启用
     */
    fun enableKiosk(context: Context): Boolean {
        if (!isDeviceOwner(context)) {
            return false
        }
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(context, DeviceAdminReceiver::class.java)
            // 设置允许锁定的应用包名
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
            prefs.edit().putBoolean(KEY_KIOSK_ENABLED, true).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 关闭Kiosk模式
     */
    fun disableKiosk(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(context, DeviceAdminReceiver::class.java)
            // 清空允许锁定的应用包名
            if (isDeviceOwner(context)) {
                dpm.setLockTaskPackages(admin, emptyArray())
            }
        } catch (e: Exception) {
            // 忽略
        }
        prefs.edit().putBoolean(KEY_KIOSK_ENABLED, false).apply()
    }

    /**
     * 在Activity中应用Kiosk模式
     * 如果Kiosk已启用，调用startLockTask()进入锁定
     * 如果Kiosk已关闭，调用stopLockTask()退出锁定
     */
    fun applyKioskToActivity(activity: Activity) {
        if (isKioskEnabled() && isDeviceOwner(activity)) {
            try {
                activity.startLockTask()
            } catch (e: Exception) {
                // 忽略
            }
        } else {
            try {
                activity.stopLockTask()
            } catch (e: Exception) {
                // 忽略
            }
        }
    }

    /**
     * 退出Kiosk模式（在锁定状态下调用）
     */
    fun exitKiosk(activity: Activity) {
        try {
            activity.stopLockTask()
        } catch (e: Exception) {
            // 忽略
        }
        disableKiosk(activity)
    }
}
