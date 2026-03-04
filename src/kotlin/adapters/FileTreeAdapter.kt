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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_file_tree, parent, false)

        val item = items[position]

        val expandIndicator = view.findViewById<ImageView>(R.id.iv_expand_indicator)
        val fileIcon = view.findViewById<ImageView>(R.id.iv_file_icon)
        val fileName = view.findViewById<TextView>(R.id.tv_file_name)
        val fileSize = view.findViewById<TextView>(R.id.tv_file_size)
        val fileModified = view.findViewById<TextView>(R.id.tv_file_modified)

        // 设置图标
        if (item.isDirectory) {
            // 文件夹使用文件夹图标
            expandIndicator?.visibility = View.VISIBLE
            expandIndicator?.setImageResource(android.R.drawable.ic_menu_add)
            expandIndicator?.rotation = 0f
            fileIcon?.visibility = View.GONE
            fileSize?.visibility = View.VISIBLE
            fileSize?.text = "${item.name}/"
            fileModified?.visibility = View.VISIBLE
        } else {
            // 文件使用文件图标
            expandIndicator?.visibility = View.GONE
            fileIcon?.visibility = View.VISIBLE
            fileIcon?.setImageResource(android.R.drawable.ic_menu_info_details)
            fileSize?.visibility = View.VISIBLE
            fileSize?.text = FileUtils.formatFileSize(item.size)
            fileModified?.visibility = View.VISIBLE
        }

        // 设置文件名
        fileName?.text = item.name
        fileName?.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)

        // 设置修改时间
        if (item.lastModified > 0) {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            fileModified?.text = formatter.format(java.util.Date(item.lastModified))
        } else {
            fileModified?.text = ""
        }

        // 检查是否是当前正在编辑的文件
        if (!item.isDirectory && currentFile != null && item.filePath == currentFile.absolutePath) {
            // 高亮当前文件
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

        // 清除缩进
        view.setPadding(8, view.paddingTop, view.paddingRight, view.paddingBottom)

        return view
    }
}