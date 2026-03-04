package com.venside.x1n.managers.interfaces

import java.io.File

/**
 * 文件操作管理器接口
 * 负责文件操作的执行和结果显示
 */
interface IFileOperationManager {
    
    /**
     * 打开文件
     * @param file 文件对象
     * @param onSuccess 成功回调 (内容)
     * @param onFailure 失败回调 (错误信息)
     */
    fun openFile(file: File, onSuccess: (String) -> Unit, onFailure: (String) -> Unit)
    
    /**
     * 保存文件
     * @param content 文件内容
     * @param onSuccess 成功回调
     * @param onFailure 失败回调 (错误信息)
     */
    fun saveFile(content: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    
    /**
     * 创建文件
     * @param fileName 文件名
     * @param onSuccess 成功回调
     * @param onFailure 失败回调 (错误信息)
     */
    fun createFile(fileName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    
    /**
     * 创建文件夹
     * @param folderName 文件夹名
     * @param onSuccess 成功回调
     * @param onFailure 失败回调 (错误信息)
     */
    fun createFolder(folderName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    
    /**
     * 重命名文件
     * @param file 文件对象
     * @param newName 新名称
     * @param onSuccess 成功回调
     * @param onFailure 失败回调 (错误信息)
     */
    fun renameFile(file: File, newName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    
    /**
     * 删除文件
     * @param file 文件对象
     * @param onSuccess 成功回调
     * @param onFailure 失败回调 (错误信息)
     */
    fun deleteFile(file: File, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    
    /**
     * 批量删除
     * @param files 文件列表
     * @param onComplete 完成回调 (成功数, 失败数)
     */
    fun batchDelete(files: List<File>, onComplete: (Int, Int) -> Unit)
    
    /**
     * 显示消息
     * @param message 消息内容
     */
    fun showMessage(message: String)
    
    /**
     * 显示错误
     * @param error 错误信息
     */
    fun showError(error: String)
}
