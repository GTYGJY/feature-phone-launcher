package com.featurephone.launcher.util

import android.app.Activity
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import com.featurephone.launcher.R

/**
 * 页面过渡动画工具类
 * 统一管理所有页面跳转的淡入淡出效果
 */
object AnimationHelper {

    /** 【新增】所有界面切换（Activity 跳转 + 应用内界面切换）统一使用 500ms */
    const val TRANSITION_DURATION_MS = 500L

    /**
     * 启动新Activity时的过渡动画（淡入）
     */
    fun overrideStartTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    /**
     * 关闭Activity时的过渡动画（淡出）
     */
    fun overrideFinishTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    /**
     * 【新增】应用内界面切换（待机 / 菜单 / 拨号）的统一过渡动画。
     *
     * 用法：在真正修改各控件 visibility **之前** 调用它，之后再切换可见性，
     * TransitionManager 会自动捕捉前后两个场景并做 500ms 的淡入淡出 + 位置变化动画。
     *
     * 之前的实现是直接 setVisibility()，界面是"瞬切"的，和 Activity 跳转的 500ms 淡入淡出一致性很差。
     */
    fun animateScreenChange(sceneRoot: View?) {
        val root = sceneRoot as? ViewGroup ?: return
        // 尚未完成首次布局时（例如 onCreate 里初始化界面）不做动画，避免出现无意义的闪动
        if (!root.isLaidOut) return
        try {
            val transition = TransitionSet().apply {
                addTransition(Fade().apply {
                    duration = TRANSITION_DURATION_MS
                })
                addTransition(ChangeBounds().apply {
                    duration = TRANSITION_DURATION_MS
                })
            }
            TransitionManager.beginDelayedTransition(root, transition)
        } catch (_: Exception) {
            // 动画失败不影响功能
        }
    }
}
