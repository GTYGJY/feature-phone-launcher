package com.featurephone.launcher.apps

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.featurephone.launcher.DeviceAdminReceiver
import com.featurephone.launcher.MainActivity
import com.featurephone.launcher.R
import com.featurephone.launcher.ShortcutManager
import com.featurephone.launcher.databinding.ActivityOtherSettingsBinding
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.util.KioskManager
import com.featurephone.launcher.util.ModeManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.AnimationHelper

class OtherSettingsActivity : Activity() {

    private lateinit var binding: ActivityOtherSettingsBinding
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private var adminClickCount = 0
    private var adminLastClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        ShortcutManager.init(this)
        dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        ModeManager.init(this)
        binding = ActivityOtherSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        applyColors()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        binding.itemShortcut.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, AppListActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        binding.itemKiosk.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            if (!KioskManager.isDeviceOwner(this)) {
                Toast.makeText(this,
                    "需ADB设为设备所有者:\nadb shell dpm set-device-owner com.featurephone.launcher/.DeviceAdminReceiver\n\n警告：高级功能，非必要不建议开启，可能导致无法退出等严重后果",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (KioskManager.isKioskEnabled()) {
                // 关闭Kiosk模式
                KioskManager.disableKiosk(this)
                Toast.makeText(this, "Kiosk模式已关闭", Toast.LENGTH_SHORT).show()
            } else {
                // 启用Kiosk模式
                if (KioskManager.enableKiosk(this)) {
                    Toast.makeText(this, "Kiosk模式已启用", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                } else {
                    Toast.makeText(this, "Kiosk模式启用失败", Toast.LENGTH_SHORT).show()
                }
            }
            updateStatus()
        }

        binding.itemDeviceAdmin.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            if (dpm.isAdminActive(adminComponent)) {
                // 已激活：跳转到系统设备管理员设置页面，用户可手动取消激活
                android.app.AlertDialog.Builder(this)
                    .setTitle("设备管理员")
                    .setMessage("设备管理员已激活。\n\n如需取消，请在系统设置中手动关闭。\n点击确定跳转到设置页面。")
                    .setPositiveButton("去设置") { _, _ ->
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
                return@setOnClickListener
            }
            val now = System.currentTimeMillis()
            if (now - adminLastClickTime > 2000) {
                adminClickCount = 1 // 超过2秒重新计数
            } else {
                adminClickCount++
            }
            adminLastClickTime = now
            val remaining = 5 - adminClickCount
            if (remaining > 0) {
                Toast.makeText(this, "再连续点击${remaining}下，进入设备管理员设置。", Toast.LENGTH_SHORT).show()
            } else {
                adminClickCount = 0
                // 显示详细风险提示，用户确认后再跳转
                android.app.AlertDialog.Builder(this)
                    .setTitle("设备管理员权限说明")
                    .setMessage("设备管理员是一项高权限系统功能，激活后本应用将获得以下能力：\n\n• 锁定屏幕\n• 禁用相机\n• 设置密码规则\n\n注意事项：\n• 此权限仅用于Kiosk锁定模式，不会用于其他用途\n• 激活后如需卸载本应用，必须先在系统设置中取消设备管理员权限\n• 您可以随时在系统设置中取消激活\n\n是否继续激活？")
                    .setPositiveButton("继续激活") { _, _ ->
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "激活后可使用Kiosk锁定功能，包括锁屏、禁用相机等。您可随时在系统设置中取消激活。")
                        startActivity(intent)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        binding.itemAdminExplain.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, AdminExplainActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        binding.itemDefaultLauncher.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } else {
                startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
            }
            Toast.makeText(this, "请将本应用设为默认桌面，设为默认后按Home键即可快速返回功能机模式", Toast.LENGTH_LONG).show()
        }

        binding.itemBackgroundApps.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(android.content.Intent(this, BackgroundAppManagerActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        binding.itemUIMode.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            val options = arrayOf("标准模式", "极简模式")
            val currentIndex = if (ModeManager.isSimpleMode()) 1 else 0
            android.app.AlertDialog.Builder(this)
                .setTitle("选择界面模式")
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val newMode = if (which == 1) ModeManager.MODE_SIMPLE else ModeManager.MODE_STANDARD
                    if (newMode != ModeManager.getMode()) {
                        ModeManager.setMode(newMode)
                        Toast.makeText(this, "已切换为${options[which]}", Toast.LENGTH_SHORT).show()
                        // 重启主界面以应用新模式
                        val intent = android.content.Intent(this, com.featurephone.launcher.MainActivity::class.java)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        binding.itemPermissions.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            // 【修改】先告诉用户具体缺哪几项权限，再打开应用设置页。
            // 之前只跳设置，用户容易误以为"权限都给了"，实际可能只给了同一权限组里的另一项。
            val missing = com.featurephone.launcher.util.PermissionHelper.missingPermissionNames(this)
            val message = if (missing.isEmpty()) {
                "所有运行时权限均已授予"
            } else {
                "未授予：" + missing.joinToString("、")
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)

        val items = listOf(
            binding.itemShortcut, binding.itemKiosk, binding.itemDeviceAdmin,
            binding.itemDefaultLauncher, binding.itemBackgroundApps,
            binding.itemUIMode, binding.itemPermissions, binding.itemAdminExplain
        )
        items.forEach { item ->
            for (i in 0 until item.childCount) {
                (item.getChildAt(i) as? android.widget.TextView)?.setTextColor(ColorManager.getScreenText())
            }
        }
        binding.tvShortcutStatus.setTextColor(ColorManager.getScreenText())
        binding.tvKioskStatus.setTextColor(ColorManager.getScreenText())
        binding.tvDeviceAdminStatus.setTextColor(ColorManager.getScreenText())
        binding.tvLauncherStatus.setTextColor(ColorManager.getScreenText())
        binding.tvUIModeStatus.setTextColor(ColorManager.getScreenText())
        binding.tvBackgroundAppsStatus.setTextColor(ColorManager.getScreenText())
    }

    private fun updateStatus() {
        val count = ShortcutManager.getShortcuts().size
        binding.tvShortcutStatus.text = "已添加 $count/${ShortcutManager.MAX_SHORTCUTS} 个"

        val isDeviceOwner = dpm.isDeviceOwnerApp(packageName)
        binding.tvKioskStatus.text = when {
            KioskManager.isKioskEnabled() -> "已启用（点击关闭）"
            isDeviceOwner -> "已就绪（点击启用）"
            else -> "未启用（需ADB设置）"
        }

        val isAdmin = dpm.isAdminActive(adminComponent)
        binding.tvDeviceAdminStatus.text = if (isAdmin) "已激活" else "未激活（点击激活）"

        binding.tvLauncherStatus.text = if (isDefaultLauncher()) "已是默认桌面" else "点击设置"

        binding.tvUIModeStatus.text = if (ModeManager.isSimpleMode()) "极简模式" else "标准模式"

        val isOwner = try {
            dpm.isDeviceOwnerApp(packageName)
        } catch (e: Exception) {
            false
        }
        binding.tvBackgroundAppsStatus.text = if (isOwner) "可用" else "需设备所有者"
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == packageName
    }
}
