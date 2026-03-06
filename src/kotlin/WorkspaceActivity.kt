package com.venside.x1n

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.PopupMenu
import android.widget.Toast
import com.venside.x1n.adapters.FileTreeAdapter
import com.venside.x1n.components.EditorComponent
import com.venside.x1n.engine.WorkspaceEngine
import com.venside.x1n.managers.*
import com.venside.x1n.managers.interfaces.*
import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.DialogHelper
import com.venside.x1n.utils.SlideSelectHelper
import com.venside.x1n.viewmodel.WorkspaceViewModel
import java.io.File

class WorkspaceActivity : Activity() {

    private var workspacePath: String? = null
    private lateinit var engine: WorkspaceEngine
    private lateinit var viewModel: WorkspaceViewModel
    private lateinit var editorComponent: EditorComponent

    private val uiBindingManager: IUIBindingManager by lazy { UIBindingManager(this) }
    private val dialogCoordinator: IDialogCoordinator by lazy { DialogCoordinator() }
    private val uiStateManager: UIStateManager by lazy { 
        UIStateManager(engine.fileManager, engine.editorManager, workspacePath) 
    }
    private val sidebarManager: ISidebarManager by lazy { SidebarManager(this, engine.preferencesManager) }
    private val symbolBarManager: SymbolBarManager by lazy {
        SymbolBarManager(
            this,
            onSymbolInsert = { editorComponent.insertSymbol(it) },
            shouldShow = { viewModel.isFileOpen() && !viewModel.isTerminalVisible() }
        )
    }
    private val recentFilesBarManager: RecentFilesBarManager by lazy {
        RecentFilesBarManager(this, engine.recentFilesManager, engine.editorManager)
    }
    private val terminalManager = TerminalManager()
    private lateinit var fileTreeController: FileTreeController
    private val slideSelectHelper = SlideSelectHelper()
    private lateinit var fileTreeAdapter: FileTreeAdapter
    private var fileTree: MutableList<FileTreeItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace)

        workspacePath = intent.getStringExtra("workspace_path") ?: "/"
        engine = WorkspaceEngine(this, workspacePath!!)
        viewModel = WorkspaceViewModel(this, engine, terminalManager)
        editorComponent = EditorComponent(engine, uiStateManager)

        editorComponent.init(
            editorContent = uiBindingManager.getEditorContent(),
            lineNumbers = uiBindingManager.getLineNumbers(),
            undoButton = uiBindingManager.getUndoButton(),
            redoButton = uiBindingManager.getRedoButton(),
            wordCountBar = uiBindingManager.getWordCountBar(),
            wordCountTextView = uiBindingManager.getWordCountTextView()
        )
        editorComponent.onLineNumbersUpdate = {}
        editorComponent.onWordCountUpdate = { updateWordCount() }

        uiBindingManager.getTerminalPanel()?.let { terminalPanel ->
            uiBindingManager.getTerminalOutput()?.let { terminalOutput ->
                terminalManager.init(terminalPanel, terminalOutput)
            }
        }

        engine.fileManager.setSortModes(viewModel.getSortModes())
        sidebarManager.bindSidebar(uiBindingManager.getSidebar())
        sidebarManager.loadState()
        symbolBarManager.init()
        (engine.selectionManager as SelectionManager).setFileTree(fileTree)

        setupUI()
        loadCurrentDir()
    }

    private fun setupUI() {
        uiBindingManager.bindSidebarToggle(
            onToggle = { toggleSidebar() },
            onLongPress = { handleSidebarLongPress() }
        )

        uiBindingManager.bindRunButton { runLuaFile() }

        uiBindingManager.bindMenuButton { showMenu() }

        uiBindingManager.bindUndoRedoButtons(
            onUndo = { editorComponent.performUndo() },
            onRedo = { editorComponent.performRedo() }
        )

        uiBindingManager.bindPathDisplay(
            onLongPress = { handlePathLongPress() },
            onClick = { handlePathClick() }
        )

        uiBindingManager.bindDragDrop { fromIndex, toIndex ->
            viewModel.moveRecentFile(fromIndex, toIndex)
        }

        uiBindingManager.bindNewButton {
            dialogCoordinator.showNewOptionsDialog(
                context = this,
                onCreateFile = { fileName -> createFile(fileName) },
                onCreateFolder = { folderName -> createFolder(folderName) }
            )
        }

        uiBindingManager.bindFileTree(
            onItemClick = { position -> fileTreeController.handleFileTreeItemClick(position) },
            onItemLongClick = { position -> fileTreeController.handleFileTreeItemLongClick(position) },
            onTouch = { event -> fileTreeController.handleFileTreeTouch(event) }
        )

        uiBindingManager.bindEditorListeners(
            onEditorClick = { if (engine.selectionManager.isSelectionMode()) exitSelectionMode() },
            onContentChanged = { updateWordCount() },
            onTextChanged = { _, _, _ -> /* 由 EditorComponent 处理 */ }
        )

        uiBindingManager.getEditorContent()?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && viewModel.isFileOpen()) {
                uiBindingManager.showUndoRedoBar()
            } else {
                uiBindingManager.hideUndoRedoBar()
            }
        }

        uiBindingManager.bindWordCountButton {
            dialogCoordinator.showWordCountDetailsDialog(this, editorComponent.getContent())
        }

        uiBindingManager.getTerminalPanel()?.findViewById<android.widget.ImageView>(R.id.btn_clear_terminal)?.setOnClickListener {
            viewModel.clearTerminal()
        }
        uiBindingManager.getTerminalPanel()?.findViewById<android.widget.ImageView>(R.id.btn_hide_terminal)?.setOnClickListener {
            viewModel.hideTerminal()
        }
    }

    private fun showMenu() {
        val menuButton = uiBindingManager.getMenuButton() ?: return
        val popup = PopupMenu(this, menuButton)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_save -> { saveCurrentFile(); true }
                R.id.menu_terminal -> { viewModel.toggleTerminal(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun toggleSidebar() {
        sidebarManager.toggleSidebar()
        if (!sidebarManager.isSidebarVisible() && engine.selectionManager.isSelectionMode()) {
            exitSelectionMode()
        }
        updateWordCount()
    }

    private fun handleSidebarLongPress() {
        sidebarManager.handleLongPress(
            sidebarVisible = sidebarManager.isSidebarVisible(),
            hasRecentFiles = viewModel.getRecentFilesCount() > 0,
            hasOpenFile = viewModel.isFileOpen(),
            onCloseAll = {
                viewModel.clearRecentFiles()
                updateRecentFilesBar()
                if (viewModel.isFileOpen()) clearEditor()
            },
            onCloseCurrent = { clearEditor() },
            onShowSettings = { showExplorerSettingsDialog() }
        )
    }

    private fun handlePathLongPress(): Boolean {
        val currentFile = viewModel.getCurrentFile() ?: return false
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = ClipData.newPlainText("文件路径", currentFile.absolutePath)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制完整路径", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun handlePathClick() {
        when {
            engine.selectionManager.isSelectionMode() -> exitSelectionMode()
            viewModel.getCurrentFile() == null -> {
                val intent = Intent(this, WorkspaceManagementActivity::class.java)
                intent.putExtra("current_workspace_path", workspacePath)
                startActivity(intent)
            }
            else -> showFullPathDialog()
        }
    }

    private fun loadCurrentDir() {
        fileTree.clear()
        fileTree.addAll(viewModel.loadCurrentDir())
        fileTreeAdapter = FileTreeAdapter(
            this,
            fileTree,
            viewModel.getCurrentFile(),
            engine.selectionManager.isSelectionMode()
        )
        
        val fileTreeView = uiBindingManager.getFileTreeView()
        fileTreeAdapter.listView = fileTreeView
        fileTreeView?.adapter = fileTreeAdapter
        
        fileTreeController = FileTreeController(
            this,
            engine.fileTreeCoordinator,
            engine.selectionManager,
            fileTreeAdapter,
            slideSelectHelper
        )
        
        fileTreeController.setCallbacks(object : FileTreeController.FileTreeCallbacks {
            override fun onOpenFileWithUnsavedCheck(file: File) { openFileWithUnsavedCheck(file) }
            override fun onShowFileOptions(item: FileTreeItem) { showFileOptions(item) }
            override fun onShowBatchOptionsDialog(selectedCount: Int) { showBatchOptionsDialog() }
            override fun onExitSelectionMode() { exitSelectionMode() }
            override fun onRefreshFileTree() { loadCurrentDir() }
            override fun showToast(message: String) { showToast(message) }
        })
        
        updatePathDisplay()
    }

    private fun openFileWithUnsavedCheck(file: File) {
        if (viewModel.isCurrentFile(file)) return

        if (viewModel.hasUnsavedChanges(editorComponent.getContent())) {
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

    private fun saveCurrentFileAndOpen(file: File) = viewModel.saveFile(editorComponent.getContent(), { openFile(file) }, ::showError)

    private fun openFile(file: File) {
        viewModel.openFile(file,
            onSuccess = { content ->
                viewModel.ensureRecentFile(file)
                uiBindingManager.getEmptyState()?.visibility = android.view.View.GONE
                uiBindingManager.getEditorScroll()?.visibility = android.view.View.VISIBLE
                editorComponent.setContent(content)
                editorComponent.reset()
                updatePathDisplay()
                updateRecentFilesBar()
                loadCurrentDir()
                updateWordCount()
            },
            onFailure = ::showError
        )
    }

    private fun saveCurrentFile() {
        viewModel.saveFile(editorComponent.getContent(),
            onSuccess = {
                updateFileSizeInTree()
                showToast("文件已保存")
            },
            onFailure = ::showError
        )
    }

    private fun createFile(fileName: String) = viewModel.createFile(fileName, { loadCurrentDir() }, ::showError)

    private fun createFolder(folderName: String) = viewModel.createFolder(folderName, { loadCurrentDir() }, ::showError)

    private fun renameFile(file: File, newName: String) = viewModel.renameFile(file, newName, { loadCurrentDir() }, ::showError)

    private fun deleteFile(file: File) {
        viewModel.deleteFile(file,
            onSuccess = {
                viewModel.removeRecentFile(file)
                updateRecentFilesBar()
                if (viewModel.isCurrentFile(file)) clearEditor()
                loadCurrentDir()
            },
            onFailure = ::showError
        )
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun showError(message: String) = Toast.makeText(this, "错误: $message", Toast.LENGTH_SHORT).show()

    private fun runLuaFile() {
        val currentFile = viewModel.getCurrentFile()

        when {
            currentFile == null -> showToast("请先打开一个 Lua 文件")
            !viewModel.canExecuteLua(currentFile) -> showToast("当前文件不是 Lua 文件")
            viewModel.hasUnsavedChanges(editorComponent.getContent()) -> {
                DialogHelper.showConfirmDialog(
                    context = this,
                    title = "未保存的更改",
                    message = "执行前需要保存文件，是否保存？",
                    positiveText = "保存并执行",
                    negativeText = "取消",
                    onConfirm = {
                        saveCurrentFile()
                        viewModel.executeLuaFile(currentFile)
                    }
                )
            }
            else -> viewModel.executeLuaFile(currentFile)
        }
    }

    private fun exitSelectionMode() = fileTreeController.exitSelectionMode()

    private fun showBatchOptionsDialog() {
        dialogCoordinator.showBatchOptionsDialog(
            context = this,
            selectedCount = fileTreeController.getSelectedItems().size,
            onSelectAll = { fileTreeController.selectAll() },
            onClearSelection = { fileTreeController.clearSelection() },
            onBatchDelete = { performBatchDelete() },
            onExitSelectionMode = { exitSelectionMode() }
        )
    }

    private fun performBatchDelete() {
        val selectedItems = fileTreeController.getSelectedItems()
        if (selectedItems.isEmpty()) {
            showToast("请先选择要删除的文件")
            return
        }

        DialogHelper.showConfirmDialog(
            context = this,
            title = "批量删除",
            message = "确定要删除选中的 ${selectedItems.size} 个项目吗？",
            onConfirm = {
                viewModel.batchDelete(selectedItems.map { it.file }) { _, _ ->
                    exitSelectionMode()
                    loadCurrentDir()
                }
            }
        )
    }

    private fun showFileOptions(item: FileTreeItem) {
        dialogCoordinator.showFileOptionsDialog(
            context = this,
            item = item,
            onOpen = { if (!item.isDirectory) openFileWithUnsavedCheck(item.file) },
            onRename = { showRenameDialog(it) },
            onDelete = { 
                dialogCoordinator.showDeleteDialog(
                    context = this,
                    item = it,
                    onDelete = ::deleteFile
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
        val fullPath = viewModel.getCurrentFile()?.absolutePath
            ?: viewModel.getRootDir()?.absolutePath
            ?: "/workspace"
        dialogCoordinator.showFullPathDialog(this, fullPath)
    }

    private fun showExplorerSettingsDialog() {
        dialogCoordinator.showExplorerSettingsDialog(
            context = this,
            currentSortModes = viewModel.getSortModes(),
            onApply = { modes ->
                viewModel.setSortModes(modes)
                viewModel.saveSortModes(modes)
                val modeNames = (dialogCoordinator as DialogCoordinator).getSortModeNames(modes)
                showToast("已按 $modeNames 排序")
                loadCurrentDir()
            }
        )
    }

    private fun updatePathDisplay() = uiStateManager.updatePathDisplay(
        uiBindingManager.getPathTextView(),
        uiBindingManager.getSidebarPathTextView()
    )

    private fun updateWordCount() = editorComponent.updateWordCountDisplay(
        sidebarVisible = sidebarManager.isSidebarVisible(),
        isFileOpen = viewModel.isFileOpen()
    )

    private fun updateRecentFilesBar() {
        val container = uiBindingManager.getRecentFilesContainer() ?: return

        recentFilesBarManager.updateRecentFilesBar(
            container = container,
            onFileClick = { file -> openFileWithUnsavedCheck(file) },
            onDeleteClick = { file, index ->
                val isCurrentFile = viewModel.isCurrentFile(file)
                engine.recentFilesManager.removeFileAt(index)
                updateRecentFilesBar()
                if (isCurrentFile) clearEditor()
            }
        )
    }

    private fun updateFileSizeInTree() {
        val currentFile = viewModel.getCurrentFile() ?: return

        for (item in fileTree) {
            if (!item.isDirectory && item.filePath == currentFile.absolutePath) {
                fileTree[fileTree.indexOf(item)] = FileTreeItem.fromFile(currentFile)
                break
            }
        }
        fileTreeAdapter.notifyDataSetChanged()
    }

    private fun clearEditor() {
        uiStateManager.clearEditor(
            emptyState = uiBindingManager.getEmptyState(),
            editorScroll = uiBindingManager.getEditorScroll(),
            editorContent = uiBindingManager.getEditorContent(),
            lineNumbers = uiBindingManager.getLineNumbers(),
            wordCountBar = uiBindingManager.getWordCountBar()
        )
        editorComponent.reset()
        updatePathDisplay()
        loadCurrentDir()
    }

    override fun onBackPressed() = handleBackPress()

    private fun handleBackPress() {
        if (viewModel.hasUnsavedChanges(editorComponent.getContent())) {
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
