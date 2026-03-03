package com.venside.x1n.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * 存储助手工具类
 * 负责存储权限和目录初始化
 */
object StorageHelper {

    /**
     * 初始化 VensIDE 基础目录
     * @param context 上下文
     * @return 初始化后的目录文件，失败返回 null
     */
    fun initVensIDEDirectory(context: Context): File? {
        return try {
            val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(context.getExternalFilesDir(null), "VensIDE")
            } else {
                File(Environment.getExternalStorageDirectory(), "VensIDE")
            }

            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }

            baseDir
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取 VensIDE 基础目录（不创建）
     * @param context 上下文
     * @return 目录文件对象
     */
    fun getVensIDEDirectory(context: Context): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(null), "VensIDE")
        } else {
            File(Environment.getExternalStorageDirectory(), "VensIDE")
        }
    }
}