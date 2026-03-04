package com.venside.x1n.managers.interfaces

/**
 * 返回键处理器接口
 * 负责返回键逻辑的处理
 */
interface IBackPressHandler {
    
    /**
     * 检查内容是否被修改
     * @param currentContent 当前内容
     * @return 是否被修改
     */
    fun isContentModified(currentContent: String): Boolean
    
    /**
     * 处理返回键按下事件
     * @param hasUnsavedChanges 是否有未保存的更改
     * @param onSaveAndExit 保存并退出回调
     * @param onExitWithoutSave 不保存退出回调
     * @param onConfirmExit 确认退出回调
     */
    fun handleBackPress(
        hasUnsavedChanges: Boolean,
        onSaveAndExit: () -> Unit,
        onExitWithoutSave: () -> Unit,
        onConfirmExit: () -> Unit
    )
    
    /**
     * 是否可以返回上级目录
     * @return 是否可以返回上级目录
     */
    fun canNavigateToParent(): Boolean
    
    /**
     * 检查文件操作前的未保存更改
     * @param hasUnsavedChanges 是否有未保存的更改
     * @param onSaveAndOpen 保存并打开回调
     * @param onOpenWithoutSave 不保存打开回调
     * @param onOpen 直接打开回调（无未保存更改时）
     */
    fun checkUnsavedChangesBeforeOpen(
        hasUnsavedChanges: Boolean,
        onSaveAndOpen: () -> Unit,
        onOpenWithoutSave: () -> Unit,
        onOpen: () -> Unit
    )
}
