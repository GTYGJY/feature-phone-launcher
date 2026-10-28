package com.featurephone.launcher.apps

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.util.VibrationHelper

/**
 * 基础Activity，统一所有二级页面的构建逻辑
 * 子类需要：
 * 1. 在onCreate中调用super.onCreate(savedInstanceState)
 * 2. 调用setContentView设置布局
 * 3. 调用setupCommonViews初始化公共控件
 * 4. 实现applyColors方法设置颜色
 */
abstract class BaseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
    }

    override fun onResume() {
        super.onResume()
        applyColors()
    }

    /**
     * 初始化公共控件：返回键震动、字体大小
     * @param root 根View，用于应用字体大小
     * @param backButton 返回键按钮，null表示不需要返回键
     * @param screenArea 屏幕区域，用于设置背景色（可选）
     * @param keysArea 按键区域，用于设置背景色（可选）
     * @param titleView 标题View，用于设置文字色（可选）
     */
    protected fun setupCommonViews(
        root: View,
        backButton: Button? = null,
        screenArea: View? = null,
        keysArea: View? = null,
        titleView: TextView? = null
    ) {
        // 应用字体大小
        FontSizeManager.applyFontSizeToView(root)

        // 返回键震动
        backButton?.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }
    }

    /**
     * 应用颜色，子类必须实现
     */
    abstract fun applyColors()

    /**
     * 统一的基础颜色设置
     */
    protected fun applyBaseColors(
        screenArea: View? = null,
        keysArea: View? = null,
        titleView: TextView? = null,
        backButton: Button? = null
    ) {
        screenArea?.setBackgroundColor(ColorManager.getScreenBg())
        keysArea?.setBackgroundColor(ColorManager.getKeysBg())
        titleView?.setTextColor(ColorManager.getScreenText())
        backButton?.setBackgroundColor(ColorManager.END_BG)
        backButton?.setTextColor(ColorManager.END_TEXT)
    }

    /**
     * 带震动的点击事件，所有可点击项统一使用此方法
     */
    protected fun View.onClickWithVibration(action: () -> Unit) {
        setOnClickListener {
            VibrationHelper.vibrateKeyPress(this@BaseActivity)
            action()
        }
    }

    /**
     * 遍历设置所有TextView的文字颜色（用于纯文本页面，如解释页面）
     */
    protected fun applyTextColorToAllChildren(container: View, color: Int) {
        if (container is android.view.ViewGroup) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is TextView) {
                    child.setTextColor(color)
                } else if (child is android.view.ViewGroup) {
                    applyTextColorToAllChildren(child, color)
                }
            }
        }
    }
}
