package com.venside.x1n.managers.interfaces

import com.venside.x1n.models.FileTreeItem
import java.io.File

/**
 * 文件树协调器接口
 * 负责文件树的加载、导航和交互协调
 */
interface IFileTreeCoordinator {
    
    /**
     * 加载当前目录
     * @return 文件树列表
     */
    fun loadCurrentDir(): List<FileTreeItem>
    
    /**
     * 处理文件树项点击
     * @param item 文件树项
     * @param onOpenFile 打开文件回调
     * @param onNavigate 导航回调
     */
    fun onFileTreeItemClick(
        item: FileTreeItem,
        onOpenFile: (File) -> Unit,
        onNavigate: () -> Unit
    )
    
    /**
     * 导航到上级目录
     * @return 是否成功导航
     */
    fun navigateToParent(): Boolean
    
    /**
     * 导航到根目录
     */
    fun navigateToRoot()
    
    /**
     * 获取当前目录
     * @return 当前目录文件
     */
    fun getCurrentDir(): File?
    
    /**
     * 获取根目录
     * @return 根目录文件
     */
    fun getRootDir(): File?
    
    /**
     * 创建文件
     * @param name 文件名
     * @return Result 包装的 File 对象或错误
     */
    fun createFile(name: String): Result<File>
    
    /**
     * 创建文件夹
     * @param name 文件夹名称
     * @return Result 包装的 File 对象或错误
     */
    fun createFolder(name: String): Result<File>
    
    /**
     * 重命名文件
     * @param file 要重命名的文件
     * @param newName 新名称
     * @return Result 包装的新 File 对象或错误
     */
    fun renameFile(file: File, newName: String): Result<File>
    
    /**
     * 删除文件
     * @param file 要删除的文件
     * @return Result 包装的 Unit 或错误
     */
    fun deleteFile(file: File): Result<Unit>
    
    /**
     * 获取当前路径
     * @return 当前路径
     */
    fun getCurrentPath(): String?
}
