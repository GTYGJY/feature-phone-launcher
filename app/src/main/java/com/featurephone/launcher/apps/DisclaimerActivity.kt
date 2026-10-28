package com.featurephone.launcher.apps

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.featurephone.launcher.R

class DisclaimerActivity : Activity() {

    private var countdown = 10
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var btnAgree: Button
    private val countdownRunnable = object : Runnable {
        override fun run() {
            countdown--
            if (countdown > 0) {
                btnAgree.text = "请阅读免责声明（${countdown}秒）"
                handler.postDelayed(this, 1000)
            } else {
                btnAgree.text = "同意并继续"
                btnAgree.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 用代码创建布局，避免布局文件兼容性问题
        val root = android.widget.LinearLayout(this)
        root.orientation = android.widget.LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        root.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        // dp转像素
        val density = resources.displayMetrics.density
        val buttonHeight = (64 * density).toInt()

        // 标题
        val title = TextView(this)
        title.text = "用户须知与免责声明"
        title.textSize = 20f
        title.setPadding(40, 30, 40, 20)
        title.setTextColor(Color.BLACK)
        root.addView(title)

        // 内容滚动区域
        val scrollView = ScrollView(this)
        scrollView.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        val contentLayout = android.widget.LinearLayout(this)
        contentLayout.orientation = android.widget.LinearLayout.VERTICAL
        contentLayout.setPadding(40, 0, 40, 20)

        val content = TextView(this)
        content.text = "欢迎使用功能机模式！\n\n在使用本软件前，请仔细阅读以下用户须知与免责声明：\n\n1. 本软件仅供学习和个人使用，不得用于任何商业用途。\n\n2. 本软件按现状提供，尽力保障功能稳定，但不保证在所有品牌、所有系统版本的手机上均完美运行，不对因使用本软件造成的任何直接或间接损失承担责任。\n\n3. 本软件不会收集、上传或泄露用户的任何个人信息、通讯录、通话记录、短信内容等隐私数据，不使用任何第三方统计或广告SDK。\n\n4. 本软件申请的电话、通讯录、短信、通话记录、位置等权限，仅用于对应功能的正常使用，不会用于收集个人信息或其他商业用途。\n\n5. 本软件的天气数据由 Open-Meteo 提供，采用 CC BY 4.0 许可证，数据准确性以接口提供方为准。\n\n6. 紧急电话（110、120、119等）功能依赖手机系统、SIM卡和运营商网络，请确保手机处于正常服务状态。拨打紧急电话时，系统会跳转到手机自带的拨号界面，需要再次点击拨打键才能完成呼叫。如遇紧急情况但软件无法正常使用，请直接使用手机系统拨号或其他方式求助。在其他应用中被广告或误操作跳转时，按手机Home键即可立即返回功能机模式。\n\n7. 如果从不明渠道获取本软件，软件开发者不承担软件可能被他人修改引发的不良后果。\n\n8. 通过本软件启动的其他应用程序，其功能、内容和行为由该应用的开发者负责，与本软件无关。\n\n9. 因手机硬件故障、电量耗尽、系统更新、运营商网络异常、信号不佳等非本软件原因导致的功能异常，本软件不承担责任。\n\n10. 用户应妥善保管手机，防止他人误操作或恶意使用。利用本软件从事任何违法违规活动，责任由用户自行承担。\n\n11. 本软件的Kiosk锁定模式、设备管理员、后台应用管理等功能需要通过ADB命令或系统高级设置开启，本软件并未对所有设备和系统版本进行完善测试。在非必要情况下不建议开启这些功能，本软件不对开启这些功能可能造成的严重后果负责。如遇软件卡死，请长按电源键强制重启手机。\n\n12. 本软件为免费软件，永久免费使用，不包含任何付费内容、广告或内购项目。若你以任意渠道付费获取该软件，请进行退款并维护自身权益。\n\n点击下方「同意并继续」即表示您已阅读并同意以上全部条款。"
        content.textSize = 14f
        content.setTextColor(Color.BLACK)
        content.setLineSpacing(6f, 1f)
        contentLayout.addView(content)
        scrollView.addView(contentLayout)
        root.addView(scrollView)

        // 同意按钮
        btnAgree = Button(this)
        btnAgree.text = "请阅读免责声明（10秒）"
        btnAgree.textSize = 17f
        btnAgree.isEnabled = false
        btnAgree.setBackgroundColor(Color.parseColor("#2E7D32"))
        btnAgree.setTextColor(Color.WHITE)
        btnAgree.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            buttonHeight
        )
        btnAgree.setOnClickListener {
            getSharedPreferences("feature_phone_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("disclaimer_agreed", true)
                .apply()
            startActivity(Intent(this, com.featurephone.launcher.MainActivity::class.java))
            finish()
        }
        root.addView(btnAgree)

        // 退出按钮
        val btnExit = Button(this)
        btnExit.text = "不同意并退出"
        btnExit.textSize = 17f
        btnExit.setBackgroundColor(Color.parseColor("#C62828"))
        btnExit.setTextColor(Color.WHITE)
        btnExit.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            buttonHeight
        )
        btnExit.setOnClickListener {
            finish()
        }
        root.addView(btnExit)

        setContentView(root)

        // 开始倒计时
        handler.postDelayed(countdownRunnable, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(countdownRunnable)
    }
}
