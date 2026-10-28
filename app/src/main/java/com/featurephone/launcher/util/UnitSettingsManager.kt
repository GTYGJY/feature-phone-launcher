package com.featurephone.launcher.util

import android.content.Context

/**
 * 单位设置管理：温度单位、日期格式
 */
object UnitSettingsManager {

    private const val PREFS_NAME = "unit_settings"
    private const val KEY_TEMP_UNIT = "temp_unit"
    private const val KEY_DATE_FORMAT = "date_format"

    // 温度单位
    const val TEMP_CELSIUS = "celsius"
    const val TEMP_FAHRENHEIT = "fahrenheit"

    // 日期格式
    const val DATE_YMD = "ymd" // 年月日
    const val DATE_DMY = "dmy" // 日月年

    fun setTempUnit(context: Context, unit: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TEMP_UNIT, unit).apply()
    }

    fun getTempUnit(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TEMP_UNIT, TEMP_CELSIUS) ?: TEMP_CELSIUS
    }

    fun setDateFormat(context: Context, format: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DATE_FORMAT, format).apply()
    }

    fun getDateFormat(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DATE_FORMAT, DATE_YMD) ?: DATE_YMD
    }

    /**
     * 摄氏度转华氏度
     */
    fun celsiusToFahrenheit(celsius: Int): Int {
        return (celsius * 9 / 5 + 32)
    }

    /**
     * 获取温度显示文本
     */
    fun formatTemperature(context: Context, celsius: Int): String {
        return when (getTempUnit(context)) {
            TEMP_FAHRENHEIT -> "${celsiusToFahrenheit(celsius)}°F"
            else -> "${celsius}°C"
        }
    }
}
