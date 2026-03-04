package com.venside.x1n.managers

/**
 * 编辑操作类型
 */
sealed class EditAction {
    abstract val start: Int
    abstract val text: String
    
    data class Insert(
        override val start: Int,
        override val text: String
    ) : EditAction()
    
    data class Delete(
        override val start: Int,
        override val text: String
    ) : EditAction()
}

/**
 * 撤销/重做管理器
 * 负责管理编辑器内容的撤销和重做操作
 */
class UndoRedoManager(
    private val maxHistorySize: Int = DEFAULT_MAX_HISTORY_SIZE
) {
    companion object {
        const val DEFAULT_MAX_HISTORY_SIZE = 100
        const val MERGE_THRESHOLD_MS = 500 // 合并操作的时间阈值（毫秒）
    }
    
    private val undoStack: ArrayDeque<EditAction> = ArrayDeque()
    private val redoStack: ArrayDeque<EditAction> = ArrayDeque()
    private var lastActionTime: Long = 0
    private var lastAction: EditAction? = null
    private var isPerformingAction: Boolean = false
    
    /**
     * 记录插入操作
     */
    fun recordInsert(start: Int, text: String) {
        if (isPerformingAction || text.isEmpty()) return
        
        val currentTime = System.currentTimeMillis()
        val action = EditAction.Insert(start, text)
        
        // 尝试合并连续的插入操作（如连续输入）
        if (shouldMergeInsert(action, currentTime)) {
            mergeInsert(action)
        } else {
            pushUndo(action)
        }
        
        lastActionTime = currentTime
        lastAction = action
        clearRedoStack()
    }
    
    /**
     * 记录删除操作
     */
    fun recordDelete(start: Int, text: String) {
        if (isPerformingAction || text.isEmpty()) return
        
        val currentTime = System.currentTimeMillis()
        val action = EditAction.Delete(start, text)
        
        // 尝试合并连续的删除操作
        if (shouldMergeDelete(action, currentTime)) {
            mergeDelete(action)
        } else {
            pushUndo(action)
        }
        
        lastActionTime = currentTime
        lastAction = action
        clearRedoStack()
    }
    
    /**
     * 执行撤销操作
     * @param content 当前编辑器内容
     * @return 撤销后的内容，如果无法撤销则返回null
     */
    fun undo(content: String): String? {
        if (!canUndo()) return null
        
        val action = undoStack.removeLast()
        isPerformingAction = true
        
        val newContent = when (action) {
            is EditAction.Insert -> {
                // 插入的撤销是删除
                val end = action.start + action.text.length
                content.removeRange(action.start, end)
            }
            is EditAction.Delete -> {
                // 删除的撤销是插入
                content.substring(0, action.start) + action.text + content.substring(action.start)
            }
        }
        
        redoStack.addLast(action)
        isPerformingAction = false
        lastAction = null
        
        return newContent
    }
    
    /**
     * 执行重做操作
     * @param content 当前编辑器内容
     * @return 重做后的内容，如果无法重做则返回null
     */
    fun redo(content: String): String? {
        if (!canRedo()) return null
        
        val action = redoStack.removeLast()
        isPerformingAction = true
        
        val newContent = when (action) {
            is EditAction.Insert -> {
                // 重新执行插入
                content.substring(0, action.start) + action.text + content.substring(action.start)
            }
            is EditAction.Delete -> {
                // 重新执行删除
                val end = action.start + action.text.length
                content.removeRange(action.start, end)
            }
        }
        
        undoStack.addLast(action)
        isPerformingAction = false
        lastAction = action
        
        return newContent
    }
    
    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * 获取撤销栈大小
     */
    fun undoStackSize(): Int = undoStack.size
    
    /**
     * 获取重做栈大小
     */
    fun redoStackSize(): Int = redoStack.size
    
    /**
     * 清空所有历史记录
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastAction = null
        lastActionTime = 0
    }
    
    /**
     * 重置状态（切换文件时调用）
     */
    fun reset() {
        clear()
    }
    
    private fun pushUndo(action: EditAction) {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(action)
    }
    
    private fun clearRedoStack() {
        redoStack.clear()
    }
    
    private fun shouldMergeInsert(action: EditAction.Insert, currentTime: Long): Boolean {
        val lastInsert = lastAction as? EditAction.Insert ?: return false
        
        // 检查是否是连续输入（位置相邻且时间间隔短）
        val isAdjacent = lastInsert.start + lastInsert.text.length == action.start
        val isRecent = currentTime - lastActionTime < MERGE_THRESHOLD_MS
        val isSingleChar = action.text.length == 1
        
        return isAdjacent && isRecent && isSingleChar
    }
    
    private fun shouldMergeDelete(action: EditAction.Delete, currentTime: Long): Boolean {
        val lastDelete = lastAction as? EditAction.Delete ?: return false
        
        // 检查是否是连续删除（位置相同或相邻且时间间隔短）
        val isSamePosition = action.start == lastDelete.start
        val isRecent = currentTime - lastActionTime < MERGE_THRESHOLD_MS
        val isSingleChar = action.text.length == 1
        
        return isSamePosition && isRecent && isSingleChar
    }
    
    private fun mergeInsert(action: EditAction.Insert) {
        val lastInsert = undoStack.removeLast() as EditAction.Insert
        val merged = EditAction.Insert(lastInsert.start, lastInsert.text + action.text)
        pushUndo(merged)
    }
    
    private fun mergeDelete(action: EditAction.Delete) {
        val lastDelete = undoStack.removeLast() as EditAction.Delete
        // 删除操作合并时，新的删除内容放在前面（因为是从后往前删）
        val merged = EditAction.Delete(action.start, action.text + lastDelete.text)
        pushUndo(merged)
    }
}
