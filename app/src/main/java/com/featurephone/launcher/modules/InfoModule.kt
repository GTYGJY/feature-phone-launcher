package com.featurephone.launcher.modules

import android.content.Context

/**
 * 信息模块接口
 * 用于主界面中间区域的可切换显示内容
 * 以后添加新模块只需实现此接口并在ModuleManager中注册
 */
interface InfoModule {

    /** 模块唯一标识 */
    fun getId(): String

    /** 模块显示名称（设置界面中展示） */
    fun getDisplayName(): String

    /** 获取当前显示内容 */
    fun getContent(context: Context): String

    /** 获取副标题（如天气模块的城市名），返回null则不显示 */
    fun getSubTitle(context: Context): String? = null

    /** 内容字号（sp） */
    fun getContentSize(): Float = 22f

    /** 模块是否需要持续更新（如电量需要监听广播） */
    fun needsLiveUpdate(): Boolean = false

    /** 开始监听更新（仅在needsLiveUpdate=true时调用） */
    fun startListening(context: Context, onUpdate: () -> Unit) {}

    /** 停止监听更新 */
    fun stopListening(context: Context) {}
}
