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
import com.venside.x1n.managers.PreferencesManager
import com.venside.x1n.managers.RecentFilesManager
import com.venside.x1n.managers.RecentFilesBarManager
import com.venside.x1n.managers.SymbolBarManager
import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.DialogHelper
import com.venside.x1n.utils.DragDropHelper
import com.venside.x1n.utils.FileUtils
import com.venside.x1n.utils.LineNumberHelper
import com.venside.x1n.utils.PathDisplayHelper
import com.venside.x1n.utils.SlideSelectHelper
import com.venside.x1n.utils.ThemeConstants
import java.io.File

class WorkspaceActivity : Activity() {

    private var workspacePath: String? = null
    private var sidebarVisible = true

    // 管理器实例
    private val fileManager = FileManager()
    private val editorManager = EditorManager()
    private val recentFilesManager = RecentFilesManager()
    private val preferencesManager: PreferencesManager by lazy { PreferencesManager(this) }
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
    private val slideSelectHelper = SlideSelectHelper()

    private lateinit var fileTreeAdapter: FileTreeAdapter
    private var fileTree: MutableList<FileTreeItem> = mutableListOf()
    private var isSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace)

        workspacePath = intent.getStringExtra("workspace_path")
        fileManager.init(workspacePath ?: "/")

        // 从持久化设置中加载排序规则
        fileManager.setSortModes(preferencesManager.getSortModes())

        // 从持久化设置中加载侧边栏状态
        sidebarVisible = preferencesManager.getSidebarState()
        val sidebar = findViewById<LinearLayout>(R.id.sidebar)
        sidebar?.visibility = if (sidebarVisible) View.VISIBLE else View.GONE

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

        // 路径显示长按事件
        findViewById<TextView>(R.id.tv_workspace_path)?.setOnLongClickListener {
            val currentFile = editorManager.getCurrentFile()
            if (currentFile != null) {
                // 复制完整路径到剪贴板
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("文件路径", currentFile.absolutePath)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "已复制完整路径", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
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

        // 资源管理器展开/关闭按钮长按事件
        findViewById<ImageView>(R.id.btn_toggle_sidebar)?.setOnLongClickListener {
            if (!sidebarVisible) {
                // 侧边栏未显示时，关闭最近打开的所有项
                if (recentFilesManager.size() > 0) {
                    DialogHelper.showConfirmDialog(
                        context = this,
                        title = "关闭所有最近文件",
                        message = "确定要关闭所有最近打开的文件和正在编辑的文件吗？",
                        onConfirm = {
                            // 清空最近文件列表
                            recentFilesManager.clear()
                            updateRecentFilesBar()

                            // 关闭正在编辑的文件
                            if (editorManager.isFileOpen()) {
                                clearEditor()
                            }

                            Toast.makeText(this, "已关闭所有文件", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // 即使没有最近文件，也要关闭正在编辑的文件
                    if (editorManager.isFileOpen()) {
                        DialogHelper.showConfirmDialog(
                            context = this,
                            title = "关闭正在编辑的文件",
                            message = "确定要关闭正在编辑的文件吗？",
                            onConfirm = {
                                clearEditor()
                                Toast.makeText(this, "已关闭正在编辑的文件", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Toast.makeText(this, "没有打开的文件", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // 侧边栏显示时，进入资源管理器设置
                showExplorerSettingsDialog()
            }
            true
        }

        // 文件树列表
        val fileTreeView = findViewById<ListView>(R.id.file_tree)
        fileTreeAdapter = FileTreeAdapter(this, fileTree)
        fileTreeView.adapter = fileTreeAdapter

        // 文件树点击事件
        fileTreeView.setOnItemClickListener { _, _, position, _ ->
            val item = fileTree[position]
            if (isSelectionMode) {
                if (item.isParentDir) {
                    // 点击".."退出选择模式并导航
                    exitSelectionMode()
                    onFileTreeItemClick(item)
                } else {
                    // 切换选中状态
                    val isSelected = fileTreeAdapter.toggleSelection(position)
                    // 如果取消选择后没有任何选中项，退出选择模式
                    if (!isSelected && !fileTreeAdapter.hasSelection()) {
                        exitSelectionMode()
                    }
                }
            } else {
                onFileTreeItemClick(item)
            }
        }

        // 文件树长按事件
        fileTreeView.setOnItemLongClickListener { _, _, position, _ ->
            val item = fileTree[position]
            // 如果长按的是"..."项，直接返回工作区根目录
            if (item.name == "..") {
                fileManager.navigateToRoot()
                loadCurrentDir()
                true
            } else if (isSelectionMode) {
                // 选择模式下显示批量操作菜单
                showBatchOptionsDialog()
                true
            } else {
                // 非选择模式显示单个文件管理弹窗
                showFileOptions(item)
                true
            }
        }

        // 文件树触摸事件（用于水平滑动触发选择）
        fileTreeView.setOnTouchListener { v, event ->
            val handled = slideSelectHelper.handleTouchEvent(
                fileTreeView,
                event,
                onEnterSelection = {
                    // 水平滑动触发选择模式
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        fileTreeAdapter.setSelectionMode(true)
                        Toast.makeText(this, "已进入选择模式", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { startPos: Int, endPos: Int, clearPrevious: Boolean, isDeselect: Boolean ->
                // 处理区间选择
                handleSlideSelection(startPos, endPos, clearPrevious, isDeselect)
            }
            // 如果正在选择模式中滑动，消费事件；否则让点击事件正常传递
            handled
        }

        // 编辑器点击事件 - 点击编辑器区域退出选择模式
        val editorScroll = findViewById<android.widget.ScrollView>(R.id.editor_scroll)
        editorScroll?.setOnClickListener {
            if (isSelectionMode) {
                exitSelectionMode()
            }
        }
        
        // 编辑器区域点击事件 - 点击空白区域退出选择模式
        val editorArea = findViewById<LinearLayout>(R.id.editor_area)
        editorArea?.setOnClickListener {
            if (isSelectionMode) {
                exitSelectionMode()
            }
        }
        
        // 空状态点击事件 - 点击退出选择模式
        val emptyState = findViewById<TextView>(R.id.empty_state)
        emptyState?.setOnClickListener {
            if (isSelectionMode) {
                exitSelectionMode()
            }
        }
        
        // 顶部栏元素点击事件 - 退出选择模式
        findViewById<TextView>(R.id.tv_workspace_path)?.setOnClickListener {
            if (isSelectionMode) {
                exitSelectionMode()
            } else {
                // 原有逻辑：判断当前是否有打开的文件
                if (editorManager.getCurrentFile() == null) {
                    val intent = Intent(this, WorkspaceManagementActivity::class.java)
                    intent.putExtra("current_workspace_path", workspacePath)
                    startActivity(intent)
                } else {
                    showFullPathDialog()
                }
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
                // 更新字数统计
                updateWordCount()
            }
        })

        // 字数统计栏详情按钮
        findViewById<ImageView>(R.id.btn_word_count_details)?.setOnClickListener {
            showWordCountDetailsDialog()
        }
    }

    private fun toggleSidebar() {
        val sidebar = findViewById<LinearLayout>(R.id.sidebar)
        sidebarVisible = !sidebarVisible
        sidebar?.visibility = if (sidebarVisible) View.VISIBLE else View.GONE
        // 保存侧边栏状态到持久化设置
        preferencesManager.saveSidebarState(sidebarVisible)
        // 关闭侧边栏时自动退出选择模式
        if (!sidebarVisible && isSelectionMode) {
            exitSelectionMode()
        }
        // 更新字数统计栏的文字显示
        updateWordCount()
    }

    private fun loadCurrentDir() {
        fileTree.clear()
        fileTree.addAll(fileManager.loadCurrentDir())
        // 重新创建适配器，传入当前文件信息和选择模式
        fileTreeAdapter = FileTreeAdapter(this, fileTree, editorManager.getCurrentFile(), isSelectionMode)
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
                // 更新字数统计
                updateWordCount()
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

    private fun showExplorerSettingsDialog() {
        // 资源管理器设置模式 - 显示勾选式排序对话框
        val sortOptions = arrayOf(
            "名称 (A→Z)",
            "名称 (Z→A)",
            "大小 (小→大)",
            "大小 (大→小)",
            "时间 (旧→新)",
            "时间 (新→旧)"
        )
        val sortModeList = listOf(
            FileManager.SortMode.NAME_ASC,
            FileManager.SortMode.NAME_DESC,
            FileManager.SortMode.SIZE_ASC,
            FileManager.SortMode.SIZE_DESC,
            FileManager.SortMode.MODIFIED_ASC,
            FileManager.SortMode.MODIFIED_DESC
        )

        // 获取当前选中的排序规则
        val currentSortModes = fileManager.getSortModes()
        val checkedItems = BooleanArray(sortModeList.size) { index ->
            sortModeList[index] in currentSortModes
        }

        DialogHelper.showMultiChoiceDialog(
            context = this,
            title = "资源管理器排序设置",
            items = sortOptions,
            checkedItems = checkedItems
        ) { selectedItems ->
            // 根据选中顺序构建排序规则列表
            val selectedModes = mutableListOf<FileManager.SortMode>()
            for (i in selectedItems.indices) {
                if (selectedItems[i]) {
                    selectedModes.add(sortModeList[i])
                }
            }

            if (selectedModes.isEmpty()) {
                // 如果没有选择任何排序规则，使用默认值
                selectedModes.add(FileManager.SortMode.NAME_ASC)
            }

            // 应用新的排序规则
            fileManager.setSortModes(selectedModes)
            preferencesManager.saveSortModes(selectedModes)

            // 构建排序规则显示名称
            val modeNames = selectedModes.map { mode ->
                when (mode) {
                    FileManager.SortMode.NAME_ASC -> "名称 (A→Z)"
                    FileManager.SortMode.NAME_DESC -> "名称 (Z→A)"
                    FileManager.SortMode.SIZE_ASC -> "大小 (小→大)"
                    FileManager.SortMode.SIZE_DESC -> "大小 (大→小)"
                    FileManager.SortMode.MODIFIED_ASC -> "时间 (旧→新)"
                    FileManager.SortMode.MODIFIED_DESC -> "时间 (新→旧)"
                }
            }

            Toast.makeText(
                this,
                "已按 ${modeNames.joinToString(" + ")} 排序",
                Toast.LENGTH_SHORT
            ).show()

            // 重新加载文件树
            loadCurrentDir()
        }
    }

    /**
     * 处理滑动选择（区间选择）
     * @param startPos 起始位置
     * @param endPos 结束位置
     * @param clearPrevious 是否清除之前的选择
     * @param isDeselect 是否是取消选择操作
     */
    private fun handleSlideSelection(startPos: Int, endPos: Int, clearPrevious: Boolean, isDeselect: Boolean) {
        // 如果是新选择操作，清除之前的选择
        if (clearPrevious) {
            fileTree.forEach { it.isSelected = false }
        }

        // 确定起始和结束位置（处理方向）
        val actualStart = minOf(startPos, endPos)
        val actualEnd = maxOf(startPos, endPos)

        if (isDeselect) {
            // 取消选择：取消选中区间内的所有项
            for (i in actualStart..actualEnd) {
                if (i in fileTree.indices) {
                    val item = fileTree[i]
                    if (!item.isParentDir) {
                        item.isSelected = false
                    }
                }
            }
        } else {
            // 正常选择：选中区间内的所有项
            for (i in actualStart..actualEnd) {
                if (i in fileTree.indices) {
                    val item = fileTree[i]
                    if (!item.isParentDir) {
                        item.isSelected = true
                    }
                }
            }
        }

        // 通知适配器更新
        fileTreeAdapter.notifyDataSetChanged()
        
        // 检查是否还有选中项，没有则退出选择模式
        if (!fileTreeAdapter.hasSelection()) {
            exitSelectionMode()
        }
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        slideSelectHelper.exitSelectionMode()
        fileTreeAdapter.setSelectionMode(false)
        fileTreeAdapter.clearSelection()
        Toast.makeText(this, "已退出批量选择模式", Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示批量操作菜单
     */
    private fun showBatchOptionsDialog() {
        val selectedCount = fileTreeAdapter.getSelectedItems().size
        val options = arrayOf("全选", "取消选择", "删除选中 ($selectedCount)", "退出选择模式")
        DialogHelper.showOptionsDialog(
            context = this,
            title = "批量操作",
            options = options
        ) { which ->
            when (which) {
                0 -> selectAll()
                1 -> clearSelection()
                2 -> batchDelete()
                3 -> exitSelectionMode()
            }
        }
    }

    private fun selectAll() {
        fileTree.forEach { if (!it.isParentDir) it.isSelected = true }
        fileTreeAdapter.notifyDataSetChanged()
    }

    private fun clearSelection() {
        fileTreeAdapter.clearSelection()
    }

    private fun batchDelete() {
        val selectedItems = fileTreeAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的文件", Toast.LENGTH_SHORT).show()
            return
        }

        DialogHelper.showConfirmDialog(
            context = this,
            title = "批量删除",
            message = "确定要删除选中的 ${selectedItems.size} 个项目吗？",
            onConfirm = {
                var successCount = 0
                var failCount = 0

                selectedItems.forEach { item ->
                    fileManager.deleteFile(item.file)
                        .onSuccess { successCount++ }
                        .onFailure { failCount++ }
                }

                val message = if (failCount == 0) {
                    "成功删除 $successCount 个项目"
                } else {
                    "成功删除 $successCount 个项目，失败 $failCount 个"
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                exitSelectionMode()
                loadCurrentDir()
            }
        )
    }

    private fun updateWordCount() {
        val editorContent = findViewById<EditText>(R.id.editor_content)?.text.toString()
        val wordCountBar = findViewById<LinearLayout>(R.id.word_count_bar)

        if (editorManager.isFileOpen()) {
            wordCountBar?.visibility = View.VISIBLE

            // 只在侧边栏关闭时显示字数统计文字
            if (!sidebarVisible) {
                val charCount = editorContent.length
                val lineCount = editorContent.lines().size
                findViewById<TextView>(R.id.tv_word_count)?.text = "$charCount 字符 | $lineCount 行"
            } else {
                findViewById<TextView>(R.id.tv_word_count)?.text = ""
            }
        } else {
            wordCountBar?.visibility = View.GONE
        }
    }

    private fun showWordCountDetailsDialog() {
        val editorContent = findViewById<EditText>(R.id.editor_content)?.text.toString()
        val lines = editorContent.lines()

        val charCount = editorContent.length
        val lineCount = lines.size
        val wordCount = editorContent.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val nonWhitespaceCount = editorContent.filter { !it.isWhitespace() }.length
        val maxLineLength = lines.maxOfOrNull { it.length } ?: 0

        val message = StringBuilder()
        message.append("字符数（含空格）: $charCount\n")
        message.append("字符数（不含空格）: $nonWhitespaceCount\n")
        message.append("单词数: $wordCount\n")
        message.append("行数: $lineCount\n")
        message.append("最长行字符数: $maxLineLength")

        DialogHelper.showMessageDialog(
            context = this,
            title = "字数统计详情",
            message = message.toString()
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
            if (item.isDirectory) {
                when (which) {
                    0 -> showRenameDialog(item)
                    1 -> showDeleteDialog(item)
                }
            } else {
                when (which) {
                    0 -> openFile(item.file)
                    1 -> showRenameDialog(item)
                    2 -> showDeleteDialog(item)
                }
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
        // 隐藏字数统计栏
        findViewById<LinearLayout>(R.id.word_count_bar)?.visibility = View.GONE
    }

    

    
}
