package com.featurephone.launcher.apps

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.R
import com.featurephone.launcher.databinding.ActivityProtectionSettingsBinding
import com.featurephone.launcher.util.AnimationHelper
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.FontSizeManager

class ProtectionSettingsActivity : Activity() {

    private lateinit var binding: ActivityProtectionSettingsBinding

    // 【修改】其他页面都有这个覆写，这里漏了：切换语言后本页仍显示旧语言
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.featurephone.launcher.util.LocaleManager.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityProtectionSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        applyColors()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
            AnimationHelper.overrideFinishTransition(this)
        }
    }

    override fun onResume() {
        super.onResume()
        applyColors()
    }

    // 【修改】本页只有一段很长的说明文字和「返回」按钮，没有别的可聚焦控件。
    // 在非触摸的功能机（方向键操作）上，焦点只会停在按钮上，ScrollView 不会滚动，
    // 后半段说明永远看不到。这里把方向键上下接过来直接翻页滚动。
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!::binding.isInitialized) return super.onKeyDown(keyCode, event)
        val step = (binding.root.resources.displayMetrics.density * 60).toInt()
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                binding.screenArea.scrollBy(0, step)
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                binding.screenArea.scrollBy(0, -step)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.tvExplain.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)
    }
}
