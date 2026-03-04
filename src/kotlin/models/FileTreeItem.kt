package com.venside.x1n.models

import android.os.Parcel
import android.os.Parcelable
import java.io.File

/**
 * 文件树项数据模型
 * 表示文件树中的一个文件或文件夹
 */
data class FileTreeItem(
    val filePath: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    var isSelected: Boolean = false
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

        /**
         * Parcelable Creator
         */
        @JvmField
        val CREATOR = object : Parcelable.Creator<FileTreeItem> {
            override fun createFromParcel(parcel: Parcel): FileTreeItem {
                return FileTreeItem(
                    filePath = parcel.readString() ?: "",
                    name = parcel.readString() ?: "",
                    isDirectory = parcel.readByte() != 0.toByte(),
                    size = parcel.readLong(),
                    lastModified = parcel.readLong(),
                    isSelected = parcel.readByte() != 0.toByte()
                )
            }

            override fun newArray(size: Int): Array<FileTreeItem?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(filePath)
        parcel.writeString(name)
        parcel.writeByte(if (isDirectory) 1 else 0)
        parcel.writeLong(size)
        parcel.writeLong(lastModified)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }
}