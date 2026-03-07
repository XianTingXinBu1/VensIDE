package com.venside.x1n.utils

import android.view.MotionEvent
import android.widget.AbsListView
import android.widget.ListView

/**
 * 滑动选择助手
 * 负责处理 ListView 的水平滑动触发选择功能
 * 
 * 核心逻辑：
 * 1. 向左/右滑动一定距离触发选择模式
 * 2. 处于选择状态后，滑动选择会进行区间选择
 * 3. 再次滑动相同区域会取消选择
 */
class SlideSelectHelper {

    companion object {
        /** 触发选择的最小水平滑动距离（像素） */
        private const val TRIGGER_THRESHOLD = 80
    }

    // 触摸状态
    private var isTracking = false
    private var startX = 0f
    private var startY = 0f
    private var startPosition = -1
    
    // 选择状态
    private var firstSelectedPosition = -1
    private var lastSelectedPosition = -1
    
    // 是否正在滑动选择（用于判断是否消费事件）
    private var isSlideSelecting = false
    
    // 是否是新的选择操作（用于清除之前的选择）
    private var isNewSelection = true
    
    // 上一次选择的区间（用于检测是否再次滑动相同区域）
    private var lastSelectionStart = -1
    private var lastSelectionEnd = -1

    /**
     * 处理触摸事件
     * @param listView 列表视图
     * @param event 触摸事件
     * @param onEnterSelection 进入选择模式回调
     * @param onExitSelection 退出选择模式回调
     * @param onSelectionChanged 选择变化回调，参数为（起始位置，结束位置，是否清除之前选择，是否是取消操作）
     * @return 是否已处理该事件（消费事件）
     */
    fun handleTouchEvent(
        listView: ListView,
        event: MotionEvent,
        onEnterSelection: () -> Unit,
        onExitSelection: () -> Unit,
        onSelectionChanged: (startPos: Int, endPos: Int, clearPrevious: Boolean, isDeselect: Boolean) -> Unit
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 记录起始位置
                startX = event.x
                startY = event.y
                startPosition = listView.pointToPosition(event.x.toInt(), event.y.toInt())
                isTracking = startPosition != AbsListView.INVALID_POSITION
                isSlideSelecting = false

                // 记录起点，选择逻辑由外部控制
                if (isTracking) {
                    firstSelectedPosition = startPosition
                    lastSelectedPosition = startPosition
                    isNewSelection = true
                }
                return false // 不消费DOWN事件，让ListView正常处理点击
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isTracking || startPosition == -1) return false

                val deltaX = event.x - startX
                val deltaY = event.y - startY
                val absDeltaX = kotlin.math.abs(deltaX)
                val absDeltaY = kotlin.math.abs(deltaY)

                // 检测是否为水平滑动（水平移动距离大于垂直移动距离的2倍）
                if (absDeltaX > absDeltaY * 2 && absDeltaX >= TRIGGER_THRESHOLD) {
                    // 水平滑动超过阈值，通知外部进入选择模式
                    isSlideSelecting = true
                    onEnterSelection()
                    // 新的选择操作，清除之前的选择
                    onSelectionChanged(firstSelectedPosition, lastSelectedPosition, true, false)
                    isNewSelection = false
                    return true
                }

                // 处理垂直滑动进行区间选择
                if (absDeltaY > 10) {
                    isSlideSelecting = true
                    val currentPosition = listView.pointToPosition(event.x.toInt(), event.y.toInt())
                    if (currentPosition != AbsListView.INVALID_POSITION && currentPosition != lastSelectedPosition) {
                        lastSelectedPosition = currentPosition

                        // 检查是否再次滑动相同区域（取消选择）
                        val currentStart = minOf(firstSelectedPosition, lastSelectedPosition)
                        val currentEnd = maxOf(firstSelectedPosition, lastSelectedPosition)
                        val isDeselect = (currentStart == lastSelectionStart && currentEnd == lastSelectionEnd)

                        if (isDeselect) {
                            // 取消选择
                            onSelectionChanged(firstSelectedPosition, lastSelectedPosition, true, true)
                        } else {
                            // 继续选择，不清除之前的选择（同一选择操作内）
                            onSelectionChanged(firstSelectedPosition, lastSelectedPosition, isNewSelection, false)
                        }
                        isNewSelection = false
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasSlideSelecting = isSlideSelecting
                
                // 记录本次选择的区间（用于下次检测取消操作）
                if (wasSlideSelecting && firstSelectedPosition != -1 && lastSelectedPosition != -1) {
                    lastSelectionStart = minOf(firstSelectedPosition, lastSelectedPosition)
                    lastSelectionEnd = maxOf(firstSelectedPosition, lastSelectedPosition)
                }
                
                isTracking = false
                isSlideSelecting = false
                isNewSelection = true
                // 只有在滑动选择时才消费UP事件，否则让点击事件正常触发
                return wasSlideSelecting
            }
        }

        return false
    }

    /**
     * 获取首次选中的位置
     */
    fun getFirstSelectedPosition(): Int = firstSelectedPosition

    /**
     * 获取最后选中的位置
     */
    fun getLastSelectedPosition(): Int = lastSelectedPosition

    /**
     * 重置选择相关的状态（不包括模式状态，模式状态由外部管理）
     */
    fun resetSelectionState() {
        firstSelectedPosition = -1
        lastSelectedPosition = -1
        isNewSelection = true
        lastSelectionStart = -1
        lastSelectionEnd = -1
    }

    /**
     * 重置所有状态
     */
    fun reset() {
        isTracking = false
        startPosition = -1
        isSlideSelecting = false
        isNewSelection = true
        lastSelectionStart = -1
        lastSelectionEnd = -1
        resetSelectionState()
    }
}