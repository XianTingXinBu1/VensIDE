package com.venside.x1n.managers

import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.venside.x1n.managers.interfaces.IUIStateManager
import com.venside.x1n.utils.LineNumberHelper
import com.venside.x1n.utils.PathDisplayHelper

/**
 * UI状态管理器
 * 负责UI控件的状态同步和更新
 */
class UIStateManager(
    private val fileManager: FileManager,
    private val editorManager: EditorManager,
    private val workspacePath: String?
) : IUIStateManager {

    private val pathDisplayHelper: PathDisplayHelper by lazy {
        PathDisplayHelper(fileManager, editorManager, workspacePath)
    }
    private val lineNumberHelper = LineNumberHelper()

    override fun updatePathDisplay(pathTextView: TextView?, sidebarPathTextView: TextView?) {
        pathDisplayHelper.updatePathDisplay(pathTextView, sidebarPathTextView)
    }

    override fun updateLineNumbers(editorContent: EditText?, lineNumbers: TextView?) {
        lineNumberHelper.updateLineNumbers(editorContent, lineNumbers)
    }

    override fun updateWordCount(
        editorContent: EditText?,
        wordCountBar: LinearLayout?,
        wordCountTextView: TextView?,
        sidebarVisible: Boolean,
        isFileOpen: Boolean
    ) {
        val content = editorContent?.text.toString()
        
        if (isFileOpen) {
            wordCountBar?.visibility = View.VISIBLE

            if (!sidebarVisible) {
                val charCount = content.length
                val lineCount = content.lines().size
                wordCountTextView?.text = "$charCount 字符 | $lineCount 行"
            } else {
                wordCountTextView?.text = ""
            }
        } else {
            wordCountBar?.visibility = View.GONE
        }
    }

    override fun clearEditor(
        emptyState: TextView?,
        editorScroll: ScrollView?,
        editorContent: EditText?,
        lineNumbers: TextView?,
        wordCountBar: LinearLayout?
    ) {
        editorManager.closeFile()
        emptyState?.visibility = View.VISIBLE
        editorScroll?.visibility = View.GONE
        editorContent?.text?.clear()
        lineNumbers?.text = ""
        wordCountBar?.visibility = View.GONE
    }

    override fun updateFileSizeInTree() {
        // 此方法需要由 Activity 调用，因为需要访问 fileTree 和 fileTreeAdapter
        // 在管理器中只提供逻辑，具体实现由 Activity 处理
    }

    override fun insertSymbol(editorContent: EditText?, symbol: String, onInserted: () -> Unit) {
        if (editorContent == null) return

        val start = editorContent.selectionStart
        val end = editorContent.selectionEnd

        if (start >= 0 && end >= 0) {
            val editable = editorContent.editableText
            editable.replace(start, end, symbol)
            onInserted()
        }
    }

    override fun getShortPath(): String {
        return pathDisplayHelper.getShortPath()
    }
}
