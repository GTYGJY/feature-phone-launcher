package com.featurephone.launcher.apps

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.R
import com.featurephone.launcher.databinding.ActivityUnitSettingsBinding
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.util.UnitSettingsManager

class UnitSettingsActivity : Activity() {

    private lateinit var binding: ActivityUnitSettingsBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.featurephone.launcher.util.LocaleManager.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityUnitSettingsBinding.inflate(layoutInflater)
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

        // 温度单位
        binding.itemTempUnit.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            val currentUnit = UnitSettingsManager.getTempUnit(this)
            val options = arrayOf(getString(R.string.unit_celsius), getString(R.string.unit_fahrenheit))
            val currentIndex = if (currentUnit == UnitSettingsManager.TEMP_FAHRENHEIT) 1 else 0

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.unit_temperature))
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val selectedUnit = if (which == 1) UnitSettingsManager.TEMP_FAHRENHEIT else UnitSettingsManager.TEMP_CELSIUS
                    UnitSettingsManager.setTempUnit(this, selectedUnit)
                    refreshStatus()
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // 日期格式
        binding.itemDateFormat.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            val currentFormat = UnitSettingsManager.getDateFormat(this)
            val options = arrayOf(getString(R.string.date_ymd), getString(R.string.date_dmy))
            val currentIndex = if (currentFormat == UnitSettingsManager.DATE_DMY) 1 else 0

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.unit_date_format))
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val selectedFormat = if (which == 1) UnitSettingsManager.DATE_DMY else UnitSettingsManager.DATE_YMD
                    UnitSettingsManager.setDateFormat(this, selectedFormat)
                    refreshStatus()
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun refreshStatus() {
        // 温度单位状态
        binding.tvTempUnitStatus.text = if (UnitSettingsManager.getTempUnit(this) == UnitSettingsManager.TEMP_FAHRENHEIT) {
            getString(R.string.unit_fahrenheit)
        } else {
            getString(R.string.unit_celsius)
        }

        // 日期格式状态
        binding.tvDateFormatStatus.text = if (UnitSettingsManager.getDateFormat(this) == UnitSettingsManager.DATE_DMY) {
            getString(R.string.date_dmy)
        } else {
            getString(R.string.date_ymd)
        }
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)

        val textColor = ColorManager.getScreenText()
        val items = listOf(binding.itemTempUnit, binding.itemDateFormat)
        items.forEach { item ->
            (item.getChildAt(0) as? android.widget.TextView)?.setTextColor(textColor)
            (item.getChildAt(1) as? android.widget.TextView)?.setTextColor(textColor)
        }
    }
}
