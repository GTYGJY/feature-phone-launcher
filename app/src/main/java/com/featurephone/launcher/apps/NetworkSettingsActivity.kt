package com.featurephone.launcher.apps

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.AnimationHelper
import com.featurephone.launcher.databinding.ActivityNetworkSettingsBinding
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.util.LocaleManager

class NetworkSettingsActivity : Activity() {

    private lateinit var binding: ActivityNetworkSettingsBinding
    private lateinit var prefs: android.content.SharedPreferences

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        prefs = getSharedPreferences("network_settings", Context.MODE_PRIVATE)
        binding = ActivityNetworkSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        refreshStates()
        applyColors()
    }

    override fun onResume() {
        super.onResume()
        refreshStates()
        applyColors()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        // 联网总开关
        binding.switchNetworkMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("network_enabled", isChecked).apply()
            // 【修改】位置权限改为按需申请：只有开启联网（天气要用定位）时才请求粗略定位，
            // 不再放在桌面启动时和电话/短信等权限一起批量申请，减少一次被整体拒绝的机会。
            if (isChecked) requestCoarseLocationIfNeeded()
        }

        // 联网设置功能介绍
        binding.itemNetworkExplain.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, NetworkExplainActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }
    }

    private fun refreshStates() {
        val networkEnabled = prefs.getBoolean("network_enabled", false)
        binding.switchNetworkMaster.isChecked = networkEnabled
    }

    /** 【修改】按需申请天气所需的粗略定位权限（仅联网开关打开时调用） */
    private fun requestCoarseLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) return
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)

        val textColor = ColorManager.getScreenText()
        val items = listOf(binding.itemNetworkMaster, binding.itemNetworkExplain)
        items.forEach { item ->
            (item.getChildAt(0) as? android.widget.TextView)?.setTextColor(textColor)
        }
    }
}
