package com.featurephone.launcher.apps

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Toast
import com.featurephone.launcher.R
import com.featurephone.launcher.databinding.ActivityColorSettingsBinding
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.FontSizeManager

class ColorSettingsActivity : Activity() {

    private lateinit var binding: ActivityColorSettingsBinding

    private val screenTextColors = listOf(
        "黑色" to Color.BLACK, "白色" to Color.WHITE, "红色" to Color.RED,
        "米白" to Color.parseColor("#F5F5DC")
    )
    private val screenBgColors = listOf(
        "白色" to Color.WHITE, "黑色" to Color.BLACK,
        "米白" to Color.parseColor("#F5F5DC"),
        "浅绿" to Color.parseColor("#C8E6C9"),
        "浅黑" to Color.parseColor("#212121")
    )
    private val keyTextColors = listOf(
        "黑色" to Color.BLACK, "白色" to Color.WHITE, "黄色" to Color.YELLOW, "绿色" to Color.GREEN
    )
    private val keyBgColors = listOf(
        "白色" to Color.WHITE, "黑色" to Color.BLACK, "深灰" to Color.DKGRAY,
        "深蓝" to Color.parseColor("#0d47a1")
    )

    // 屏幕颜色集合：白系和黑系，同集合搭配会看不见
    private val screenWhiteSet = setOf(
        Color.WHITE,
        Color.parseColor("#F5F5DC") // 米白
    )
    private val screenBlackSet = setOf(
        Color.BLACK,
        Color.parseColor("#212121") // 浅黑
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityColorSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        applyColors()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        binding.itemScreenText.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            showColorPicker("选择屏幕文字色", screenTextColors, ColorManager.getScreenBg(), useScreenRule = true) { color ->
                ColorManager.setScreenText(color)
                val dim = adjustColorBrightness(color, 0.6f)
                ColorManager.setScreenTextDim(dim)
                applyColors()
            }
        }

