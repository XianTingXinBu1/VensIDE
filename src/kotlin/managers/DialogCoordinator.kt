package com.venside.x1n.managers

import android.content.Context
import com.venside.x1n.managers.interfaces.IDialogCoordinator
import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.DialogHelper
import java.io.File

/**
 * 对话框协调器
 * 负责对话框业务逻辑的协调和显示
 */
class DialogCoordinator : IDialogCoordinator {

    override fun showNewOptionsDialog(
        context: Context,
        onCreateFile: (String) -> Unit,
        onCreateFolder: (String) -> Unit
    ) {
        DialogHelper.showNewFileDialog(
            context = context,
            title = "新建",
            hint = "输入名称",
            onCreateFile = onCreateFile,
            onCreateFolder = onCreateFolder
        )
    }

    override fun showFileOptionsDialog(
        context: Context,
        item: FileTreeItem,
        onOpen: (() -> Unit)?,
        onRename: (FileTreeItem) -> Unit,
        onDelete: (FileTreeItem) -> Unit
    ) {
        val options = if (item.isDirectory) {
            arrayOf("重命名", "删除")
        } else {
            arrayOf("打开", "重命名", "删除")
        }

        DialogHelper.showOptionsDialog(
            context = context,
            title = "文件选项",
            options = options
        ) { which ->
            if (item.isDirectory) {
                when (which) {
                    0 -> onRename(item)
                    1 -> onDelete(item)
                }
            } else {
                when (which) {
                    0 -> onOpen?.invoke()
                    1 -> onRename(item)
                    2 -> onDelete(item)
                }
            }
        }
    }

    override fun showRenameDialog(
        context: Context,
        item: FileTreeItem,
        onRename: (File, String) -> Unit
    ) {
        DialogHelper.showInputDialog(
            context = context,
            title = "重命名",
            hint = item.name,
            defaultValue = item.name
        ) { newName ->
            if (newName != item.name) {
                onRename(item.file, newName)
            }
        }
    }

    override fun showDeleteDialog(
        context: Context,
        item: FileTreeItem,
        onDelete: (File) -> Unit
    ) {
        DialogHelper.showConfirmDialog(
            context = context,
            title = "删除",
            message = "确定要删除 \"${item.name}\" 吗？此操作不可恢复。",
            positiveText = "删除",
            onConfirm = { onDelete(item.file) }
        )
    }

    override fun showExplorerSettingsDialog(
        context: Context,
        currentSortModes: List<FileManager.SortMode>,
        onApply: (List<FileManager.SortMode>) -> Unit
    ) {
        val sortOptions = arrayOf(
            "名称 (A→Z)",
            "名称 (Z→A)",
            "大小 (小→大)",
            "大小 (大→小)",
            "时间 (旧→新)",
            "时间 (新→旧)"
        )
        val sortModeList = listOf(
            FileManager.SortMode.NAME_ASC,
            FileManager.SortMode.NAME_DESC,
            FileManager.SortMode.SIZE_ASC,
            FileManager.SortMode.SIZE_DESC,
            FileManager.SortMode.MODIFIED_ASC,
            FileManager.SortMode.MODIFIED_DESC
        )

        val checkedItems = BooleanArray(sortModeList.size) { index ->
            sortModeList[index] in currentSortModes
        }

        DialogHelper.showMultiChoiceDialog(
            context = context,
            title = "资源管理器排序设置",
            items = sortOptions,
            checkedItems = checkedItems
        ) { selectedItems ->
            val selectedModes = mutableListOf<FileManager.SortMode>()
            for (i in selectedItems.indices) {
                if (selectedItems[i]) {
                    selectedModes.add(sortModeList[i])
                }
            }

            if (selectedModes.isEmpty()) {
                selectedModes.add(FileManager.SortMode.NAME_ASC)
            }

            onApply(selectedModes)
        }
    }

    override fun showBatchOptionsDialog(
        context: Context,
        selectedCount: Int,
        onSelectAll: () -> Unit,
        onClearSelection: () -> Unit,
        onBatchDelete: () -> Unit,
        onExitSelectionMode: () -> Unit
    ) {
        val options = arrayOf("全选", "取消选择", "删除选中 ($selectedCount)", "退出选择模式")
        DialogHelper.showOptionsDialog(
            context = context,
            title = "批量操作",
            options = options
        ) { which ->
            when (which) {
                0 -> onSelectAll()
                1 -> onClearSelection()
                2 -> onBatchDelete()
                3 -> onExitSelectionMode()
            }
        }
    }

    override fun showFullPathDialog(context: Context, fullPath: String) {
        DialogHelper.showMessageDialog(
            context = context,
            title = "完整路径",
            message = fullPath
        )
    }

    override fun showWordCountDetailsDialog(context: Context, content: String) {
        val lines = content.lines()

        val charCount = content.length
        val lineCount = lines.size
        val wordCount = content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val nonWhitespaceCount = content.filter { !it.isWhitespace() }.length
        val maxLineLength = lines.maxOfOrNull { it.length } ?: 0

        val message = StringBuilder()
        message.append("字符数（含空格）: $charCount\n")
        message.append("字符数（不含空格）: $nonWhitespaceCount\n")
        message.append("单词数: $wordCount\n")
        message.append("行数: $lineCount\n")
        message.append("最长行字符数: $maxLineLength")

        DialogHelper.showMessageDialog(
            context = context,
            title = "字数统计详情",
            message = message.toString()
        )
    }

    override fun showProjectTemplateDialog(
        context: Context,
        onTemplateSelected: (String) -> Unit
    ) {
        DialogHelper.showProjectTemplateDialog(context, onTemplateSelected)
    }

    /**
     * 获取排序模式显示名称
     */
    fun getSortModeNames(modes: List<FileManager.SortMode>): String {
        return modes.joinToString(" + ") { mode ->
            when (mode) {
                FileManager.SortMode.NAME_ASC -> "名称 (A→Z)"
                FileManager.SortMode.NAME_DESC -> "名称 (Z→A)"
                FileManager.SortMode.SIZE_ASC -> "大小 (小→大)"
                FileManager.SortMode.SIZE_DESC -> "大小 (大→小)"
                FileManager.SortMode.MODIFIED_ASC -> "时间 (旧→新)"
                FileManager.SortMode.MODIFIED_DESC -> "时间 (新→旧)"
            }
        }
    }
}
