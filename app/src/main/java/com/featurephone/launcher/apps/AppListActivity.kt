package com.featurephone.launcher.apps

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import com.featurephone.launcher.util.FontSizeManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.R
import com.featurephone.launcher.ShortcutManager
import com.featurephone.launcher.databinding.ActivityAppListBinding
import com.featurephone.launcher.databinding.ItemAppBinding

class AppListActivity : Activity() {

    private lateinit var binding: ActivityAppListBinding
    private val appList = mutableListOf<AppInfo>()
    private lateinit var adapter: AppAdapter

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        ShortcutManager.init(this)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyColors()
        FontSizeManager.applyFontSizeToView(binding.root)

        adapter = AppAdapter(appList,
            onClick = { app ->
                VibrationHelper.vibrateKeyPress(this)
                launchApp(app.packageName)
            },
            onLongClick = { app ->
                VibrationHelper.vibrateKeyPress(this)
                toggleShortcut(app)
            }
        )
        binding.appList.layoutManager = LinearLayoutManager(this)
        binding.appList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.appList.adapter = adapter

        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        applyColors()
        // 【修改】从"显示设置"改完字体大小返回时，页面标题等也要立即生效
        FontSizeManager.applyFontSizeToView(binding.root)
    }

    private fun applyColors() {
        val cm = ColorManager
        binding.screenArea.setBackgroundColor(cm.getScreenBg())
        binding.keysArea.setBackgroundColor(cm.getKeysBg())
        binding.tvTitle.setTextColor(cm.getScreenText())
        binding.tvEmpty.setTextColor(cm.getScreenTextDim())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)
        updateTitle()
    }

    private fun updateTitle() {
        val title = "软件"
        val hint = "（长按添加快捷应用）"
        val spannable = SpannableString(title + hint)
        // 括号部分红色加粗
        val start = title.length
        val end = spannable.length
        spannable.setSpan(ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTitle.text = spannable
    }

    private fun loadApps() {
        // 异步加载应用列表，避免主线程卡顿
        Thread {
            val tempList = mutableListOf<AppInfo>()
            try {
                val pm = packageManager
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                val resolveInfos = pm.queryIntentActivities(intent, 0)

                for (info in resolveInfos) {
                    val pkg = info.activityInfo.packageName
                    if (pkg == packageName) continue
                    val appName = info.loadLabel(pm).toString()
                    val icon = info.loadIcon(pm)
                    tempList.add(AppInfo(pkg, appName, icon))
                }
                tempList.sortBy { it.appName }
            } catch (_: Exception) {}

            runOnUiThread {
                // 【修改】页面已销毁时不再回写 UI，避免持有已销毁的 Activity
                if (isFinishing || isDestroyed) return@runOnUiThread
                appList.clear()
                appList.addAll(tempList)
                adapter.notifyDataSetChanged()
                binding.tvEmpty.visibility = if (appList.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }


    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "无法启动该应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleShortcut(app: AppInfo) {
        val shortcuts = ShortcutManager.getShortcuts()
        if (shortcuts.any { it.packageName == app.packageName }) {
            ShortcutManager.removeShortcut(app.packageName)
            // 标题已有提示，不再弹Toast
        } else {
            if (shortcuts.size >= 8) {
                Toast.makeText(this, "快捷应用最多8个，请先移除一个", Toast.LENGTH_SHORT).show()
                return
            }
            ShortcutManager.addShortcut(app.packageName, app.appName)
            // 标题已有提示，不再弹Toast
        }
    }

    // ========== 适配器 ==========

    inner class AppAdapter(
        private val items: List<AppInfo>,
        private val onClick: (AppInfo) -> Unit,
        private val onLongClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemAppBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemAppBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            // 【修改】列表行跟随"字体大小"设置（行在 onBind 时才创建，onCreate 的全量缩放覆盖不到）
            FontSizeManager.applyToListItem(holder.itemView)
            holder.binding.ivIcon.setImageDrawable(item.icon)
            holder.binding.tvName.text = item.appName
            holder.binding.tvName.setTextColor(ColorManager.getScreenText())

            // 已添加到快捷应用的标记
            val isShortcut = ShortcutManager.getShortcuts().any { it.packageName == item.packageName }
            if (isShortcut) {
                holder.binding.tvName.text = "${item.appName} ★"
            }

            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
