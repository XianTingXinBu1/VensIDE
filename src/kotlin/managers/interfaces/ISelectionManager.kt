package com.venside.x1n.managers.interfaces

import com.venside.x1n.models.FileTreeItem

/**
 * 选择模式管理器接口
 * 负责选择模式的状态管理、批量操作
 */
interface ISelectionManager {
    
    /**
     * 是否处于选择模式
     */
    fun isSelectionMode(): Boolean
    
    /**
     * 进入选择模式
     */
    fun enterSelectionMode()
    
    /**
     * 退出选择模式
     */
    fun exitSelectionMode()
    
    /**
     * 切换选中状态
     * @param position 列表位置
     * @return 切换后的选中状态
     */
    fun toggleSelection(position: Int): Boolean
    
    /**
     * 获取选中的项目列表
     * @return 选中的文件项列表
     */
    fun getSelectedItems(): List<FileTreeItem>
    
    /**
     * 是否有选中项
     * @return 是否有选中项
     */
    fun hasSelection(): Boolean
    
    /**
     * 全选
     */
    fun selectAll()
    
    /**
     * 清除所有选择
     */
    fun clearSelection()
    
    /**
     * 处理滑动选择（区间选择）
     * @param startPos 起始位置
     * @param endPos 结束位置
     * @param clearPrevious 是否清除之前的选择
     * @param isDeselect 是否是取消选择操作
     */
    fun handleSlideSelection(startPos: Int, endPos: Int, clearPrevious: Boolean, isDeselect: Boolean)
    
    /**
     * 批量删除选中的项目
     * @return 成功删除的数量和失败的数量
     */
    fun batchDelete(): Pair<Int, Int>
    
    /**
     * 设置文件树数据源
     * @param fileTree 文件树列表
     */
    fun setFileTree(fileTree: MutableList<FileTreeItem>)
}
