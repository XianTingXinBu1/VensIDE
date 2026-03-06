package com.venside.x1n.engine

import android.content.Context
import com.venside.x1n.lua.LuaEngine
import com.venside.x1n.managers.*
import com.venside.x1n.managers.interfaces.*

/**
 * 工作空间引擎 - 聚合所有非 UI 相关的业务逻辑和数据管理
 * 
 * 职责：
 * - 管理所有业务逻辑相关的 Manager
 * - 提供统一的业务接口
 * - 隐藏内部复杂的 Manager 依赖关系
 * 
 * 这个类是 WorkspaceActivity 的"业务大脑"，负责所有数据操作和业务逻辑
 */
class WorkspaceEngine(
    private val context: Context,
    val workspacePath: String
) {

    // ==================== 核心业务 Manager ====================

    /** 文件管理器 */
    val fileManager = FileManager().apply { init(workspacePath) }

    /** 编辑器管理器 */
    val editorManager = EditorManager()

    /** 撤销重做管理器 */
    val undoRedoManager = UndoRedoManager()

    /** 最近文件管理器 */
    val recentFilesManager = RecentFilesManager()

    /** 设置管理器 */
    val preferencesManager = PreferencesManager(context)

    /** Lua 执行引擎 */
    val luaEngine = LuaEngine()

    // ==================== 选择与文件树管理 ====================

    /** 选择管理器 */
    val selectionManager: ISelectionManager = SelectionManager(fileManager)

    /** 文件树协调器 */
    val fileTreeCoordinator: IFileTreeCoordinator = FileTreeCoordinator(fileManager)

    /** 文件操作管理器 */
    val fileOperationManager: IFileOperationManager = FileOperationManager(
        context,
        editorManager,
        fileTreeCoordinator
    )

    // ==================== 辅助管理器 ====================

    /** 返回键处理管理器 */
    val backPressHandler: IBackPressHandler = BackPressHandler(editorManager, fileManager)

    // ==================== 业务接口封装 ====================

    /**
     * 检查是否有未保存的更改
     */
    fun hasUnsavedChanges(currentContent: String): Boolean {
        return backPressHandler.isContentModified(currentContent)
    }

    /**
     * 保存文件
     */
    fun saveFile(
        content: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        fileOperationManager.saveFile(content, onSuccess, onFailure)
    }

    /**
     * 批量删除文件
     */
    fun batchDelete(
        files: List<java.io.File>,
        onComplete: (Int, Int) -> Unit
    ) {
        fileOperationManager.batchDelete(files, onComplete)
    }

    /**
     * 加载当前目录文件列表
     */
    fun loadCurrentDir(): List<com.venside.x1n.models.FileTreeItem> {
        return fileTreeCoordinator.loadCurrentDir()
    }

    /**
     * 执行 Lua 文件
     */
    fun executeLuaFile(file: java.io.File): com.venside.x1n.lua.LuaResult {
        return luaEngine.executeFile(file)
    }

    /**
     * 添加到最近文件列表
     */
    fun ensureRecentFile(file: java.io.File) {
        recentFilesManager.ensureFile(file)
    }

    /**
     * 从最近文件列表移除
     */
    fun removeRecentFile(file: java.io.File) {
        recentFilesManager.removeFile(file)
    }

    /**
     * 移动最近文件位置
     */
    fun moveRecentFile(fromIndex: Int, toIndex: Int) {
        recentFilesManager.moveFile(fromIndex, toIndex)
    }

    /**
     * 获取最近文件数量
     */
    fun getRecentFilesCount(): Int = recentFilesManager.size()

    /**
     * 清空最近文件列表
     */
    fun clearRecentFiles() {
        recentFilesManager.clear()
    }

    /**
     * 获取排序模式
     */
    fun getSortModes(): List<com.venside.x1n.managers.FileManager.SortMode> {
        return fileManager.getSortModes()
    }

    /**
     * 设置排序模式
     */
    fun setSortModes(modes: List<com.venside.x1n.managers.FileManager.SortMode>) {
        fileManager.setSortModes(modes)
    }

    /**
     * 保存排序模式到设置
     */
    fun saveSortModes(modes: List<com.venside.x1n.managers.FileManager.SortMode>) {
        preferencesManager.saveSortModes(modes)
    }

    /**
     * 加载排序模式
     */
    fun loadSortModes(): List<com.venside.x1n.managers.FileManager.SortMode> {
        return preferencesManager.getSortModes()
    }

    /**
     * 执行撤销操作
     */
    fun undo(currentContent: String): String? {
        return undoRedoManager.undo(currentContent)
    }

    /**
     * 执行重做操作
     */
    fun redo(currentContent: String): String? {
        return undoRedoManager.redo(currentContent)
    }

    /**
     * 检查是否可以撤销
     */
    fun canUndo(): Boolean = undoRedoManager.canUndo()

    /**
     * 检查是否可以重做
     */
    fun canRedo(): Boolean = undoRedoManager.canRedo()

    /**
     * 重置撤销重做状态
     */
    fun resetUndoRedo() {
        undoRedoManager.reset()
    }

    /**
     * 记录文本删除操作
     */
    fun recordDelete(start: Int, text: String) {
        undoRedoManager.recordDelete(start, text)
    }

    /**
     * 记录文本插入操作
     */
    fun recordInsert(start: Int, text: String) {
        undoRedoManager.recordInsert(start, text)
    }
}