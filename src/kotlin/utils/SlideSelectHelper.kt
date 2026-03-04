package com.venside.x1n.utils

import android.view.MotionEvent
import android.widget.AbsListView
import android.widget.ListView

/**
 * 滑动选择助手
 * 负责处理 ListView 的滑动选择功能
 */
class SlideSelectHelper {

    private var isSliding = false
    private var startPosition = -1
    private var lastPosition = -1

    /**
     * 处理触摸事件
     * @param listView 列表视图
     * @param event 触摸事件
     * @param onSlideStart 滑动开始回调（可选），用于通知调用者进入选择模式
     * @param onSelectionChanged 选择变化回调，参数为（起始位置，结束位置）
     * @return 是否已处理该事件
     */
    fun handleTouchEvent(
        listView: ListView,
        event: MotionEvent,
        onSlideStart: (() -> Unit)? = null,
        onSelectionChanged: (startPos: Int, endPos: Int) -> Unit
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 记录起始位置
                val position = listView.pointToPosition(event.x.toInt(), event.y.toInt())
                if (position != AbsListView.INVALID_POSITION) {
                    isSliding = true
                    startPosition = position
                    lastPosition = position
                    onSlideStart?.invoke()
                    onSelectionChanged(startPosition, lastPosition)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isSliding) {
                    // 获取当前位置
                    val position = listView.pointToPosition(event.x.toInt(), event.y.toInt())
                    if (position != AbsListView.INVALID_POSITION && position != lastPosition) {
                        lastPosition = position
                        onSelectionChanged(startPosition, lastPosition)
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isSliding = false
                startPosition = -1
                lastPosition = -1
            }
        }

        return false
    }

    /**
     * 获取选中范围
     * @return 起始位置和结束位置，如果未在滑动则返回 null
     */
    fun getSelectionRange(): Pair<Int, Int>? {
        if (!isSliding || startPosition == -1 || lastPosition == -1) {
            return null
        }
        return Pair(startPosition, lastPosition)
    }

    /**
     * 重置滑动状态
     */
    fun reset() {
        isSliding = false
        startPosition = -1
        lastPosition = -1
    }
}