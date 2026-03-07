package com.venside.x1n.components

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.venside.x1n.engine.WorkspaceEngine
import com.venside.x1n.managers.UIStateManager
import com.venside.x1n.utils.LineNumberHelper

/**
 * 编辑器组件 - 封装所有编辑器相关的逻辑
 * 
 * 职责：
 * - 文本变更记录和撤销重做
 * - 行号更新
 * - 字数统计
 * - 撤销重做按钮状态管理
 */
class EditorComponent(
    private val engine: WorkspaceEngine,
    private val uiStateManager: UIStateManager
) {

    private val lineNumberHelper = LineNumberHelper()
    private var previousContent: String = ""
    private var isUndoRedoAction: Boolean = false

    // 缩放相关变量
    private var scaleGestureDetector: android.view.ScaleGestureDetector? = null
    private var currentTextSize = 14f
    private var minTextSize = 8f
    private var maxTextSize = 32f
    
    // UI 控件引用
    private var editorContent: EditText? = null
    private var lineNumbers: TextView? = null
    private var undoButton: ImageView? = null
    private var redoButton: ImageView? = null
    private var wordCountBar: LinearLayout? = null
    private var wordCountTextView: TextView? = null
    
    // 回调接口
    var onLineNumbersUpdate: (() -> Unit)? = null
    var onWordCountUpdate: (() -> Unit)? = null
    
    /**
     * 初始化编辑器组件
     */
    fun init(
        editorContent: EditText?,
        lineNumbers: TextView?,
        undoButton: ImageView?,
        redoButton: ImageView?,
        wordCountBar: LinearLayout?,
        wordCountTextView: TextView?
    ) {
        this.editorContent = editorContent
        this.lineNumbers = lineNumbers
        this.undoButton = undoButton
        this.redoButton = redoButton
        this.wordCountBar = wordCountBar
        this.wordCountTextView = wordCountTextView

        setupTextWatcher()
        setupScaleGestureDetector()
        updateUndoRedoButtons()
    }
    
    /**
     * 设置文本监听器
     */
    private fun setupTextWatcher() {
        editorContent?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recordTextChange(start, before, count)
            }
            
            override fun afterTextChanged(s: Editable?) {
                updateLineNumbers()
                updateWordCount()
            }
        })
    }
    
    /**
     * 记录文本变更
     */
    private fun recordTextChange(start: Int, before: Int, count: Int) {
        if (isUndoRedoAction) return
        
        val currentContent = editorContent?.text?.toString() ?: return
        
        // 边界检查：确保 substring 不会越界
        val safeStart = start.coerceIn(0, previousContent.length)
        val safeEnd = (start + before).coerceIn(0, previousContent.length)

        when {
            before > 0 && count == 0 && safeEnd > safeStart -> {
                // 删除操作
                val deletedText = previousContent.substring(safeStart, safeEnd)
                engine.recordDelete(safeStart, deletedText)
            }
            count > 0 && before == 0 -> {
                // 插入操作
                val insertedText = currentContent.substring(start, (start + count).coerceAtMost(currentContent.length))
                engine.recordInsert(start, insertedText)
            }
            before > 0 && count > 0 && safeEnd > safeStart -> {
                // 替换操作（记录为删除+插入）
                val deletedText = previousContent.substring(safeStart, safeEnd)
                val insertedText = currentContent.substring(start, (start + count).coerceAtMost(currentContent.length))
                engine.recordDelete(safeStart, deletedText)
                engine.recordInsert(start, insertedText)
            }
        }

        previousContent = currentContent
        updateUndoRedoButtons()
    }
    
    /**
     * 执行撤销
     */
    fun performUndo() {
        val currentContent = editorContent?.text?.toString() ?: return

        val newContent = engine.undo(currentContent)
        if (newContent != null) {
            isUndoRedoAction = true
            editorContent?.setText(newContent)
            previousContent = newContent
            isUndoRedoAction = false
            updateLineNumbers()
            updateWordCount()
        }
        updateUndoRedoButtons()
    }
    
    /**
     * 执行重做
     */
    fun performRedo() {
        val currentContent = editorContent?.text?.toString() ?: return

        val newContent = engine.redo(currentContent)
        if (newContent != null) {
            isUndoRedoAction = true
            editorContent?.setText(newContent)
            previousContent = newContent
            isUndoRedoAction = false
            updateLineNumbers()
            updateWordCount()
        }
        updateUndoRedoButtons()
    }
    
    /**
     * 更新撤销重做按钮状态
     */
    private fun updateUndoRedoButtons() {
        undoButton?.let { btn ->
            val canUndo = engine.canUndo()
            btn.isEnabled = canUndo
            btn.imageAlpha = if (canUndo) 255 else 128
            btn.setColorFilter(if (canUndo) 0xFFCCCCCC.toInt() else 0xFF666666.toInt())
        }

        redoButton?.let { btn ->
            val canRedo = engine.canRedo()
            btn.isEnabled = canRedo
            btn.imageAlpha = if (canRedo) 255 else 128
            btn.setColorFilter(if (canRedo) 0xFFCCCCCC.toInt() else 0xFF666666.toInt())
        }
    }
    
    /**
     * 更新行号
     */
    private fun updateLineNumbers() {
        lineNumberHelper.updateLineNumbers(editorContent, lineNumbers)
        onLineNumbersUpdate?.invoke()
    }
    
    /**
     * 更新字数统计
     */
    private fun updateWordCount() {
        // 由外部管理，因为需要侧边栏状态
        onWordCountUpdate?.invoke()
    }
    
    /**
     * 重置撤销重做状态
     */
    fun reset() {
        engine.resetUndoRedo()
        previousContent = ""
        updateUndoRedoButtons()
    }
    
    /**
     * 获取编辑器内容
     */
    fun getContent(): String {
        return editorContent?.text?.toString() ?: ""
    }
    
    /**
     * 设置编辑器内容
     */
    fun setContent(content: String) {
        editorContent?.setText(content)
        previousContent = content
        updateLineNumbers()
    }
    
    /**
     * 插入符号
     */
    fun insertSymbol(symbol: String) {
        if (editorContent == null) return

        val start = editorContent!!.selectionStart
        val end = editorContent!!.selectionEnd

        if (start >= 0 && end >= 0) {
            val editable = editorContent!!.editableText
            editable.replace(start, end, symbol)
            updateLineNumbers()
        }
    }
    
    /**
     * 清空编辑器
     */
    fun clear() {
        editorContent?.text?.clear()
        lineNumbers?.text = ""
        wordCountBar?.visibility = LinearLayout.GONE
        reset()
    }

    /**
     * 设置缩放手势检测器
     */
    private fun setupScaleGestureDetector() {
        scaleGestureDetector = android.view.ScaleGestureDetector(
            engine.appContext,
            object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    val newTextSize = currentTextSize * scaleFactor

                    // 限制字体大小范围
                    if (newTextSize >= minTextSize && newTextSize <= maxTextSize) {
                        currentTextSize = newTextSize
                        updateTextSize()
                    }

                    return true
                }
            }
        )
    }

    /**
     * 处理触摸事件
     */
    fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        scaleGestureDetector?.onTouchEvent(event)
        return false // 让编辑器继续处理触摸事件
    }

    /**
     * 更新编辑器和行号的字体大小
     */
    private fun updateTextSize() {
        editorContent?.textSize = currentTextSize
        lineNumbers?.textSize = currentTextSize
    }

    /**
     * 重置字体大小
     */
    fun resetTextSize() {
        currentTextSize = 14f
        updateTextSize()
    }

    /**
     * 更新字数显示
     */
    fun updateWordCountDisplay(sidebarVisible: Boolean, isFileOpen: Boolean) {
        val content = editorContent?.text.toString()
        
        if (isFileOpen) {
            wordCountBar?.visibility = LinearLayout.VISIBLE

            if (!sidebarVisible) {
                val charCount = content.length
                val lineCount = content.lines().size
                wordCountTextView?.text = "$charCount 字符 | $lineCount 行"
            } else {
                wordCountTextView?.text = ""
            }
        } else {
            wordCountBar?.visibility = LinearLayout.GONE
        }
    }
}