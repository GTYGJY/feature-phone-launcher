package com.featurephone.launcher.apps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.featurephone.launcher.databinding.ActivitySettingsBinding
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.AnimationHelper

class SettingsActivity : Activity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.featurephone.launcher.util.LocaleManager.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        applyColors()
    }

    override fun onResume() {
        super.onResume()
        applyColors()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        // 联网设置
        binding.itemNetworkSettings.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, NetworkSettingsActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        // 颜色设置 -> 二级菜单
        binding.itemColorSettings.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, ColorSettingsActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        // 显示设置 -> 二级菜单
        binding.itemDisplaySettings.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, DisplaySettingsActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        // 语言和单位设置 -> 二级菜单
        binding.itemLanguageUnitSettings.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, LanguageUnitSettingsActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        // 通知管理 -> 二级菜单
        binding.itemNotification.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, NotificationManagerActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        // 防护提示 -> 二级菜单
        binding.itemProtection.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, ProtectionSettingsActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        // 省电优化 -> 跳转到系统电池设置
        binding.itemBatteryOptimization.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            try {
                // 跳转到电池设置页面
                val intent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                startActivity(intent)
                AnimationHelper.overrideStartTransition(this)
                android.widget.Toast.makeText(this, "请开启省电模式，本应用运行时屏幕保持常亮", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // 备用：跳转到应用信息页面
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        // 其他设置 -> 二级菜单
        binding.itemOtherSettings.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, OtherSettingsActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }

        // 关于 -> 二级菜单
        binding.itemAbout.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, AboutActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)

        val items = listOf(
            binding.itemNetworkSettings, binding.itemColorSettings,
            binding.itemDisplaySettings, binding.itemLanguageUnitSettings,
            binding.itemNotification, binding.itemProtection,
            binding.itemBatteryOptimization, binding.itemOtherSettings,
            binding.itemAbout
        )
        items.forEach { item ->
            (item.getChildAt(0) as? android.widget.TextView)?.setTextColor(ColorManager.getScreenText())
            (item.getChildAt(1) as? android.widget.TextView)?.setTextColor(ColorManager.getScreenText())
        }
    }
}
