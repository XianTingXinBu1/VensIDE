package com.venside.x1n

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.venside.x1n.adapters.FileTreeAdapter
import com.venside.x1n.managers.EditorManager
import com.venside.x1n.managers.FileManager
import com.venside.x1n.managers.RecentFilesManager
import com.venside.x1n.managers.RecentFilesBarManager
import com.venside.x1n.managers.SymbolBarManager
import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.DialogHelper
import com.venside.x1n.utils.DragDropHelper
import com.venside.x1n.utils.FileUtils
import com.venside.x1n.utils.LineNumberHelper
import com.venside.x1n.utils.PathDisplayHelper
import com.venside.x1n.utils.ThemeConstants
import java.io.File

class WorkspaceActivity : Activity() {

    private var workspacePath: String? = null
    private var sidebarVisible = true

    // 管理器实例
    private val fileManager = FileManager()
    private val editorManager = EditorManager()
    private val recentFilesManager = RecentFilesManager()
    private val symbolBarManager: SymbolBarManager by lazy {
        SymbolBarManager(
            this,
            onSymbolInsert = { symbol -> insertSymbol(symbol) },
            shouldShow = { editorManager.isFileOpen() }
        )
    }

    // 辅助类实例
    private val recentFilesBarManager: RecentFilesBarManager by lazy {
        RecentFilesBarManager(this, recentFilesManager, editorManager)
    }
    private val dragDropHelper = DragDropHelper()
    private val pathDisplayHelper: PathDisplayHelper by lazy {
        PathDisplayHelper(fileManager, editorManager, workspacePath)
    }
    private val lineNumberHelper = LineNumberHelper()

    private lateinit var fileTreeAdapter: FileTreeAdapter
    private var fileTree: MutableList<FileTreeItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace)

        workspacePath = intent.getStringExtra("workspace_path")
        fileManager.init(workspacePath ?: "/")

        // 初始化符号栏管理器
        symbolBarManager.init()

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
            // 判断当前是否有打开的文件
            if (editorManager.getCurrentFile() == null) {
                // 没有打开文件，跳转到工作区管理页面
                val intent = Intent(this, WorkspaceManagementActivity::class.java)
                // 传递当前工作区路径，用于高亮显示
                intent.putExtra("current_workspace_path", workspacePath)
                startActivity(intent)
            } else {
                // 有打开文件，显示完整路径对话框
                showFullPathDialog()
            }
        }

        // 设置最近文件栏的拖拽事件监听器
        val recentFilesContainer = findViewById<LinearLayout>(R.id.recent_files_container)
        recentFilesContainer?.let {
            dragDropHelper.setupDragListener(it) { fromIndex, toIndex ->
                recentFilesManager.moveFile(fromIndex, toIndex)
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
                fileManager.navigateToRoot()
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
        // 重新创建适配器，传入当前文件信息以实现高亮
        fileTreeAdapter = FileTreeAdapter(this, fileTree, editorManager.getCurrentFile())
        val fileTreeView = findViewById<ListView>(R.id.file_tree)
        fileTreeView.adapter = fileTreeAdapter
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
        pathDisplayHelper.updatePathDisplay(
            findViewById(R.id.tv_workspace_path),
            findViewById(R.id.tv_sidebar_path)
        )
    }

    private fun updateRecentFilesBar() {
        val container = findViewById<LinearLayout>(R.id.recent_files_container) ?: return

        recentFilesBarManager.updateRecentFilesBar(
            container = container,
            onFileClick = { file ->
                if (isContentModified()) {
                    DialogHelper.showConfirmDialog(
                        context = this,
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
            },
            onDeleteClick = { file, index ->
                val isCurrentFile = editorManager.isCurrentFile(file)
                recentFilesManager.removeFileAt(index)
                updateRecentFilesBar()
                if (isCurrentFile) {
                    clearEditor()
                }
            }
        )
    }

    private fun getShortPath(): String {
        return pathDisplayHelper.getShortPath()
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
                // 刷新文件树以更新高亮状态
                loadCurrentDir()
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
        DialogHelper.showNewFileDialog(
            context = this,
            title = "新建",
            hint = "输入名称",
            onCreateFile = { fileName ->
                createFile(fileName)
            },
            onCreateFolder = { folderName ->
                createFolder(folderName)
            }
        )
    }

    private fun showFullPathDialog() {
        val currentFile = editorManager.getCurrentFile()
        val fullPath = currentFile?.absolutePath ?: fileManager.getRootDir()?.absolutePath ?: "/workspace"
        DialogHelper.showMessageDialog(
            context = this,
            title = "完整路径",
            message = fullPath
        )
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
        lineNumberHelper.updateLineNumbers(
            findViewById(R.id.editor_content),
            findViewById(R.id.line_numbers)
        )
    }

    /**
     * 插入符号到编辑器
     * @param symbol 要插入的符号
     */
    private fun insertSymbol(symbol: String) {
        val editorContent = findViewById<EditText>(R.id.editor_content) ?: return

        val start = editorContent.selectionStart
        val end = editorContent.selectionEnd

        if (start >= 0 && end >= 0) {
            val editable = editorContent.editableText
            editable.replace(start, end, symbol)

            // 更新行号
            updateLineNumbers()
        }
    }

    private fun clearEditor() {
        editorManager.closeFile()
        val emptyState = findViewById<TextView>(R.id.empty_state)
        val editorScroll = findViewById<android.widget.ScrollView>(R.id.editor_scroll)
        val editorContent = findViewById<EditText>(R.id.editor_content)
        val lineNumbers = findViewById<TextView>(R.id.line_numbers)

        emptyState?.visibility = View.VISIBLE
        editorScroll?.visibility = View.GONE
        editorContent?.text?.clear()
        lineNumbers?.text = ""
        updatePathDisplay()
        // 刷新文件树以移除高亮状态
        loadCurrentDir()
    }

    

    
}
