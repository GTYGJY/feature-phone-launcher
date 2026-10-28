package com.featurephone.launcher.util

import android.content.Context
import android.content.SharedPreferences
import com.featurephone.launcher.R

/**
 * 字体大小管理器
 * 三档：标准(1.0x)、大(1.3x)、巨大(1.5x)
 */
object FontSizeManager {
    private const val PREFS_NAME = "font_size_prefs"
    private const val KEY_FONT_SCALE = "font_scale"

    const val SCALE_STANDARD = 1.0f
    const val SCALE_LARGE = 1.3f
    const val SCALE_HUGE = 1.5f

    private lateinit var prefs: SharedPreferences
    private var currentScale: Float = SCALE_STANDARD

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentScale = prefs.getFloat(KEY_FONT_SCALE, SCALE_STANDARD)
    }

    fun getScale(): Float = currentScale

    fun setScale(scale: Float) {
        currentScale = scale
        prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }

    fun getCurrentLevelName(): String = when (currentScale) {
        SCALE_LARGE -> "大"
        SCALE_HUGE -> "巨大"
        else -> "标准"
    }

    fun getScaledSize(baseSize: Float): Float = baseSize * currentScale

    fun getLevels(): List<Pair<String, Float>> = listOf(
        "标准" to SCALE_STANDARD,
        "大" to SCALE_LARGE,
        "巨大" to SCALE_HUGE
    )

    /**
     * 应用字体大小到View树中的所有TextView
     * 使用应用自身资源ID作为tag key，避免IllegalArgumentException
     */
    fun applyFontSizeToView(view: android.view.View, scale: Float = currentScale) {
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyFontSizeToView(view.getChildAt(i), scale)
            }
        } else if (view is android.widget.TextView) {
            val originalSize = view.getTag(R.id.font_size_original) as? Float
            // 【修改】textSize 的单位是 px，换算成 sp 要用 scaledDensity（density 不含系统字体缩放）
            val baseSize = originalSize
                ?: (view.textSize / view.resources.displayMetrics.scaledDensity)
            if (originalSize == null) {
                view.setTag(R.id.font_size_original, baseSize)
            }
            view.textSize = baseSize * scale
        }
    }

    /**
     * 【新增】列表行专用缩放，供各 RecyclerView 适配器在 onBindViewHolder 里调用。
     *
     * 原因：`applyFontSizeToView(binding.root)` 只在 Activity onCreate 时执行一次，
     * 而 RecyclerView 的每一行是之后由适配器 inflate 出来的，这时早就过了那次调用，
     * 所以"字体大小"设置对通讯录 / 通话记录 / 信息 / 软件列表 / 拨号 T9 列表统统不生效。
     *
     * 这里把行内文字、行最小高度、行内边距一起按当前比例放大，让"每一条"整体变大。
     * 原始值用 tag 记录，列表复用（recycle）时重复绑定也不会累加。
     */
    fun applyToListItem(itemView: android.view.View) {
        applyFontSizeToView(itemView, currentScale)

        // 1) 行高：按原始最小高度等比放大
        val originalMinHeight = itemView.getTag(R.id.font_size_original_min_height) as? Int
        val baseMinHeight = originalMinHeight ?: itemView.minimumHeight
        if (originalMinHeight == null && baseMinHeight > 0) {
            itemView.setTag(R.id.font_size_original_min_height, baseMinHeight)
        }
        if (baseMinHeight > 0) {
            itemView.minimumHeight = (baseMinHeight * currentScale).toInt()
        }

        // 2) 内边距：按原始内边距等比放大，让行更宽松、更好点按
        val originalPadding = itemView.getTag(R.id.font_size_original_padding) as? IntArray
        val basePadding = originalPadding ?: intArrayOf(
            itemView.paddingLeft, itemView.paddingTop,
            itemView.paddingRight, itemView.paddingBottom
        ).also { padding ->
            if (padding.any { it > 0 }) {
                itemView.setTag(R.id.font_size_original_padding, padding)
            }
        }
        if (basePadding.any { it > 0 }) {
            itemView.setPadding(
                (basePadding[0] * currentScale).toInt(),
                (basePadding[1] * currentScale).toInt(),
                (basePadding[2] * currentScale).toInt(),
                (basePadding[3] * currentScale).toInt()
            )
        }
    }
}
