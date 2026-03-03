package com.venside.x1n.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * 文件树项数据模型
 * 表示文件树中的一个文件或文件夹
 */
@Parcelize
data class FileTreeItem(
    val filePath: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0
) : Parcelable {

    /**
     * 获取 File 对象
     */
    val file: File
        get() = File(filePath)

    /**
     * 获取父目录
     */
    val parent: File?
        get() = file.parentFile

    /**
     * 获取扩展名
     */
    val extension: String
        get() {
            val dotIndex = name.lastIndexOf('.')
            return if (dotIndex > 0 && dotIndex < name.length - 1) {
                name.substring(dotIndex + 1).lowercase()
            } else {
                ""
            }
        }

    /**
     * 是否为上级目录项（".."）
     */
    val isParentDir: Boolean
        get() = name == ".."

    companion object {
        /**
         * 从 File 对象创建 FileTreeItem
         */
        fun fromFile(file: File): FileTreeItem {
            return FileTreeItem(
                filePath = file.absolutePath,
                name = file.name,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified()
            )
        }

        /**
         * 创建上级目录项
         */
        fun createParentItem(parentFile: File): FileTreeItem {
            return FileTreeItem(
                filePath = parentFile.absolutePath,
                name = "..",
                isDirectory = true,
                size = 0,
                lastModified = parentFile.lastModified()
            )
        }
    }
}