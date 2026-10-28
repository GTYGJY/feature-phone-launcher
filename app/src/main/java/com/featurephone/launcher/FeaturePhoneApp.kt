package com.featurephone.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeaturePhoneApp : Application() {

    companion object {
        const val CHANNEL_ID = "feature_phone_service"
        const val CHANNEL_NAME = "功能机模式服务"
        lateinit var instance: FeaturePhoneApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 全局异常捕获：记录崩溃日志，崩溃后重启到主界面
        // 【修改】
        // 1) 不再把 disclaimer_agreed 自动置为 true（那等于替用户接受免责声明）；
        // 2) 增加崩溃循环熔断：10 秒内连续崩溃超过 3 次时不再自动重启，
        //    避免"启动→崩溃→重启→崩溃"的死循环（此前 T9 崩溃在 7 秒内重启了 6 次）。
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                saveCrashLog(e)
            } catch (_: Throwable) {}

            var shouldRestart = true
            try {
                val prefs = getSharedPreferences("feature_phone_prefs", MODE_PRIVATE)
                val now = System.currentTimeMillis()
                val lastCrashAt = prefs.getLong("last_crash_at", 0L)
                val crashCount = if (now - lastCrashAt <= 10_000L) {
                    prefs.getInt("crash_count_10s", 0) + 1
                } else {
                    1
                }
                prefs.edit()
                    .putLong("last_crash_at", now)
                    .putInt("crash_count_10s", crashCount)
                    .commit() // 进程即将被杀，必须同步落盘
                shouldRestart = crashCount <= 3
                if (!shouldRestart) {
                    // 【修改】熔断后留一个标记，主界面下次启动时提示用户；
                    // 原来熔断只会"不再重启"，用户看到的是桌面反复闪退后彻底没反应，没有任何说明。
                    prefs.edit().putBoolean("crash_loop_detected", true).commit()
                }
            } catch (_: Throwable) {}

            if (shouldRestart) {
                try {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } catch (_: Throwable) {}
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }

        com.featurephone.launcher.util.FontSizeManager.init(this)
        createNotificationChannel()
    }

    /**
     * 【修改】崩溃日志脱敏：把 7~15 位的连续数字（手机号、含号码的 Uri 等）替换掉，
     * 避免用户把 crash_log.txt 发给开发者时带出通讯录/通话相关号码。
     */
    private fun sanitize(text: String): String {
        return try {
            text.replace(Regex("(?<!\\d)\\d{7,15}(?!\\d)"), "[号码已隐藏]")
        } catch (_: Throwable) {
            text
        }
    }

    private fun saveCrashLog(e: Throwable) {
        try {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val log = "=== 崩溃时间: $time ===\n${sanitize(sw.toString())}\n\n"
            val dir = getExternalFilesDir(null)
            val file = File(dir, "crash_log.txt")
            // 【修改】日志超过 512KB 时轮转，避免长期使用无限增长占满存储
            if (file.exists() && file.length() > 512 * 1024) {
                val old = File(dir, "crash_log.old.txt")
                if (old.exists()) old.delete()
                file.renameTo(old)
            }
            file.appendText(log)
        } catch (_: Throwable) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "功能机模式常驻前台服务"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}