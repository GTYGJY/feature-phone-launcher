package com.featurephone.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.featurephone.launcher.ColorManager
import com.featurephone.launcher.R
import com.featurephone.launcher.model.AppItem
import com.featurephone.launcher.util.FontSizeManager

class MenuAdapter(
    private val items: List<AppItem>,
    private val onItemClick: (AppItem, Int) -> Unit
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    // 【修改】记录当前选中项。原实现 setSelected/moveUp/moveDown/getSelectedItem 全为空实现，
    // 导致五向键与左右软键永远操作第 0 项，且菜单没有任何选中高亮。此处补上真正的选中状态。
    private var selectedPosition = 0

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: TextView = view.findViewById(R.id.tvMenuIcon)
        val textView: TextView = view.findViewById(R.id.tvMenuItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        // 【修改】菜单行也统一走 FontSizeManager 的列表缩放：文字、行高、内边距一起放大，
        // 不再单独 setTextSize（那样行高/内边距不会变，大字体时行会挤在一起）。
        FontSizeManager.applyToListItem(holder.itemView)
        // 【修改】选中项反色显示（深色底 + 屏幕底色文字），未选中项保持原配色
        val isSelected = position == selectedPosition
        if (isSelected) {
            holder.itemView.setBackgroundColor(ColorManager.getScreenText())
            holder.textView.setTextColor(ColorManager.getScreenBg())
            holder.iconView.setTextColor(ColorManager.getScreenBg())
        } else {
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            holder.textView.setTextColor(ColorManager.getScreenText())
            holder.iconView.setTextColor(ColorManager.getScreenText())
        }
        // 图标和文字分开显示，确保对齐
        holder.iconView.text = item.icon ?: ""
        holder.textView.text = item.name
        // 统一样式
        holder.textView.setBackgroundResource(android.R.color.transparent)

        holder.itemView.setOnClickListener {
            // 【修改】点击时同步选中位置，保证软键/五向键与触摸操作一致
            setSelected(position)
            onItemClick(item, position)
        }
    }

    override fun getItemCount(): Int = items.size

    /** 【修改】设置选中项并刷新旧/新两项的显示 */
    fun setSelected(position: Int) {
        if (items.isEmpty()) return
        val newPosition = position.coerceIn(0, items.lastIndex)
        if (newPosition == selectedPosition) {
            notifyItemChanged(newPosition)
            return
        }
        val oldPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
    }

    fun getSelectedPosition(): Int =
        if (items.isEmpty()) 0 else selectedPosition.coerceIn(0, items.lastIndex)

    fun getSelectedItem(): AppItem? = items.getOrNull(getSelectedPosition())

    /** 【修改】上/下移动选择，返回新的选中位置 */
    fun moveUp(): Int {
        setSelected(getSelectedPosition() - 1)
        return getSelectedPosition()
    }

    fun moveDown(): Int {
        setSelected(getSelectedPosition() + 1)
        return getSelectedPosition()
    }
}
