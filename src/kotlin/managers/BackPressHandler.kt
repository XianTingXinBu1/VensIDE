package com.venside.x1n.managers

import com.venside.x1n.managers.interfaces.IBackPressHandler

/**
 * 返回键处理器
 * 负责返回键逻辑的处理
 */
class BackPressHandler(
    private val editorManager: EditorManager,
    private val fileManager: FileManager
) : IBackPressHandler {

    override fun isContentModified(currentContent: String): Boolean {
        if (!editorManager.isFileOpen()) return false
        return editorManager.isModified(currentContent)
    }

    override fun handleBackPress(
        hasUnsavedChanges: Boolean,
        onSaveAndExit: () -> Unit,
        onExitWithoutSave: () -> Unit,
        onConfirmExit: () -> Unit
    ) {
        if (hasUnsavedChanges) {
            onSaveAndExit()
        } else {
            onConfirmExit()
        }
    }

    override fun canNavigateToParent(): Boolean {
        return fileManager.getCurrentDir()?.parentFile != null &&
               fileManager.getCurrentDir()?.absolutePath != fileManager.getRootDir()?.absolutePath
    }

    override fun checkUnsavedChangesBeforeOpen(
        hasUnsavedChanges: Boolean,
        onSaveAndOpen: () -> Unit,
        onOpenWithoutSave: () -> Unit,
        onOpen: () -> Unit
    ) {
        if (hasUnsavedChanges) {
            onSaveAndOpen()
        } else {
            onOpen()
        }
    }
}
