package com.venside.x1n.managers.interfaces

import android.widget.LinearLayout

/**
 * 侧边栏管理器接口
 * 负责侧边栏状态和复杂交互逻辑
 */
interface ISidebarManager {
    
    /**
     * 绑定侧边栏控件
     * @param sidebar 侧边栏控件
     */
    fun bindSidebar(sidebar: LinearLayout?)
    
    /**
     * 切换侧边栏可见性
     * @return 切换后的可见状态
     */
    fun toggleSidebar(): Boolean
    
    /**
     * 获取侧边栏可见状态
     * @return 是否可见
     */
    fun isSidebarVisible(): Boolean
    
    /**
     * 设置侧边栏可见状态
     * @param visible 是否可见
     */
    fun setSidebarVisible(visible: Boolean)
    
    /**
     * 处理侧边栏长按事件
     * @param sidebarVisible 当前侧边栏是否可见
     * @param hasRecentFiles 是否有最近文件
     * @param hasOpenFile 是否有打开的文件
     * @param onCloseAll 关闭所有文件回调
     * @param onCloseCurrent 关闭当前文件回调
     * @param onShowSettings 显示设置回调
     */
    fun handleLongPress(
        sidebarVisible: Boolean,
        hasRecentFiles: Boolean,
        hasOpenFile: Boolean,
        onCloseAll: () -> Unit,
        onCloseCurrent: () -> Unit,
        onShowSettings: () -> Unit
    )
    
    /**
     * 保存侧边栏状态到持久化存储
     */
    fun saveState()
    
    /**
     * 加载侧边栏状态从持久化存储
     */
    fun loadState()
}
