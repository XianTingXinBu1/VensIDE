package com.venside.x1n.managers

import com.venside.x1n.utils.ThemeConstants
import java.io.File

/**
 * 最近文件管理器
 * 负责管理最近打开的文件列表
 */
class RecentFilesManager {

    private val recentFiles = mutableListOf<File>()
    private var maxFiles = ThemeConstants.MAX_RECENT_FILES

    /**
     * 设置最大文件数量
     * @param max 最大文件数量
     */
    fun setMaxFiles(max: Int) {
        maxFiles = max
        trimExcessFiles()
    }

    /**
     * 添加文件到最近文件列表
     * @param file 文件对象
     */
    fun addFile(file: File) {
        // 移除已存在的相同文件
        recentFiles.removeAll { it.absolutePath == file.absolutePath }
        // 添加到列表开头
        recentFiles.add(0, file)
        // 限制最大数量
        trimExcessFiles()
    }

    /**
     * 确保文件在列表中（用于切换文件，不改变顺序）
     * @param file 文件对象
     */
    fun ensureFile(file: File) {
        // 如果文件已在列表中，不改变顺序
        if (recentFiles.any { it.absolutePath == file.absolutePath }) {
            return
        }
        // 如果文件不在列表中，添加到开头
        recentFiles.add(0, file)
        trimExcessFiles()
    }

    /**
     * 从最近文件列表中移除文件
     * @param file 文件对象
     */
    fun removeFile(file: File) {
        recentFiles.removeAll { it.absolutePath == file.absolutePath }
    }

    /**
     * 从最近文件列表中移除文件（按索引）
     * @param index 文件索引
     */
    fun removeFileAt(index: Int) {
        if (index in recentFiles.indices) {
            recentFiles.removeAt(index)
        }
    }

    /**
     * 移动文件到新位置（用于拖拽排序）
     * @param fromIndex 源索引
     * @param toIndex 目标索引
     */
    fun moveFile(fromIndex: Int, toIndex: Int) {
        if (fromIndex in recentFiles.indices && toIndex in recentFiles.indices) {
            val file = recentFiles.removeAt(fromIndex)
            recentFiles.add(toIndex, file)
        }
    }

    /**
     * 获取最近文件列表
     * @return 最近文件列表的副本
     */
    fun getRecentFiles(): List<File> {
        return recentFiles.toList()
    }

    /**
     * 获取文件在列表中的索引
     * @param file 文件对象
     * @return 文件索引，如果不存在返回 -1
     */
    fun getFileIndex(file: File): Int {
        return recentFiles.indexOfFirst { it.absolutePath == file.absolutePath }
    }

    /**
     * 检查文件是否在最近文件列表中
     * @param file 文件对象
     * @return 是否在列表中
     */
    fun contains(file: File): Boolean {
        return recentFiles.any { it.absolutePath == file.absolutePath }
    }

    /**
     * 清空最近文件列表
     */
    fun clear() {
        recentFiles.clear()
    }

    /**
     * 获取最近文件数量
     * @return 文件数量
     */
    fun size(): Int {
        return recentFiles.size
    }

    /**
     * 检查是否为空
     * @return 是否为空
     */
    fun isEmpty(): Boolean {
        return recentFiles.isEmpty()
    }

    /**
     * 超出最大数量时删除多余的文件
     */
    private fun trimExcessFiles() {
        while (recentFiles.size > maxFiles) {
            recentFiles.removeAt(recentFiles.size - 1)
        }
    }

    /**
     * 获取文件在列表中的位置（从 1 开始，用于显示）
     * @param file 文件对象
     * @return 位置，如果不存在返回 0
     */
    fun getFilePosition(file: File): Int {
        val index = getFileIndex(file)
        return if (index >= 0) index + 1 else 0
    }
}