package com.featurephone.launcher.apps

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.databinding.ActivityAboutBinding

class AboutActivity : Activity() {

    private lateinit var binding: ActivityAboutBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.featurephone.launcher.util.LocaleManager.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        // 显示版本号
        // 【修改】同时显示 versionCode（形如"版本 1.2 (3)"）。装完新包只看这一行
        // 就能确认装的是哪一次构建，不必再靠行为猜。
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            binding.tvVersion.text = "版本 $version ($versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvVersion.text = "版本 1.2.3 (6)"
        }

        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        applyColors()
    }

    override fun onResume() {
        super.onResume()
        applyColors()
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.tvAppName.setTextColor(ColorManager.getScreenText())
        binding.tvVersion.setTextColor(ColorManager.getScreenText())
        binding.tvDeveloper.setTextColor(ColorManager.getScreenText())
        binding.tvFree.setTextColor(ColorManager.getScreenText())
        binding.tvDisclaimerTitle.setTextColor(ColorManager.getScreenText())
        binding.tvDisclaimer.setTextColor(ColorManager.getScreenText())
        binding.tvPermissionTitle.setTextColor(ColorManager.getScreenText())
        binding.tvPermission.setTextColor(ColorManager.getScreenText())
        binding.tvOpensourceTitle.setTextColor(ColorManager.getScreenText())
        binding.tvOpensource.setTextColor(ColorManager.getScreenText())
        binding.tvPrivacyTitle.setTextColor(ColorManager.getScreenText())
        binding.tvPrivacy.setTextColor(ColorManager.getScreenText())
        binding.tvEmergencyTitle.setTextColor(ColorManager.getScreenText())
        binding.tvEmergency.setTextColor(ColorManager.getScreenText())
        binding.tvThanksTitle.setTextColor(ColorManager.getScreenText())
        binding.tvThanks.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)
    }
}
