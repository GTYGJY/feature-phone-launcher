package com.featurephone.launcher

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.util.LunarCalendar
import com.featurephone.launcher.util.PermissionHelper
import com.featurephone.launcher.util.KioskManager
import com.featurephone.launcher.util.ModeManager
import com.featurephone.launcher.util.AnimationHelper
import com.featurephone.launcher.util.VibrationHelper
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.featurephone.launcher.apps.AppListActivity
import com.featurephone.launcher.apps.CallLogActivity
import com.featurephone.launcher.apps.ContactsActivity
import com.featurephone.launcher.apps.MessagesActivity
import com.featurephone.launcher.apps.SettingsActivity
import com.featurephone.launcher.databinding.ActivityMainBinding
import com.featurephone.launcher.model.AppItem
import com.featurephone.launcher.modules.ModuleManager
import com.featurephone.launcher.ui.MenuAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuAdapter: MenuAdapter
    private val mainMenu = mutableListOf<AppItem>()
    private val handler = Handler(Looper.getMainLooper())
    private var isInMenu = false
    private var isInDialer = false
    private var dialerNumber = ""
    private val dialerMatches = mutableListOf<com.featurephone.launcher.util.T9Matcher.ContactMatch>()
    private lateinit var dialerMatchAdapter: DialerMatchAdapter
    private var pendingAction: (() -> Unit)? = null
    private var pendingPermissionCode = 0
    // 【修改】等待 CALL_PHONE 授权后要继续拨打的号码
    private var pendingCallNumber: String? = null
    // 【修改】记住菜单选中位置，避免每次 onResume 重建列表后选中项被重置到第一项
    private var menuIndex = 0
    // 【修改】T9 查询改为单线程串行 + 令牌丢弃过期结果（原来每次按键 new 一个 Thread）
    private val t9Executor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var t9QueryToken = 0
    // 【修改】保存上一次的查询任务，新输入时先取消排队中的任务，避免快速按键时任务堆积
    private var t9Future: java.util.concurrent.Future<*>? = null
    private lateinit var telephonyManager: TelephonyManager
    private val smsNotificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val sender = intent?.getStringExtra("sender") ?: return
            val body = intent?.getStringExtra("body") ?: ""
            // 在主界面显示短信通知
            val display = if (body.length > 20) "${body.take(20)}..." else body
            Toast.makeText(this@MainActivity, "新短信 - $sender: $display", Toast.LENGTH_LONG).show()
        }
    }
    private val signalListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            super.onSignalStrengthsChanged(signalStrength)
            updateSignalDisplay(signalStrength)
        }
        override fun onServiceStateChanged(serviceState: ServiceState) {
            super.onServiceStateChanged(serviceState)
            runOnUiThread {
                if (serviceState.state != ServiceState.STATE_IN_SERVICE) {
                    binding.tvSignal.text = "无服务"
                }
                val operator = telephonyManager.networkOperatorName
                if (operator.isNotBlank()) {
                    binding.tvIdleCarrier.text = operator
                }
            }
        }
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            // 来电时主动退到后台，让系统电话APP全屏弹出，避免被压缩成通知栏
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                runOnUiThread {
                    try {
                        moveTaskToBack(true)
                    } catch (e: Exception) {
                        // 部分ROM不允许moveTaskToBack，忽略即可
                    }
                }
            }
        }
    }
    private var backPressedTime = 0L

    // 时间更新
    // 时间更新：每分钟更新一次，减少唤醒次数，降低耗电
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 60000)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.featurephone.launcher.util.LocaleManager.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查是否已同意免责声明
        val prefs = getSharedPreferences("feature_phone_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("disclaimer_agreed", false)) {
            val intent = Intent(this, com.featurephone.launcher.apps.DisclaimerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }

        ColorManager.init(this)
        ModuleManager.init(this)
        ShortcutManager.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 【修改】崩溃熔断后给出明确提示：此前连续崩溃会直接停止自动重启，用户只看到桌面闪退没有任何说明
        if (prefs.getBoolean("crash_loop_detected", false)) {
            prefs.edit().putBoolean("crash_loop_detected", false).apply()
            Toast.makeText(
                this,
                "检测到连续崩溃，已停止自动重启；若反复出现请查看崩溃日志或清除应用数据",
                Toast.LENGTH_LONG
            ).show()
        }

        // 保持屏幕常亮（功能机待机界面）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 禁止截图和录屏，防止敏感信息泄露
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        applyColors()
        FontSizeManager.applyFontSizeToView(binding.root)
        resetStatusBarFixedSize()
        // 【修改】拨号匹配列表提前到按键注册之前初始化。
        // 原顺序是先 setupButtons() 再初始化 dialerMatchAdapter，一旦在初始化完成前触发了
        // 数字键点击（例如界面重建、无障碍/自动化点击），就会抛 UninitializedPropertyAccessException
        // 崩溃，被全局异常处理器重启到主界面，表现就是"点数字键进不去拨号界面"。
        dialerMatchAdapter = DialerMatchAdapter(dialerMatches) { match ->
            VibrationHelper.vibrateKeyPress(this)
            dialerNumber = match.number
            binding.tvDialerNumber.text = dialerNumber
            dialerMatches.clear()
            dialerMatchAdapter.notifyDataSetChanged()
            binding.rvDialerMatches.visibility = View.GONE
            makeCall(dialerNumber)
        }
        binding.rvDialerMatches.layoutManager = LinearLayoutManager(this)
        binding.rvDialerMatches.adapter = dialerMatchAdapter

        // 初始化管理器（必须在setupMenu之前）
        KioskManager.init(this)
        ModeManager.init(this)
        // 【修改】不再初始化来电静音（CallGuardManager）：从 1.2.2 起「防护模式」已改成
        // 纯文字「防护提示」页，软件本身不再接管铃声；保留初始化只会让老用户
        // 残留的 whitelist_enabled=true 继续静音，而新界面里已经没有开关可以关掉它。

        setupMenu()
        setupButtons()

        // 注册短信通知接收器
        // 【修改】1) 带上签名级权限 + RECEIVER_NOT_EXPORTED，防止第三方应用伪造「新短信」广播；
        // 2) 补齐 Android 14（targetSdk 34）要求的导出标志——此前这里会抛 SecurityException
        //    并被静默吞掉，导致 Android 14+ 上短信提醒完全不工作。
        try {
            ContextCompat.registerReceiver(
                this,
                smsNotificationReceiver,
                IntentFilter(SmsReceiver.ACTION_SMS_RECEIVED),
                SmsReceiver.PERMISSION_INTERNAL,
                null,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}

        // 初始化信号监听
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        setupNumberKeyText()

        // 启动常驻服务
        FeaturePhoneService.start(this)

        // 显示待机界面
        showIdleScreen()

        // 监听信号和电池
        observePhoneState()

        // 启动时一次性请求所有需要的权限
        requestAllPermissionsIfNeeded()
    }

    /**
     * 启动时一次性请求所有需要的危险权限
     * 避免用户在使用各功能时分别授权，防止漏掉权限
     */
    private fun requestAllPermissionsIfNeeded() {
        // 【修改】Android 13+ 需要 POST_NOTIFICATIONS，否则前台服务的常驻通知不显示
        val permissions = REQUIRED_PERMISSIONS.toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) return

        // 【修改】启动批量申请只做一次：MainActivity 是桌面，会被系统频繁重建，
        // 原来每次 onCreate 都会再弹一遍授权框，很容易被误认为"点数字键就提示无权限"。
        // 申请过一次后，差的权限统一由「设置 > 其他设置 > 权限管理」提示并处理。
        val prefs = getSharedPreferences("feature_phone_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("all_permissions_requested", false)) {
            // 【修改】READ_PHONE_STATE 是本次新增（信号栏与"来电自动退到后台"都依赖它），
            // 老版本安装过的用户已经走过"只申请一次"的分支，这里单独补申请一次，
            // 否则该功能在已安装用户上会静默失效（listen 回调根本不会触发）。
            val needsPhoneState = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
            if (needsPhoneState && !prefs.getBoolean("phone_state_requested", false)) {
                prefs.edit().putBoolean("phone_state_requested", true).apply()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    REQUEST_PHONE_STATE
                )
            }
            return
        }
        prefs.edit().putBoolean("all_permissions_requested", true).apply()

        // 请求前说明权限用途
        android.widget.Toast.makeText(
            this,
            // 【修改】启动批量已不含位置权限（改为联网设置里按需申请），提示文案同步更新
            "需要电话、短信、通话记录、通讯录等权限，仅用于对应功能正常使用，不收集个人信息",
            android.widget.Toast.LENGTH_LONG
        ).show()
        ActivityCompat.requestPermissions(
            this,
            missingPermissions.toTypedArray(),
            REQUEST_ALL_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            // 【修改】授权完成后重新注册电话状态监听：onResume 时还没有权限，
            // 此时不重注册的话，信号栏要等到下一次 onResume 才正常
            observePhoneState()
            // 启动时全部权限请求完成，刷新模块显示
            updateInfoModule()
            return
        }
        if (requestCode == REQUEST_PHONE_STATE) {
            // 【修改】补申请电话状态权限完成后重新注册监听，信号栏/来电检测立即生效
            observePhoneState()
            return
        }
        // 【修改】拨号权限申请结果：授权成功就直接把刚才的号码拨出去
        if (requestCode == REQUEST_CALL_PERMISSION) {
            val number = pendingCallNumber
            pendingCallNumber = null
            if (number != null) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    placeCallIntent(number)
                } else {
                    Toast.makeText(this, "未授予拨号权限，无法拨打电话", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }
        // 功能入口权限请求：如果全部授予，执行待启动功能
        if (requestCode == pendingPermissionCode && grantResults.isNotEmpty()) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                pendingAction?.invoke()
            }
            pendingAction = null
            pendingPermissionCode = 0
        }
    }

    // 【修改】原 requestLocationPermissionIfNeeded() 从未被调用，位置权限已改为在
    // 「联网设置」开启时按需申请，故删除该无用方法及其 requestCode（避免死代码和无用常量）。

    companion object {
        private const val REQUEST_ALL_PERMISSIONS = 1002
        // 【修改】拨号权限的独立请求码（原来硬编码 100 且没有回调处理）
        private const val REQUEST_CALL_PERMISSION = 1003
        // 【修改】电话状态权限的补申请请求码（老用户升级后单独补一次）
        private const val REQUEST_PHONE_STATE = 1004

        // 启动时一次性请求的所有危险权限
        // 【修改】位置权限整体改为按需申请：天气默认关闭（联网开关默认关），只需粗略定位，
        // 因此在「联网设置」打开开关时才请求，不再放在桌面启动的批量申请里。
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
            // 【修改】信号栏与"来电时自动退到后台"都依赖 LISTEN_SIGNAL_STRENGTHS /
            // LISTEN_CALL_STATE，没有该权限时监听回调不会触发，功能会静默失效
            Manifest.permission.READ_PHONE_STATE
        )
    }

    override fun onResume() {
        super.onResume()
        // 【修改】未同意免责声明时 onCreate 会提前 finish，此时 binding 等尚未初始化，
        // 若不拦截，onResume 里的 applyColors()/ModuleManager 会抛异常导致崩溃。
        if (!::binding.isInitialized) return
        // 注册信号和服务状态监听
        observePhoneState()
        handler.post(timeRunnable)
        updateBattery()
        applyColors()
        // 【修改】字体大小改动后回到主界面要立即生效（含拨号号码、菜单、列表行）
        FontSizeManager.applyFontSizeToView(binding.root)
        resetStatusBarFixedSize()
        startInfoModule()
        // 应用Kiosk锁定模式
        KioskManager.applyKioskToActivity(this)
        // 刷新菜单（快捷应用可能被修改）
        if (isInMenu) {
            setupMenu()
            menuAdapter.notifyDataSetChanged()
        }
    }

    override fun onPause() {
        super.onPause()
        // 取消信号监听，减少耗电
        try {
            telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_NONE)
        } catch (_: Exception) {}
        handler.removeCallbacks(timeRunnable)
        try {
            stopInfoModule()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销短信通知接收器
        try {
            unregisterReceiver(smsNotificationReceiver)
        } catch (_: Exception) {}
        // 清理所有回调和监听器，防止内存泄漏
        handler.removeCallbacksAndMessages(null)
        try {
            telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_NONE)
        } catch (_: Exception) {}
        try {
            stopInfoModule()
        } catch (_: Exception) {}
        // 【修改】释放 T9 查询线程，避免 Activity 销毁后仍持有引用
        try {
            t9Future?.cancel(true)
            t9Future = null
            t9Executor.shutdownNow()
        } catch (_: Exception) {}
    }

    /**
     * 状态栏（信号/电量）固定字号，不跟随字体大小设置缩放。
     * 信号柱状图和电量百分比属于图标性质，放大后会挤压中间时间。
     */
    private fun resetStatusBarFixedSize() {
        binding.tvSignal.textSize = 20f
        binding.tvBattery.textSize = 26f
    }

    /**
     * 应用自定义颜色到所有View
     */
    private fun applyColors() {
        val cm = ColorManager
        // 屏幕区域
        binding.screenArea.setBackgroundColor(cm.getScreenBg())
        // 按键区域
        binding.keysArea.setBackgroundColor(cm.getKeysBg())

        // 屏幕文字
        // 【修改】补上拨号号码 tvDialerNumber 与 T9 列表颜色：原来这里漏掉了拨号号码，
        // 号码一直用 XML 里的默认颜色，用户把屏幕背景改成深色后号码就"看不见/不跟着配色"。
        arrayOf(binding.tvSignal, binding.tvTime, binding.tvBattery,
            binding.tvIdleTime, binding.tvLeftSoft, binding.tvRightSoft,
            binding.tvDialerNumber
        ).forEach { it.setTextColor(cm.getScreenText()) }
        arrayOf(binding.tvIdleDate, binding.tvIdleCarrier, binding.tvIdleLunar
        ).forEach { it.setTextColor(cm.getScreenTextDim()) }

        // 菜单标题颜色
        binding.tvMenuTitle.setTextColor(cm.getScreenText())

        // 信息模块文字（用按键文字色，背景用按键底色）
        binding.tvInfoModule.setTextColor(cm.getKeyText())
        binding.tvInfoModule.setBackgroundColor(cm.getKeyBg())

        // 普通按键背景和文字
        val keyBtns = arrayOf(
            binding.btnLeftSoft, binding.btnRightSoft,
            binding.btn1, binding.btn2, binding.btn3, binding.btn4, binding.btn5,
            binding.btn6, binding.btn7, binding.btn8, binding.btn9,
            binding.btn0, binding.btnStar, binding.btnPound
        )
        keyBtns.forEach {
            it.setBackgroundColor(cm.getKeyBg())
            it.setTextColor(cm.getKeyText())
        }

        // 拨打键（固定绿色）
        binding.btnCall.setBackgroundColor(ColorManager.CALL_BG)
        binding.btnCall.setTextColor(ColorManager.CALL_TEXT)
        // 挂断键（固定红色）
        binding.btnEnd.setBackgroundColor(ColorManager.END_BG)
        binding.btnEnd.setTextColor(ColorManager.END_TEXT)

        // 底部模块区域（按键底色，和电量天气模块一致）
        binding.bottomModuleArea.setBackgroundColor(cm.getKeyBg())
        binding.tvBottomModuleTitle.setTextColor(cm.getKeyText())
        binding.tvBottomModuleContent.setTextColor(cm.getKeyText())
    }

    override fun onBackPressed() {
        if (isInMenu) {
            showIdleScreen()
        } else if (isInDialer) {
            // 【修改】原实现漏了拨号态：在拨号界面按返回键什么都不做，现改为退回待机
            hideDialer()
        } else {
            // 功能机模式下禁止退出
            val now = System.currentTimeMillis()
            if (now - backPressedTime < 2000) {
                backPressedTime = 0
            } else {
                backPressedTime = now
            }
        }
    }

    // ========== 信息模块 ==========

    private fun startInfoModule() {
        // 模块1（中间区域）
        val module1 = ModuleManager.getModule1()
        // 【修改】模块字号也要跟随“字体大小”设置：原来直接赋 textSize 会覆盖用户选择的缩放比例
        binding.tvInfoModule.textSize = FontSizeManager.getScaledSize(module1.getContentSize())
        binding.tvInfoModule.setTag(R.id.font_size_original, module1.getContentSize())
        if (module1.needsLiveUpdate()) {
            module1.startListening(this) {
                runOnUiThread { updateInfoModule() }
            }
        }
        // 模块2（底部区域）
        val module2 = ModuleManager.getModule2()
        if (module2.needsLiveUpdate()) {
            module2.startListening(this) {
                runOnUiThread { updateInfoModule() }
            }
        }
        updateInfoModule()
    }

    private fun stopInfoModule() {
        val module1 = ModuleManager.getModule1()
        if (module1.needsLiveUpdate()) {
            module1.stopListening(this)
        }
        val module2 = ModuleManager.getModule2()
        if (module2.needsLiveUpdate()) {
            module2.stopListening(this)
        }
    }

    private fun updateInfoModule() {
        // 中间区域显示模块1
        val module1 = ModuleManager.getModule1()
        binding.tvInfoModule.text = module1.getContent(this)
        // 底部区域显示模块2
        val module2 = ModuleManager.getModule2()
        val content2 = module2.getContent(this)
        if (content2.isEmpty()) {
            // 空白模块：隐藏标题和内容，只保留底色
            binding.tvBottomModuleTitle.visibility = View.GONE
            binding.tvBottomModuleContent.visibility = View.GONE
        } else {
            binding.tvBottomModuleTitle.visibility = View.VISIBLE
            binding.tvBottomModuleContent.visibility = View.VISIBLE
            // 优先显示副标题（如天气模块的城市名），没有则显示模块名称
            val subTitle = module2.getSubTitle(this)
            binding.tvBottomModuleTitle.text = subTitle ?: module2.getDisplayName()
            binding.tvBottomModuleContent.text = content2
        }
    }

    // ========== 菜单设置 ==========

    /**
     * 带权限检查的功能启动
     * 有权限直接执行，无权限则请求，请求成功后执行
     */
    private fun launchWithPermission(
        permissions: Array<String>,
        requestCode: Int,
        action: () -> Unit
    ) {
        if (PermissionHelper.hasPermissions(this, permissions)) {
            action()
            return
        }
        if (PermissionHelper.hasPermanentlyDeniedPermission(this, permissions)) {
            // 权限被永久拒绝，引导去设置
            android.app.AlertDialog.Builder(this)
                .setTitle("权限不足")
                .setMessage("该功能需要相关权限，请到设置中开启。")
                .setPositiveButton("去设置") { _, _ ->
                    PermissionHelper.openAppSettings(this)
                }
                .setNegativeButton("取消", null)
                .show()
            return
        }
        pendingAction = action
        pendingPermissionCode = requestCode
        PermissionHelper.requestPermissions(this, permissions, requestCode)
    }

    private fun setupMenu() {
        mainMenu.clear()

        // 极简模式：只显示核心功能
        if (ModeManager.isSimpleMode()) {
            mainMenu.add(AppItem("dialer", getString(R.string.menu_dialer), "📞") {
                VibrationHelper.vibrateKeyPress(this)
                // 【修改】不再打开独立的 DialerActivity，直接切到主界面的拨号状态
                showDialer()
            })
            mainMenu.add(AppItem("messages", getString(R.string.menu_messages), "✉") {
                VibrationHelper.vibrateKeyPress(this)
                launchWithPermission(PermissionHelper.PermissionGroups.SMS, 2002) {
                    startActivity(Intent(this, MessagesActivity::class.java))
                    AnimationHelper.overrideStartTransition(this)
                }
            })
            mainMenu.add(AppItem("contacts", getString(R.string.menu_contacts), "👤") {
                VibrationHelper.vibrateKeyPress(this)
                launchWithPermission(PermissionHelper.PermissionGroups.CONTACTS, 2003) {
                    startActivity(Intent(this, ContactsActivity::class.java))
                    AnimationHelper.overrideStartTransition(this)
                }
            })
            mainMenu.add(AppItem("settings", getString(R.string.menu_settings), "⚙") {
                VibrationHelper.vibrateKeyPress(this)
                startActivity(Intent(this, SettingsActivity::class.java))
                AnimationHelper.overrideStartTransition(this)
            })
            menuAdapter = MenuAdapter(mainMenu) { item, _ ->
                item.action?.invoke()
            }
            binding.menuList.layoutManager = LinearLayoutManager(this)
            binding.menuList.adapter = menuAdapter
            // 【修改】恢复上次的选中位置
            menuAdapter.setSelected(menuIndex)
            menuIndex = menuAdapter.getSelectedPosition()
            return
        }

        // 标准模式：显示所有功能
        mainMenu.add(AppItem("dialer", getString(R.string.menu_dialer), "📞") {
            VibrationHelper.vibrateKeyPress(this)
            // 【修改】同上：菜单「电话」改为进入主界面的拨号状态
            showDialer()
        })
        mainMenu.add(AppItem("messages", getString(R.string.menu_messages), "✉") {
            VibrationHelper.vibrateKeyPress(this)
            launchWithPermission(PermissionHelper.PermissionGroups.SMS, 2002) {
                startActivity(Intent(this, MessagesActivity::class.java))
                AnimationHelper.overrideStartTransition(this)
            }
        })
        mainMenu.add(AppItem("contacts", getString(R.string.menu_contacts), "👤") {
            VibrationHelper.vibrateKeyPress(this)
            launchWithPermission(PermissionHelper.PermissionGroups.CONTACTS, 2003) {
                startActivity(Intent(this, ContactsActivity::class.java))
                AnimationHelper.overrideStartTransition(this)
            }
        })
        mainMenu.add(AppItem("call_log", getString(R.string.menu_call_log), "📋") {
            VibrationHelper.vibrateKeyPress(this)
            launchWithPermission(PermissionHelper.PermissionGroups.CALL_LOG, 2004) {
                startActivity(Intent(this, CallLogActivity::class.java))
                AnimationHelper.overrideStartTransition(this)
            }
        })
        mainMenu.add(AppItem("apps", getString(R.string.menu_apps), "📱") {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, AppListActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        })

        // 动态添加快捷应用（最多8个）
        val shortcuts = ShortcutManager.getShortcuts()
        for (shortcut in shortcuts) {
            val pkg = shortcut.packageName
            mainMenu.add(AppItem("shortcut_$pkg", shortcut.appName, "⭐") {
                VibrationHelper.vibrateKeyPress(this)
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    AnimationHelper.overrideStartTransition(this)
                }
            })
        }

        mainMenu.add(AppItem("settings", getString(R.string.menu_settings), "⚙") {
            VibrationHelper.vibrateKeyPress(this)
            startActivity(Intent(this, SettingsActivity::class.java))
            AnimationHelper.overrideStartTransition(this)
        })

        menuAdapter = MenuAdapter(mainMenu) { item, _ ->
            item.action?.invoke()
        }
        binding.menuList.layoutManager = LinearLayoutManager(this)
        binding.menuList.adapter = menuAdapter
        // 【修改】恢复上次的选中位置
        menuAdapter.setSelected(menuIndex)
        menuIndex = menuAdapter.getSelectedPosition()
    }

    // ========== 按键设置 ==========

    private fun setupButtons() {
        // 软键
        binding.btnLeftSoft.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            when {
                isInDialer -> {
                    // 拨号状态：清空号码
                    dialerNumber = ""
                    binding.tvDialerNumber.text = ""
                    dialerMatches.clear()
                    dialerMatchAdapter.notifyDataSetChanged()
                    binding.rvDialerMatches.visibility = View.GONE
                }
                isInMenu -> menuAdapter.getSelectedItem()?.action?.invoke()
                else -> showMenu()
            }
        }
        binding.btnRightSoft.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            when {
                isInDialer -> hideDialer()
                isInMenu -> showIdleScreen()
                else -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                    AnimationHelper.overrideStartTransition(this)
                }
            }
        }

        // 拨打键
        binding.btnCall.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            if (isInDialer) {
                // 拨号状态：拨打当前号码
                makeCall(dialerNumber)
            } else if (isInMenu) {
                // 菜单状态：执行选中项
                menuAdapter.getSelectedItem()?.action?.invoke()
            } else {
                // 待机状态：切换到拨号状态
                showDialer()
            }
        }


        // 挂断/电源键
        binding.btnEnd.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            when {
                isInDialer -> hideDialer()
                isInMenu -> showIdleScreen()
                else -> moveTaskToBack(true)
            }
        }

        // 数字键
        val numKeys = mapOf(
            binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3",
            binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6",
            binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9",
            binding.btn0 to "0", binding.btnStar to "*", binding.btnPound to "#"
        )
        numKeys.forEach { (btn, num) ->
            btn.setOnClickListener {
                VibrationHelper.vibrateKeyPress(this)
                appendDialerDigit(num)
            }
        }
    }

    /**
     * 拨号状态下输入数字
     * 【修改】进入拨号界面本身不涉及任何权限：这里只做界面切换 + 号码拼接，
     * 只有真正"拨出去"（makeCall/placeCallIntent）时才需要 CALL_PHONE。
     * 因此点击数字键绝不应该出现"无权限"提示。
     */
    private fun appendDialerDigit(digit: String) {
        // 【修改】兜底 try-catch：此前如果这里抛异常，会被 FeaturePhoneApp 的全局异常处理器
        // 直接杀掉进程并重启到主界面，用户看到的就是"点数字键没反应、还弹出权限/提示"。
        // 现在把异常显示出来并写日志，便于定位到底哪一步失败。
        try {
            if (!isInDialer) {
                // 待机状态：切换到拨号状态
                showDialer()
                dialerNumber = digit
            } else {
                // 拨号状态：追加数字
                if (dialerNumber.length < 20) {
                    dialerNumber += digit
                }
            }
            binding.tvDialerNumber.text = dialerNumber
            // 理论上在 onCreate 里已初始化，这里再兜一层，避免任何时序问题导致崩溃
            if (::dialerMatchAdapter.isInitialized) {
                performT9Match(dialerNumber)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "appendDialerDigit 失败", e)
            Toast.makeText(this, "打开拨号界面失败：${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 切换到拨号状态
     */
    private fun showDialer() {
        // 【新增】界面切换统一 500ms 过渡（必须在改 visibility 之前调用）
        animateScreenSwitch()
        isInDialer = true
        // 【修改】拨号状态现在可以从菜单直接进入：必须同时退出菜单状态并恢复数字键盘。
        // 否则会残留 isInMenu=true（软键仍是"确认/返回"）且 keypadArea 还是 GONE，
        // 表现就是"进不去拨号界面 / 进去也打不了字"。
        isInMenu = false
        binding.idleScreen.visibility = View.GONE
        binding.menuContainer.visibility = View.GONE
        binding.dialerScreen.visibility = View.VISIBLE
        binding.keypadArea.visibility = View.VISIBLE
        // 【修改】确保拨号屏在最上层（内容区是 FrameLayout，避免被其他子 View 盖住）
        binding.dialerScreen.bringToFront()
        // 号码与 T9 列表跟随颜色/字体设置
        binding.tvDialerNumber.setTextColor(ColorManager.getScreenText())
        FontSizeManager.applyFontSizeToView(binding.tvDialerNumber)
        // 软键提示
        binding.tvLeftSoft.text = "清空"
        binding.tvRightSoft.text = "返回"
        binding.btnLeftSoft.text = "清空"
        binding.btnRightSoft.text = "返回"
    }

    /**
     * 从拨号状态返回待机
     * 【修改】统一走 showIdleScreen()：由它负责 500ms 过渡动画、清理拨号状态和恢复软键文字，
     * 避免"两个方法各改一半 visibility"导致状态不一致。
     */
    private fun hideDialer() {
        showIdleScreen()
    }

    /**
     * T9智能匹配
     */
    private fun performT9Match(input: String) {
        // 【修改】令牌自增：任何一次新输入（包括清空）都会让此前排队的结果作废
        val token = ++t9QueryToken
        // 【修改】取消仍在排队的旧查询：单线程池里积压的任务会在新输入后依次执行，
        // 虽然结果会被令牌丢弃，但白做查询、拖慢最新一次匹配。取消后新任务立即排到队首。
        try {
            t9Future?.cancel(true)
        } catch (_: Exception) {}
        t9Future = null
        if (input.isEmpty()) {
            dialerMatches.clear()
            dialerMatchAdapter.notifyDataSetChanged()
            binding.rvDialerMatches.visibility = View.GONE
            return
        }
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            binding.rvDialerMatches.visibility = View.GONE
            return
        }
        // 【修改】Activity 已销毁（线程池已关闭）时直接返回，避免 RejectedExecutionException
        if (t9Executor.isShutdown) return
        // 【修改】原来每按一个键就 new 一个 Thread，快速输入会产生大量并发查询且结果乱序；
        // 改为单线程串行执行，并在回到主线程时丢弃过期（令牌不一致）的结果。
        t9Future = t9Executor.submit(java.lang.Runnable {
            val results = queryContactsForMatch(input)
            // 【修改】任务被取消（有更新的输入）时不再回调 UI；
            // 用 Thread.interrupted() 同时清掉中断标记，避免残留标记影响线程池里的下一次查询
            if (Thread.interrupted()) return@Runnable
            runOnUiThread {
                if (token != t9QueryToken) return@runOnUiThread
                if (isFinishing || isDestroyed) return@runOnUiThread
                dialerMatches.clear()
                dialerMatches.addAll(results)
                dialerMatchAdapter.notifyDataSetChanged()
                binding.rvDialerMatches.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE
            }
        })
    }

    /**
     * 查询通讯录并匹配
     */
    private fun queryContactsForMatch(input: String): List<com.featurephone.launcher.util.T9Matcher.ContactMatch> {
        val results = mutableListOf<com.featurephone.launcher.util.T9Matcher.ContactMatch>()
        var cursor: android.database.Cursor? = null
        try {
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            cursor = contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null, null
            )
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    // 【修改】任务被取消时尽快退出遍历，快速连按数字键不再做无用查询
                    if (Thread.currentThread().isInterrupted) break
                    val name = it.getString(nameIndex) ?: continue
                    val number = it.getString(numberIndex) ?: continue
                    val match = com.featurephone.launcher.util.T9Matcher.match(input, name, number)
                    if (match != null && results.none { r -> r.number == number }) {
                        results.add(match)
                        if (results.size >= 20) break
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }
        return results.sortedBy { it.matchType.ordinal }
    }

    /**
     * 拨打电话
     */
    private fun makeCall(number: String) {
        if (number.isBlank()) {
            Toast.makeText(this, "请输入号码", Toast.LENGTH_SHORT).show()
            return
        }
        val emergencyNumbers = listOf("110", "120", "119", "122", "999", "112", "911")
        if (emergencyNumbers.contains(number)) {
            placeCallIntent(number)
            return
        }
        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // 【修改】记住待拨号码，授权回调里继续拨打；原来只发请求不记录，
            // 授权成功后什么也不会发生，用户会以为"权限给了还是不能用"。
            pendingCallNumber = number
            requestPermissions(arrayOf(android.Manifest.permission.CALL_PHONE), REQUEST_CALL_PERMISSION)
            return
        }
        placeCallIntent(number)
    }

    /**
     * 【修改】真正发起通话，并把"权限问题"和"没有电话应用"区分开：
     * - SecurityException：确实没权限，提示去系统设置授权；
     * - 其他异常（ActivityNotFoundException 等）：没有能处理 ACTION_CALL 的应用，
     *   退回系统拨号界面 ACTION_DIAL，而不是错误地提示"无权限"。
     */
    private fun placeCallIntent(number: String) {
        val callIntent = Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:$number"))
        try {
            startActivity(callIntent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "未授予拨号权限，请在系统设置中允许“打电话”权限", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number")))
                Toast.makeText(this, "已转到系统拨号界面，请按拨号键完成呼叫", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Toast.makeText(this, "没有可用的电话应用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 物理按键支持（如果设备有实体键盘）
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                handlePhysicalNavigation(keyCode)
                true
            }
            KeyEvent.KEYCODE_CALL -> {
                // 物理拨打键：切换到拨号状态或拨打
                if (isInDialer) {
                    makeCall(dialerNumber)
                } else {
                    showDialer()
                }
                true
            }
            KeyEvent.KEYCODE_ENDCALL -> {
                // 物理挂断键：根据当前状态返回
                when {
                    isInDialer -> hideDialer()
                    isInMenu -> showIdleScreen()
                    else -> moveTaskToBack(true)
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun handlePhysicalNavigation(keyCode: Int) {
        // 【修改】拨号状态下方向键不应弹出菜单，否则会出现“拨号屏 + 菜单”两个界面同时显示
        if (isInDialer) return
        if (!isInMenu) {
            showMenu()
            return
        }
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> menuIndex = menuAdapter.moveUp()
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> menuIndex = menuAdapter.moveDown()
            KeyEvent.KEYCODE_DPAD_CENTER -> menuAdapter.getSelectedItem()?.action?.invoke()
        }
        val pos = menuAdapter.getSelectedPosition()
        (binding.menuList.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(pos, 0)
    }

    // ========== 界面切换 ==========

    /**
     * 【新增】应用内界面切换（待机 / 菜单 / 拨号）统一 500ms 过渡动画。
     * 必须在修改 visibility **之前** 调用，TransitionManager 才能捕捉到前后两个场景。
     */
    private fun animateScreenSwitch() {
        AnimationHelper.animateScreenChange(binding.root)
    }

    /**
     * 【新增】清空拨号状态：号码、T9 匹配列表、拨号屏可见性。
     * 由待机/菜单/退出拨号共用，避免各方法各改一半导致状态残留。
     */
    private fun resetDialerState() {
        isInDialer = false
        dialerNumber = ""
        dialerMatches.clear()
        if (::dialerMatchAdapter.isInitialized) {
            dialerMatchAdapter.notifyDataSetChanged()
        }
        binding.rvDialerMatches.visibility = View.GONE
        binding.tvDialerNumber.text = ""
        binding.dialerScreen.visibility = View.GONE
    }

    private fun showIdleScreen() {
        animateScreenSwitch()
        isInMenu = false
        resetDialerState()
        binding.idleScreen.visibility = View.VISIBLE
        binding.menuContainer.visibility = View.GONE
        binding.keypadArea.visibility = View.VISIBLE
        binding.tvLeftSoft.text = getString(R.string.menu_title)
        binding.tvRightSoft.text = getString(R.string.menu_contacts)
        binding.btnLeftSoft.text = getString(R.string.menu_title)
        binding.btnRightSoft.text = getString(R.string.menu_contacts)
    }

    private fun showMenu() {
        // 【修改】这里不再调用 hideDialer()：它会再触发一次 showIdleScreen 的动画并造成双重切换。
        // 现在统一在一次 500ms 过渡内完成"清拨号状态 + 显示菜单"。
        animateScreenSwitch()
        isInMenu = true
        resetDialerState()
        binding.idleScreen.visibility = View.GONE
        binding.menuContainer.visibility = View.VISIBLE
        binding.keypadArea.visibility = View.GONE
        binding.tvLeftSoft.text = getString(R.string.confirm)
        binding.tvRightSoft.text = getString(R.string.back)
        binding.btnLeftSoft.text = getString(R.string.confirm)
        binding.btnRightSoft.text = getString(R.string.back)
        // 【修改】进入菜单时恢复到上次的位置（原来固定为 0，且未渲染选中态）
        menuAdapter.setSelected(menuIndex)
    }

    // ========== 状态更新 ==========

    private fun updateTime() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        // 根据设置选择日期格式
        val datePattern = if (com.featurephone.launcher.util.UnitSettingsManager.getDateFormat(this) ==
            com.featurephone.launcher.util.UnitSettingsManager.DATE_DMY) {
            "dd/MM/yyyy EEE" // 日月年
        } else {
            "yyyy/MM/dd EEE" // 年月日
        }
        val dateFormat = SimpleDateFormat(datePattern, Locale.getDefault())
        val timeStr = timeFormat.format(now)
        binding.tvTime.text = timeStr
        binding.tvIdleTime.text = timeStr
        binding.tvIdleDate.text = dateFormat.format(now)
        binding.tvIdleLunar.text = LunarCalendar.getCurrentLunarDate()
    }

    private fun updateSignalDisplay(signalStrength: SignalStrength?) {
        if (signalStrength == null) {
            binding.tvSignal.text = "--"
            return
        }
        try {
            // 使用通用信号等级（API 23+），兼容GSM/LTE/5G等所有网络类型
            val level = signalStrength.level
            if (level <= 0) {
                binding.tvSignal.text = "--"
            } else {
                // 带间距的信号格，模拟普通手机样式
                val bars = arrayOf("▂", "▂ ▄", "▂ ▄ ▆", "▂ ▄ ▆ █")
                binding.tvSignal.text = bars[level.coerceIn(1, 4) - 1]
            }
        } catch (_: Exception) {
            binding.tvSignal.text = "--"
        }
    }

    private fun updateBattery() {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) (level * 100 / scale) else 100
        }
        binding.tvBattery.text = "${level}%"
    }

    private fun observePhoneState() {
        try {
            telephonyManager.listen(
                signalListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or
                    PhoneStateListener.LISTEN_SERVICE_STATE or
                    PhoneStateListener.LISTEN_CALL_STATE
            )
        } catch (e: SecurityException) {
            binding.tvSignal.text = "--"
        }
    }

    // ========== 数字键 ==========

    /**
     * 设置数字键文本：只显示数字，不显示字母
     */
    private fun setupNumberKeyText() {
        val buttons = listOf(
            binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3",
            binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6",
            binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9",
            binding.btnStar to "*", binding.btn0 to "0", binding.btnPound to "#"
        )
        for ((button, num) in buttons) {
            button.setPadding(0, 0, 0, 0)
            button.includeFontPadding = false
            button.text = num
        }
    }

    // ========== 拨号匹配列表适配器 ==========

    /**
     * 【修改】修复"点数字键闪回主界面"的崩溃：
     * 原实现在 onCreateViewHolder 里手工拼了一个 LinearLayout，把两个 TextView addView 进去后，
     * 却把 **已经挂在父布局上的 tvName** 当作 itemView 传给 ViewHolder：
     *
     *     inner class ViewHolder(val tvName: TextView, ...) : RecyclerView.ViewHolder(tvName)
     *
     * RecyclerView 会因此抛
     * IllegalStateException: ViewHolder views must not be attached when created，
     * 而数字键一按、T9 匹配到联系人、列表开始布局时就会触发，随后被全局异常处理器重启应用，
     * 表现就是"点数字键闪一下回到主界面"。
     *
     * 现在改为标准写法：inflate 独立的 item_dialer_match.xml（attachToRoot = false），
     * itemView 是整行根布局，TextView 只作为子控件。
     */
    inner class DialerMatchAdapter(
        private val items: List<com.featurephone.launcher.util.T9Matcher.ContactMatch>,
        private val onClick: (com.featurephone.launcher.util.T9Matcher.ContactMatch) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<DialerMatchAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dialer_match, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            // 【修改】T9 匹配行同样跟随"字体大小"设置（行在 onBind 时才创建）
            FontSizeManager.applyToListItem(holder.itemView)
            holder.tvName.text = item.name
            holder.tvNumber.text = item.number
            holder.tvName.setTextColor(ColorManager.getScreenText())
            holder.tvNumber.setTextColor(ColorManager.getScreenTextDim())
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}