package com.venside.x1n.managers

import com.venside.x1n.managers.interfaces.ISelectionManager
import com.venside.x1n.models.FileTreeItem

/**
 * 选择模式管理器
 * 负责选择模式的状态管理、批量操作
 */
class SelectionManager(
    private val fileManager: FileManager
) : ISelectionManager {

    private var isSelectionModeActive = false
    private var fileTree: MutableList<FileTreeItem> = mutableListOf()

    override fun isSelectionMode(): Boolean = isSelectionModeActive

    override fun enterSelectionMode() {
        isSelectionModeActive = true
    }

    override fun exitSelectionMode() {
        isSelectionModeActive = false
        clearSelection()
    }

    override fun toggleSelection(position: Int): Boolean {
        if (position !in fileTree.indices) return false
        
        val item = fileTree[position]
        if (item.isParentDir) return false
        
        item.isSelected = !item.isSelected
        return item.isSelected
    }

    override fun getSelectedItems(): List<FileTreeItem> {
        return fileTree.filter { it.isSelected && !it.isParentDir }
    }

    override fun hasSelection(): Boolean {
        return fileTree.any { it.isSelected && !it.isParentDir }
    }

    override fun selectAll() {
        fileTree.forEach { if (!it.isParentDir) it.isSelected = true }
    }

    override fun clearSelection() {
        fileTree.forEach { it.isSelected = false }
    }

    override fun handleSlideSelection(startPos: Int, endPos: Int, clearPrevious: Boolean, isDeselect: Boolean) {
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
    }

    override fun batchDelete(): Pair<Int, Int> {
        val selectedItems = getSelectedItems()
        var successCount = 0
        var failCount = 0

        selectedItems.forEach { item ->
            fileManager.deleteFile(item.file)
                .onSuccess { successCount++ }
                .onFailure { failCount++ }
        }

        return Pair(successCount, failCount)
    }

    override fun setFileTree(fileTree: MutableList<FileTreeItem>) {
        this.fileTree = fileTree
    }
}
