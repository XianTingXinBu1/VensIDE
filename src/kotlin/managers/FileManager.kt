package com.venside.x1n.managers

import com.venside.x1n.models.FileTreeItem
import com.venside.x1n.utils.FileUtils
import java.io.File

/**
 * 文件管理器
 * 负责文件树的管理、导航和文件操作
 */
class FileManager {

    enum class SortMode {
        NAME_ASC,          // 名称升序
        NAME_DESC,         // 名称降序
        SIZE_ASC,          // 大小升序
        SIZE_DESC,         // 大小降序
        MODIFIED_ASC,      // 修改时间升序
        MODIFIED_DESC      // 修改时间降序
    }

    private var rootDir: File? = null
    private var currentDir: File? = null
    private val fileTree = mutableListOf<FileTreeItem>()
    private var sortModes = listOf(SortMode.NAME_ASC)

    /**
     * 初始化文件管理器
     * @param workspacePath 工作区路径
     */
    fun init(workspacePath: String) {
        rootDir = File(workspacePath)
        currentDir = rootDir
    }

    /**
     * 设置排序模式列表（按优先级排序）
     * @param modes 排序模式列表
     */
    fun setSortModes(modes: List<SortMode>) {
        sortModes = if (modes.isEmpty()) listOf(SortMode.NAME_ASC) else modes
    }

    /**
     * 获取当前排序模式列表
     * @return 排序模式列表
     */
    fun getSortModes(): List<SortMode> = sortModes

    /**
     * 设置单个排序模式（兼容旧代码）
     * @param mode 排序模式
     */
    fun setSortMode(mode: SortMode) {
        sortModes = listOf(mode)
    }

    /**
     * 获取当前排序模式（兼容旧代码）
     * @return 排序模式
     */
    fun getSortMode(): SortMode = sortModes.firstOrNull() ?: SortMode.NAME_ASC

    /**
     * 获取根目录
     */
    fun getRootDir(): File? = rootDir

    /**
     * 获取当前目录
     */
    fun getCurrentDir(): File? = currentDir

    /**
     * 设置当前目录
     * @param dir 目录文件
     */
    fun setCurrentDir(dir: File) {
        currentDir = dir
    }

    /**
     * 加载当前目录内容
     * @return 文件树列表
     */
    fun loadCurrentDir(): List<FileTreeItem> {
        fileTree.clear()

        currentDir?.let { dir ->
            // 添加返回上级目录项（如果不是根目录）
            if (currentDir?.absolutePath != rootDir?.absolutePath) {
                val parentItem = FileTreeItem.createParentItem(
                    currentDir?.parentFile ?: rootDir!!
                )
                fileTree.add(parentItem)
            }

            // 加载当前目录的内容
            val files = dir.listFiles()?.toList() ?: emptyList()

            // 分离文件夹和文件
            val directories = files.filter { it.isDirectory }
            val normalFiles = files.filter { !it.isDirectory }

            // 根据排序模式分别排序文件夹和文件
            val sortedDirs = sortFiles(directories)
            val sortedFiles = sortFiles(normalFiles)

            // 文件夹在前，文件在后
            for (directory in sortedDirs) {
                fileTree.add(FileTreeItem.fromFile(directory))
            }
            for (file in sortedFiles) {
                fileTree.add(FileTreeItem.fromFile(file))
            }
        }

        return fileTree.toList()
    }

    /**
     * 根据当前排序模式列表排序文件列表
     * @param files 文件列表
     * @return 排序后的文件列表
     */
    private fun sortFiles(files: List<File>): List<File> {
        if (sortModes.isEmpty()) return files

        var sortedFiles = files

        // 按照排序模式列表的顺序依次应用排序
        for (mode in sortModes) {
            sortedFiles = when (mode) {
                SortMode.NAME_ASC -> sortedFiles.sortedBy { it.name.lowercase() }
                SortMode.NAME_DESC -> sortedFiles.sortedByDescending { it.name.lowercase() }
                SortMode.SIZE_ASC -> sortedFiles.sortedBy { it.length() }
                SortMode.SIZE_DESC -> sortedFiles.sortedByDescending { it.length() }
                SortMode.MODIFIED_ASC -> sortedFiles.sortedBy { it.lastModified() }
                SortMode.MODIFIED_DESC -> sortedFiles.sortedByDescending { it.lastModified() }
            }
        }

        return sortedFiles
    }

    /**
     * 导航到指定文件/文件夹
     * @param item 文件树项
     * @return 是否成功导航（文件夹返回 true，文件返回 false）
     */
    fun navigateTo(item: FileTreeItem): Boolean {
        return if (item.isDirectory) {
            currentDir = item.file
            true
        } else {
            false
        }
    }

    /**
     * 导航到上级目录
     * @return 是否成功导航
     */
    fun navigateToParent(): Boolean {
        currentDir?.parentFile?.let {
            currentDir = it
            return true
        }
        return false
    }

    /**
     * 导航到根目录
     */
    fun navigateToRoot() {
        currentDir = rootDir
    }

    /**
     * 获取当前路径
     */
    fun getCurrentPath(): String? = currentDir?.absolutePath

    /**
     * 获取相对路径（相对于工作区）
     * @param file 文件对象
     * @return 相对路径
     */
    fun getRelativePath(file: File): String {
        return FileUtils.getRelativePath(file, rootDir ?: file)
    }

    /**
     * 获取短路径（用于显示）
     * @param file 文件对象
     * @return 短路径
     */
    fun getShortPath(file: File): String {
        val workspacePath = rootDir?.absolutePath ?: return file.name
        return FileUtils.getShortPath(file, workspacePath)
    }

    /**
     * 创建文件
     * @param name 文件名
     * @return Result 包装的 File 对象或错误
     */
    fun createFile(name: String): Result<File> {
        currentDir?.let { dir ->
            return FileUtils.createFile(dir, name)
        }
        return Result.failure(Exception("当前目录未初始化"))
    }

    /**
     * 创建文件夹
     * @param name 文件夹名称
     * @return Result 包装的 File 对象或错误
     */
    fun createFolder(name: String): Result<File> {
        currentDir?.let { dir ->
            return FileUtils.createFolder(dir, name)
        }
        return Result.failure(Exception("当前目录未初始化"))
    }

    /**
     * 重命名文件
     * @param file 要重命名的文件
     * @param newName 新名称
     * @return Result 包装的新 File 对象或错误
     */
    fun renameFile(file: File, newName: String): Result<File> {
        return FileUtils.renameFile(file, newName)
    }

    /**
     * 删除文件
     * @param file 要删除的文件
     * @return Result 包装的 Unit 或错误
     */
    fun deleteFile(file: File): Result<Unit> {
        return FileUtils.deleteFile(file)
    }

    /**
     * 检查文件是否在当前目录中
     * @param file 文件对象
     * @return 是否在当前目录中
     */
    fun isInCurrentDir(file: File): Boolean {
        return currentDir?.absolutePath == file.parentFile?.absolutePath
    }

    /**
     * 刷新当前目录
     * @return 刷新后的文件树列表
     */
    fun refresh(): List<FileTreeItem> {
        return loadCurrentDir()
    }
}