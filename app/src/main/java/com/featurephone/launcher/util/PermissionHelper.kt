package com.featurephone.launcher.util

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限工具类
 * 统一处理权限检查、请求、永久拒绝判断和设置引导
 */
object PermissionHelper {

    // 【修改】记录"是否已经向用户申请过某个权限"，用于区分"从未申请"和"永久拒绝"
    private const val ASKED_PREFS = "permission_asked"
    private fun askedPrefs(activity: Activity) =
        activity.getSharedPreferences(ASKED_PREFS, android.content.Context.MODE_PRIVATE)
    private fun askedKey(permission: String) = "asked_$permission"

    /**
     * 检查权限是否全部已授予
     */
    fun hasPermissions(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 请求权限
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        // 【修改】记录"已申请过"，供 isPermissionPermanentlyDenied 判断使用
        val editor = askedPrefs(activity).edit()
        permissions.forEach { editor.putBoolean(askedKey(it), true) }
        editor.apply()
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * 判断权限是否被永久拒绝（用户勾选了"不再询问"）
     */
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            return false
        }
        // 【修改】关键修复：用户"从未申请过"该权限时，shouldShowRequestPermissionRationale 也返回 false，
        // 原实现会把它误判为"永久拒绝"，于是直接弹出"权限不足，请到设置中开启"，
        // 用户看到的就是"权限明明给了/还没问过，却提示无权限"。
        // 现在只有确实申请过、且系统不再显示申请理由时，才认为是永久拒绝。
        val asked = askedPrefs(activity).getBoolean(askedKey(permission), false)
        if (!asked) return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 判断是否有权限被永久拒绝
     */
    fun hasPermanentlyDeniedPermission(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any { isPermissionPermanentlyDenied(activity, it) }
    }

    /**
     * 打开应用设置页面，引导用户手动开启权限
     */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    /**
     * 检查并请求权限，返回是否需要请求
     * 如果权限被永久拒绝，返回false（需要引导去设置）
     */
    fun checkAndRequest(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ): Boolean {
        if (hasPermissions(activity, permissions)) {
            return true // 已有权限
        }
        if (hasPermanentlyDeniedPermission(activity, permissions)) {
            return false // 被永久拒绝，需要引导去设置
        }
        requestPermissions(activity, permissions, requestCode)
        return false // 正在请求
    }

    /**
     * 【修改】列出当前"未授予"的运行时权限中文名。
     * 用于「设置 > 权限管理」直接告诉用户到底缺哪一项，避免用户以为"权限都给了"
     * 却仍被功能拦截（Android 10+ 同一权限组内的权限不再互相连带授予）。
     */
    fun missingPermissionNames(context: android.content.Context): List<String> {
        // 【修改】移除已不再需要的 SEND_SMS / WRITE_CONTACTS（对应功能走系统 Intent）
        val items = listOf(
            android.Manifest.permission.CALL_PHONE to "打电话",
            android.Manifest.permission.READ_PHONE_STATE to "读取电话状态",
            android.Manifest.permission.READ_CONTACTS to "读取通讯录",
            android.Manifest.permission.READ_SMS to "读取短信",
            android.Manifest.permission.RECEIVE_SMS to "接收短信",
            android.Manifest.permission.READ_CALL_LOG to "读取通话记录",
            android.Manifest.permission.WRITE_CALL_LOG to "写入通话记录",
            android.Manifest.permission.ACCESS_COARSE_LOCATION to "大致位置（天气）"
        )
        return items.filter {
            ContextCompat.checkSelfPermission(context, it.first) != PackageManager.PERMISSION_GRANTED
        }.map { it.second }
    }

    /**
     * 各功能所需的权限组
     */
    object PermissionGroups {
        val DIALER = arrayOf(android.Manifest.permission.CALL_PHONE)
        // 【修改】只读即可：添加联系人走 ACTION_INSERT，由系统联系人应用处理
        val CONTACTS = arrayOf(
            android.Manifest.permission.READ_CONTACTS
        )
        // 【修改】短信只读 / 接收；发送走 ACTION_SENDTO，不需要 SEND_SMS
        val SMS = arrayOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        val CALL_LOG = arrayOf(
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_CALL_LOG
        )
        // 【修改】删除了未使用的 LOCATION / ALL 两个权限组：
        // 位置权限已改为在「联网设置」开启时按需申请，且天气只需要粗略定位。
    }
}
