package com.venside.x1n.managers

import android.app.Activity
import android.graphics.Color
import android.graphics.PixelFormat
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
    private var symbolBarContainer: LinearLayout? = null
    private var isKeyboardVisible = false
    private var keyboardHeight = 0

    // 常用符号列表
    private val commonSymbols = listOf(
        "{", "}", "(", ")", "[", "]", ";", ":",
        "=", "+", "-", "*", "/", "%",
        "<", ">", "!", "&", "|", "^", "~",
        "\"", "'", "`", "\\", "@", "#", "$",
        ",", ".", "?", "_", "→"
    )

    /**
     * 初始化符号栏
     */
    fun init() {
        // 创建符号栏布局 - 使用正确的布局加载方式
        val inflater = LayoutInflater.from(activity)
        val rootView = inflater.inflate(R.layout.layout_symbol_bar, null)
        symbolBarContainer = rootView.findViewById<LinearLayout>(R.id.symbol_bar_container)

        // 创建符号按钮
        setupSymbolButtons()

        // 设置键盘监听器
        setupKeyboardListener()
    }

    /**
     * 设置符号按钮
     */
    private fun setupSymbolButtons() {
        val container = symbolBarContainer?.findViewById<LinearLayout>(R.id.symbol_bar_container) ?: return

        for (symbol in commonSymbols) {
            val symbolButton = Button(activity).apply {
                text = symbol
                textSize = 16f
                minWidth = 48
                minHeight = 48
                setPadding(12, 4, 12, 4)
                setBackgroundColor(0x25CCCCCC.toInt())
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(4, 0, 4, 0)
                }

                setOnClickListener {
                    onSymbolInsert(symbol)
                }
            }

            container.addView(symbolButton)
        }
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
     * 显示符号栏
     */
    private fun showSymbolBar() {
        // 检查是否应该显示符号栏（例如：是否打开了文件）
        if (!shouldShow()) {
            return
        }

        if (popupWindow == null) {
            // 获取整个符号栏视图（HorizontalScrollView）
            val symbolBarView = LayoutInflater.from(activity).inflate(R.layout.layout_symbol_bar, null)
            // 重新设置符号按钮到新的视图中
            val container = symbolBarView.findViewById<LinearLayout>(R.id.symbol_bar_container)
            container.removeAllViews()
            for (symbol in commonSymbols) {
                val symbolButton = Button(activity).apply {
                    text = symbol
                    textSize = 16f
                    minWidth = 48
                    minHeight = 48
                    setPadding(12, 4, 12, 4)
                    setBackgroundColor(0x25CCCCCC.toInt())
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        setMargins(4, 0, 4, 0)
                    }

                    setOnClickListener {
                        onSymbolInsert(symbol)
                    }
                }
                container.addView(symbolButton)
            }

            popupWindow = PopupWindow(
                symbolBarView,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
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
        }

        // 确保符号栏可见
        popupWindow?.contentView?.visibility = View.VISIBLE

        // 计算显示位置：在键盘上方
        val decorView = activity.window.decorView
        val anchorView = decorView

        // 在屏幕底部显示，偏移量为键盘高度
        popupWindow?.showAtLocation(
            anchorView,
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
        symbolBarContainer = null
    }
}