        binding.itemScreenBg.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            showColorPicker("选择屏幕背景色", screenBgColors, ColorManager.getScreenText(), useScreenRule = true) { color ->
                ColorManager.setScreenBg(color)
                applyColors()
            }
        }

        binding.itemKeyText.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            showColorPicker("选择按键文字色", keyTextColors, ColorManager.getKeyBg(), useScreenRule = false) { color ->
                ColorManager.setKeyText(color)
                applyColors()
            }
        }

        binding.itemKeyBg.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            showColorPicker("选择按键背景色", keyBgColors, ColorManager.getKeyText(), useScreenRule = false) { color ->
                ColorManager.setKeyBg(color)
                applyColors()
            }
        }

        // 快捷黑底白字（仅作用于屏幕）
        binding.itemQuickBlackBg.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            ColorManager.setScreenBg(Color.BLACK)
            ColorManager.setScreenText(Color.WHITE)
            val dim = adjustColorBrightness(Color.WHITE, 0.6f)
            ColorManager.setScreenTextDim(dim)
            applyColors()
            Toast.makeText(this, "已切换为黑底白字", Toast.LENGTH_SHORT).show()
        }

        // 快捷白底黑字（仅作用于屏幕）
        binding.itemQuickWhiteBg.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            ColorManager.setScreenBg(Color.WHITE)
            ColorManager.setScreenText(Color.BLACK)
            val dim = adjustColorBrightness(Color.BLACK, 0.6f)
            ColorManager.setScreenTextDim(dim)
            applyColors()
            Toast.makeText(this, "已切换为白底黑字", Toast.LENGTH_SHORT).show()
        }

        binding.itemResetColors.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            AlertDialog.Builder(this)
                .setTitle("恢复默认颜色")
                .setMessage("确定要恢复默认配色吗？")
                .setPositiveButton("确定") { _, _ ->
                    ColorManager.resetDefaults()
                    applyColors()
                    Toast.makeText(this, "已恢复默认颜色", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }
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

        // 更新预览和状态
        updateColorPreview(binding.viewScreenTextPreview, ColorManager.getScreenText(), ColorManager.getScreenBg(), isScreen = true)
        updateColorPreview(binding.viewScreenBgPreview, ColorManager.getScreenBg(), ColorManager.getScreenText(), isScreen = true)
        updateColorPreview(binding.viewKeyTextPreview, ColorManager.getKeyText(), ColorManager.getKeyBg())
        updateColorPreview(binding.viewKeyBgPreview, ColorManager.getKeyBg(), ColorManager.getKeyText())

        binding.tvScreenTextStatus.text = getColorName(screenTextColors, ColorManager.getScreenText())
        binding.tvScreenBgStatus.text = getColorName(screenBgColors, ColorManager.getScreenBg())
        binding.tvKeyTextStatus.text = getColorName(keyTextColors, ColorManager.getKeyText())
        binding.tvKeyBgStatus.text = getColorName(keyBgColors, ColorManager.getKeyBg())

        // 设置项文字颜色
        val textViews = listOf(
            binding.itemScreenText.getChildAt(0) as android.widget.TextView,
            binding.itemScreenBg.getChildAt(0) as android.widget.TextView,
            binding.itemKeyText.getChildAt(0) as android.widget.TextView,
            binding.itemKeyBg.getChildAt(0) as android.widget.TextView,
            binding.itemQuickBlackBg.getChildAt(0) as android.widget.TextView,
            binding.itemQuickWhiteBg.getChildAt(0) as android.widget.TextView,
            binding.itemResetColors.getChildAt(0) as android.widget.TextView
        )
        textViews.forEach { it.setTextColor(ColorManager.getScreenText()) }
        binding.tvScreenTextStatus.setTextColor(ColorManager.getScreenText())
        binding.tvScreenBgStatus.setTextColor(ColorManager.getScreenText())
        binding.tvKeyTextStatus.setTextColor(ColorManager.getScreenText())
        binding.tvKeyBgStatus.setTextColor(ColorManager.getScreenText())
    }

    private fun updateColorPreview(view: android.view.View, color: Int, bgColor: Int, isScreen: Boolean = false) {
        // 屏幕颜色：仅黑集合用白框，其余一律黑框
        // 按键颜色：浅色黑框，深色白框
        val borderColor = if (isScreen) {
            if (color in screenBlackSet) Color.WHITE else Color.BLACK
        } else {
            if (isColorLight(color)) Color.BLACK else Color.WHITE
        }
        val strokeWidth = (1 * resources.displayMetrics.density).toInt()
        val drawable = GradientDrawable().apply {
            setColor(color)
            setStroke(strokeWidth, borderColor)
        }
        view.background = drawable
    }

    private fun getColorName(colors: List<Pair<String, Int>>, color: Int): String {
        return colors.find { it.second == color }?.first ?: "自定义"
    }

    private fun showColorPicker(
        title: String,
        colors: List<Pair<String, Int>>,
        conflictColor: Int? = null,
        useScreenRule: Boolean = false,
        onSelected: (Int) -> Unit
    ) {
        val names = colors.map { (name, color) ->
            if (conflictColor != null && isColorConflict(color, conflictColor, useScreenRule)) {
                "$name（冲突，禁止选择）"
            } else {
                name
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(names) { _, which ->
                val selectedColor = colors[which].second
                if (conflictColor != null && isColorConflict(selectedColor, conflictColor, useScreenRule)) {
                    Toast.makeText(this, "这是容易看不见的冲突设置，已经禁止该操作", Toast.LENGTH_LONG).show()
                    return@setItems
                }
                onSelected(selectedColor)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun isColorLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return luminance > 0.5
    }

    private fun isColorConflict(color1: Int, color2: Int, useScreenRule: Boolean): Boolean {
        return if (useScreenRule) {
            // 屏幕颜色：基于白系/黑系集合判断
            val c1White = color1 in screenWhiteSet
            val c1Black = color1 in screenBlackSet
            val c2White = color2 in screenWhiteSet
            val c2Black = color2 in screenBlackSet
            (c1White && c2White) || (c1Black && c2Black)
        } else {
            // 按键颜色：仅纯黑+纯黑、纯白+纯白冲突
            (color1 == Color.BLACK && color2 == Color.BLACK) ||
                    (color1 == Color.WHITE && color2 == Color.WHITE)
        }
    }

    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
