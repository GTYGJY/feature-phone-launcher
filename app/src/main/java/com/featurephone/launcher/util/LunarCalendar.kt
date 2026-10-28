package com.featurephone.launcher.util

import java.util.Calendar

/**
 * 农历转换工具类
 * 支持1900-2100年的公历转农历
 */
object LunarCalendar {

    // 农历数据：1900-2100年，每年的数据用16位整数表示
    // 前4位：闰月月份（0表示无闰月）
    // 后12位：每个月的大小（1表示大月30天，0表示小月29天）
    private val lunarInfo = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0
    )

    private val lunarMonths = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val lunarDays = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )
    private val heavenlyStems = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val earthlyBranches = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val zodiacs = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

    /**
     * 获取指定公历日期的农历字符串
     * 格式：农历X月XX（如：农历七月十五）
     */
    fun getLunarDate(year: Int, month: Int, day: Int): String {
        return try {
            var offset = daysBetween(1900, 1, 31, year, month, day)
            if (offset < 0) return "农历 --"
            var lunarYear = 1900
            var temp = 0
            // 【修改】lunarInfo 只覆盖 1900~2039（共 140 项），原来循环到 2100 会数组越界，
            // 导致 2040 年以后农历一直显示“农历错误”。这里按数据表实际范围循环并做越界保护。
            val maxYear = 1900 + lunarInfo.size - 1
            for (i in 1900..maxYear) {
                temp = daysInLunarYear(i)
                if (offset < temp) break
                offset -= temp
                lunarYear++
                if (i == maxYear) return "农历 --"
            }
            if (lunarYear > maxYear) return "农历 --"

            val leap = leapMonth(lunarYear)
            var isLeap = false
            var lunarMonth = 1
            val maxIter = if (leap > 0) 13 else 12
            for (i in 1..maxIter) {
                val isLeapMonth = leap > 0 && i == leap + 1 && isLeap
                val days = if (isLeapMonth) {
                    leapDays(lunarYear)
                } else {
                    val m = if (leap > 0 && i > leap) i - 1 else i
                    monthDays(lunarYear, m)
                }
                if (offset < days) break
                offset -= days
                if (leap > 0 && i == leap && !isLeap) {
                    isLeap = true
                    lunarMonth--
                }
                if (isLeapMonth) {
                    isLeap = false
                }
                lunarMonth++
            }

            val lunarDay = offset + 1
            // 边界保护
            if (lunarMonth < 1 || lunarMonth > 12) return "农历 --"
            if (lunarDay < 1 || lunarDay > 30) return "农历 --"
            val monthStr = if (isLeap) "闰${lunarMonths[lunarMonth - 1]}" else lunarMonths[lunarMonth - 1]
            "农历${monthStr}月${lunarDays[lunarDay - 1]}"
        } catch (e: Exception) {
            "农历错误"
        }
    }

    /**
     * 获取当前日期的农历字符串
     */
    fun getCurrentLunarDate(): String {
        val cal = Calendar.getInstance()
        return getLunarDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * 获取生肖
     */
    fun getZodiac(year: Int): String {
        return zodiacs[(year - 4) % 12]
    }

    /**
     * 获取天干地支纪年
     */
    fun getGanZhiYear(year: Int): String {
        val stem = (year - 4) % 10
        val branch = (year - 4) % 12
        return "${heavenlyStems[stem]}${earthlyBranches[branch]}"
    }

    private fun leapMonth(year: Int): Int {
        return lunarInfo[year - 1900] and 0xf
    }

    private fun leapDays(year: Int): Int {
        return if (leapMonth(year) != 0) {
            if (lunarInfo[year - 1900] and 0x10000 != 0) 30 else 29
        } else 0
    }

    private fun monthDays(year: Int, month: Int): Int {
        return if (lunarInfo[year - 1900] and (0x10000 shr month) != 0) 30 else 29
    }

    private fun daysInLunarYear(year: Int): Int {
        var sum = 348
        var bit = 0x8000
        while (bit >= 0x10) {
            if (lunarInfo[year - 1900] and bit != 0) sum++
            bit = bit ushr 1
        }
        return sum + leapDays(year)
    }

    private fun daysBetween(y1: Int, m1: Int, d1: Int, y2: Int, m2: Int, d2: Int): Int {
        // 【修改】改用 UTC 日历计算天数差，避免夏令时/时区导致少算或多算一天
        val cal1 = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y1, m1 - 1, d1)
        }
        val cal2 = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y2, m2 - 1, d2)
        }
        val diff = cal2.timeInMillis - cal1.timeInMillis
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}
