package com.venside.x1n.managers.interfaces

import android.content.Context
import com.venside.x1n.models.FileTreeItem
import java.io.File

/**
 * 对话框协调器接口
 * 负责对话框业务逻辑的协调和显示
 */
interface IDialogCoordinator {
    
    /**
     * 显示新建选项对话框
     * @param context 上下文
     * @param onCreateFile 创建文件回调
     * @param onCreateFolder 创建文件夹回调
     */
    fun showNewOptionsDialog(
        context: Context,
        onCreateFile: (String) -> Unit,
        onCreateFolder: (String) -> Unit
    )
    
    /**
     * 显示文件选项对话框
     * @param context 上下文
     * @param item 文件项
     * @param onOpen 打开文件回调
     * @param onRename 重命名回调
     * @param onDelete 删除回调
     */
    fun showFileOptionsDialog(
        context: Context,
        item: FileTreeItem,
        onOpen: (() -> Unit)? = null,
        onRename: (FileTreeItem) -> Unit,
        onDelete: (FileTreeItem) -> Unit
    )
    
    /**
     * 显示重命名对话框
     * @param context 上下文
     * @param item 文件项
     * @param onRename 重命名回调
     */
    fun showRenameDialog(
        context: Context,
        item: FileTreeItem,
        onRename: (File, String) -> Unit
    )
    
    /**
     * 显示删除确认对话框
     * @param context 上下文
     * @param item 文件项
     * @param onDelete 删除回调
     */
    fun showDeleteDialog(
        context: Context,
        item: FileTreeItem,
        onDelete: (File) -> Unit
    )
    
    /**
     * 显示资源管理器设置对话框
     * @param context 上下文
     * @param currentSortModes 当前排序模式列表
     * @param onApply 应用排序规则回调
     */
    fun showExplorerSettingsDialog(
        context: Context,
        currentSortModes: List<com.venside.x1n.managers.FileManager.SortMode>,
        onApply: (List<com.venside.x1n.managers.FileManager.SortMode>) -> Unit
    )
    
    /**
     * 显示批量操作对话框
     * @param context 上下文
     * @param selectedCount 选中数量
     * @param onSelectAll 全选回调
     * @param onClearSelection 清除选择回调
     * @param onBatchDelete 批量删除回调
     * @param onExitSelectionMode 退出选择模式回调
     */
    fun showBatchOptionsDialog(
        context: Context,
        selectedCount: Int,
        onSelectAll: () -> Unit,
        onClearSelection: () -> Unit,
        onBatchDelete: () -> Unit,
        onExitSelectionMode: () -> Unit
    )
    
    /**
     * 显示完整路径对话框
     * @param context 上下文
     * @param fullPath 完整路径
     */
    fun showFullPathDialog(context: Context, fullPath: String)
    
    /**
     * 显示字数统计详情对话框
     * @param context 上下文
     * @param content 编辑器内容
     */
    fun showWordCountDetailsDialog(context: Context, content: String)
    
    /**
     * 显示项目模板对话框
     * @param context 上下文
     * @param onTemplateSelected 模板选中回调，参数为模板 ID
     */
    fun showProjectTemplateDialog(
        context: Context,
        onTemplateSelected: (String) -> Unit
    )
}
