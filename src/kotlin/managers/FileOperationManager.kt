package com.venside.x1n.managers

import android.content.Context
import android.widget.Toast
import com.venside.x1n.managers.interfaces.IFileOperationManager
import com.venside.x1n.managers.interfaces.IFileTreeCoordinator
import java.io.File

/**
 * 文件操作管理器
 * 负责文件操作的执行和结果显示
 */
class FileOperationManager(
    private val context: Context,
    private val editorManager: EditorManager,
    private val fileTreeCoordinator: IFileTreeCoordinator
) : IFileOperationManager {

    override fun openFile(file: File, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        editorManager.openFile(file)
            .onSuccess { content -> onSuccess(content) }
            .onFailure { e -> onFailure("无法打开文件: ${e.message}") }
    }

    override fun saveFile(content: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        editorManager.saveFile(content)
            .onSuccess { onSuccess() }
            .onFailure { e -> onFailure("保存失败: ${e.message}") }
    }

    override fun createFile(fileName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        fileTreeCoordinator.createFile(fileName)
            .onSuccess {
                showMessage("文件创建成功")
                onSuccess()
            }
            .onFailure { e ->
                val message = when (e) {
                    is java.nio.file.FileAlreadyExistsException -> "文件已存在"
                    else -> "文件创建失败: ${e.message}"
                }
                onFailure(message)
            }
    }

    override fun createFolder(folderName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        fileTreeCoordinator.createFolder(folderName)
            .onSuccess {
                showMessage("文件夹创建成功")
                onSuccess()
            }
            .onFailure { e ->
                val message = when (e) {
                    is java.nio.file.FileAlreadyExistsException -> "文件夹已存在"
                    else -> "文件夹创建失败: ${e.message}"
                }
                onFailure(message)
            }
    }

    override fun renameFile(file: File, newName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        fileTreeCoordinator.renameFile(file, newName)
            .onSuccess {
                showMessage("重命名成功")
                onSuccess()
            }
            .onFailure { e ->
                val message = when (e) {
                    is java.nio.file.FileAlreadyExistsException -> "文件已存在"
                    else -> "重命名失败: ${e.message}"
                }
                onFailure(message)
            }
    }

    override fun deleteFile(file: File, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        fileTreeCoordinator.deleteFile(file)
            .onSuccess {
                showMessage("删除成功")
                onSuccess()
            }
            .onFailure { e -> onFailure("删除失败: ${e.message}") }
    }

    override fun batchDelete(files: List<File>, onComplete: (Int, Int) -> Unit) {
        var successCount = 0
        var failCount = 0

        files.forEach { file ->
            fileTreeCoordinator.deleteFile(file)
                .onSuccess { successCount++ }
                .onFailure { failCount++ }
        }

        val message = if (failCount == 0) {
            "成功删除 $successCount 个项目"
        } else {
            "成功删除 $successCount 个项目，失败 $failCount 个"
        }
        showMessage(message)

        onComplete(successCount, failCount)
    }

    override fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(error: String) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }
}
