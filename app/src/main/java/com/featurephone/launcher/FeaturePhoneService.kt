package com.featurephone.launcher

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.featurephone.launcher.util.KioskManager

/**
 * 功能机模式常驻前台服务
 * 职责：
 * 1. 保持应用在前台运行，防止被系统杀死
 * 2. 监控应用状态，被切换到后台时自动拉回
 * 3. 提供Kiosk模式支持
 */
class FeaturePhoneService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.featurephone.launcher.START"
        private const val ACTION_STOP = "com.featurephone.launcher.STOP"

        fun start(context: Context) {
            val intent = Intent(context, FeaturePhoneService::class.java)
            intent.action = ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            // 【修改】原来用 startService 发送 ACTION_STOP：Android 8+ 应用在后台时
            // startService 会抛 IllegalStateException（后台启动服务限制），改为直接 stopService。
            context.stopService(Intent(context, FeaturePhoneService::class.java))
        }
    }

    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, buildNotification())
            // 尝试启用Kiosk锁定
            maybeStartLockTask()
        }

        // 服务被杀死后自动重启
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 当用户从最近任务移除时，重启服务
        // 【修改】Android 12+ 在后台启动前台服务可能抛 ForegroundServiceStartNotAllowedException，
        // 未捕获会导致"被划掉→崩溃→重启"的循环；这里包一层 try/catch。
        try {
            val restartIntent = Intent(applicationContext, FeaturePhoneService::class.java)
            restartIntent.action = ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        } catch (_: Exception) {
            // 系统不允许后台重启时忽略即可，用户下次打开桌面时服务会重新启动
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FeaturePhoneApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setShowWhen(false)
            .build()
    }

    private fun maybeStartLockTask() {
        // 【修改】这里只负责“把本应用加入锁定任务白名单”，真正的锁定由
        // MainActivity.onResume() 中的 startLockTask() 完成（方法名容易误解，注释已澄清）。
        // 另外原实现不论 Kiosk 开关是否打开都会写入白名单，语义不对；现在只在开启时才设置。
        try {
            // 服务可能由 BootReceiver 先启动，此时 KioskManager 尚未初始化
            KioskManager.init(this)
            if (!KioskManager.isKioskEnabled()) return
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (dpm.isDeviceOwnerApp(packageName)) {
                val admin = ComponentName(this, DeviceAdminReceiver::class.java)
                dpm.setLockTaskPackages(admin, arrayOf(packageName))
            }
        } catch (e: Exception) {
            // 忽略，非设备所有者
        }
    }
}
