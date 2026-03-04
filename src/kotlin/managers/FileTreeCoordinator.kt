package com.venside.x1n.managers

import com.venside.x1n.managers.interfaces.IFileTreeCoordinator
import com.venside.x1n.models.FileTreeItem
import java.io.File

/**
 * 文件树协调器
 * 负责文件树的加载、导航和交互协调
 */
class FileTreeCoordinator(
    private val fileManager: FileManager
) : IFileTreeCoordinator {

    override fun loadCurrentDir(): List<FileTreeItem> {
        return fileManager.loadCurrentDir()
    }

    override fun onFileTreeItemClick(
        item: FileTreeItem,
        onOpenFile: (File) -> Unit,
        onNavigate: () -> Unit
    ) {
        if (item.isDirectory) {
            fileManager.navigateTo(item)
            onNavigate()
        } else {
            onOpenFile(item.file)
        }
    }

    override fun navigateToParent(): Boolean {
        return fileManager.navigateToParent()
    }

    override fun navigateToRoot() {
        fileManager.navigateToRoot()
    }

    override fun getCurrentDir(): File? {
        return fileManager.getCurrentDir()
    }

    override fun getRootDir(): File? {
        return fileManager.getRootDir()
    }

    override fun createFile(name: String): Result<File> {
        return fileManager.createFile(name)
    }

    override fun createFolder(name: String): Result<File> {
        return fileManager.createFolder(name)
    }

    override fun renameFile(file: File, newName: String): Result<File> {
        return fileManager.renameFile(file, newName)
    }

    override fun deleteFile(file: File): Result<Unit> {
        return fileManager.deleteFile(file)
    }

    override fun getCurrentPath(): String? {
        return fileManager.getCurrentPath()
    }
}
