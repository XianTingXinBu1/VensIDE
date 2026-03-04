package com.venside.x1n

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import com.venside.x1n.adapters.FileTreeAdapter
import com.venside.x1n.managers.BackPressHandler
import com.venside.x1n.managers.DialogCoordinator
import com.venside.x1n.managers.EditorManager
import com.venside.x1n.managers.FileManager
import com.venside.x1n.managers.FileOperationManager
import com.venside.x1n.managers.FileTreeCoordinator
import com.venside.x1n.managers.PreferencesManager
import com.venside.x1n.managers.RecentFilesBarManager
import com.venside.x1n.managers.RecentFilesManager
import com.venside.x1n.managers.SelectionManager
import com.venside.x1n.managers.SidebarManager
import com.venside.x1n.managers.SymbolBarManager
import com.venside.x1n.managers.UIBindingManager
import com.venside.x1n.managers.UIStateManager
import com.venside.x1n.managers.interfaces.IBackPressHandler
import com.venside.x1n.managers.interfaces.IDialogCoordinator
import com.venside.x1n.managers.interfaces.IFileOperationManager
import com.venside.x1n.managers.interfaces.IFileTreeCoordinator
import com.venside.x1n.managers.interfaces.ISelectionManager
import com.venside.x1n.managers.interfaces.ISidebarManager
import com.venside.x1n.managers.interfaces.IUIBindingManager
import com.venside.x1n.managers.interfaces.IUIStateManager
import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.DialogHelper
import com.venside.x1n.utils.DragDropHelper
import com.venside.x1n.utils.SlideSelectHelper
import java.io.File

class WorkspaceActivity : Activity() {

    private var workspacePath: String? = null

    // 核心管理器
    private val fileManager = FileManager()
    private val editorManager = EditorManager()
    private val recentFilesManager = RecentFilesManager()
    private val preferencesManager: PreferencesManager by lazy { PreferencesManager(this) }

    // 解耦后的管理器（通过接口类型注入）
    private val uiBindingManager: IUIBindingManager by lazy { UIBindingManager(this) }
    private val selectionManager: ISelectionManager by lazy { SelectionManager(fileManager) }
    private val dialogCoordinator: IDialogCoordinator by lazy { DialogCoordinator() }
    private val uiStateManager: IUIStateManager by lazy { 
        UIStateManager(fileManager, editorManager, workspacePath) 
    }
    private val backPressHandler: IBackPressHandler by lazy { 
        BackPressHandler(editorManager, fileManager) 
    }
    private val fileTreeCoordinator: IFileTreeCoordinator by lazy { 
        FileTreeCoordinator(fileManager) 
    }
    private val fileOperationManager: IFileOperationManager by lazy {
        FileOperationManager(this, editorManager, fileTreeCoordinator)
    }
    private val sidebarManager: ISidebarManager by lazy {
        SidebarManager(this, preferencesManager)
    }

    // UI 管理器
    private val symbolBarManager: SymbolBarManager by lazy {
        SymbolBarManager(
            this,
            onSymbolInsert = { symbol -> insertSymbol(symbol) },
            shouldShow = { editorManager.isFileOpen() }
        )
    }
    private val recentFilesBarManager: RecentFilesBarManager by lazy {
        RecentFilesBarManager(this, recentFilesManager, editorManager)
    }

    // 辅助类
    private val dragDropHelper = DragDropHelper()
    private val slideSelectHelper = SlideSelectHelper()

