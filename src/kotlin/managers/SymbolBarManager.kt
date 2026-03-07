package com.venside.x1n.managers

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.venside.x1n.R
import com.venside.x1n.utils.KeyboardUtils

/**
 * 符号快捷输入栏管理器
 * 使用 PopupWindow 在软键盘上方显示符号栏
 */
class SymbolBarManager(
    private val activity: Activity,
    private val onSymbolInsert: (String) -> Unit,
    private val shouldShow: () -> Boolean
) {

    private var popupWindow: PopupWindow? = null
    private var isKeyboardVisible = false
    private var keyboardHeight = 0

    // 每行显示的符号数量
    private val symbolsPerRow = 6

    // 符号按钮尺寸
    private val buttonSizeDp = 28
    private val buttonTextSizeSp = 12f

    // 常用符号列表
    private val commonSymbols = listOf(
        "Tab", "→", "←", "↑", "↓",
        "{", "}", "(", ")", "[", "]",
        ";", ":", "=", "+", "-", "*",
        "/", "%", "<", ">", "!", "&",
        "|", "^", "~", "\"", "'",
        "`", "\\", "@", "#", "$",
        ",", ".", "?", "_"
    )

    /**
     * 初始化符号栏
     */
    fun init() {
        setupKeyboardListener()
    }

    /**
     * 设置键盘监听器
     */
    private fun setupKeyboardListener() {
        KeyboardUtils.setKeyboardListener(activity, object : KeyboardUtils.KeyboardListener {
            override fun onKeyboardShown(keyboardHeight: Int) {
                this@SymbolBarManager.keyboardHeight = keyboardHeight
                isKeyboardVisible = true
                showSymbolBar()
            }

            override fun onKeyboardHidden() {
                isKeyboardVisible = false
                hideSymbolBar()
            }
        })
    }

    /**
     * 创建符号按钮视图
     */
    private fun createSymbolButtonsView(): LinearLayout {
        val container = LinearLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dp = activity.resources.displayMetrics.density
        val buttonSizePx = (buttonSizeDp * dp).toInt()

        commonSymbols.forEach { symbol ->
            val button = Button(activity).apply {
                // Tab 按钮特殊处理
                val displayText = when (symbol) {
                    "Tab" -> "Tab"
                    else -> symbol
                }
                
                // Tab 按钮插入实际的 Tab 字符
                val insertText = when (symbol) {
                    "Tab" -> "\t"
                    else -> symbol
                }

                text = displayText
                textSize = buttonTextSizeSp
                minWidth = buttonSizePx
                minHeight = buttonSizePx
                setPadding(0, 0, 0, 0)
                setBackgroundColor(0x25CCCCCC.toInt())
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    buttonSizePx,
                    buttonSizePx
                ).apply {
                    leftMargin = 2
                    rightMargin = 2
                }

                setOnClickListener {
                    onSymbolInsert(insertText)
                }
            }

            container.addView(button)
        }

        return container
    }

    /**
     * 显示符号栏
     */
    private fun showSymbolBar() {
        if (!shouldShow()) return

        // 先关闭之前的 popupWindow
        popupWindow?.dismiss()
        popupWindow = null

        val contentView = LayoutInflater.from(activity)
            .inflate(R.layout.layout_symbol_bar, null)

        val container = contentView.findViewById<LinearLayout>(R.id.symbol_bar_container)
        container.removeAllViews()
        container.addView(createSymbolButtonsView())

        val dp = activity.resources.displayMetrics.density
        val heightPx = (40 * dp).toInt()

        popupWindow = PopupWindow(
            contentView,
            WindowManager.LayoutParams.MATCH_PARENT,
            heightPx,
            false
        ).apply {
            isFocusable = false
            isTouchable = true
            isOutsideTouchable = false
            animationStyle = android.R.style.Animation_Dialog
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }

        popupWindow?.showAtLocation(
            activity.window.decorView,
            Gravity.BOTTOM,
            0,
            keyboardHeight
        )
    }

    /**
     * 隐藏符号栏
     */
    private fun hideSymbolBar() {
        popupWindow?.dismiss()
    }

    /**
     * 销毁符号栏
     */
    fun destroy() {
        popupWindow?.dismiss()
        popupWindow = null
    }
}