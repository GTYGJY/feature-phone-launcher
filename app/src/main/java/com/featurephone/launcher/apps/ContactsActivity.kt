package com.featurephone.launcher.apps

import android.Manifest
import com.featurephone.launcher.util.FontSizeManager
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
import com.featurephone.launcher.databinding.ActivityContactsBinding
import com.featurephone.launcher.databinding.ItemContactBinding

class ContactsActivity : Activity() {

    private lateinit var binding: ActivityContactsBinding
    private val REQUEST_CONTACTS_PERMISSION = 300
    private val contactList = mutableListOf<ContactItem>()
    private lateinit var adapter: ContactAdapter

    data class ContactItem(
        val id: String,
        val name: String,
        val number: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorManager.init(this)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyColors()
        FontSizeManager.applyFontSizeToView(binding.root)

        adapter = ContactAdapter(contactList) { item ->
            // 点击拨打电话
            // 【修改】加 try-catch：设备上没有可处理 ACTION_CALL/ACTION_DIAL 的应用时，
            // 原来会抛 ActivityNotFoundException 直接崩溃。
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${item.number}")))
                } else {
                    // 无权限时用拨号界面
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.number}")))
                }
            } catch (e: Exception) {
                Toast.makeText(this, "无法拨号：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.contactList.layoutManager = LinearLayoutManager(this)
        binding.contactList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contactList.adapter = adapter

        binding.btnAdd.setOnClickListener {
            // 打开系统添加联系人
            // 【修改】同样加异常兜底，避免没有联系人应用时崩溃
            try {
                startActivity(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI))
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开联系人添加界面", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnBack.setOnClickListener {
            VibrationHelper.vibrateKeyPress(this)
            finish()
        }

        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                // 【修改】只申请 READ_CONTACTS：添加联系人走 ACTION_INSERT，由系统联系人应用完成
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CONTACTS_PERMISSION
            )
        } else {
            loadContacts()
        }
    }

    override fun onResume() {
        super.onResume()
        applyColors()
        // 【修改】从"显示设置"改完字体大小返回时，页面标题等也要立即生效
        FontSizeManager.applyFontSizeToView(binding.root)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        } else {
            binding.tvEmpty.text = "请设置通讯录权限以查看联系人"
            binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun applyColors() {
        val cm = ColorManager
        binding.screenArea.setBackgroundColor(cm.getScreenBg())
        binding.keysArea.setBackgroundColor(cm.getKeysBg())
        binding.tvTitle.setTextColor(cm.getScreenText())
        binding.tvEmpty.setTextColor(cm.getScreenTextDim())
        binding.btnAdd.setBackgroundColor(ColorManager.CALL_BG)
        binding.btnAdd.setTextColor(ColorManager.CALL_TEXT)
        binding.btnBack.setBackgroundColor(ColorManager.END_BG)
        binding.btnBack.setTextColor(ColorManager.END_TEXT)
    }

    private fun loadContacts() {
        contactList.clear()
        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use {
                val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    contactList.add(
                        ContactItem(
                            it.getString(idCol) ?: "",
                            it.getString(nameCol) ?: "",
                            it.getString(numCol) ?: ""
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "无联系人权限", Toast.LENGTH_SHORT).show()
        }

        adapter.notifyDataSetChanged()
        binding.tvEmpty.visibility = if (contactList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CONTACTS_PERMISSION && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        }
    }

    inner class ContactAdapter(
        private val items: List<ContactItem>,
        private val onClick: (ContactItem) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            // 【修改】列表行是 onBind 时才 inflate 的，onCreate 里的全量缩放覆盖不到它们，
            // 这里按当前字体大小缩放行内文字、行高与内边距。
            FontSizeManager.applyToListItem(holder.itemView)
            holder.binding.tvName.text = item.name
            holder.binding.tvNumber.text = item.number
            holder.binding.tvName.setTextColor(ColorManager.getScreenText())
            holder.binding.tvNumber.setTextColor(ColorManager.getScreenTextDim())
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
