package com.featurephone.launcher.apps

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import com.featurephone.launcher.databinding.ActivityDisplaySettingsBinding
import com.featurephone.launcher.modules.EmptyModule
import com.featurephone.launcher.modules.ModuleManager
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.FontSizeManager

class DisplaySettingsActivity : Activity() {

    private lateinit var binding: ActivityDisplaySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        ModuleManager.init(this)
        binding = ActivityDisplaySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        applyColors()
        updateStatus()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        binding.itemFontSize.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            val levels = FontSizeManager.getLevels()
            val names = levels.map { it.first }.toTypedArray()
            val currentIndex = levels.indexOfFirst { it.second == FontSizeManager.getScale() }
            AlertDialog.Builder(this)
                .setTitle("选择字体大小")
                .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                    val selected = levels[which]
                    FontSizeManager.setScale(selected.second)
                    updateStatus()
                    dialog.dismiss()
                    Toast.makeText(this, "字体大小已设为：${selected.first}，重启应用后生效", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 显示模块1
        binding.itemModule1.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            showModulePicker(1)
        }

        // 显示模块2
        binding.itemModule2.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            showModulePicker(2)
        }
    }

    private fun showModulePicker(moduleNum: Int) {
        val modules = ModuleManager.allModules
        val names = modules.map { it.getDisplayName() }.toTypedArray()
        val currentIndex = if (moduleNum == 1) ModuleManager.getModule1Index() else ModuleManager.getModule2Index()

        AlertDialog.Builder(this)
            .setTitle("选择显示模块$moduleNum")
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selectedModule = modules[which]
                val success = if (moduleNum == 1) {
                    ModuleManager.setModule1(selectedModule.getId())
                } else {
                    ModuleManager.setModule2(selectedModule.getId())
                }

                if (success) {
                    updateStatus()
                    dialog.dismiss()
                    Toast.makeText(this, "显示模块$moduleNum 已设为：${selectedModule.getDisplayName()}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "不能与另一个模块选择相同内容（空白模块除外）", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        applyColors()
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)

        val items = listOf(binding.itemFontSize, binding.itemModule1, binding.itemModule2)
        items.forEach { item ->
            (item.getChildAt(0) as? android.widget.TextView)?.setTextColor(ColorManager.getScreenText())
        }
        binding.tvFontSizeStatus.setTextColor(ColorManager.getScreenText())
        binding.tvModule1Status.setTextColor(ColorManager.getScreenText())
        binding.tvModule2Status.setTextColor(ColorManager.getScreenText())
    }

    private fun updateStatus() {
        binding.tvFontSizeStatus.text = FontSizeManager.getCurrentLevelName()
        binding.tvModule1Status.text = ModuleManager.getModule1().getDisplayName()
        binding.tvModule2Status.text = ModuleManager.getModule2().getDisplayName()
    }
}
