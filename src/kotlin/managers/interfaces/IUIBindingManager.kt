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
     * 绑定运行按钮
     * @param onRun 执行回调
     */
    fun bindRunButton(onRun: () -> Unit)

    /**
     * 绑定菜单按钮
     * @param onClick 点击回调
     */
    fun bindMenuButton(onClick: () -> Unit)
    
    /**
     * 绑定撤销/重做按钮
     * @param onUndo 撤销回调
     * @param onRedo 重做回调
     */
    fun bindUndoRedoButtons(onUndo: () -> Unit, onRedo: () -> Unit)
    
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
     * @param onTextChanged 文本变化回调 (start, before, count)
     */
    fun bindEditorListeners(
        onEditorClick: () -> Unit, 
        onContentChanged: () -> Unit,
        onTextChanged: (start: Int, before: Int, count: Int) -> Unit = { _, _, _ -> }
    )
    
    /**
     * 绑定字数统计按钮
     * @param onClick 点击回调
     */
    fun bindWordCountButton(onClick: () -> Unit)

    /**
     * 显示撤销/重做按钮栏
     */
    fun showUndoRedoBar()

    /**
     * 隐藏撤销/重做按钮栏
     */
    fun hideUndoRedoBar()
    
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
    
    /**
     * 获取撤销按钮
     */
    fun getUndoButton(): android.widget.ImageView?
    
    /**
     * 获取重做按钮
     */
    fun getRedoButton(): android.widget.ImageView?
    
    /**
     * 获取终端面板
     */
    fun getTerminalPanel(): android.widget.LinearLayout?
    
    /**
     * 获取终端输出视图
     */
    fun getTerminalOutput(): TextView?

    /**
     * 获取菜单按钮
     */
    fun getMenuButton(): android.widget.ImageView?
}
