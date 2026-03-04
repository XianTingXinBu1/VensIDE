package com.venside.x1n.managers

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.venside.x1n.R
import com.venside.x1n.managers.interfaces.ISidebarManager
import com.venside.x1n.utils.DialogHelper

/**
 * 侧边栏管理器
 * 负责侧边栏状态和复杂交互逻辑
 */
class SidebarManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) : ISidebarManager {

    private var sidebarVisible = true
    private var sidebar: LinearLayout? = null

    override fun bindSidebar(sidebar: LinearLayout?) {
        this.sidebar = sidebar
    }

    override fun toggleSidebar(): Boolean {
        sidebarVisible = !sidebarVisible
        sidebar?.visibility = if (sidebarVisible) View.VISIBLE else View.GONE
        saveState()
        return sidebarVisible
    }

    override fun isSidebarVisible(): Boolean = sidebarVisible

    override fun setSidebarVisible(visible: Boolean) {
        sidebarVisible = visible
        sidebar?.visibility = if (sidebarVisible) View.VISIBLE else View.GONE
    }

    override fun handleLongPress(
        sidebarVisible: Boolean,
        hasRecentFiles: Boolean,
        hasOpenFile: Boolean,
        onCloseAll: () -> Unit,
        onCloseCurrent: () -> Unit,
        onShowSettings: () -> Unit
    ) {
        if (!sidebarVisible) {
            // 侧边栏未显示时
            when {
                hasRecentFiles -> {
                    DialogHelper.showConfirmDialog(
                        context = context,
                        title = "关闭所有最近文件",
                        message = "确定要关闭所有最近打开的文件和正在编辑的文件吗？",
                        onConfirm = {
                            onCloseAll()
                            Toast.makeText(context, "已关闭所有文件", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                hasOpenFile -> {
                    DialogHelper.showConfirmDialog(
                        context = context,
                        title = "关闭正在编辑的文件",
                        message = "确定要关闭正在编辑的文件吗？",
                        onConfirm = {
                            onCloseCurrent()
                            Toast.makeText(context, "已关闭正在编辑的文件", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                else -> {
                    Toast.makeText(context, "没有打开的文件", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // 侧边栏显示时，进入资源管理器设置
            onShowSettings()
        }
    }

    override fun saveState() {
        preferencesManager.saveSidebarState(sidebarVisible)
    }

    override fun loadState() {
        sidebarVisible = preferencesManager.getSidebarState()
        sidebar?.visibility = if (sidebarVisible) View.VISIBLE else View.GONE
    }
}
