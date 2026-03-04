package com.venside.x1n.utils

import android.view.View
import android.widget.LinearLayout

/**
 * 拖拽功能辅助类
 */
class DragDropHelper {

    /**
     * 设置最近文件容器的拖拽监听器
     * @param container 拖拽容器
     * @param onMoveFile 文件移动回调
     */
    fun setupDragListener(
        container: LinearLayout,
        onMoveFile: (fromIndex: Int, toIndex: Int) -> Unit
    ) {
        container.setOnDragListener { _, event ->
            val draggedView = event.localState as? View ?: return@setOnDragListener false

            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> true
                android.view.DragEvent.ACTION_DRAG_ENTERED -> true
                android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                    // 获取当前拖拽位置下的子视图
                    val targetView = findChildAt(container, event.x, event.y)
                    if (targetView != null && targetView != draggedView) {
                        // 找到源位置和目标位置
                        val fromIndex = container.indexOfChild(draggedView)
                        val toIndex = container.indexOfChild(targetView)

                        // 交换位置
                        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                            container.removeView(draggedView)
                            container.addView(draggedView, toIndex)

                            // 更新最近文件列表
                            onMoveFile(fromIndex - 1, toIndex - 1)  // 减1因为第一个是标题
                        }
                    }
                    true
                }
                android.view.DragEvent.ACTION_DRAG_EXITED -> true
                android.view.DragEvent.ACTION_DROP -> {
                    draggedView.alpha = 1.0f
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    draggedView.alpha = 1.0f
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 查找指定坐标下的子视图
     * @param container 父容器
     * @param x X坐标
     * @param y Y坐标
     * @return 找到的子视图，未找到返回null
     */
    fun findChildAt(container: LinearLayout, x: Float, y: Float): View? {
        for (i in 1 until container.childCount) {  // 从1开始，跳过标题
            val child = container.getChildAt(i)
            if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                return child
            }
        }
        return null
    }
}