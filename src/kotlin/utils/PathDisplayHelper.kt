package com.venside.x1n.utils

import android.widget.TextView
import com.venside.x1n.managers.EditorManager
import com.venside.x1n.managers.FileManager
import java.io.File

/**
 * 路径显示辅助类
 */
class PathDisplayHelper(
    private val fileManager: FileManager,
    private val editorManager: EditorManager,
    private val workspacePath: String?
) {

    /**
     * 更新路径显示
     * @param workspacePathTextView 工作区路径文本视图
     * @param sidebarPathTextView 侧边栏路径文本视图
     */
    fun updatePathDisplay(
        workspacePathTextView: TextView?,
        sidebarPathTextView: TextView?
    ) {
        // 更新顶栏路径：显示当前编辑文件的路径
        val currentFile = editorManager.getCurrentFile()
        if (currentFile != null) {
            workspacePathTextView?.text = fileManager.getShortPath(currentFile)
        } else {
            workspacePathTextView?.text = fileManager.getRootDir()?.name ?: "/workspace"
        }

        // 更新侧边栏路径：显示资源管理器的当前路径
        updateSidebarPath(sidebarPathTextView)
    }

    /**
     * 更新侧边栏路径
     * @param sidebarPathTextView 侧边栏路径文本视图
     */
    private fun updateSidebarPath(sidebarPathTextView: TextView?) {
        val currentDir = fileManager.getCurrentDir()
        val currentPath = currentDir?.absolutePath ?: fileManager.getRootDir()?.absolutePath ?: "/workspace"
        val workspaceName = workspacePath?.let { File(it).name } ?: ""

        val index = currentPath.indexOf(workspaceName)
        val shortPath = if (index >= 0) {
            currentPath.substring(index)
        } else {
            currentPath
        }

        sidebarPathTextView?.text = shortPath
    }

    /**
     * 获取短路径
     * @return 短路径字符串
     */
    fun getShortPath(): String {
        val currentFile = editorManager.getCurrentFile()
        if (currentFile == null) {
            return fileManager.getRootDir()?.name ?: "/workspace"
        }
        return fileManager.getShortPath(currentFile)
    }
}