    // 适配器和数据
    private lateinit var fileTreeAdapter: FileTreeAdapter
    private var fileTree: MutableList<FileTreeItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace)

        workspacePath = intent.getStringExtra("workspace_path")
        fileManager.init(workspacePath ?: "/")

        // 加载设置
        fileManager.setSortModes(preferencesManager.getSortModes())
        sidebarManager.bindSidebar(uiBindingManager.getSidebar())
        sidebarManager.loadState()

        // 初始化
        symbolBarManager.init()
        (selectionManager as SelectionManager).setFileTree(fileTree)

        setupUI()
        loadCurrentDir()
    }

    private fun setupUI() {
        uiBindingManager.bindSidebarToggle(
            onToggle = { toggleSidebar() },
            onLongPress = { handleSidebarLongPress() }
        )

        uiBindingManager.bindNavigationButtons(
            onBack = { handleBackPress() },
            onSave = { saveCurrentFile() }
        )

        uiBindingManager.bindPathDisplay(
            onLongPress = { handlePathLongPress() },
            onClick = { handlePathClick() }
        )

        uiBindingManager.bindDragDrop { fromIndex, toIndex ->
            recentFilesManager.moveFile(fromIndex, toIndex)
        }

        uiBindingManager.bindNewButton {
            dialogCoordinator.showNewOptionsDialog(
                context = this,
                onCreateFile = { fileName -> createFile(fileName) },
                onCreateFolder = { folderName -> createFolder(folderName) }
            )
        }

        uiBindingManager.bindFileTree(
            onItemClick = { position -> handleFileTreeItemClick(position) },
            onItemLongClick = { position -> handleFileTreeItemLongClick(position) },
            onTouch = { event -> handleFileTreeTouch(event) }
        )

        uiBindingManager.bindEditorListeners(
            onEditorClick = { if (selectionManager.isSelectionMode()) exitSelectionMode() },
            onContentChanged = { updateLineNumbers(); updateWordCount() }
        )

        uiBindingManager.bindWordCountButton {
            val content = uiBindingManager.getEditorContent()?.text.toString()
            dialogCoordinator.showWordCountDetailsDialog(this, content)
        }
    }

    // ==================== 侧边栏操作 ====================

    private fun toggleSidebar() {
        sidebarManager.toggleSidebar()
        if (!sidebarManager.isSidebarVisible() && selectionManager.isSelectionMode()) {
            exitSelectionMode()
        }
        updateWordCount()
    }

    private fun handleSidebarLongPress() {
        sidebarManager.handleLongPress(
            sidebarVisible = sidebarManager.isSidebarVisible(),
            hasRecentFiles = recentFilesManager.size() > 0,
            hasOpenFile = editorManager.isFileOpen(),
            onCloseAll = {
                recentFilesManager.clear()
                updateRecentFilesBar()
                if (editorManager.isFileOpen()) clearEditor()
            },
            onCloseCurrent = { clearEditor() },
            onShowSettings = { showExplorerSettingsDialog() }
        )
    }

    // ==================== 路径显示操作 ====================

    private fun handlePathLongPress(): Boolean {
        val currentFile = editorManager.getCurrentFile()
        return if (currentFile != null) {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
                as android.content.ClipboardManager
            val clip = ClipData.newPlainText("文件路径", currentFile.absolutePath)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "已复制完整路径", Toast.LENGTH_SHORT).show()
            true
        } else {
            false
        }
    }

    private fun handlePathClick() {
        if (selectionManager.isSelectionMode()) {
            exitSelectionMode()
        } else if (editorManager.getCurrentFile() == null) {
            val intent = Intent(this, WorkspaceManagementActivity::class.java)
            intent.putExtra("current_workspace_path", workspacePath)
            startActivity(intent)
        } else {
            showFullPathDialog()
        }
    }

    // ==================== 文件树操作 ====================

    private fun loadCurrentDir() {
        fileTree.clear()
        fileTree.addAll(fileTreeCoordinator.loadCurrentDir())
        fileTreeAdapter = FileTreeAdapter(
            this, 
            fileTree, 
            editorManager.getCurrentFile(), 
            selectionManager.isSelectionMode()
        )
        uiBindingManager.getFileTreeView()?.adapter = fileTreeAdapter
        updatePathDisplay()
    }

    private fun handleFileTreeItemClick(position: Int) {
        val item = fileTree[position]
        
        if (selectionManager.isSelectionMode()) {
            if (item.isParentDir) {
                exitSelectionMode()
                onFileTreeItemClick(item)
            } else {
                val isSelected = selectionManager.toggleSelection(position)
                fileTreeAdapter.notifyDataSetChanged()
                if (!isSelected && !selectionManager.hasSelection()) {
                    exitSelectionMode()
                }
            }
        } else {
            onFileTreeItemClick(item)
        }
    }

    private fun handleFileTreeItemLongClick(position: Int): Boolean {
        val item = fileTree[position]
        
        if (item.name == "..") {
            fileTreeCoordinator.navigateToRoot()
            loadCurrentDir()
        } else if (selectionManager.isSelectionMode()) {
            showBatchOptionsDialog()
        } else {
            showFileOptions(item)
        }
        return true
    }

    private fun handleFileTreeTouch(event: MotionEvent): Boolean {
        return slideSelectHelper.handleTouchEvent(
            uiBindingManager.getFileTreeView()!!,
            event,
            onEnterSelection = {
                if (!selectionManager.isSelectionMode()) {
                    selectionManager.enterSelectionMode()
                    fileTreeAdapter.setSelectionMode(true)
                    Toast.makeText(this, "已进入选择模式", Toast.LENGTH_SHORT).show()
                }
            }
        ) { startPos, endPos, clearPrevious, isDeselect ->
            handleSlideSelection(startPos, endPos, clearPrevious, isDeselect)
        }
    }

    private fun onFileTreeItemClick(item: FileTreeItem) {
        fileTreeCoordinator.onFileTreeItemClick(
            item = item,
            onOpenFile = { file -> openFileWithUnsavedCheck(file) },
            onNavigate = { loadCurrentDir() }
        )
    }

    // ==================== 文件操作 ====================

    private fun openFileWithUnsavedCheck(file: File) {
        val content = uiBindingManager.getEditorContent()?.text.toString() ?: ""
        val hasUnsavedChanges = backPressHandler.isContentModified(content)
        
        if (hasUnsavedChanges) {
            DialogHelper.showConfirmDialog(
                context = this,
                title = "未保存的更改",
                message = "当前文件有未保存的更改，是否保存？",
                positiveText = "保存并打开",
                negativeText = "不保存打开",
                onConfirm = { saveCurrentFileAndOpen(file) },
                onCancel = { openFile(file) }
            )
        } else {
            openFile(file)
        }
    }

    private fun saveCurrentFileAndOpen(file: File) {
        val content = uiBindingManager.getEditorContent()?.text.toString() ?: ""
        fileOperationManager.saveFile(content, { openFile(file) }, { showError(it) })
    }

    private fun openFile(file: File) {
        fileOperationManager.openFile(file,
            onSuccess = { content ->
                recentFilesManager.addFile(file)
                uiBindingManager.getEmptyState()?.visibility = android.view.View.GONE
                uiBindingManager.getEditorScroll()?.visibility = android.view.View.VISIBLE
                uiBindingManager.getEditorContent()?.setText(content)
                updatePathDisplay()
                updateLineNumbers()
                updateRecentFilesBar()
                loadCurrentDir()
                updateWordCount()
            },
            onFailure = { showError(it) }
        )
    }

    private fun saveCurrentFile() {
        val content = uiBindingManager.getEditorContent()?.text.toString() ?: ""
        fileOperationManager.saveFile(content,
            onSuccess = {
                updateFileSizeInTree()
                Toast.makeText(this, "文件已保存", Toast.LENGTH_SHORT).show()
            },
            onFailure = { showError(it) }
        )
    }

    private fun createFile(fileName: String) {
        fileOperationManager.createFile(fileName, { loadCurrentDir() }, { showError(it) })
    }

    private fun createFolder(folderName: String) {
        fileOperationManager.createFolder(folderName, { loadCurrentDir() }, { showError(it) })
    }

    private fun renameFile(file: File, newName: String) {
        fileOperationManager.renameFile(file, newName, { loadCurrentDir() }, { showError(it) })
    }

    private fun deleteFile(file: File) {
        fileOperationManager.deleteFile(file,
            onSuccess = {
                recentFilesManager.removeFile(file)
                updateRecentFilesBar()
                if (editorManager.isCurrentFile(file)) clearEditor()
                loadCurrentDir()
            },
            onFailure = { showError(it) }
        )
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ==================== 选择模式操作 ====================

    private fun handleSlideSelection(startPos: Int, endPos: Int, clearPrevious: Boolean, isDeselect: Boolean) {
        selectionManager.handleSlideSelection(startPos, endPos, clearPrevious, isDeselect)
        fileTreeAdapter.notifyDataSetChanged()
        
        if (!selectionManager.hasSelection()) {
            exitSelectionMode()
        }
    }

    private fun exitSelectionMode() {
        selectionManager.exitSelectionMode()
        slideSelectHelper.exitSelectionMode()
        fileTreeAdapter.setSelectionMode(false)
        fileTreeAdapter.clearSelection()
        Toast.makeText(this, "已退出批量选择模式", Toast.LENGTH_SHORT).show()
    }

    private fun showBatchOptionsDialog() {
        dialogCoordinator.showBatchOptionsDialog(
            context = this,
            selectedCount = selectionManager.getSelectedItems().size,
            onSelectAll = {
                selectionManager.selectAll()
                fileTreeAdapter.notifyDataSetChanged()
            },
            onClearSelection = {
                selectionManager.clearSelection()
                fileTreeAdapter.notifyDataSetChanged()
            },
            onBatchDelete = { performBatchDelete() },
            onExitSelectionMode = { exitSelectionMode() }
        )
    }

    private fun performBatchDelete() {
        val selectedItems = selectionManager.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的文件", Toast.LENGTH_SHORT).show()
            return
        }

        DialogHelper.showConfirmDialog(
            context = this,
            title = "批量删除",
            message = "确定要删除选中的 ${selectedItems.size} 个项目吗？",
            onConfirm = {
                fileOperationManager.batchDelete(selectedItems.map { it.file }) { _, _ ->
                    exitSelectionMode()
                    loadCurrentDir()
                }
            }
        )
    }

    // ==================== 对话框操作 ====================

    private fun showFileOptions(item: FileTreeItem) {
        dialogCoordinator.showFileOptionsDialog(
            context = this,
            item = item,
            onOpen = { if (!item.isDirectory) openFileWithUnsavedCheck(item.file) },
            onRename = { dialogItem -> showRenameDialog(dialogItem) },
            onDelete = { dialogItem -> 
                dialogCoordinator.showDeleteDialog(
                    context = this,
                    item = dialogItem,
                    onDelete = { file -> deleteFile(file) }
                )
            }
        )
    }

    private fun showRenameDialog(item: FileTreeItem) {
        dialogCoordinator.showRenameDialog(
            context = this,
            item = item,
            onRename = { file, newName -> renameFile(file, newName) }
        )
    }

    private fun showFullPathDialog() {
        val fullPath = editorManager.getCurrentFile()?.absolutePath 
            ?: fileTreeCoordinator.getRootDir()?.absolutePath 
            ?: "/workspace"
        dialogCoordinator.showFullPathDialog(this, fullPath)
    }

    private fun showExplorerSettingsDialog() {
        dialogCoordinator.showExplorerSettingsDialog(
            context = this,
            currentSortModes = fileManager.getSortModes(),
            onApply = { modes ->
                fileManager.setSortModes(modes)
                preferencesManager.saveSortModes(modes)
                val modeNames = (dialogCoordinator as DialogCoordinator).getSortModeNames(modes)
                Toast.makeText(this, "已按 $modeNames 排序", Toast.LENGTH_SHORT).show()
                loadCurrentDir()
            }
        )
    }

    // ==================== UI 更新操作 ====================

    private fun updatePathDisplay() {
        uiStateManager.updatePathDisplay(
            uiBindingManager.getPathTextView(),
            uiBindingManager.getSidebarPathTextView()
        )
    }

    private fun updateLineNumbers() {
        uiStateManager.updateLineNumbers(
            uiBindingManager.getEditorContent(),
            uiBindingManager.getLineNumbers()
        )
    }

    private fun updateWordCount() {
        uiStateManager.updateWordCount(
            editorContent = uiBindingManager.getEditorContent(),
            wordCountBar = uiBindingManager.getWordCountBar(),
            wordCountTextView = uiBindingManager.getWordCountTextView(),
            sidebarVisible = sidebarManager.isSidebarVisible(),
            isFileOpen = editorManager.isFileOpen()
        )
    }

    private fun updateRecentFilesBar() {
        val container = uiBindingManager.getRecentFilesContainer() ?: return

        recentFilesBarManager.updateRecentFilesBar(
            container = container,
            onFileClick = { file -> openFileWithUnsavedCheck(file) },
            onDeleteClick = { file, index ->
                val isCurrentFile = editorManager.isCurrentFile(file)
                recentFilesManager.removeFileAt(index)
                updateRecentFilesBar()
                if (isCurrentFile) clearEditor()
            }
        )
    }

    private fun updateFileSizeInTree() {
        val currentFile = editorManager.getCurrentFile() ?: return

        for (item in fileTree) {
            if (!item.isDirectory && item.filePath == currentFile.absolutePath) {
                val updatedItem = FileTreeItem.fromFile(currentFile)
                fileTree[fileTree.indexOf(item)] = updatedItem
                break
            }
        }
        fileTreeAdapter.notifyDataSetChanged()
    }

    private fun insertSymbol(symbol: String) {
        uiStateManager.insertSymbol(
            editorContent = uiBindingManager.getEditorContent(),
            symbol = symbol,
            onInserted = { updateLineNumbers() }
        )
    }

    private fun clearEditor() {
        uiStateManager.clearEditor(
            emptyState = uiBindingManager.getEmptyState(),
            editorScroll = uiBindingManager.getEditorScroll(),
            editorContent = uiBindingManager.getEditorContent(),
            lineNumbers = uiBindingManager.getLineNumbers(),
            wordCountBar = uiBindingManager.getWordCountBar()
        )
        updatePathDisplay()
        loadCurrentDir()
    }

    // ==================== 返回键处理 ====================

    override fun onBackPressed() {
        handleBackPress()
    }

    private fun handleBackPress() {
        val content = uiBindingManager.getEditorContent()?.text.toString() ?: ""
        val hasUnsavedChanges = backPressHandler.isContentModified(content)

        if (hasUnsavedChanges) {
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
                onCancel = { finish() }
            )
        } else {
            DialogHelper.showConfirmDialog(
                context = this,
                title = "返回主页",
                message = "确定要返回主页吗？",
                onConfirm = { finish() }
            )
        }
    }
}
