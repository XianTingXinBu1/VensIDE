package com.venside.x1n.managers.interfaces

import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView

/**
 * UI 绑定管理器接口
 * 负责控件绑定和监听器设置
 */
interface IUIBindingManager {
    
    /**
     * 绑定侧边栏切换按钮
     * @param onToggle 切换回调
     * @param onLongPress 长按回调
     */
    fun bindSidebarToggle(onToggle: () -> Unit, onLongPress: () -> Unit)
    
    /**
     * 绑定导航按钮
     * @param onBack 返回回调
     * @param onSave 保存回调
     */
    fun bindNavigationButtons(onBack: () -> Unit, onSave: () -> Unit)
    
    /**
     * 绑定路径显示控件
     * @param onLongPress 长按复制路径回调
     * @param onClick 点击回调
     */
    fun bindPathDisplay(onLongPress: () -> Boolean, onClick: () -> Unit)
    
    /**
     * 绑定拖拽功能
     * @param onMove 移动回调
     */
    fun bindDragDrop(onMove: (fromIndex: Int, toIndex: Int) -> Unit)
    
    /**
     * 绑定新建按钮
     * @param onClick 点击回调
     */
    fun bindNewButton(onClick: () -> Unit)
    
    /**
     * 绑定文件树列表
     * @param onItemClick 点击回调
     * @param onItemLongClick 长按回调
     * @param onTouch 触摸回调
     */
    fun bindFileTree(
        onItemClick: (position: Int) -> Unit,
        onItemLongClick: (position: Int) -> Boolean,
        onTouch: (event: android.view.MotionEvent) -> Boolean
    )
    
    /**
     * 绑定编辑器区域监听器
     * @param onEditorClick 编辑器点击回调
     * @param onContentChanged 内容变化回调
     */
    fun bindEditorListeners(onEditorClick: () -> Unit, onContentChanged: () -> Unit)
    
    /**
     * 绑定字数统计按钮
     * @param onClick 点击回调
     */
    fun bindWordCountButton(onClick: () -> Unit)
    
    /**
     * 获取编辑器内容控件
     */
    fun getEditorContent(): EditText?
    
    /**
     * 获取文件树列表视图
     */
    fun getFileTreeView(): ListView?
    
    /**
     * 获取侧边栏控件
     */
    fun getSidebar(): LinearLayout?
    
    /**
     * 获取空状态视图
     */
    fun getEmptyState(): TextView?
    
    /**
     * 获取编辑器滚动视图
     */
    fun getEditorScroll(): ScrollView?
    
    /**
     * 获取行号控件
     */
    fun getLineNumbers(): TextView?
    
    /**
     * 获取最近文件容器
     */
    fun getRecentFilesContainer(): LinearLayout?
    
    /**
     * 获取字数统计栏
     */
    fun getWordCountBar(): LinearLayout?
    
    /**
     * 获取路径文本视图
     */
    fun getPathTextView(): TextView?
    
    /**
     * 获取侧边栏路径文本视图
     */
    fun getSidebarPathTextView(): TextView?
    
    /**
     * 获取字数统计文本视图
     */
    fun getWordCountTextView(): TextView?
}
