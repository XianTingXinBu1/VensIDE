package com.venside.x1n.utils

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.venside.x1n.R

/**
 * 对话框助手工具类
 * 提供统一的对话框构建方法
 */
object DialogHelper {

    /**
     * 显示输入对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param hint 输入框提示文字
     * @param defaultValue 默认值（可选）
     * @param onConfirm 确认回调，参数为输入内容
     */
    fun showInputDialog(
        context: Context,
        title: String,
        hint: String,
        defaultValue: String = "",
        onConfirm: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            this.hint = hint
            setText(defaultValue)
            setPadding(
                ThemeConstants.PADDING_EDITTEXT,
                ThemeConstants.PADDING_EDITTEXT_VERTICAL,
                ThemeConstants.PADDING_EDITTEXT,
                ThemeConstants.PADDING_EDITTEXT_VERTICAL
            )
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ThemeConstants.PADDING_EDITTEXT,
                ThemeConstants.PADDING_EDITTEXT + 10,
                ThemeConstants.PADDING_EDITTEXT,
                10
            )
            addView(editText)
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    onConfirm(input)
                } else {
                    Toast.makeText(context, "请输入内容", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示确认对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param message 对话框消息
     * @param positiveText 确定按钮文字
     * @param negativeText 取消按钮文字
     * @param onConfirm 确认回调
     * @param onCancel 取消回调（可选）
     */
    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "确定",
        negativeText: String = "取消",
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(negativeText) { _, _ ->
                onCancel?.invoke()
            }
            .show()
    }

    /**
     * 显示选项对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param options 选项列表
     * @param onOptionSelected 选项选中回调，参数为选项索引
     */
    fun showOptionsDialog(
        context: Context,
        title: String,
        options: Array<String>,
        onOptionSelected: (Int) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(options) { _, which ->
                onOptionSelected(which)
            }
            .show()
    }

    /**
     * 显示消息对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param message 对话框消息
     * @param onDismiss 关闭回调（可选）
     */
    fun showMessageDialog(
        context: Context,
        title: String,
        message: String,
        onDismiss: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ ->
                onDismiss?.invoke()
            }
            .show()
    }
}