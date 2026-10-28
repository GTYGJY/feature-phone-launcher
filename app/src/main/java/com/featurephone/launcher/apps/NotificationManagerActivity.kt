package com.featurephone.launcher.apps

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.R
import com.featurephone.launcher.databinding.ActivityNotificationManagerBinding
import com.featurephone.launcher.util.AnimationHelper
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.FontSizeManager
import java.util.concurrent.Executors

class NotificationManagerActivity : Activity() {

    private lateinit var binding: ActivityNotificationManagerBinding
    private val executor = Executors.newSingleThreadExecutor()

    data class AppInfo(val packageName: String, val appName: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityNotificationManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        applyColors()
        loadApps()
        updateDndStatus()
    }

    override fun onResume() {
        super.onResume()
        updateDndStatus()
        // 【修改】从"显示设置"改完字体大小返回时立即生效
        FontSizeManager.applyFontSizeToView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
            AnimationHelper.overrideFinishTransition(this)
        }

        binding.itemDnd.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            openDndSettings()
        }
    }

    private fun updateDndStatus() {
        // 用Settings.Global读取勿扰模式状态（0=关闭，其他=开启）
        val isDnd = try {
            Settings.Global.getInt(contentResolver, "zen_mode", 0) != 0
        } catch (e: Exception) {
            false
        }
        binding.tvDndStatus.text = if (isDnd) "已开启" else "未开启"
    }

    private fun openDndSettings() {
        try {
            // 优先跳转到勿扰模式设置界面
            val intent = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // 部分设备不支持，回退到声音设置
                startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
            } catch (e2: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {}
            }
        }
    }

    private fun loadApps() {
        executor.execute {
            val pm = packageManager
            val apps = mutableListOf<AppInfo>()
            try {
                val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (info in installed) {
                    // 只显示第三方应用，排除系统应用和自己
                    if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue
                    if (info.packageName == packageName) continue
                    val appName = info.loadLabel(pm).toString()
                    apps.add(AppInfo(info.packageName, appName))
                }
                apps.sortBy { it.appName }
            } catch (_: Exception) {}

            runOnUiThread {
                // 【修改】页面已销毁时不再回写 UI
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.tvAppCount.text = "共${apps.size}个应用"
                renderAppList(apps)
            }
        }
    }

    private fun renderAppList(apps: List<AppInfo>) {
        val container = binding.appListContainer
        container.removeAllViews()

        for (app in apps) {
            // 【修改】这个列表是代码动态生成的，行文字/内边距同样要跟随"字体大小"设置
            val scale = FontSizeManager.getScale()
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(
                    (16 * scale).toInt(),
                    (24 * scale).toInt(),
                    (16 * scale).toInt(),
                    (24 * scale).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val nameTv = TextView(this).apply {
                text = app.appName
                textSize = FontSizeManager.getScaledSize(16f)
                setTextColor(ColorManager.getScreenText())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val arrowTv = TextView(this).apply {
                text = ">"
                textSize = FontSizeManager.getScaledSize(16f)
                setTextColor(ColorManager.getScreenText())
            }

            item.addView(nameTv)
            item.addView(arrowTv)

            item.setOnClickListener {
                VibrationHelper.vibrateKeyPress(this)
                openAppNotificationSettings(app.packageName, app.appName)
            }

            container.addView(item)

            // 分割线
            val divider = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                setBackgroundColor(ColorManager.getScreenTextDim())
            }
            container.addView(divider)
        }
    }

    private fun openAppNotificationSettings(packageName: String, appName: String) {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
            Toast.makeText(this, "请关闭\"$appName\"的通知权限", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "无法打开通知设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.tvDndStatus.setTextColor(ColorManager.getScreenText())
        binding.tvAppCount.setTextColor(ColorManager.getScreenTextDim())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)

        (binding.itemDnd.getChildAt(0) as? TextView)?.setTextColor(ColorManager.getScreenText())
    }
}
