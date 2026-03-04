package com.venside.x1n.utils

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager

/**
 * 键盘工具类
 * 用于监听软键盘的显示和隐藏
 */
object KeyboardUtils {

    /**
     * 键盘状态监听器接口
     */
    interface KeyboardListener {
        /**
         * 键盘显示
         * @param keyboardHeight 键盘高度
         */
        fun onKeyboardShown(keyboardHeight: Int)

        /**
         * 键盘隐藏
         */
        fun onKeyboardHidden()
    }

    private var keyboardHeight = 0
    private var isKeyboardVisible = false

    /**
     * 设置键盘监听器
     * @param activity 当前活动
     * @param listener 键盘监听器
     */
    fun setKeyboardListener(activity: Activity, listener: KeyboardListener) {
        val rootView = activity.findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private val rect = Rect()

            override fun onGlobalLayout() {
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.rootView.height
                val keyboardHeight = screenHeight - rect.bottom

                if (keyboardHeight > 200) { // 键盘高度阈值
                    if (!isKeyboardVisible) {
                        isKeyboardVisible = true
                        this@KeyboardUtils.keyboardHeight = keyboardHeight
                        listener.onKeyboardShown(keyboardHeight)
                    }
                } else {
                    if (isKeyboardVisible) {
                        isKeyboardVisible = false
                        listener.onKeyboardHidden()
                    }
                }
            }
        })
    }

    /**
     * 显示软键盘
     * @param view 要显示键盘的视图
     */
    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * 隐藏软键盘
     * @param activity 当前活动
     */
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * 获取键盘高度
     * @return 键盘高度
     */
    fun getKeyboardHeight(): Int = keyboardHeight

    /**
     * 判断键盘是否可见
     * @return 是否可见
     */
    fun isKeyboardVisible(): Boolean = isKeyboardVisible
}