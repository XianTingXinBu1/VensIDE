package com.venside.x1n.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.venside.x1n.R
import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.FileUtils
import com.venside.x1n.utils.ThemeConstants
import java.io.File

class FileTreeAdapter(
    context: Activity,
    private val items: List<FileTreeItem>,
    private val currentFile: File? = null
) : ArrayAdapter<FileTreeItem>(context, R.layout.item_file_tree, items) {

    var listView: android.widget.ListView? = null
    var isSelectionMode: Boolean = false

    fun setSelectionMode(mode: Boolean) {
        isSelectionMode = mode
        notifyDataSetChanged()
    }

    fun isSelectionMode(): Boolean = isSelectionMode

    fun toggleSelection(position: Int): Boolean {
        items[position].isSelected = !items[position].isSelected
        notifyDataSetChanged()
        return items[position].isSelected
    }

    /**
     * 检查是否有选中的项
     */
    fun hasSelection(): Boolean {
        return items.any { it.isSelected && !it.isParentDir }
    }

    fun getSelectedItems(): List<FileTreeItem> {
        return items.filter { it.isSelected && !it.isParentDir }
    }

    fun clearSelection() {
        items.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_file_tree, parent, false)

        val item = items[position]

        val expandIndicator = view.findViewById<ImageView>(R.id.iv_expand_indicator)
        val fileIcon = view.findViewById<ImageView>(R.id.iv_file_icon)
        val fileName = view.findViewById<TextView>(R.id.tv_file_name)
        val fileSize = view.findViewById<TextView>(R.id.tv_file_size)
        val fileModified = view.findViewById<TextView>(R.id.tv_file_modified)

        // 设置图标（文件夹和文件统一大小）
        if (item.isDirectory) {
            // 文件夹使用文件夹图标
            expandIndicator?.visibility = if (item.isParentDir) View.VISIBLE else View.GONE
            expandIndicator?.setImageResource(android.R.drawable.ic_menu_add)
            expandIndicator?.rotation = 0f
            fileIcon?.visibility = View.VISIBLE
            fileIcon?.setImageResource(android.R.drawable.ic_menu_sort_alphabetically)
            fileIcon?.setColorFilter(android.graphics.Color.parseColor("#FFC107"))
            fileSize?.visibility = View.VISIBLE
            fileSize?.text = "${item.name}/"
            fileModified?.visibility = View.VISIBLE
        } else {
            // 文件使用文件图标
            expandIndicator?.visibility = View.GONE
            fileIcon?.visibility = View.VISIBLE
            fileIcon?.setImageResource(android.R.drawable.ic_menu_info_details)
            fileIcon?.setColorFilter(android.graphics.Color.parseColor("#CCCCCC"))
            fileSize?.visibility = View.VISIBLE
            fileSize?.text = FileUtils.formatFileSize(item.size)
            fileModified?.visibility = View.VISIBLE
        }

        // 设置文件名（统一大小）
        fileName?.text = item.name
        fileName?.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)

        // 设置修改时间
        if (item.lastModified > 0) {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            fileModified?.text = formatter.format(java.util.Date(item.lastModified))
        } else {
            fileModified?.text = ""
        }

        // 选择模式下，不显示打开文件的高亮（避免混淆），只显示选择高亮
        if (item.isSelected) {
            // 选择状态 - 暗蓝色
            view.setBackgroundColor(ThemeConstants.COLOR_SELECTION_BG)
            fileName?.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
            fileSize?.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
            fileModified?.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
        } else if (!item.isDirectory && currentFile != null && item.filePath == currentFile.absolutePath && !isSelectionMode) {
            // 打开状态 - 亮蓝色（仅在非选择模式下显示）
            view.setBackgroundColor(ThemeConstants.COLOR_TAG_BG_SELECTED)
            fileName?.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
            fileSize?.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
            fileModified?.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
        } else {
            view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            fileName?.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)
            fileSize?.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)
            fileModified?.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)
        }

        return view
    }
}