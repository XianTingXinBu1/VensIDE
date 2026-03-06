package com.venside.x1n.viewmodel

import android.content.Context
import com.venside.x1n.engine.WorkspaceEngine
import com.venside.x1n.lua.LuaException
import com.venside.x1n.managers.TerminalManager
import java.io.File

/**
 * 工作空间视图模型 - 处理业务逻辑和状态管理
 * 
 * 职责：
 * - 文件操作业务逻辑
 * - Lua 执行逻辑
 * - 终端输出管理
 * - 业务状态管理
 */
class WorkspaceViewModel(
    private val context: Context,
    private val engine: WorkspaceEngine,
    private val terminalManager: TerminalManager
) {
    
    // ==================== UI 状态 ====================
    
    data class UiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val isError: Boolean = false
    )
    
    private var _uiState = UiState()
    val uiState: UiState get() = _uiState
    
    // ==================== 文件操作 ====================
    
    /**
     * 保存当前文件
     */
    fun saveFile(
        content: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        engine.saveFile(content, onSuccess, onFailure)
    }
    
    /**
     * 打开文件
     */
    fun openFile(
        file: File,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        engine.fileOperationManager.openFile(file, onSuccess, onFailure)
    }
    
    /**
     * 创建新文件
     */
    fun createFile(
        fileName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        engine.fileOperationManager.createFile(fileName, onSuccess, onFailure)
    }
    
    /**
     * 创建新文件夹
     */
    fun createFolder(
        folderName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        engine.fileOperationManager.createFolder(folderName, onSuccess, onFailure)
    }
    
    /**
     * 重命名文件
     */
    fun renameFile(
        file: File,
        newName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        engine.fileOperationManager.renameFile(file, newName, onSuccess, onFailure)
    }
    
    /**
     * 删除文件
     */
    fun deleteFile(
        file: File,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        engine.fileOperationManager.deleteFile(file, onSuccess, onFailure)
    }
    
    /**
     * 批量删除文件
     */
    fun batchDelete(
        files: List<File>,
        onComplete: (Int, Int) -> Unit
    ) {
        engine.batchDelete(files, onComplete)
    }
    
    /**
     * 加载当前目录
     */
    fun loadCurrentDir(): List<com.venside.x1n.models.FileTreeItem> {
        return engine.loadCurrentDir()
    }
    
    // ==================== Lua 执行 ====================
    
    /**
     * 执行 Lua 文件
     */
    fun executeLuaFile(file: File): LuaExecutionResult {
        // 显示终端
        terminalManager.showTerminal()
        
        // 添加分隔线，区分不同的执行记录
        if (terminalManager.hasHistory()) {
            terminalManager.appendTerminalOutput("══════════════════════════════════", isError = false)
        }
        terminalManager.appendTerminalOutput("正在执行: ${file.name}")

        return try {
            val result = engine.executeLuaFile(file)

            if (result.isSuccess) {
                // 先显示print输出（如果有）
                if (result.output.isNotEmpty()) {
                    terminalManager.appendTerminalOutput("输出:", isError = false)
                    // 按行显示print输出
                    result.output.lines().forEach { line ->
                        if (line.isNotBlank()) {
                            terminalManager.appendTerminalOutput(line, isError = false)
                        }
                    }
                }

                // 再显示返回值（如果有）
                if (result.returnValue.isNotEmpty()) {
                    terminalManager.appendTerminalOutput("返回值: ${result.returnValue}", isError = false)
                }

                terminalManager.appendTerminalOutput("执行成功", isError = false)
                LuaExecutionResult.Success(result)
            } else {
                val errorMsg = "执行失败\n错误: ${result.message}\n行号: ${result.lineNumber}"
                terminalManager.appendTerminalOutput(errorMsg, isError = true)
                LuaExecutionResult.Error(result.message, result.lineNumber)
            }
        } catch (e: LuaException) {
            terminalManager.appendTerminalOutput("执行异常: ${e.message ?: "未知错误"}", isError = true)
            LuaExecutionResult.Error(e.message ?: "未知错误", -1)
        } catch (e: Exception) {
            terminalManager.appendTerminalOutput("执行异常: ${e.message ?: "未知错误"}", isError = true)
            LuaExecutionResult.Error(e.message ?: "未知错误", -1)
        }
    }
    
    /**
     * 检查文件是否可以执行 Lua
     */
    fun canExecuteLua(file: File?): Boolean {
        return file != null && file.name.endsWith(".lua", ignoreCase = true)
    }
    
    // ==================== 最近文件管理 ====================
    
    /**
     * 添加到最近文件列表
     */
    fun ensureRecentFile(file: File) {
        engine.ensureRecentFile(file)
    }
    
    /**
     * 从最近文件列表移除
     */
    fun removeRecentFile(file: File) {
        engine.removeRecentFile(file)
    }
    
    /**
     * 移动最近文件位置
     */
    fun moveRecentFile(fromIndex: Int, toIndex: Int) {
        engine.moveRecentFile(fromIndex, toIndex)
    }
    
    /**
     * 获取最近文件数量
     */
    fun getRecentFilesCount(): Int = engine.getRecentFilesCount()
    
    /**
     * 清空最近文件列表
     */
    fun clearRecentFiles() {
        engine.clearRecentFiles()
    }
    
    // ==================== 排序管理 ====================
    
    /**
     * 获取排序模式
     */
    fun getSortModes(): List<com.venside.x1n.managers.FileManager.SortMode> {
        return engine.getSortModes()
    }
    
    /**
     * 设置排序模式
     */
    fun setSortModes(modes: List<com.venside.x1n.managers.FileManager.SortMode>) {
        engine.setSortModes(modes)
    }
    
    /**
     * 保存排序模式到设置
     */
    fun saveSortModes(modes: List<com.venside.x1n.managers.FileManager.SortMode>) {
        engine.saveSortModes(modes)
    }
    
    /**
     * 加载排序模式
     */
    fun loadSortModes(): List<com.venside.x1n.managers.FileManager.SortMode> {
        return engine.loadSortModes()
    }
    
    // ==================== 检查未保存更改 ====================
    
    /**
     * 检查是否有未保存的更改
     */
    fun hasUnsavedChanges(currentContent: String): Boolean {
        return engine.hasUnsavedChanges(currentContent)
    }
    
    /**
     * 检查是否是当前文件
     */
    fun isCurrentFile(file: File): Boolean {
        return engine.editorManager.isCurrentFile(file)
    }
    
    /**
     * 获取当前文件
     */
    fun getCurrentFile(): File? {
        return engine.editorManager.getCurrentFile()
    }
    
    /**
     * 检查是否有文件打开
     */
    fun isFileOpen(): Boolean {
        return engine.editorManager.isFileOpen()
    }
    
    /**
     * 关闭当前文件
     */
    fun closeFile() {
        engine.editorManager.closeFile()
    }
    
    /**
     * 获取根目录
     */
    fun getRootDir(): File? {
        return engine.fileTreeCoordinator.getRootDir()
    }
    
    // ==================== 终端操作 ====================
    
    /**
     * 切换终端显示
     */
    fun toggleTerminal() {
        terminalManager.toggleTerminal()
    }
    
    /**
     * 显示终端
     */
    fun showTerminal() {
        terminalManager.showTerminal()
    }
    
    /**
     * 隐藏终端
     */
    fun hideTerminal() {
        terminalManager.hideTerminal()
    }
    
    /**
     * 清空终端
     */
    fun clearTerminal(clearHistory: Boolean = true) {
        terminalManager.clearTerminal(clearHistory)
    }
    
    /**
     * 检查终端是否有历史记录
     */
    fun hasTerminalHistory(): Boolean {
        return terminalManager.hasHistory()
    }
    
    /**
     * 检查终端是否可见
     */
    fun isTerminalVisible(): Boolean {
        return terminalManager.isVisible()
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 显示消息
     */
    fun showMessage(message: String, isError: Boolean = false) {
        _uiState = UiState(message = message, isError = isError)
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState = UiState()
    }
    
    /**
     * 设置加载状态
     */
    fun setLoading(isLoading: Boolean) {
        _uiState = _uiState.copy(isLoading = isLoading)
    }
}

/**
 * Lua 执行结果封装
 */
sealed class LuaExecutionResult {
    data class Success(val result: com.venside.x1n.lua.LuaResult) : LuaExecutionResult()
    data class Error(val message: String, val lineNumber: Int) : LuaExecutionResult()
}