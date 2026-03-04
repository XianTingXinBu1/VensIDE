package com.venside.x1n.managers

import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.venside.x1n.utils.ThemeConstants
import java.io.File

class RecentFilesBarManager(
    private val activity: Activity,
    private val recentFilesManager: RecentFilesManager,
    private val editorManager: EditorManager
) {

    /**
     * 更新最近文件栏
     */
    fun updateRecentFilesBar(
        container: LinearLayout,
        onFileClick: (File) -> Unit,
        onDeleteClick: (File, Int) -> Unit
    ) {
        // 保留第一个 TextView（"最近打开: "）
        val titleView = container.getChildAt(0) as? TextView
        container.removeAllViews()
        titleView?.let { container.addView(it) }

        // 添加最近文件标签
        for ((index, file) in recentFilesManager.getRecentFiles().withIndex()) {
            val fileLayout = createFileTag(file, index, onFileClick, onDeleteClick)
            container.addView(fileLayout)
        }
    }

    /**
     * 创建文件标签
     */
    private fun createFileTag(
        file: File,
        index: Int,
        onFileClick: (File) -> Unit,
        onDeleteClick: (File, Int) -> Unit
    ): LinearLayout {
        // 创建扁平长方形按钮容器，固定宽度 240dp
        val fileLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val layoutParams = LinearLayout.LayoutParams(240, LinearLayout.LayoutParams.MATCH_PARENT)
            layoutParams.setMargins(0, 0, 8, 0)
            this.layoutParams = layoutParams
        }

        // 高亮指示器（左边框）
        val indicator = android.view.View(activity).apply {
            val indLayoutParams = LinearLayout.LayoutParams(3, LinearLayout.LayoutParams.MATCH_PARENT)
            this.layoutParams = indLayoutParams
        }

        // 文件名显示，使用权重布局，长文件名省略但保留后缀
        val fileTag = createFileNameTag(file, onFileClick)
        // 关闭按钮，固定宽度
        val deleteBtn = createDeleteButton(file, index, onDeleteClick)

        // 高亮当前打开的文件
        if (editorManager.isCurrentFile(file)) {
            highlightFileTag(fileLayout, indicator, fileTag, deleteBtn)
        } else {
            unhighlightFileTag(fileLayout, indicator, fileTag, deleteBtn)
        }

        fileLayout.addView(indicator)
        fileLayout.addView(fileTag)
        fileLayout.addView(deleteBtn)

        return fileLayout
    }

    /**
     * 创建文件名标签
     */
    private fun createFileNameTag(
        file: File,
        onFileClick: (File) -> Unit
    ): TextView {
        val fileName = file.name
        val extension = if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf("."))
        } else {
            ""
        }
        val baseName = if (fileName.contains(".")) {
            fileName.substring(0, fileName.lastIndexOf("."))
        } else {
            fileName
        }

        // 如果文件名过长，截取前面并保留后缀
        val displayText = if (fileName.length > 10) {
            val maxLength = 8
            if (baseName.length > maxLength) {
                baseName.substring(0, maxLength) + "..." + extension
            } else {
                fileName
            }
        } else {
            fileName
        }

        return TextView(activity).apply {
            text = displayText
            textSize = 12f
            setPadding(8, 0, 0, 0)
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
            // 使用权重布局，确保叉号有空间
            val tagLayoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            this.layoutParams = tagLayoutParams
            setOnClickListener {
                onFileClick(file)
            }

            // 添加拖拽功能
            setOnLongClickListener {
                startDrag(this.parent as LinearLayout)
                true
            }
        }
    }

    /**
     * 创建删除按钮
     */
    private fun createDeleteButton(
        file: File,
        index: Int,
        onDeleteClick: (File, Int) -> Unit
    ): TextView {
        return TextView(activity).apply {
            text = "×"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(4, 0, 8, 0)
            minWidth = 30
            setOnClickListener {
                onDeleteClick(file, index)
            }
        }
    }

    /**
     * 高亮文件标签
     */
    private fun highlightFileTag(
        fileLayout: LinearLayout,
        indicator: android.view.View,
        fileTag: TextView,
        deleteBtn: TextView
    ) {
        fileLayout.setBackgroundColor(ThemeConstants.COLOR_TAG_BG_SELECTED)
        indicator.setBackgroundColor(0xFFFFFFFF.toInt())
        fileTag.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
        deleteBtn.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
    }

    /**
     * 取消高亮文件标签
     */
    private fun unhighlightFileTag(
        fileLayout: LinearLayout,
        indicator: android.view.View,
        fileTag: TextView,
        deleteBtn: TextView
    ) {
        fileLayout.setBackgroundColor(ThemeConstants.COLOR_TAG_BG)
        indicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        fileTag.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)
        deleteBtn.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)
    }

    /**
     * 开始拖拽
     */
    private fun startDrag(view: View) {
        view.alpha = ThemeConstants.DRAG_ALPHA
        val shadowBuilder = android.view.View.DragShadowBuilder(view)
        view.startDragAndDrop(null, shadowBuilder, view, 0)
    }
}