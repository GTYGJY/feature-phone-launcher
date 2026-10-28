package com.featurephone.launcher.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat

/**
 * 电量模块
 * 显示当前电量百分比，大字号
 * 实时监听电量变化
 */
class BatteryModule : InfoModule {

    private var batteryReceiver: BroadcastReceiver? = null
    private var currentLevel = -1

    override fun getId(): String = "battery"

    override fun getDisplayName(): String = "电量"

    override fun getContent(context: Context): String {
        if (currentLevel < 0) {
            currentLevel = getCurrentBattery(context)
        }
        return "$currentLevel%"
    }

    override fun getContentSize(): Float = 26f

    override fun needsLiveUpdate(): Boolean = true

    override fun startListening(context: Context, onUpdate: () -> Unit) {
        // 【修改】先用局部变量持有接收器，再赋给字段并注册：
        // 字段是可空 var，直接在 registerReceiver 里传字段会因无法智能转换而编译失败。
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (level >= 0 && scale > 0) {
                    currentLevel = (level * 100 / scale)
                    onUpdate()
                }
            }
        }
        batteryReceiver = receiver
        // 【修改】targetSdk 34 下动态注册广播需要显式声明导出标记；
        // ACTION_BATTERY_CHANGED 是系统广播，用 RECEIVER_NOT_EXPORTED 既满足要求也不会漏收。
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        currentLevel = getCurrentBattery(context)
    }

    override fun stopListening(context: Context) {
        batteryReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
        }
        batteryReceiver = null
    }

    private fun getCurrentBattery(context: Context): Int {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (level >= 0 && scale > 0) level * 100 / scale else 100
        } catch (_: Exception) {
            100
        }
    }
}
