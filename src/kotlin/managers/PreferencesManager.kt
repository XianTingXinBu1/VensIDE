package com.venside.x1n.managers

import android.content.Context
import android.content.SharedPreferences

/**
 * 持久化设置管理器
 * 负责全局和长期有效的设置存储
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "VensIDE_Preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SORT_MODES = "sort_modes"
        private const val KEY_SHOW_SIDEBAR = "show_sidebar"
    }

    /**
     * 保存排序规则列表
     * @param sortModes 排序规则列表，按优先级排序
     */
    fun saveSortModes(sortModes: List<FileManager.SortMode>) {
        val modeStrings = sortModes.joinToString(",") { it.name }
        prefs.edit().putString(KEY_SORT_MODES, modeStrings).apply()
    }

    /**
     * 获取排序规则列表
     * @return 排序规则列表，如果未设置则返回默认值（名称升序）
     */
    fun getSortModes(): List<FileManager.SortMode> {
        val modeStrings = prefs.getString(KEY_SORT_MODES, null)
        return if (modeStrings != null) {
            modeStrings.split(",").mapNotNull { modeName ->
                try {
                    FileManager.SortMode.valueOf(modeName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        } else {
            listOf(FileManager.SortMode.NAME_ASC)
        }
    }

    /**
     * 保存侧边栏显示状态
     * @param show 是否显示侧边栏
     */
    fun saveSidebarState(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SIDEBAR, show).apply()
    }

    /**
     * 获取侧边栏显示状态
     * @return 是否显示侧边栏，默认为 true
     */
    fun getSidebarState(): Boolean {
        return prefs.getBoolean(KEY_SHOW_SIDEBAR, true)
    }

    /**
     * 清除所有设置
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}