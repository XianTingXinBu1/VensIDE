package com.venside.x1n.models

/**
 * 项目模板类
 * 
 * 职责：
 * - 定义项目模板的结构
 * - 提供模板的元数据（名称、描述、图标等）
 * - 提供模板的文件结构
 */
data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconResId: Int,
    val files: List<TemplateFile>
) {
    
    /**
     * 模板文件类
     * 定义模板中的单个文件
     */
    data class TemplateFile(
        val path: String,  // 相对于项目根目录的路径
        val content: String  // 文件内容
    )
    
    companion object {
        /**
         * 获取所有可用的项目模板
         */
        fun getAllTemplates(): List<ProjectTemplate> {
            return listOf(
                AndroLuaTemplate
            )
        }
        
        /**
         * AndroLua 项目模板
         */
        private val AndroLuaTemplate = ProjectTemplate(
            id = "andro_lua",
            name = "AndroLua 项目",
            description = "基于 AndroLua 的 Android 应用开发模板",
            iconResId = android.R.drawable.ic_menu_info_details,
            files = listOf(
                TemplateFile(
                    path = "main.lua",
                    content = """-- AndroLua 项目模板
-- 主入口文件

require "import"
import "android.widget.*"
import "android.view.*"
import "android.content.*"

-- 创建布局
layout = {
    LinearLayout,
    orientation="vertical",
    gravity="center",
    {
        TextView,
        text="Hello, AndroLua!",
        textSize="24sp",
        textColor="#FF000000",
        layout_marginTop="50dp"
    },
    {
        Button,
        text="点击我",
        layout_marginTop="20dp",
        onClick=function()
            Toast.makeText(activity, "你点击了按钮！", Toast.LENGTH_SHORT).show()
        end
    }
}

-- 设置主界面
activity.setTitle("AndroLua 应用")
activity.setContentView(loadlayout(layout))

print("AndroLua 应用已启动！")
"""
                ),
                TemplateFile(
                    path = "init.lua",
                    content = """-- AndroLua 初始化文件
-- 此文件会在 main.lua 之前加载

print("初始化 AndroLua 环境...")
"""
                )
            )
        )
    }
}