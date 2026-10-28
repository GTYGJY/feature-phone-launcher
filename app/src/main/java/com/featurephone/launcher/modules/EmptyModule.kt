package com.featurephone.launcher.modules

import android.content.Context

/**
 * 空白模块
 * 没有实际功能，显示空白内容
 * 用于占位，保持界面美观（有按键底色但无内容）
 */
class EmptyModule : InfoModule {

    override fun getId(): String = "empty"

    override fun getDisplayName(): String = "空白"

    override fun getContent(context: Context): String = ""

    override fun getContentSize(): Float = 16f

    override fun needsLiveUpdate(): Boolean = false
}
