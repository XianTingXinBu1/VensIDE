package com.venside.x1n.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 工作区数据模型
 * 表示一个工作区
 */
@Parcelize
data class Workspace(
    val path: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * 获取 File 对象
     */
    val file: File
        get() = File(path)

    /**
     * 工作区是否存在
     */
    val exists: Boolean
        get() = file.exists()

    /**
     * 工作区是否为目录
     */
    val isDirectory: Boolean
        get() = file.isDirectory

    /**
     * 获取工作区大小（字节）
     */
    val size: Long
        get() = calculateSize(file)

    /**
     * 获取工作区中的文件数量
     */
    val fileCount: Int
        get() = countFiles(file)

    /**
     * 格式化的创建时间
     */
    val formattedCreatedAt: String
        get() = formatDate(createdAt)

    /**
     * 格式化的最后修改时间
     */
    val formattedLastModified: String
        get() = formatDate(lastModified)

    companion object {
        /**
         * 从 File 对象创建 Workspace
         */
        fun fromFile(file: File): Workspace {
            return Workspace(
                path = file.absolutePath,
                name = file.name,
                createdAt = 0,
                lastModified = file.lastModified()
            )
        }

        /**
         * 格式化日期时间
         */
        private fun formatDate(timestamp: Long): String {
            return if (timestamp > 0) {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                formatter.format(Date(timestamp))
            } else {
                "未知"
            }
        }

        /**
         * 计算目录大小
         */
        private fun calculateSize(file: File): Long {
            if (file.isFile) {
                return file.length()
            }

            var size = 0L
            file.listFiles()?.forEach { child ->
                size += calculateSize(child)
            }
            return size
        }

        /**
         * 计算文件数量
         */
        private fun countFiles(file: File): Int {
            if (file.isFile) {
                return 1
            }

            var count = 0
            file.listFiles()?.forEach { child ->
                count += countFiles(child)
            }
            return count
        }
    }

    /**
     * 格式化工作区大小
     */
    fun formatSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}