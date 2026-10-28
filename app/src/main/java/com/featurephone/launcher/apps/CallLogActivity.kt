package com.featurephone.launcher.apps

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.featurephone.launcher.util.FontSizeManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.R
import com.featurephone.launcher.databinding.ActivityCallLogBinding
import com.featurephone.launcher.databinding.ItemCallLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallLogActivity : Activity() {

    private lateinit var binding: ActivityCallLogBinding
    private val REQUEST_CALL_LOG_PERMISSION = 400
    private val callLogList = mutableListOf<CallLogItem>()
    private lateinit var adapter: CallLogAdapter

    data class CallLogItem(
        val number: String,
        val name: String?,
        val type: Int,
        val date: Long,
        val duration: Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityCallLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyColors()
        FontSizeManager.applyFontSizeToView(binding.root)

        adapter = CallLogAdapter(callLogList,
            onClick = { item ->
                // 点击拨打电话
                VibrationHelper.vibrateKeyPress(this)
                // 【修改】加 try-catch，避免没有电话应用时 ActivityNotFoundException 崩溃
                try {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED) {
                        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${item.number}")))
                    } else {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.number}")))
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "无法拨号：${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onLongClick = { item ->
                VibrationHelper.vibrateKeyPress(this)
                showLongPressMenu(item)
            }
        )
        binding.callLogList.layoutManager = LinearLayoutManager(this)
        binding.callLogList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.callLogList.adapter = adapter

        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        checkPermissionsAndLoad()
    }

    override fun onResume() {
        super.onResume()
        applyColors()
        // 【修改】从"显示设置"改完字体大小返回时，页面标题等也要立即生效
        FontSizeManager.applyFontSizeToView(binding.root)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            == PackageManager.PERMISSION_GRANTED) {
            loadCallLog()
        } else {
            binding.tvEmpty.text = "请设置通话记录权限以查看记录"
            binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun applyColors() {
        val cm = ColorManager
        binding.screenArea.setBackgroundColor(cm.getScreenBg())
        binding.keysArea.setBackgroundColor(cm.getKeysBg())
        binding.tvTitle.setTextColor(cm.getScreenText())
        binding.tvEmpty.setTextColor(cm.getScreenTextDim())
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)
    }

    private fun checkPermissionsAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALL_LOG),
                REQUEST_CALL_LOG_PERMISSION
            )
        } else {
            loadCallLog()
        }
    }

    private fun loadCallLog() {
        callLogList.clear()
        binding.tvEmpty.text = "暂无通话记录"
        try {
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                null, null,
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
                val durCol = it.getColumnIndex(CallLog.Calls.DURATION)
                while (it.moveToNext()) {
                    callLogList.add(
                        CallLogItem(
                            it.getString(numCol) ?: "",
                            it.getString(nameCol),
                            it.getInt(typeCol),
                            it.getLong(dateCol),
                            it.getLong(durCol)
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            binding.tvEmpty.text = "请设置通话记录权限以查看记录"
        }

        adapter.notifyDataSetChanged()
        binding.tvEmpty.visibility = if (callLogList.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * 长按弹出菜单：添加到联系人
     */
    private fun showLongPressMenu(item: CallLogItem) {
        val options = arrayOf("添加到联系人", "复制号码", "删除该记录")
        AlertDialog.Builder(this)
            .setTitle(item.name ?: item.number)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addToContacts(item)
                    1 -> copyNumber(item)
                    2 -> deleteCallLog(item)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addToContacts(item: CallLogItem) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, item.number)
            item.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开联系人添加界面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyNumber(item: CallLogItem) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("电话号码", item.number)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "号码已复制", Toast.LENGTH_SHORT).show()
    }

    private fun deleteCallLog(item: CallLogItem) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CALL_LOG),
                401
            )
            Toast.makeText(this, "需要写入通话记录权限", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls.NUMBER}=? AND ${CallLog.Calls.DATE}=?",
                arrayOf(item.number, item.date.toString())
            )
            loadCallLog()
            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_LOG_PERMISSION && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadCallLog()
        }
    }

    // ========== 适配器 ==========

    inner class CallLogAdapter(
        private val items: List<CallLogItem>,
        private val onClick: (CallLogItem) -> Unit,
        private val onLongClick: (CallLogItem) -> Unit
    ) : RecyclerView.Adapter<CallLogAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemCallLogBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCallLogBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val b = holder.binding
            // 【修改】列表行跟随"字体大小"设置（行在 onBind 时才创建，onCreate 的全量缩放覆盖不到）
            FontSizeManager.applyToListItem(holder.itemView)

            // 通话类型图标
            b.tvType.text = when (item.type) {
                CallLog.Calls.INCOMING_TYPE -> "📞"
                CallLog.Calls.OUTGOING_TYPE -> "📱"
                CallLog.Calls.MISSED_TYPE -> "❌"
                else -> "📞"
            }

            // 名称和号码
            b.tvName.text = item.name ?: "未知号码"
            b.tvNumber.text = item.number

            // 时间
            val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            b.tvDate.text = dateFormat.format(Date(item.date))

            // 时长
            b.tvDuration.text = formatDuration(item.duration)

            // 颜色
            b.tvName.setTextColor(ColorManager.getScreenText())
            b.tvNumber.setTextColor(ColorManager.getScreenTextDim())
            b.tvDate.setTextColor(ColorManager.getScreenTextDim())
            b.tvDuration.setTextColor(ColorManager.getScreenTextDim())

            // 未接来电标红
            if (item.type == CallLog.Calls.MISSED_TYPE) {
                b.tvName.setTextColor(ColorManager.END_BG)
            }

            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }

        override fun getItemCount(): Int = items.size

        private fun formatDuration(seconds: Long): String {
            if (seconds <= 0) return "未接通"
            val min = seconds / 60
            val sec = seconds % 60
            return if (min > 0) "${min}分${sec}秒" else "${sec}秒"
        }
    }
}
