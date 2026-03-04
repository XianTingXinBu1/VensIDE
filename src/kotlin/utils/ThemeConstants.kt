package com.venside.x1n.utils

/**
 * 主题常量
 * 集中管理应用的颜色、字体、间距等常量
 */
object ThemeConstants {

    // ==================== 颜色常量 ====================

    /** 背景色 - 深色 */
    const val COLOR_BG_DARK = 0xFF1E1E1E.toInt()

    /** 工具栏背景色 */
    const val COLOR_BG_TOOLBAR = 0xFF2D2D2D.toInt()

    /** 高亮色 - 蓝色 */
    const val COLOR_HIGHLIGHT = 0xFF3399FF.toInt()

    /** 标签背景色 */
    const val COLOR_TAG_BG = 0xFF3C3C3C.toInt()

    /** 标签背景色（选中） */
    const val COLOR_TAG_BG_SELECTED = 0xFF3399FF.toInt()

    /** 选择状态背景色（比打开状态暗） */
    const val COLOR_SELECTION_BG = 0xFF2266AA.toInt()

    /** 文字颜色 - 正常 */
    const val COLOR_TEXT_NORMAL = 0xFFCCCCCC.toInt()

    /** 文字颜色 - 暗淡 */
    const val COLOR_TEXT_DIM = 0xFF999999.toInt()

    /** 文字颜色 - 亮色（选中状态） */
    const val COLOR_TEXT_LIGHT = 0xFFFFFFFF.toInt()

    /** 分隔线颜色 */
    const val COLOR_SEPARATOR = 0xFF444444.toInt()

    // ==================== 字体大小常量 ====================

    /** 超大字体 - 48sp */
    const val FONT_SIZE_XL = 48f

    /** 大字体 - 32sp */
    const val FONT_SIZE_L = 32f

    /** 中等字体 - 18sp */
    const val FONT_SIZE_M = 18f

    /** 正常字体 - 16sp */
    const val FONT_SIZE_N = 16f

    /** 小字体 - 14sp */
    const val FONT_SIZE_S = 14f

    /** 超小字体 - 12sp */
    const val FONT_SIZE_XS = 12f

    // ==================== 间距常量 ====================

    /** EditText 水平内边距 */
    const val PADDING_EDITTEXT = 50

    /** EditText 垂直内边距 */
    const val PADDING_EDITTEXT_VERTICAL = 30

    /** 小间距 - 4dp */
    const val PADDING_SMALL = 4

    /** 中间距 - 8dp */
    const val PADDING_MEDIUM = 8

    /** 大间距 - 16dp */
    const val PADDING_LARGE = 16

    /** 超大间距 - 24dp */
    const val PADDING_XL = 24

    // ==================== 布局常量 ====================

    /** 按钮最小宽度 */
    const val BUTTON_MIN_WIDTH = 120

    /** 列表项最小高度 */
    const val LIST_ITEM_MIN_HEIGHT = 48

    /** 图标大小 */
    const val ICON_SIZE = 24

    /** 拖拽透明度 */
    const val DRAG_ALPHA = 0.5f

    // ==================== 其他常量 ====================

    /** 最近文件最大数量 */
    const val MAX_RECENT_FILES = 10

    /** 对话框动画时长 */
    const val DIALOG_ANIMATION_DURATION = 300L

    /** 加载延迟时间 */
    const val LOADING_DELAY = 1500L
}