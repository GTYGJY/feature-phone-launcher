package com.featurephone.launcher.util

/**
 * T9智能拨号匹配工具
 * 支持号码包含匹配和姓名拼音首字母匹配
 * 使用GB2312编码范围判断拼音首字母（GB2312汉字按拼音排序）
 */
object T9Matcher {

    // T9按键映射
    private val T9_MAP = mapOf(
        '2' to "ABC", '3' to "DEF", '4' to "GHI",
        '5' to "JKL", '6' to "MNO", '7' to "PQRS",
        '8' to "TUV", '9' to "WXYZ", '0' to " +",
        '1' to ""
    )

    // GB2312拼音首字母编码范围（高位字节，低位字节辅助）
    // GB2312汉字区从0xB0A1开始，按拼音排序
    private data class PinyinRange(val initial: Char, val start: Int, val end: Int)

    // GB2312高位字节范围（每个字母对应的起始高位字节）
    // 参考GB2312编码表，按拼音首字母分段
    private val PINYIN_RANGES = listOf(
        PinyinRange('A', 0xB0A1, 0xB0C4),
        PinyinRange('B', 0xB0C5, 0xB2C0),
        PinyinRange('C', 0xB2C1, 0xB4ED),
        PinyinRange('D', 0xB4EE, 0xB6E9),
        PinyinRange('E', 0xB6EA, 0xB7A1),
        PinyinRange('F', 0xB7A2, 0xB8C0),
        PinyinRange('G', 0xB8C1, 0xB9FD),
        PinyinRange('H', 0xB9FE, 0xBBF6),
        PinyinRange('J', 0xBBF7, 0xBFA5),
        PinyinRange('K', 0xBFA6, 0xC0AB),
        PinyinRange('L', 0xC0AC, 0xC2E7),
        PinyinRange('M', 0xC2E8, 0xC4C2),
        PinyinRange('N', 0xC4C3, 0xC5B5),
        PinyinRange('O', 0xC5B6, 0xC5BD),
        PinyinRange('P', 0xC5BE, 0xC6D9),
        PinyinRange('Q', 0xC6DA, 0xC8BA),
        PinyinRange('R', 0xC8BB, 0xC8F5),
        PinyinRange('S', 0xC8F6, 0xCBF9),
        PinyinRange('T', 0xCBFA, 0xCDD9),
        PinyinRange('W', 0xCDDA, 0xCEF3),
        PinyinRange('X', 0xCEF4, 0xD1B8),
        PinyinRange('Y', 0xD1B9, 0xD4D0),
        PinyinRange('Z', 0xD4D1, 0xD7F9)
    )

    data class ContactMatch(
        val name: String,
        val number: String,
        val matchType: MatchType
    )

    enum class MatchType {
        NUMBER, // 号码匹配
        NAME    // 姓名拼音匹配
    }

    /**
     * 检查联系人是否匹配输入的数字
     */
    fun match(input: String, name: String, number: String): ContactMatch? {
        if (input.isEmpty()) return null

        // 1. 号码包含匹配
        val cleanNumber = number.replace(Regex("[^0-9]"), "")
        if (cleanNumber.contains(input)) {
            return ContactMatch(name, number, MatchType.NUMBER)
        }

        // 2. 姓名拼音首字母匹配
        val initials = getNameInitials(name)
        if (initials.isNotEmpty() && initials.length >= input.length) {
            val inputLetters = digitsToLetters(input)
            if (inputLetters != null && matchesInitials(initials, inputLetters)) {
                return ContactMatch(name, number, MatchType.NAME)
            }
        }

        return null
    }

    /**
     * 获取姓名的拼音首字母序列
     */
    private fun getNameInitials(name: String): String {
        val sb = StringBuilder()
        for (c in name) {
            when {
                // 【修改】必须先判断汉字：Char.isLetter() 对汉字也返回 true，
                // 原来的顺序会让汉字永远走不到 GB2312 分支，导致拼音首字母匹配完全失效。
                c in '\u4e00'..'\u9fa5' -> sb.append(getChineseInitial(c))
                c.isLetter() -> sb.append(c.uppercaseChar())
                c.isDigit() -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * 获取单个汉字的拼音首字母
     * 使用GB2312编码范围判断
     */
    private fun getChineseInitial(c: Char): Char {
        return try {
            // 转换为GB2312编码
            val bytes = c.toString().toByteArray(charset("GB2312"))
            if (bytes.size >= 2) {
                val code = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
                for (range in PINYIN_RANGES) {
                    if (code in range.start..range.end) {
                        return range.initial
                    }
                }
            }
            '#'
        } catch (_: Exception) {
            '#'
        }
    }

    /**
     * 将数字转换为对应的字母集合
     */
    private fun digitsToLetters(digits: String): List<String>? {
        val result = mutableListOf<String>()
        for (c in digits) {
            val letters = T9_MAP[c] ?: return null
            result.add(letters)
        }
        return result
    }

    /**
     * 检查姓名首字母是否匹配输入的数字字母
     * 支持前缀匹配
     */
    private fun matchesInitials(initials: String, letterGroups: List<String>): Boolean {
        if (letterGroups.size > initials.length) return false
        for (i in letterGroups.indices) {
            if (initials[i] !in letterGroups[i]) return false
        }
        return true
    }
}
