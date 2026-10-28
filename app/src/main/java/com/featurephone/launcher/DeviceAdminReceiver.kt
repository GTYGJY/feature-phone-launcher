package com.featurephone.launcher

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 设备管理员接收器
 * 用于获取设备管理权限，支持：
 * - 禁用状态栏下拉
 * - 锁定屏幕
 * - 禁用相机
 * - Kiosk模式支持
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DeviceAdminReceiver"

        fun getComponentName(context: Context): android.content.ComponentName {
            return android.content.ComponentName(context, DeviceAdminReceiver::class.java)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "设备管理员已启用")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "设备管理员已禁用")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "禁用设备管理员将退出功能机模式，确定要继续吗？"
    }
}
