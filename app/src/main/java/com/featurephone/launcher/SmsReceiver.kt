package com.featurephone.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * 短信接收器
 * 接收短信并在功能机界面显示通知
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        /** 【新增】内部广播 action 与签名级权限，供 SmsReceiver 与 MainActivity 共用 */
        const val ACTION_SMS_RECEIVED = "com.featurephone.launcher.SMS_RECEIVED"
        const val PERMISSION_INTERNAL = "com.featurephone.launcher.permission.INTERNAL_BROADCAST"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }

        // 发送广播给主界面显示
        // 【修改】用签名级自定义权限发送：只有同签名的应用（也就是自己）能收到，
        // 同时 setPackage 限定包名，双重防止短信内容外泄。
        try {
            val notifyIntent = Intent(ACTION_SMS_RECEIVED)
            notifyIntent.setPackage(context.packageName)
            notifyIntent.putExtra("sender", sender)
            notifyIntent.putExtra("body", body)
            context.sendBroadcast(notifyIntent, PERMISSION_INTERNAL)
        } catch (_: Exception) {
            // 广播失败不影响振动提醒
        }

        // 振动提醒
        // 【修改】先判空，且 Android 12+ 走 VibratorManager，避免个别设备上 NPE 中断处理
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as? android.os.VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        android.os.VibrationEffect.createOneShot(
                            500,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (_: Exception) {
            // 忽略振动失败
        }
    }
}
