package com.featurephone.launcher.apps

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.DeviceAdminReceiver
import com.featurephone.launcher.R
import com.featurephone.launcher.databinding.ActivityBackgroundAppManagerBinding
import com.featurephone.launcher.util.AnimationHelper
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.FontSizeManager

/**
 * 后台应用管理
 * 设备所有者模式下，可以暂停/恢复第三方应用，防止后台偷跑耗电
 */
class BackgroundAppManagerActivity : Activity() {

    private lateinit var binding: ActivityBackgroundAppManagerBinding
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val appList = mutableListOf<AppInfo>()
    private lateinit var adapter: AppAdapter

    data class AppInfo(
        val packageName: String,
        val appName: String,
        var isSuspended: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        binding = ActivityBackgroundAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)

        setupClicks()
        applyColors()
        loadApps()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
            AnimationHelper.overrideFinishTransition(this)
        }
    }

    private fun isDeviceOwner(): Boolean {
        return try {
            dpm.isDeviceOwnerApp(packageName)
        } catch (e: Exception) {
            false
        }
    }

    private fun loadApps() {
        if (!isDeviceOwner()) {
            binding.tvHint.text = "需ADB设为设备所有者才能管理后台应用：\nadb shell dpm set-device-owner com.featurephone.launcher/.DeviceAdminReceiver\n\n警告：此为高级功能，未对所有设备完善测试，非必要不建议开启，可能导致无法卸载等严重后果。"
            binding.appList.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
            return
        }

        binding.tvHint.text = "点击应用可暂停/恢复，暂停后应用无法运行且不耗电\n\n注意：暂停系统关键应用可能导致异常，请谨慎操作"
        binding.appList.visibility = View.VISIBLE

        // 异步加载应用列表
        Thread {
            val tempList = mutableListOf<AppInfo>()
            try {
                val pm = packageManager
                val packages = pm.getInstalledApplications(0)
                for (info in packages) {
                    // 只显示第三方应用，排除系统应用和自己
                    if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue
                    if (info.packageName == packageName) continue

                    val appName = info.loadLabel(pm).toString()
                    val isSuspended = try {
                        dpm.isPackageSuspended(adminComponent, info.packageName)
                    } catch (e: Exception) {
                        false
                    }
                    tempList.add(AppInfo(info.packageName, appName, isSuspended))
                }
                tempList.sortBy { it.appName }
            } catch (_: Exception) {}

            runOnUiThread {
                // 【修改】页面已销毁时不再回写 UI
                if (isFinishing || isDestroyed) return@runOnUiThread
                appList.clear()
                appList.addAll(tempList)
                adapter = AppAdapter(appList) { app ->
                    toggleAppSuspended(app)
                }
                binding.appList.layoutManager = LinearLayoutManager(this)
                binding.appList.adapter = adapter
                binding.tvEmpty.visibility = if (appList.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun toggleAppSuspended(app: AppInfo) {
        VibrationHelper.vibrateKeyPress(this)
        if (!isDeviceOwner()) {
            Toast.makeText(this, "需要设备所有者权限", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (app.isSuspended) {
                // 恢复应用
                dpm.setPackagesSuspended(adminComponent, arrayOf(app.packageName), false)
                app.isSuspended = false
                Toast.makeText(this, "${app.appName} 已恢复", Toast.LENGTH_SHORT).show()
            } else {
                // 暂停应用
                dpm.setPackagesSuspended(adminComponent, arrayOf(app.packageName), true)
                app.isSuspended = true
                Toast.makeText(this, "${app.appName} 已暂停，不再耗电", Toast.LENGTH_SHORT).show()
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        applyColors()
        // 【修改】从"显示设置"改完字体大小返回时立即生效
        FontSizeManager.applyFontSizeToView(binding.root)
    }

    private fun applyColors() {
        binding.screenArea.setBackgroundColor(ColorManager.getScreenBg())
        binding.keysArea.setBackgroundColor(ColorManager.getKeysBg())
        binding.tvTitle.setTextColor(ColorManager.getScreenText())
        binding.tvHint.setTextColor(ColorManager.getScreenText())
        binding.tvEmpty.setTextColor(ColorManager.getScreenText())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)
    }

    inner class AppAdapter(
        private val items: List<AppInfo>,
        private val onItemClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(android.R.id.text1)
            val tvStatus: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            (view as? ViewGroup)?.setPadding(32, 24, 32, 24)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = items[position]
            // 【修改】列表行跟随"字体大小"设置（行在 onBind 时才创建，onCreate 的全量缩放覆盖不到）
            FontSizeManager.applyToListItem(holder.itemView)
            holder.tvName.text = app.appName
            holder.tvName.setTextColor(ColorManager.getScreenText())
            holder.tvStatus.text = if (app.isSuspended) "已暂停（点击恢复）" else "运行中（点击暂停）"
            holder.tvStatus.setTextColor(
                if (app.isSuspended) android.graphics.Color.RED else ColorManager.getScreenText()
            )
            holder.itemView.setOnClickListener { onItemClick(app) }
        }

        override fun getItemCount() = items.size
    }
}
