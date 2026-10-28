package com.featurephone.launcher.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 界面模式管理器
 * 支持标准模式和极简模式
 */
object ModeManager {

    private const val PREFS_NAME = "mode_settings"
    private const val KEY_MODE = "ui_mode"

    const val MODE_STANDARD = "standard"
    const val MODE_SIMPLE = "simple"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取当前模式，默认为标准模式
     */
    fun getMode(): String {
        return prefs.getString(KEY_MODE, MODE_STANDARD) ?: MODE_STANDARD
    }

    /**
     * 设置模式
     */
    fun setMode(mode: String) {
        prefs.edit().putString(KEY_MODE, mode).apply()
    }

    /**
     * 是否为极简模式
     */
    fun isSimpleMode(): Boolean {
        return getMode() == MODE_SIMPLE
    }

    /**
     * 切换模式
     */
    fun toggleMode(): String {
        val newMode = if (isSimpleMode()) MODE_STANDARD else MODE_SIMPLE
        setMode(newMode)
        return newMode
    }
}
