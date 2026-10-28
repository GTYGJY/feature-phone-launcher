package com.featurephone.launcher.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * 应用内语言切换管理
 */
object LocaleManager {

    private const val PREFS_NAME = "locale_settings"
    private const val KEY_LANGUAGE = "language"

    const val LANGUAGE_ZH = "zh"
    const val LANGUAGE_EN = "en"

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANGUAGE_ZH) ?: LANGUAGE_ZH
    }

    fun attachBaseContext(context: Context): Context {
        val language = getLanguage(context)
        val locale = Locale(language)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
