package com.venside.x1n.managers

import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 终端管理器 - 负责管理终端输出、历史记录和显示控制
 * 
 * 职责：
 * - 管理终端历史记录
 * - 控制终端面板的显示/隐藏
 * - 格式化和显示终端输出
 * - 清空终端功能
 */
class TerminalManager {

    private val terminalHistory = mutableListOf<String>()
    
    private var terminalPanel: View? = null
    private var terminalOutput: TextView? = null

    /**
     * 初始化终端管理器，绑定 UI 组件
     */
    fun init(terminalPanel: View, terminalOutput: TextView) {
        this.terminalPanel = terminalPanel
        this.terminalOutput = terminalOutput
    }

    /**
     * 切换终端显示/隐藏状态
     */
    fun toggleTerminal() {
        if (terminalPanel?.visibility == View.GONE) {
            showTerminal()
        } else {
            hideTerminal()
        }
    }

    /**
     * 显示终端面板
     */
    fun showTerminal() {
        terminalPanel?.visibility = View.VISIBLE
    }

    /**
     * 隐藏终端面板
     */
    fun hideTerminal() {
        terminalPanel?.visibility = View.GONE
    }

    /**
     * 清空终端内容
     * @param clearHistory 是否同时清空历史记录，默认为 true
     */
    fun clearTerminal(clearHistory: Boolean = true) {
        if (clearHistory) {
            terminalHistory.clear()
        }
        
        if (terminalHistory.isEmpty()) {
            terminalOutput?.text = "等待执行..."
        } else {
            terminalOutput?.text = terminalHistory.joinToString("\n")
        }
    }

    /**
     * 添加终端输出内容
     * @param text 输出文本
     * @param isError 是否为错误信息，影响文本颜色
     */
    fun appendTerminalOutput(text: String, isError: Boolean = false) {
        val output = terminalOutput ?: return
        
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newText = "[$timestamp] $text"

        // 添加到历史记录
        terminalHistory.add(newText)

        // 更新显示
        output.text = terminalHistory.joinToString("\n")
        output.setTextColor(
            if (isError) 0xFFFF5252.toInt() else 0xFF4CAF50.toInt()
        )

        // 自动滚动到底部
        (output.parent as? ScrollView)?.post {
            (output.parent as? ScrollView)?.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    /**
     * 获取终端历史记录
     */
    fun getHistory(): List<String> = terminalHistory.toList()

    /**
     * 检查终端是否可见
     */
    fun isVisible(): Boolean = terminalPanel?.visibility == View.VISIBLE

    /**
     * 检查是否有历史记录
     */
    fun hasHistory(): Boolean = terminalHistory.isNotEmpty()
}