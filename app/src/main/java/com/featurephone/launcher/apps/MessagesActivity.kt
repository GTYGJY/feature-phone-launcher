package com.featurephone.launcher.apps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.util.VibrationHelper
import com.featurephone.launcher.util.FontSizeManager
import com.featurephone.launcher.R
import com.featurephone.launcher.databinding.ActivityMessagesBinding
import com.featurephone.launcher.databinding.ItemSmsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesActivity : Activity() {

    private lateinit var binding: ActivityMessagesBinding
    private val REQUEST_SMS_PERMISSION = 200
    private val smsList = mutableListOf<SmsItem>()
    private lateinit var adapter: SmsAdapter

    data class SmsItem(
        val address: String,
        val body: String,
        val date: Long,
        val type: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FontSizeManager.applyFontSizeToView(binding.root)
        applyColors()

        adapter = SmsAdapter(smsList) { item ->
            // 点击查看/回复
            // 【修改】加 try-catch：没有短信应用时 ACTION_SENDTO 会抛 ActivityNotFoundException
            try {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${item.address}"))
                intent.putExtra("sms_body", "")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "未找到可用的短信应用", Toast.LENGTH_SHORT).show()
            }
        }
        binding.smsList.layoutManager = LinearLayoutManager(this)
        binding.smsList.adapter = adapter

        binding.btnNew.setOnClickListener {
            // 【修改】同样加异常兜底
            try {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
            } catch (e: Exception) {
                Toast.makeText(this, "未找到可用的短信应用", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                // 【修改】只申请 READ_SMS：发送短信走 ACTION_SENDTO，由系统短信应用完成
                arrayOf(Manifest.permission.READ_SMS),
                REQUEST_SMS_PERMISSION
            )
        } else {
            loadSms()
        }
    }

    override fun onResume() {
        super.onResume()
        applyColors()
        // 【修改】从"显示设置"改完字体大小返回时，页面标题等也要立即生效
        FontSizeManager.applyFontSizeToView(binding.root)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED) {
            loadSms()
        } else {
            binding.tvEmpty.text = "请设置短信权限以查看短信"
            binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun applyColors() {
        val cm = ColorManager
        binding.screenArea.setBackgroundColor(cm.getScreenBg())
        binding.keysArea.setBackgroundColor(cm.getKeysBg())
        binding.tvTitle.setTextColor(cm.getScreenText())
        binding.tvEmpty.setTextColor(cm.getScreenTextDim())
        binding.btnNew.setBackgroundColor(ColorManager.CALL_BG)
        binding.btnNew.setTextColor(ColorManager.CALL_TEXT)
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)
    }

    private fun loadSms() {
        smsList.clear()
        try {
            val cursor: Cursor? = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
                null, null, "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                val addrCol = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = it.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = it.getColumnIndex(Telephony.Sms.DATE)
                val typeCol = it.getColumnIndex(Telephony.Sms.TYPE)
                while (it.moveToNext()) {
                    smsList.add(
                        SmsItem(
                            it.getString(addrCol) ?: "",
                            it.getString(bodyCol) ?: "",
                            it.getLong(dateCol),
                            it.getInt(typeCol)
                        )
                    )
                    if (smsList.size >= 50) break
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "无短信权限", Toast.LENGTH_SHORT).show()
        }

        adapter.notifyDataSetChanged()
        binding.tvEmpty.visibility = if (smsList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSION && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSms()
        }
    }

    inner class SmsAdapter(
        private val items: List<SmsItem>,
        private val onClick: (SmsItem) -> Unit
    ) : RecyclerView.Adapter<SmsAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemSmsBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSmsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            // 【修改】列表行跟随"字体大小"设置（行在 onBind 时才创建，onCreate 的全量缩放覆盖不到）
            FontSizeManager.applyToListItem(holder.itemView)
            holder.binding.tvAddress.text = item.address
            holder.binding.tvBody.text = item.body
            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            holder.binding.tvDate.text = sdf.format(Date(item.date))
            // 应用颜色
            holder.binding.tvAddress.setTextColor(ColorManager.getScreenText())
            holder.binding.tvBody.setTextColor(ColorManager.getScreenTextDim())
            holder.binding.tvDate.setTextColor(ColorManager.getScreenTextDim())
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
