package com.venside.x1n.managers

import android.content.Context
import android.view.MotionEvent
import android.widget.Toast
import com.venside.x1n.adapters.FileTreeAdapter
import com.venside.x1n.managers.interfaces.IFileTreeCoordinator
import com.venside.x1n.managers.interfaces.ISelectionManager
import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.SlideSelectHelper
import java.io.File

/**
 * 文件树控制器 - 负责处理文件树的所有交互逻辑
 * 
 * 职责：
 * - 处理文件树点击、长按、触摸事件
 * - 管理选择模式和滑选功能
 * - 处理批量操作（批量删除等）
 * - 协调文件树导航和文件打开
 */
class FileTreeController(
    private val context: Context,
    private val fileTreeCoordinator: IFileTreeCoordinator,
    private val selectionManager: ISelectionManager,
    private val fileTreeAdapter: FileTreeAdapter,
    private val slideSelectHelper: SlideSelectHelper
) {

    /**
     * 文件树操作回调接口
     */
    interface FileTreeCallbacks {
        /** 打开文件（带未保存检查） */
        fun onOpenFileWithUnsavedCheck(file: File)
        
        /** 显示文件选项对话框 */
        fun onShowFileOptions(item: FileTreeItem)
        
        /** 显示批量操作对话框 */
        fun onShowBatchOptionsDialog(selectedCount: Int)
        
        /** 退出选择模式 */
        fun onExitSelectionMode()
        
        /** 刷新文件树 */
        fun onRefreshFileTree()
        
        /** 显示 Toast 消息 */
        fun showToast(message: String)
    }

    private var callbacks: FileTreeCallbacks? = null

    /**
     * 设置回调接口
     */
    fun setCallbacks(callbacks: FileTreeCallbacks) {
        this.callbacks = callbacks
    }

    /**
     * 处理文件树点击事件
     */
    fun handleFileTreeItemClick(position: Int) {
        val item = fileTreeAdapter.getItem(position) ?: return
        
        if (selectionManager.isSelectionMode()) {
            if (item.isParentDir) {
                // 选择模式下点击父目录，退出选择模式并导航
                callbacks?.onExitSelectionMode()
                onFileTreeItemClick(item)
            } else {
                // 选择模式下切换选中状态
                val isSelected = selectionManager.toggleSelection(position)
                fileTreeAdapter.notifyDataSetChanged()
                if (!isSelected && !selectionManager.hasSelection()) {
                    callbacks?.onExitSelectionMode()
                }
            }
        } else {
            // 普通模式：直接处理点击
            onFileTreeItemClick(item)
        }
    }

    /**
     * 处理文件树长按事件
     */
    fun handleFileTreeItemLongClick(position: Int): Boolean {
        val item = fileTreeAdapter.getItem(position) ?: return false
        
        if (item.name == "..") {
            // 长按 ".." 返回根目录
            fileTreeCoordinator.navigateToRoot()
            callbacks?.onRefreshFileTree()
        } else if (selectionManager.isSelectionMode()) {
            // 选择模式下显示批量操作
            callbacks?.onShowBatchOptionsDialog(selectionManager.getSelectedItems().size)
        } else {
            // 普通模式下显示文件选项
            callbacks?.onShowFileOptions(item)
        }
        return true
    }

    /**
     * 处理文件树触摸事件（滑选）
     */
    fun handleFileTreeTouch(event: MotionEvent): Boolean {
        val listView = fileTreeAdapter.listView ?: return false

        return slideSelectHelper.handleTouchEvent(
            listView,
            event,
            onEnterSelection = {
                if (!selectionManager.isSelectionMode()) {
                    selectionManager.enterSelectionMode()
                    fileTreeAdapter.setSelectionMode(true)
                    callbacks?.showToast("已进入选择模式")
                }
            },
            onExitSelection = {
                // 通知外部退出选择模式
                callbacks?.onExitSelectionMode()
            }
        ) { startPos, endPos, clearPrevious, isDeselect ->
            handleSlideSelection(startPos, endPos, clearPrevious, isDeselect)
        }
    }

    /**
     * 实际的文件树点击逻辑
     */
    private fun onFileTreeItemClick(item: FileTreeItem) {
        fileTreeCoordinator.onFileTreeItemClick(
            item = item,
            onOpenFile = { file -> callbacks?.onOpenFileWithUnsavedCheck(file) },
            onNavigate = { callbacks?.onRefreshFileTree() }
        )
    }

    /**
     * 处理滑选操作
     */
    private fun handleSlideSelection(startPos: Int, endPos: Int, clearPrevious: Boolean, isDeselect: Boolean) {
        selectionManager.handleSlideSelection(startPos, endPos, clearPrevious, isDeselect)
        fileTreeAdapter.notifyDataSetChanged()
        
        if (!selectionManager.hasSelection()) {
            callbacks?.onExitSelectionMode()
        }
    }

    /**
     * 退出选择模式
     */
    fun exitSelectionMode() {
        selectionManager.exitSelectionMode()
        slideSelectHelper.resetSelectionState()
        fileTreeAdapter.setSelectionMode(false)
        fileTreeAdapter.clearSelection()
        callbacks?.showToast("已退出批量选择模式")
    }

    /**
     * 全选
     */
    fun selectAll() {
        selectionManager.selectAll()
        fileTreeAdapter.notifyDataSetChanged()
    }

    /**
     * 清除选择
     */
    fun clearSelection() {
        selectionManager.clearSelection()
        fileTreeAdapter.notifyDataSetChanged()
    }

    /**
     * 获取选中的文件项
     */
    fun getSelectedItems(): List<FileTreeItem> = selectionManager.getSelectedItems()

    /**
     * 检查是否在选择模式
     */
    fun isSelectionMode(): Boolean = selectionManager.isSelectionMode()

    /**
     * 检查是否有选中项
     */
    fun hasSelection(): Boolean = selectionManager.hasSelection()
}