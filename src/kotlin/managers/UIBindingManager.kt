package com.venside.x1n.managers

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import com.venside.x1n.R
import com.venside.x1n.managers.interfaces.IUIBindingManager

/**
 * UI 绑定管理器
 * 负责控件绑定和监听器设置
 */
class UIBindingManager(
    private val activity: Activity
) : IUIBindingManager {

    override fun bindSidebarToggle(onToggle: () -> Unit, onLongPress: () -> Unit) {
        activity.findViewById<ImageView>(R.id.btn_toggle_sidebar)?.setOnClickListener {
            onToggle()
        }

        activity.findViewById<ImageView>(R.id.btn_toggle_sidebar)?.setOnLongClickListener {
            onLongPress()
            true
        }
    }

    override fun bindNavigationButtons(onBack: () -> Unit, onSave: () -> Unit) {
        activity.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            onBack()
        }

        activity.findViewById<ImageView>(R.id.btn_save)?.setOnClickListener {
            onSave()
        }
    }

    override fun bindPathDisplay(onLongPress: () -> Boolean, onClick: () -> Unit) {
        activity.findViewById<TextView>(R.id.tv_workspace_path)?.setOnLongClickListener {
            onLongPress()
        }

        activity.findViewById<TextView>(R.id.tv_workspace_path)?.setOnClickListener {
            onClick()
        }
    }

    override fun bindDragDrop(onMove: (Int, Int) -> Unit) {
        val container = activity.findViewById<LinearLayout>(R.id.recent_files_container)
        container?.setOnDragListener { v, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> true
                android.view.DragEvent.ACTION_DRAG_ENTERED -> true
                android.view.DragEvent.ACTION_DRAG_EXITED -> true
                android.view.DragEvent.ACTION_DROP -> {
                    val fromIndex = event.clipData.getItemAt(0).text.toString().toIntOrNull() ?: -1
                    val toIndex = calculateDropPosition(container, event)
                    if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                        onMove(fromIndex, toIndex)
                    }
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> true
                else -> false
            }
        }
    }

    private fun calculateDropPosition(container: LinearLayout, event: android.view.DragEvent): Int {
        val y = event.y
        var position = 0
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (y > child.bottom) {
                position = i + 1
            }
        }
        return position.coerceIn(0, container.childCount - 1)
    }

    override fun bindNewButton(onClick: () -> Unit) {
        activity.findViewById<ImageView>(R.id.btn_new)?.setOnClickListener {
            onClick()
        }
    }

    override fun bindFileTree(
        onItemClick: (Int) -> Unit,
        onItemLongClick: (Int) -> Boolean,
        onTouch: (MotionEvent) -> Boolean
    ) {
        val fileTreeView = activity.findViewById<ListView>(R.id.file_tree)
        
        fileTreeView?.setOnItemClickListener { _, _, position, _ ->
            onItemClick(position)
        }

        fileTreeView?.setOnItemLongClickListener { _, _, position, _ ->
            onItemLongClick(position)
        }

        fileTreeView?.setOnTouchListener { _, event ->
            onTouch(event)
        }
    }

    override fun bindEditorListeners(onEditorClick: () -> Unit, onContentChanged: () -> Unit) {
        activity.findViewById<ScrollView>(R.id.editor_scroll)?.setOnClickListener {
            onEditorClick()
        }

        activity.findViewById<LinearLayout>(R.id.editor_area)?.setOnClickListener {
            onEditorClick()
        }

        activity.findViewById<TextView>(R.id.empty_state)?.setOnClickListener {
            onEditorClick()
        }

        activity.findViewById<EditText>(R.id.editor_content)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onContentChanged()
            }
        })
    }

    override fun bindWordCountButton(onClick: () -> Unit) {
        activity.findViewById<ImageView>(R.id.btn_word_count_details)?.setOnClickListener {
            onClick()
        }
    }

    override fun getEditorContent(): EditText? = activity.findViewById(R.id.editor_content)

    override fun getFileTreeView(): ListView? = activity.findViewById(R.id.file_tree)

    override fun getSidebar(): LinearLayout? = activity.findViewById(R.id.sidebar)

    override fun getEmptyState(): TextView? = activity.findViewById(R.id.empty_state)

    override fun getEditorScroll(): ScrollView? = activity.findViewById(R.id.editor_scroll)

    override fun getLineNumbers(): TextView? = activity.findViewById(R.id.line_numbers)

    override fun getRecentFilesContainer(): LinearLayout? = activity.findViewById(R.id.recent_files_container)

    override fun getWordCountBar(): LinearLayout? = activity.findViewById(R.id.word_count_bar)

    override fun getPathTextView(): TextView? = activity.findViewById(R.id.tv_workspace_path)

    override fun getSidebarPathTextView(): TextView? = activity.findViewById(R.id.tv_sidebar_path)

    override fun getWordCountTextView(): TextView? = activity.findViewById(R.id.tv_word_count)
}
