package com.venside.x1n

import android.app.Activity
import android.app.AlertDialog
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
import java.io.File

class WorkspaceActivity : Activity() {

    private var workspacePath: String? = null
    private var rootDir: File? = null
    private var sidebarVisible = true
    private var currentFile: File? = null
    private var currentDir: File? = null  // 当前显示的文件夹
    private var originalContent: String? = null  // 文件原始内容，用于检测修改
    private var recentFiles: MutableList<File> = mutableListOf()  // 最近打开的文件列表
    private lateinit var fileTreeAdapter: FileTreeAdapter
    private var fileTree: MutableList<FileTreeItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace)

        workspacePath = intent.getStringExtra("workspace_path")
        rootDir = File(workspacePath ?: "/")
        currentDir = rootDir  // 初始化当前文件夹为根目录

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
                            val file = recentFiles.removeAt(fromIndex - 1)  // 减1因为第一个是标题
                            recentFiles.add(toIndex - 1, file)
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

        // 添加返回上级目录项（如果不是根目录）
        if (currentDir?.absolutePath != rootDir?.absolutePath) {
            val parentItem = FileTreeItem(
                file = currentDir?.parentFile ?: rootDir!!,
                name = "..",
                isDirectory = true,
                size = 0
            )
            fileTree.add(parentItem)
        }

        // 加载当前目录的内容
        currentDir?.let { dir ->
            val files = dir.listFiles()?.toList() ?: emptyList()
            // 排序：文件夹在前，文件在后
            val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

            for (file in sortedFiles) {
                val item = FileTreeItem(
                    file = file,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) 0 else file.length()
                )
                fileTree.add(item)
            }
        }

        fileTreeAdapter.notifyDataSetChanged()
        updatePathDisplay()
    }

    private fun onFileTreeItemClick(item: FileTreeItem) {
        if (item.isDirectory) {
            // 点击文件夹，跳转到该文件夹
            currentDir = item.file
            loadCurrentDir()
        } else {
            // 打开文件前检查是否有未保存的更改
            if (isContentModified()) {
                AlertDialog.Builder(this)
                    .setTitle("未保存的更改")
                    .setMessage("当前文件有未保存的更改，是否保存？")
                    .setPositiveButton("保存并打开") { _, _ ->
                        saveCurrentFile()
                        openFile(item.file)
                    }
                    .setNegativeButton("不保存打开") { _, _ ->
                        openFile(item.file)
                    }
                    .setNeutralButton("取消", null)
                    .show()
            } else {
                // 打开文件
                openFile(item.file)
            }
        }
    }

    private fun navigateToParentDir() {
        currentDir?.parentFile?.let { parent ->
            currentDir = parent
            loadCurrentDir()
        }
    }

    private fun updatePathDisplay() {
        // 更新顶栏路径：显示当前编辑文件的路径
        if (currentFile != null) {
            val fullPath = currentFile?.absolutePath ?: ""
            val workspaceName = workspacePath?.let { File(it).name } ?: ""
            val index = fullPath.indexOf(workspaceName)
            val shortPath = if (index >= 0) {
                fullPath.substring(index)
            } else {
                fullPath
            }
            findViewById<TextView>(R.id.tv_workspace_path)?.text = shortPath
        } else {
            findViewById<TextView>(R.id.tv_workspace_path)?.text = rootDir?.name ?: "/workspace"
        }

        // 更新侧边栏路径：显示资源管理器的当前路径
        val currentPath = currentDir?.absolutePath ?: rootDir?.absolutePath ?: "/workspace"
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
        for ((index, file) in recentFiles.withIndex()) {
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
                        AlertDialog.Builder(this@WorkspaceActivity)
                            .setTitle("未保存的更改")
                            .setMessage("当前文件有未保存的更改，是否保存？")
                            .setPositiveButton("保存并打开") { _, _ ->
                                saveCurrentFile()
                                openFile(file)
                            }
                            .setNegativeButton("不保存打开") { _, _ ->
                                openFile(file)
                            }
                            .setNeutralButton("取消", null)
                            .show()
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
            if (currentFile != null && currentFile?.absolutePath == file.absolutePath) {
                fileTag.setBackgroundColor(0xFF3399FF.toInt())
                fileTag.setTextColor(0xFFFFFFFF.toInt())
            } else {
                fileTag.setBackgroundColor(0xFF3C3C3C.toInt())
                fileTag.setTextColor(0xFFCCCCCC.toInt())
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
                    recentFiles.removeAt(index)
                    updateRecentFilesBar()
                }
            }

            deleteBtnContainer.addView(deleteBtn)
            fileLayout.addView(deleteBtnContainer)
            container.addView(fileLayout)
        }
    }

    private fun startDrag(view: View, fromIndex: Int) {
        view.alpha = 0.5f

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
        if (currentFile == null) {
            return rootDir?.name ?: "/workspace"
        }
        val fullPath = currentFile?.absolutePath ?: return "/workspace"
        val workspaceName = workspacePath?.let { File(it).name } ?: return "/workspace"
        val index = fullPath.indexOf(workspaceName)
        return if (index >= 0) {
            fullPath.substring(index)
        } else {
            fullPath
        }
    }

    private fun openFile(file: File) {
        try {
            val content = file.readText()
            currentFile = file
            originalContent = content  // 保存原始内容

            // 添加到最近文件列表
            addToRecentFiles(file)

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
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToRecentFiles(file: File) {
        // 移除已存在的相同文件
        recentFiles.removeAll { it.absolutePath == file.absolutePath }
        // 添加到列表开头
        recentFiles.add(0, file)
        // 限制最多保留 10 个最近文件
        if (recentFiles.size > 10) {
            recentFiles.removeAt(recentFiles.size - 1)
        }
    }

    private fun saveCurrentFile() {
        val editorContent = findViewById<EditText>(R.id.editor_content) ?: return
        val content = editorContent.text.toString()
        currentFile?.let { file ->
            try {
                file.writeText(content)
                originalContent = content  // 更新原始内容

                // 更新文件树中当前文件的大小
                updateFileSizeInTree(file)

                Toast.makeText(this, "文件已保存", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFileSizeInTree(file: File) {
        // 查找文件树中对应的文件项并更新大小
        for (item in fileTree) {
            if (!item.isDirectory && item.file.absolutePath == file.absolutePath) {
                item.size = file.length()
                break
            }
        }
        fileTreeAdapter.notifyDataSetChanged()
    }

    private fun isContentModified(): Boolean {
        // 如果没有打开文件，不算有未保存的更改
        if (currentFile == null) return false

        val currentContent = findViewById<EditText>(R.id.editor_content)?.text.toString()
        return currentContent != originalContent
    }

    override fun onBackPressed() {
        handleBackPress()
    }

    private fun handleBackPress() {
        // 检查是否有未保存的更改
        if (isContentModified()) {
            AlertDialog.Builder(this)
                .setTitle("未保存的更改")
                .setMessage("当前文件有未保存的更改，是否保存？")
                .setPositiveButton("保存并返回") { _, _ ->
                    saveCurrentFile()
                    finish()
                }
                .setNegativeButton("不保存返回") { _, _ ->
                    finish()
                }
                .setNeutralButton("取消", null)
                .show()
        } else {
            // 直接返回主页
            AlertDialog.Builder(this)
                .setTitle("返回主页")
                .setMessage("确定要返回主页吗？")
                .setPositiveButton("确定") { _, _ ->
                    finish()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun showNewOptionsDialog() {
        val editText = EditText(this).apply {
            hint = "输入名称"
            setPadding(50, 30, 50, 30)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        AlertDialog.Builder(this)
            .setTitle("新建")
            .setView(layout)
            .setNegativeButton("取消", null)
            .setPositiveButton("新建文件") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    createFile(name)
                } else {
                    Toast.makeText(this, "请输入名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("新建文件夹") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    createFolder(name)
                } else {
                    Toast.makeText(this, "请输入名称", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
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
        currentDir?.let { dir ->
            val newFile = File(dir, fileName)
            if (newFile.exists()) {
                Toast.makeText(this, "文件已存在", Toast.LENGTH_SHORT).show()
                return
            }
            try {
                newFile.createNewFile()
                Toast.makeText(this, "文件创建成功", Toast.LENGTH_SHORT).show()
                loadCurrentDir()
            } catch (e: Exception) {
                Toast.makeText(this, "文件创建失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createFolder(folderName: String) {
        currentDir?.let { dir ->
            val newFolder = File(dir, folderName)
            if (newFolder.exists()) {
                Toast.makeText(this, "文件夹已存在", Toast.LENGTH_SHORT).show()
                return
            }
            if (newFolder.mkdirs()) {
                Toast.makeText(this, "文件夹创建成功", Toast.LENGTH_SHORT).show()
                loadCurrentDir()
            } else {
                Toast.makeText(this, "文件夹创建失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFileOptions(item: FileTreeItem) {
        val options = if (item.isDirectory) {
            arrayOf("重命名", "删除")
        } else {
            arrayOf("打开", "重命名", "删除")
        }

        AlertDialog.Builder(this)
            .setTitle("文件选项")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> if (!item.isDirectory) openFile(item.file) else showRenameDialog(item)
                    1 -> if (!item.isDirectory) showRenameDialog(item) else showDeleteDialog(item)
                    2 -> showDeleteDialog(item)
                }
            }
            .show()
    }

    private fun showRenameDialog(item: FileTreeItem) {
        val editText = EditText(this).apply {
            setText(item.name)
            setPadding(50, 30, 50, 30)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        AlertDialog.Builder(this)
            .setTitle("重命名")
            .setView(layout)
            .setPositiveButton("重命名") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != item.name) {
                    renameFile(item.file, newName)
                } else {
                    Toast.makeText(this, "请输入新的名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun renameFile(file: File, newName: String) {
        val newFile = File(file.parentFile, newName)
        if (newFile.exists()) {
            Toast.makeText(this, "文件已存在", Toast.LENGTH_SHORT).show()
            return
        }
        if (file.renameTo(newFile)) {
            Toast.makeText(this, "重命名成功", Toast.LENGTH_SHORT).show()
            loadCurrentDir()
        } else {
            Toast.makeText(this, "重命名失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteDialog(item: FileTreeItem) {
        AlertDialog.Builder(this)
            .setTitle("删除")
            .setMessage("确定要删除 \"${item.name}\" 吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                deleteFile(item.file)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteFile(file: File) {
        if (file.deleteRecursively()) {
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()

            // 从最近文件列表中删除该文件
            recentFiles.removeAll { it.absolutePath == file.absolutePath }
            updateRecentFilesBar()

            // 检查是否删除了当前正在编辑的文件
            if (currentFile != null && currentFile?.absolutePath == file.absolutePath) {
                // 清空编辑器
                currentFile = null
                originalContent = null
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
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
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

    // 文件树项数据类
    data class FileTreeItem(
        val file: File,
        val name: String,
        val isDirectory: Boolean,
        var size: Long
    )

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
                fileSize?.text = formatFileSize(item.size)
            }

            // 设置文件名
            fileName?.text = item.name
            fileName?.setTextColor(0xFFCCCCCC.toInt())

            // 清除缩进
            view.setPadding(8, view.paddingTop, view.paddingRight, view.paddingBottom)

            return view
        }

        private fun formatFileSize(size: Long): String {
            if (size < 1024) return "$size B"
            if (size < 1024 * 1024) return "${size / 1024} KB"
            return "${size / (1024 * 1024)} MB"
        }
    }
}
