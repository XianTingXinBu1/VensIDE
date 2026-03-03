package com.venside.x1n

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.venside.x1n.managers.EditorManager
import com.venside.x1n.managers.FileManager
import com.venside.x1n.managers.RecentFilesManager
import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.DialogHelper
import com.venside.x1n.utils.FileUtils
import com.venside.x1n.utils.ThemeConstants
import java.io.File

class WorkspaceActivity : Activity() {

    private var workspacePath: String? = null
    private var sidebarVisible = true

    // 管理器实例
    private val fileManager = FileManager()
    private val editorManager = EditorManager()
    private val recentFilesManager = RecentFilesManager()

    private lateinit var fileTreeAdapter: FileTreeAdapter
    private var fileTree: MutableList<FileTreeItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace)

        workspacePath = intent.getStringExtra("workspace_path")
        fileManager.init(workspacePath ?: "/")

        setupUI()
        loadCurrentDir()
    }

    private fun setupUI() {
        // 侧边栏切换按钮
        findViewById<ImageView>(R.id.btn_toggle_sidebar)?.setOnClickListener {
            toggleSidebar()
        }

        // 返回按钮
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            handleBackPress()
        }

        // 保存按钮
        findViewById<ImageView>(R.id.btn_save)?.setOnClickListener {
            saveCurrentFile()
        }

        // 路径显示点击事件
        findViewById<TextView>(R.id.tv_workspace_path)?.setOnClickListener {
            showFullPathDialog()
        }

        // 设置最近文件栏的拖拽事件监听器
        val recentFilesContainer = findViewById<LinearLayout>(R.id.recent_files_container)
        recentFilesContainer?.setOnDragListener { view, event ->
            val draggedView = event.localState as? View ?: return@setOnDragListener false

            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> true
                android.view.DragEvent.ACTION_DRAG_ENTERED -> true
                android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                    // 获取当前拖拽位置下的子视图
                    val targetView = findChildAt(recentFilesContainer, event.x, event.y)
                    if (targetView != null && targetView != draggedView) {
                        // 找到源位置和目标位置
                        val fromIndex = recentFilesContainer.indexOfChild(draggedView)
                        val toIndex = recentFilesContainer.indexOfChild(targetView)

                        // 交换位置
                        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                            recentFilesContainer.removeView(draggedView)
                            recentFilesContainer.addView(draggedView, toIndex)

                            // 更新最近文件列表
                            recentFilesManager.moveFile(fromIndex - 1, toIndex - 1)  // 减1因为第一个是标题
                        }
                    }
                    true
                }
                android.view.DragEvent.ACTION_DRAG_EXITED -> true
                android.view.DragEvent.ACTION_DROP -> {
                    draggedView.alpha = 1.0f
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    draggedView.alpha = 1.0f
                    true
                }
                else -> false
            }
        }

        // 新建按钮（文件/文件夹）
        findViewById<ImageView>(R.id.btn_new)?.setOnClickListener {
            showNewOptionsDialog()
        }

        // 返回上级文件夹按钮（复用侧边栏切换按钮）
        findViewById<ImageView>(R.id.btn_toggle_sidebar)?.setOnLongClickListener {
            navigateToParentDir()
            true
        }

        // 文件树列表
        val fileTreeView = findViewById<ListView>(R.id.file_tree)
        fileTreeAdapter = FileTreeAdapter(this, fileTree)
        fileTreeView.adapter = fileTreeAdapter

        // 文件树点击事件
        fileTreeView.setOnItemClickListener { _, _, position, _ ->
            val item = fileTree[position]
            onFileTreeItemClick(item)
        }

        // 文件树长按事件
        fileTreeView.setOnItemLongClickListener { _, _, position, _ ->
            val item = fileTree[position]
            // 如果长按的是"..."项，直接返回工作区根目录
            if (item.name == "..") {
                currentDir = rootDir
                loadCurrentDir()
                true
            } else {
                showFileOptions(item)
                true
            }
        }

        // 编辑器内容监听
        val editorContent = findViewById<EditText>(R.id.editor_content)
        editorContent?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 更新行号
                updateLineNumbers()
            }
        })
    }

    private fun toggleSidebar() {
        val sidebar = findViewById<LinearLayout>(R.id.sidebar)
        sidebarVisible = !sidebarVisible
        sidebar?.visibility = if (sidebarVisible) View.VISIBLE else View.GONE
    }

    private fun loadCurrentDir() {
        fileTree.clear()
        fileTree.addAll(fileManager.loadCurrentDir())
        fileTreeAdapter.notifyDataSetChanged()
        updatePathDisplay()
    }

    private fun onFileTreeItemClick(item: FileTreeItem) {
        if (item.isDirectory) {
            // 点击文件夹，跳转到该文件夹
            fileManager.navigateTo(item)
            loadCurrentDir()
        } else {
            // 打开文件前检查是否有未保存的更改
            if (isContentModified()) {
                DialogHelper.showConfirmDialog(
                    context = this,
                    title = "未保存的更改",
                    message = "当前文件有未保存的更改，是否保存？",
                    positiveText = "保存并打开",
                    negativeText = "不保存打开",
                    onConfirm = {
                        saveCurrentFile()
                        openFile(item.file)
                    },
                    onCancel = {
                        openFile(item.file)
                    }
                )
            } else {
                // 打开文件
                openFile(item.file)
            }
        }
    }

    private fun navigateToParentDir() {
        if (fileManager.navigateToParent()) {
            loadCurrentDir()
        }
    }

    private fun updatePathDisplay() {
        // 更新顶栏路径：显示当前编辑文件的路径
        val currentFile = editorManager.getCurrentFile()
        if (currentFile != null) {
            findViewById<TextView>(R.id.tv_workspace_path)?.text = fileManager.getShortPath(currentFile)
        } else {
            findViewById<TextView>(R.id.tv_workspace_path)?.text = fileManager.getRootDir()?.name ?: "/workspace"
        }

        // 更新侧边栏路径：显示资源管理器的当前路径
        val currentDir = fileManager.getCurrentDir()
        val currentPath = currentDir?.absolutePath ?: fileManager.getRootDir()?.absolutePath ?: "/workspace"
        val workspaceName = workspacePath?.let { File(it).name } ?: ""
        val index = currentPath.indexOf(workspaceName)
        val shortPath = if (index >= 0) {
            currentPath.substring(index)
        } else {
            currentPath
        }
        findViewById<TextView>(R.id.tv_sidebar_path)?.text = shortPath
    }

    private fun updateRecentFilesBar() {
        val container = findViewById<LinearLayout>(R.id.recent_files_container) ?: return

        // 保留第一个 TextView（"最近打开: "）
        val titleView = container.getChildAt(0) as? TextView
        container.removeAllViews()
        titleView?.let { container.addView(it) }

        // 添加最近文件标签
        for ((index, file) in recentFilesManager.getRecentFiles().withIndex()) {
            val fileLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(4, 4, 4, 4)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.setMargins(0, 0, 4, 0)
                this.layoutParams = layoutParams
            }

            val fileTag = TextView(this).apply {
                text = file.name
                textSize = 13f
                setPadding(16, 10, 4, 10)
                setOnClickListener {
                    // 点击标签切换文件
                    if (isContentModified()) {
                        DialogHelper.showConfirmDialog(
                            context = this@WorkspaceActivity,
                            title = "未保存的更改",
                            message = "当前文件有未保存的更改，是否保存？",
                            positiveText = "保存并打开",
                            negativeText = "不保存打开",
                            onConfirm = {
                                saveCurrentFile()
                                openFile(file)
                            },
                            onCancel = {
                                openFile(file)
                            }
                        )
                    } else {
                        openFile(file)
                    }
                }

                // 添加拖拽功能
                setOnLongClickListener {
                    startDrag(fileLayout, index)
                    true
                }
            }

            // 高亮当前打开的文件
            if (editorManager.isCurrentFile(file)) {
                fileTag.setBackgroundColor(ThemeConstants.COLOR_TAG_BG_SELECTED)
                fileTag.setTextColor(ThemeConstants.COLOR_TEXT_LIGHT)
            } else {
                fileTag.setBackgroundColor(ThemeConstants.COLOR_TAG_BG)
                fileTag.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)
            }

            fileLayout.addView(fileTag)

            // 添加删除按钮容器
            val deleteBtnContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                val layoutParams = LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.setMargins(4, 0, 0, 0)
                this.layoutParams = layoutParams
            }

            // 添加删除按钮
            val deleteBtn = TextView(this).apply {
                text = "×"
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(6, 0, 6, 0)
                setOnClickListener {
                    // 从最近文件列表中删除
                    recentFilesManager.removeFileAt(index)
                    updateRecentFilesBar()
                }
            }

            deleteBtnContainer.addView(deleteBtn)
            fileLayout.addView(deleteBtnContainer)
            container.addView(fileLayout)
        }
    }

    private fun startDrag(view: View, fromIndex: Int) {
        view.alpha = ThemeConstants.DRAG_ALPHA

        // 创建拖拽阴影
        val shadowBuilder = android.view.View.DragShadowBuilder(view)

        // 开始拖拽
        view.startDrag(null, shadowBuilder, view, 0)
    }

    private fun findChildAt(container: LinearLayout, x: Float, y: Float): View? {
        for (i in 1 until container.childCount) {  // 从1开始，跳过标题
            val child = container.getChildAt(i)
            if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                return child
            }
        }
        return null
    }

    private fun getShortPath(): String {
        val currentFile = editorManager.getCurrentFile()
        if (currentFile == null) {
            return fileManager.getRootDir()?.name ?: "/workspace"
        }
        return fileManager.getShortPath(currentFile)
    }

    private fun openFile(file: File) {
        editorManager.openFile(file)
            .onSuccess { content ->
                // 添加到最近文件列表
                recentFilesManager.addFile(file)

                val emptyState = findViewById<TextView>(R.id.empty_state)
                val editorScroll = findViewById<android.widget.ScrollView>(R.id.editor_scroll)
                val editorContent = findViewById<EditText>(R.id.editor_content)

                emptyState?.visibility = View.GONE
                editorScroll?.visibility = View.VISIBLE
                editorContent?.setText(content)

                // 更新路径显示
                updatePathDisplay()
                // 更新行号
                updateLineNumbers()
                // 更新最近文件顶栏
                updateRecentFilesBar()
            }
            .onFailure { e ->
                Toast.makeText(this, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    

    private fun saveCurrentFile() {
        val editorContent = findViewById<EditText>(R.id.editor_content) ?: return
        val content = editorContent.text.toString()

        editorManager.saveFile(content)
            .onSuccess {
                // 更新文件树中当前文件的大小
                updateFileSizeInTree()
                Toast.makeText(this, "文件已保存", Toast.LENGTH_SHORT).show()
            }
            .onFailure { e ->
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFileSizeInTree() {
        val currentFile = editorManager.getCurrentFile() ?: return

        // 查找文件树中对应的文件项并更新大小
        for (item in fileTree) {
            if (!item.isDirectory && item.filePath == currentFile.absolutePath) {
                val updatedItem = FileTreeItem.fromFile(currentFile)
                fileTree[fileTree.indexOf(item)] = updatedItem
                break
            }
        }
        fileTreeAdapter.notifyDataSetChanged()
    }

    private fun isContentModified(): Boolean {
        if (!editorManager.isFileOpen()) return false

        val currentContent = findViewById<EditText>(R.id.editor_content)?.text.toString()
        return editorManager.isModified(currentContent)
    }

    override fun onBackPressed() {
        handleBackPress()
    }

    private fun handleBackPress() {
        // 检查是否有未保存的更改
        if (isContentModified()) {
            DialogHelper.showConfirmDialog(
                context = this,
                title = "未保存的更改",
                message = "当前文件有未保存的更改，是否保存？",
                positiveText = "保存并返回",
                negativeText = "不保存返回",
                onConfirm = {
                    saveCurrentFile()
                    finish()
                },
                onCancel = {
                    finish()
                }
            )
        } else {
            // 直接返回主页
            DialogHelper.showConfirmDialog(
                context = this,
                title = "返回主页",
                message = "确定要返回主页吗？",
                onConfirm = {
                    finish()
                }
            )
        }
    }

    private fun showNewOptionsDialog() {
        DialogHelper.showInputDialog(
            context = this,
            title = "新建",
            hint = "输入名称"
        ) { name ->
            showNewTypeDialog(name)
        }
    }

    private fun showNewTypeDialog(name: String) {
        val options = arrayOf("新建文件", "新建文件夹")
        DialogHelper.showOptionsDialog(
            context = this,
            title = "选择类型",
            options = options
        ) { which ->
            when (which) {
                0 -> createFile(name)
                1 -> createFolder(name)
            }
        }
    }

    private fun showFullPathDialog() {
        val fullPath = currentFile?.absolutePath ?: rootDir?.absolutePath ?: "/workspace"
        AlertDialog.Builder(this)
            .setTitle("完整路径")
            .setMessage(fullPath)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun createFile(fileName: String) {
        fileManager.createFile(fileName)
            .onSuccess {
                Toast.makeText(this, "文件创建成功", Toast.LENGTH_SHORT).show()
                loadCurrentDir()
            }
            .onFailure { e ->
                val message = when (e) {
                    is java.nio.file.FileAlreadyExistsException -> "文件已存在"
                    else -> "文件创建失败: ${e.message}"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun createFolder(folderName: String) {
        fileManager.createFolder(folderName)
            .onSuccess {
                Toast.makeText(this, "文件夹创建成功", Toast.LENGTH_SHORT).show()
                loadCurrentDir()
            }
            .onFailure { e ->
                val message = when (e) {
                    is java.nio.file.FileAlreadyExistsException -> "文件夹已存在"
                    else -> "文件夹创建失败: ${e.message}"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFileOptions(item: FileTreeItem) {
        val options = if (item.isDirectory) {
            arrayOf("重命名", "删除")
        } else {
            arrayOf("打开", "重命名", "删除")
        }

        DialogHelper.showOptionsDialog(
            context = this,
            title = "文件选项",
            options = options
        ) { which ->
            when (which) {
                0 -> if (!item.isDirectory) openFile(item.file) else showRenameDialog(item)
                1 -> if (!item.isDirectory) showRenameDialog(item) else showDeleteDialog(item)
                2 -> showDeleteDialog(item)
            }
        }
    }

    private fun showRenameDialog(item: FileTreeItem) {
        DialogHelper.showInputDialog(
            context = this,
            title = "重命名",
            hint = item.name,
            defaultValue = item.name
        ) { newName ->
            if (newName != item.name) {
                renameFile(item.file, newName)
            } else {
                Toast.makeText(this, "请输入新的名称", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renameFile(file: File, newName: String) {
        fileManager.renameFile(file, newName)
            .onSuccess {
                Toast.makeText(this, "重命名成功", Toast.LENGTH_SHORT).show()
                loadCurrentDir()
            }
            .onFailure { e ->
                val message = when (e) {
                    is java.nio.file.FileAlreadyExistsException -> "文件已存在"
                    else -> "重命名失败: ${e.message}"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteDialog(item: FileTreeItem) {
        DialogHelper.showConfirmDialog(
            context = this,
            title = "删除",
            message = "确定要删除 \"${item.name}\" 吗？此操作不可恢复。",
            positiveText = "删除",
            onConfirm = {
                deleteFile(item.file)
            }
        )
    }

    private fun deleteFile(file: File) {
        FileUtils.deleteFile(file)
            .onSuccess {
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()

                // 从最近文件列表中删除该文件
                recentFilesManager.removeFile(file)
                updateRecentFilesBar()

                // 检查是否删除了当前正在编辑的文件
                if (editorManager.isCurrentFile(file)) {
                    // 清空编辑器
                    editorManager.closeFile()
                    val emptyState = findViewById<TextView>(R.id.empty_state)
                    val editorScroll = findViewById<android.widget.ScrollView>(R.id.editor_scroll)
                    val editorContent = findViewById<EditText>(R.id.editor_content)
                    val lineNumbers = findViewById<TextView>(R.id.line_numbers)

                    emptyState?.visibility = View.VISIBLE
                    editorScroll?.visibility = View.GONE
                    editorContent?.text?.clear()
                    lineNumbers?.text = ""
                }

                loadCurrentDir()
            }
            .onFailure { e ->
                Toast.makeText(this, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLineNumbers() {
        val editorContent = findViewById<EditText>(R.id.editor_content) ?: return
        val lineNumbers = findViewById<TextView>(R.id.line_numbers) ?: return

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

    

    // 文件树适配器
    private class FileTreeAdapter(
        context: Activity,
        private val items: List<FileTreeItem>
    ) : ArrayAdapter<FileTreeItem>(context, R.layout.item_file_tree, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_file_tree, parent, false)

            val item = items[position]

            val expandIndicator = view.findViewById<ImageView>(R.id.iv_expand_indicator)
            val fileIcon = view.findViewById<ImageView>(R.id.iv_file_icon)
            val fileName = view.findViewById<TextView>(R.id.tv_file_name)
            val fileSize = view.findViewById<TextView>(R.id.tv_file_size)

            // 设置图标
            if (item.isDirectory) {
                // 文件夹使用文件夹图标
                expandIndicator?.visibility = View.VISIBLE
                expandIndicator?.setImageResource(android.R.drawable.ic_menu_add)
                expandIndicator?.rotation = 0f
                fileIcon?.visibility = View.GONE
                fileSize?.visibility = View.GONE
            } else {
                // 文件使用文件图标
                expandIndicator?.visibility = View.GONE
                fileIcon?.visibility = View.VISIBLE
                fileIcon?.setImageResource(android.R.drawable.ic_menu_info_details)
                fileSize?.visibility = View.VISIBLE
                fileSize?.text = FileUtils.formatFileSize(item.size)
            }

            // 设置文件名
            fileName?.text = item.name
            fileName?.setTextColor(ThemeConstants.COLOR_TEXT_NORMAL)

            // 清除缩进
            view.setPadding(8, view.paddingTop, view.paddingRight, view.paddingBottom)

            return view
        }
    }
}
