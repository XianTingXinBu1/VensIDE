package com.venside.x1n.utils

import android.widget.EditText
import android.widget.TextView

/**
 * 行号辅助类
 */
class LineNumberHelper {

    /**
     * 更新行号
     * @param editorContent 编辑器内容视图
     * @param lineNumbers 行号显示视图
     */
    fun updateLineNumbers(
        editorContent: EditText?,
        lineNumbers: TextView?
    ) {
        if (editorContent == null || lineNumbers == null) return

        val text = editorContent.text.toString()
        val lines = text.lines()
        val lineCount = lines.size

        val lineNumbersText = StringBuilder()
        for (i in 1..lineCount) {
            lineNumbersText.append(i)
            if (i < lineCount) {
                lineNumbersText.append("\n")
            }
        }

        lineNumbers.text = lineNumbersText.toString()
    }
}