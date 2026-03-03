package com.venside.x1n.managers

import com.venside.x1n.utils.FileUtils
import java.io.File

/**
 * 编辑器管理器
 * 负责文件内容的读取、保存和修改检测
 */
class EditorManager {

    private var currentFile: File? = null
    private var originalContent: String? = null

    /**
     * 获取当前打开的文件
     */
    fun getCurrentFile(): File? = currentFile

    /**
     * 获取原始内容
     */
    fun getOriginalContent(): String? = originalContent

    /**
     * 打开文件
     * @param file 要打开的文件
     * @return Result 包装的文件内容或错误
     */
    fun openFile(file: File): Result<String> {
        return try {
            val content = FileUtils.readFile(file)
            if (content.isSuccess) {
                currentFile = file
                originalContent = content.getOrNull()
            }
            content
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存文件
     * @param content 要保存的内容
     * @return Result 包装的 Unit 或错误
     */
    fun saveFile(content: String): Result<Unit> {
        currentFile?.let { file ->
            return try {
                val result = FileUtils.writeFile(file, content)
                if (result.isSuccess) {
                    originalContent = content
                }
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        return Result.failure(Exception("没有打开的文件"))
    }

    /**
     * 检查内容是否被修改
     * @param currentContent 当前编辑器内容
     * @return 是否被修改
     */
    fun isModified(currentContent: String): Boolean {
        return currentContent != originalContent
    }

    /**
     * 检查是否有未保存的更改
     * @return 是否有未保存的更改
     */
    fun hasUnsavedChanges(): Boolean {
        return originalContent != null
    }

    /**
     * 关闭当前文件
     */
    fun closeFile() {
        currentFile = null
        originalContent = null
    }

    /**
     * 获取文件扩展名
     * @return 文件扩展名，如果没有打开文件返回空字符串
     */
    fun getFileExtension(): String {
        return currentFile?.let { FileUtils.getFileExtension(it) } ?: ""
    }

    /**
     * 获取文件名
     * @return 文件名，如果没有打开文件返回空字符串
     */
    fun getFileName(): String {
        return currentFile?.name ?: ""
    }

    /**
     * 获取文件路径
     * @return 文件路径，如果没有打开文件返回空字符串
     */
    fun getFilePath(): String {
        return currentFile?.absolutePath ?: ""
    }

    /**
     * 判断文件是否已打开
     * @return 是否有打开的文件
     */
    fun isFileOpen(): Boolean {
        return currentFile != null
    }

    /**
     * 判断是否为指定文件
     * @param file 文件对象
     * @return 是否为当前打开的文件
     */
    fun isCurrentFile(file: File): Boolean {
        return currentFile?.absolutePath == file.absolutePath
    }
}