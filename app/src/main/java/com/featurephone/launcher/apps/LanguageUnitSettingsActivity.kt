package com.featurephone.launcher.apps

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.AnimationHelper
import com.featurephone.launcher.databinding.ActivityLanguageUnitSettingsBinding
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.util.LocaleManager

class LanguageUnitSettingsActivity : Activity() {

    private lateinit var binding: ActivityLanguageUnitSettingsBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.featurephone.launcher.util.LocaleManager.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityLanguageUnitSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        refreshStatus()
        applyColors()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        applyColors()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        // 语言：仅支持中文，点击无操作
        binding.itemLanguage.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            // 暂不支持其他语言
        }

        // 单位设置
        binding.itemUnitSettings.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, UnitSettingsActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        }
    }

    private fun refreshStatus() {
        // 仅支持中文
        binding.tvLanguageStatus.text = getString(com.featurephone.launcher.R.string.lang_chinese)
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)

        val textColor = ColorManager.getScreenText()
        val items = listOf(binding.itemLanguage, binding.itemUnitSettings)
        items.forEach { item ->
            (item.getChildAt(0) as? android.widget.TextView)?.setTextColor(textColor)
            (item.getChildAt(1) as? android.widget.TextView)?.setTextColor(textColor)
        }
    }
}
