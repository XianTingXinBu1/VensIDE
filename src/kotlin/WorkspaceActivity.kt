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
    private var currentDir: File? = null  // 当前选中的文件夹
    private lateinit var fileTreeAdapter: FileTreeAdapter
    private var fileTree: MutableList<FileTreeItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace)

        workspacePath = intent.getStringExtra("workspace_path")
        rootDir = File(workspacePath ?: "/")
        currentDir = rootDir  // 初始化当前文件夹为根目录

        setupUI()
        loadFileTree()
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

        // 路径显示点击事件
        findViewById<TextView>(R.id.tv_workspace_path)?.setOnClickListener {
            showFullPathDialog()
        }

        // 新建按钮（文件/文件夹）
        findViewById<ImageView>(R.id.btn_new)?.setOnClickListener {
            showNewOptionsDialog()
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
            showFileOptions(item)
            true
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

    private fun loadFileTree() {
        fileTree.clear()
        rootDir?.let { root ->
            buildFileTree(root, 0)
        }
        fileTreeAdapter.notifyDataSetChanged()

        // 更新路径显示（只显示当前打开文件的路径）
        updatePathDisplay()
    }

    private fun buildFileTree(dir: File, level: Int) {
        val files = dir.listFiles()?.toList() ?: emptyList()

        // 排序：文件夹在前，文件在后
        val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

        for (file in sortedFiles) {
            val item = FileTreeItem(
                file = file,
                name = file.name,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0 else file.length(),
                level = level,
                isExpanded = false
            )
            fileTree.add(item)

            // 如果是文件夹，递归添加子项
            if (file.isDirectory) {
                buildFileTree(file, level + 1)
            }
        }
    }

    private fun onFileTreeItemClick(item: FileTreeItem) {
        if (item.isDirectory) {
            // 更新当前文件夹
            currentDir = item.file
            // 切换展开/折叠状态
            toggleExpand(item)
        } else {
            // 打开文件
            openFile(item.file)
        }
    }

    private fun toggleExpand(item: FileTreeItem) {
        item.isExpanded = !item.isExpanded

        if (item.isExpanded) {
            // 展开文件夹，显示子项
            val position = fileTree.indexOf(item)
            if (position >= 0) {
                val children = getChildren(item.file, item.level + 1)
                fileTree.addAll(position + 1, children)
            }
        } else {
            // 折叠文件夹，隐藏所有子项
            val position = fileTree.indexOf(item)
            if (position >= 0) {
                val itemsToRemove = mutableListOf<FileTreeItem>()
                for (i in (position + 1) until fileTree.size) {
                    if (fileTree[i].level > item.level) {
                        itemsToRemove.add(fileTree[i])
                    } else {
                        break
                    }
                }
                fileTree.removeAll(itemsToRemove)
            }
        }

        fileTreeAdapter.notifyDataSetChanged()
    }

    private fun getChildren(dir: File, level: Int): List<FileTreeItem> {
        val children = mutableListOf<FileTreeItem>()
        val files = dir.listFiles()?.toList() ?: emptyList()

        // 排序：文件夹在前，文件在后
        val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

        for (file in sortedFiles) {
            val item = FileTreeItem(
                file = file,
                name = file.name,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0 else file.length(),
                level = level,
                isExpanded = false
            )
            children.add(item)
        }

        return children
    }

    private fun updatePathDisplay() {
        val shortPath = getShortPath()
        findViewById<TextView>(R.id.tv_workspace_path)?.text = shortPath
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
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCurrentFile() {
        val content = findViewById<EditText>(R.id.editor_content)?.text.toString()
        currentFile?.let { file ->
            try {
                file.writeText(content)
                Toast.makeText(this, "文件已保存", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        handleBackPress()
    }

    private fun handleBackPress() {
        // 直接返回主页，不再返回上级目录
        AlertDialog.Builder(this)
            .setTitle("返回主页")
            .setMessage("确定要返回主页吗？")
            .setPositiveButton("确定") { _, _ ->
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
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
                loadFileTree()
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
                loadFileTree()
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
            loadFileTree()
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
            loadFileTree()
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
        val size: Long,
        val level: Int = 0,
        var isExpanded: Boolean = false
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

            // 设置展开/折叠指示器
            if (item.isDirectory) {
                expandIndicator?.visibility = View.VISIBLE
                expandIndicator?.rotation = if (item.isExpanded) 90f else 0f
            } else {
                expandIndicator?.visibility = View.GONE
            }

            // 设置图标
            if (item.isDirectory) {
                fileIcon?.visibility = View.GONE
                fileSize?.visibility = View.GONE
            } else {
                fileIcon?.visibility = View.VISIBLE
                fileIcon?.setImageResource(android.R.drawable.ic_menu_info_details)
                fileSize?.visibility = View.VISIBLE
                fileSize?.text = formatFileSize(item.size)
            }

            // 设置文件名
            fileName?.text = item.name

            // 设置缩进
            val padding = (item.level * 24).coerceAtMost(200)
            view.setPadding(padding, view.paddingTop, view.paddingRight, view.paddingBottom)

            return view
        }

        private fun formatFileSize(size: Long): String {
            if (size < 1024) return "$size B"
            if (size < 1024 * 1024) return "${size / 1024} KB"
            return "${size / (1024 * 1024)} MB"
        }
    }
}
