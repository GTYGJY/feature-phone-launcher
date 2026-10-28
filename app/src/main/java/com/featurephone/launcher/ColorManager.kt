package com.featurephone.launcher

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

/**
 * 颜色管理器
 * 支持运行时自定义：
 * - 上半部分屏幕背景色（默认白色）
 * - 上半部分屏幕文字色（默认黑色）
 * - 下半部分按键区背景色（默认黑色）
 * - 下半部分按键底色（默认深灰）
 * - 下半部分按键文字色（默认白色）
 * 固定不可改：
 * - 拨打键 = 绿色
 * - 挂断键 = 红色
 */
object ColorManager {

    private const val PREFS_NAME = "color_settings"

    // 默认颜色
    private val DEFAULT_SCREEN_BG = Color.parseColor("#F5F5DC") // 米白
    private const val DEFAULT_SCREEN_TEXT = Color.BLACK
    private const val DEFAULT_SCREEN_TEXT_DIM = Color.GRAY
    private const val DEFAULT_KEYS_BG = Color.BLACK
    private const val DEFAULT_KEY_BG = Color.DKGRAY
    private const val DEFAULT_KEY_TEXT = Color.WHITE

    // 固定颜色
    val CALL_BG = Color.parseColor("#2E7D32")
    val CALL_TEXT = Color.WHITE
    val END_BG = Color.parseColor("#C62828")
    val END_TEXT = Color.WHITE

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getScreenBg(): Int = prefs.getInt("screen_bg", DEFAULT_SCREEN_BG)
    fun setScreenBg(color: Int) = prefs.edit().putInt("screen_bg", color).apply()

    fun getScreenText(): Int = prefs.getInt("screen_text", DEFAULT_SCREEN_TEXT)
    fun setScreenText(color: Int) = prefs.edit().putInt("screen_text", color).apply()

    fun getScreenTextDim(): Int = prefs.getInt("screen_text_dim", DEFAULT_SCREEN_TEXT_DIM)
    fun setScreenTextDim(color: Int) = prefs.edit().putInt("screen_text_dim", color).apply()

    fun getKeysBg(): Int = prefs.getInt("keys_bg", DEFAULT_KEYS_BG)
    fun setKeysBg(color: Int) = prefs.edit().putInt("keys_bg", color).apply()

    fun getKeyBg(): Int = prefs.getInt("key_bg", DEFAULT_KEY_BG)
    fun setKeyBg(color: Int) = prefs.edit().putInt("key_bg", color).apply()

    fun getKeyText(): Int = prefs.getInt("key_text", DEFAULT_KEY_TEXT)
    fun setKeyText(color: Int) = prefs.edit().putInt("key_text", color).apply()

    fun resetDefaults() {
        prefs.edit().clear().apply()
    }

    /**
     * 将颜色应用到主界面所有View
     */
    fun applyToMainActivity(
        screenBg: android.view.View,
        keysBg: android.view.View,
        screenTextViews: Array<android.widget.TextView>,
        screenTextDimViews: Array<android.widget.TextView>,
        keyBgViews: Array<android.view.View>,
        keyTextViews: Array<android.widget.TextView>,
        callBtn: android.widget.Button? = null,
        endBtn: android.widget.Button? = null
    ) {
        screenBg.setBackgroundColor(getScreenBg())
        keysBg.setBackgroundColor(getKeysBg())

        screenTextViews.forEach { it.setTextColor(getScreenText()) }
        screenTextDimViews.forEach { it.setTextColor(getScreenTextDim()) }
        keyBgViews.forEach { it.setBackgroundColor(getKeyBg()) }
        keyTextViews.forEach { it.setTextColor(getKeyText()) }

        callBtn?.apply {
            setBackgroundColor(CALL_BG)
            setTextColor(CALL_TEXT)
        }
        endBtn?.apply {
            setBackgroundColor(END_BG)
            setTextColor(END_TEXT)
        }
    }
}
