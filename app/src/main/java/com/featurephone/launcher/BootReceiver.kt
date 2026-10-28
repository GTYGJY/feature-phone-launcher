package com.featurephone.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * 开机自启动接收器
 * 手机开机后自动启动功能机模式
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        // 启动前台服务：任何版本都可以直接启动服务
        FeaturePhoneService.start(context)

        // 【修改】Android 10(Q) 起禁止后台启动 Activity，这里直接 startActivity 会被系统静默拦截
        //（不会崩溃，但也拉不起界面）；而且本应用注册了 HOME，开机后系统本来就会拉起默认桌面。
        // 因此仅在 Android 9 及以下保留这个兜底逻辑，避免无意义的调用和误导。
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val launchIntent = Intent(context, MainActivity::class.java)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
            } catch (_: Exception) {
                // 部分 ROM 仍可能拒绝，忽略即可
            }
        }
    }
}
