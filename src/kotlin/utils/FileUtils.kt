package com.venside.x1n.utils

import java.io.File

/**
 * 文件操作工具类
 * 提供文件和文件夹的创建、删除、重命名等操作
 */
object FileUtils {

    /**
     * 创建文件
     * @param parent 父目录
     * @param name 文件名
     * @return Result 包装的 File 对象或错误
     */
    fun createFile(parent: File, name: String): Result<File> {
        val file = File(parent, name)
        return if (file.exists()) {
            Result.failure(FileAlreadyExistsException(file))
        } else {
            runCatching {
                file.apply { createNewFile() }
            }
        }
    }

    /**
     * 创建文件夹
     * @param parent 父目录
     * @param name 文件夹名称
     * @return Result 包装的 File 对象或错误
     */
    fun createFolder(parent: File, name: String): Result<File> {
        val folder = File(parent, name)
        return if (folder.exists()) {
            Result.failure(FileAlreadyExistsException(folder))
        } else {
            if (folder.mkdirs()) {
                Result.success(folder)
            } else {
                Result.failure(Exception("文件夹创建失败: ${folder.absolutePath}"))
            }
        }
    }

    /**
     * 重命名文件或文件夹
     * @param file 要重命名的文件
     * @param newName 新名称
     * @return Result 包装的新 File 对象或错误
     */
    fun renameFile(file: File, newName: String): Result<File> {
        val newFile = File(file.parentFile, newName)
        return if (newFile.exists()) {
            Result.failure(FileAlreadyExistsException(newFile))
        } else {
            if (file.renameTo(newFile)) {
                Result.success(newFile)
            } else {
                Result.failure(Exception("重命名失败: ${file.absolutePath}"))
            }
        }
    }

    /**
     * 删除文件或文件夹（递归）
     * @param file 要删除的文件
     * @return Result 包装的 Unit 或错误
     */
    fun deleteFile(file: File): Result<Unit> {
        return if (file.deleteRecursively()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("删除失败: ${file.absolutePath}"))
        }
    }

    /**
     * 读取文件内容
     * @param file 要读取的文件
     * @return Result 包装的文件内容或错误
     */
    fun readFile(file: File): Result<String> {
        return runCatching {
            file.readText()
        }
    }

    /**
     * 写入文件内容
     * @param file 要写入的文件
     * @param content 文件内容
     * @return Result 包装的 Unit 或错误
     */
    fun writeFile(file: File, content: String): Result<Unit> {
        return runCatching {
            file.writeText(content)
        }
    }

    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的字符串
     */
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    /**
     * 获取文件扩展名
     * @param file 文件对象
     * @return 文件扩展名（不包含点），无扩展名返回空字符串
     */
    fun getFileExtension(file: File): String {
        val name = file.name
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < name.length - 1) {
            name.substring(dotIndex + 1).lowercase()
        } else {
            ""
        }
    }

    /**
     * 检查文件是否为文本文件
     * @param file 文件对象
     * @return 是否为文本文件
     */
    fun isTextFile(file: File): Boolean {
        if (file.isDirectory) return false

        val extension = getFileExtension(file)
        val textExtensions = listOf(
            "txt", "md", "json", "xml", "html", "css", "js", "ts", "kt",
            "java", "py", "cpp", "c", "h", "hpp", "yml", "yaml", "ini",
            "conf", "sh", "bat", "ps1", "log", "properties", "gradle"
        )

        return extension in textExtensions
    }

    /**
     * 获取相对路径
     * @param file 文件对象
     * @param baseDir 基础目录
     * @return 相对路径，如果文件不在基础目录下返回绝对路径
     */
    fun getRelativePath(file: File, baseDir: File): String {
        val absolutePath = file.absolutePath
        val basePath = baseDir.absolutePath

        return if (absolutePath.startsWith(basePath)) {
            absolutePath.substring(basePath.length).let {
                if (it.startsWith(File.separator)) {
                    it.substring(1)
                } else {
                    it
                }
            }
        } else {
            absolutePath
        }
    }

    /**
     * 获取短路径（相对于工作区）
     * @param file 文件对象
     * @param workspacePath 工作区路径
     * @return 短路径
     */
    fun getShortPath(file: File, workspacePath: String): String {
        val fullPath = file.absolutePath
        val workspaceName = File(workspacePath).name
        val index = fullPath.indexOf(workspaceName)

        return if (index >= 0) {
            fullPath.substring(index)
        } else {
            fullPath
        }
    }
}