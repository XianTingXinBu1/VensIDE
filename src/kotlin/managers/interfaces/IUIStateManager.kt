package com.venside.x1n.managers.interfaces

import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * UI状态管理器接口
 * 负责UI控件的状态同步和更新
 */
interface IUIStateManager {
    
    /**
     * 更新路径显示
     * @param pathTextView 路径文本视图
     * @param sidebarPathTextView 侧边栏路径文本视图
     */
    fun updatePathDisplay(pathTextView: TextView?, sidebarPathTextView: TextView?)
    
    /**
     * 更新行号
     * @param editorContent 编辑器内容控件
     * @param lineNumbers 行号控件
     */
    fun updateLineNumbers(editorContent: EditText?, lineNumbers: TextView?)
    
    /**
     * 更新字数统计
     * @param editorContent 编辑器内容控件
     * @param wordCountBar 字数统计栏
     * @param wordCountTextView 字数统计文本视图
     * @param sidebarVisible 侧边栏是否可见
     * @param isFileOpen 是否有文件打开
     */
    fun updateWordCount(
        editorContent: EditText?,
        wordCountBar: LinearLayout?,
        wordCountTextView: TextView?,
        sidebarVisible: Boolean,
        isFileOpen: Boolean
    )
    
    /**
     * 清空编辑器
     * @param emptyState 空状态视图
     * @param editorScroll 编辑器滚动视图
     * @param editorContent 编辑器内容控件
     * @param lineNumbers 行号控件
     * @param wordCountBar 字数统计栏
     */
    fun clearEditor(
        emptyState: TextView?,
        editorScroll: android.widget.ScrollView?,
        editorContent: EditText?,
        lineNumbers: TextView?,
        wordCountBar: LinearLayout?
    )
    
    /**
     * 更新文件大小（在文件树中）
     */
    fun updateFileSizeInTree()
    
    /**
     * 插入符号到编辑器
     * @param editorContent 编辑器内容控件
     * @param symbol 要插入的符号
     * @param onInserted 插入后回调
     */
    fun insertSymbol(
        editorContent: EditText?,
        symbol: String,
        onInserted: () -> Unit
    )
    
    /**
     * 获取短路径
     * @return 短路径字符串
     */
    fun getShortPath(): String
}
