package com.venside.x1n.lua

import java.io.File

/**
 * VensIDE Lua 引擎
 * 
 * 提供高性能的 Lua 5.4 脚本执行环境
 * 使用 JNI 调用原生 Lua 库
 */
class LuaEngine {

    // Native 引擎 ID
    private var engineId: Int = 0

    // 引擎是否已初始化
    private var initialized: Boolean = false

    init {
        initEngine()
    }

    /**
     * 初始化引擎
     */
    private fun initEngine() {
        if (!initialized) {
            engineId = nativeCreateEngine()
            initialized = true
        }
    }

    /**
     * 销毁引擎（释放资源）
     */
    fun destroy() {
        if (initialized) {
            nativeDestroyEngine(engineId)
            initialized = false
        }
    }

    /**
     * 重置引擎状态
     */
    fun reset() {
        checkInitialized()
        nativeReset(engineId)
    }

    // ==================== 脚本执行 ====================

    /**
     * 执行 Lua 代码字符串
     * @param code Lua 代码
     * @param name 代码块名称（用于错误提示）
     * @return 执行结果
     */
    fun execute(code: String, name: String = "chunk"): LuaResult {
        checkInitialized()
        val result = nativeExecute(engineId, code, name)
        return LuaResult.fromArray(result)
    }

    /**
     * 执行 Lua 文件
     * @param file Lua 文件
     * @return 执行结果
     */
    fun executeFile(file: File): LuaResult {
        return executeFile(file.absolutePath)
    }

    /**
     * 执行 Lua 文件
     * @param filepath 文件路径
     * @return 执行结果
     */
    fun executeFile(filepath: String): LuaResult {
        checkInitialized()
        val result = nativeExecuteFile(engineId, filepath)
        return LuaResult.fromArray(result)
    }

    // ==================== 全局变量设置 ====================

    /**
     * 设置整数类型全局变量
     */
    fun setGlobal(name: String, value: Int) {
        checkInitialized()
        nativeSetGlobalInt(engineId, name, value)
    }

    /**
     * 设置浮点数类型全局变量
     */
    fun setGlobal(name: String, value: Double) {
        checkInitialized()
        nativeSetGlobalDouble(engineId, name, value)
    }

    /**
     * 设置字符串类型全局变量
     */
    fun setGlobal(name: String, value: String) {
        checkInitialized()
        nativeSetGlobalString(engineId, name, value)
    }

    /**
     * 设置布尔类型全局变量
     */
    fun setGlobal(name: String, value: Boolean) {
        checkInitialized()
        nativeSetGlobalBoolean(engineId, name, value)
    }

    // ==================== 全局变量获取 ====================

    /**
     * 获取整数类型全局变量
     */
    fun getInt(name: String): Int {
        checkInitialized()
        return nativeGetGlobalInt(engineId, name)
    }

    /**
     * 获取浮点数类型全局变量
     */
    fun getDouble(name: String): Double {
        checkInitialized()
        return nativeGetGlobalDouble(engineId, name)
    }

    /**
     * 获取字符串类型全局变量
     */
    fun getString(name: String): String {
        checkInitialized()
        return nativeGetGlobalString(engineId, name)
    }

    /**
     * 获取布尔类型全局变量
     */
    fun getBoolean(name: String): Boolean {
        checkInitialized()
        return nativeGetGlobalBoolean(engineId, name)
    }

    // ==================== 表操作 ====================

    /**
     * 创建 Lua 表
     */
    fun createTable(name: String) {
        checkInitialized()
        nativeCreateTable(engineId, name)
    }

    /**
     * 设置表字段（字符串值）
     */
    fun setTableField(tableName: String, key: String, value: String) {
        checkInitialized()
        nativeSetTableFieldString(engineId, tableName, key, value)
    }

    /**
     * 设置表字段（整数值）
     */
    fun setTableField(tableName: String, key: String, value: Int) {
        checkInitialized()
        nativeSetTableFieldInt(engineId, tableName, key, value)
    }

    /**
     * 设置表字段（浮点数值）
     */
    fun setTableField(tableName: String, key: String, value: Double) {
        checkInitialized()
        nativeSetTableFieldDouble(engineId, tableName, key, value)
    }

    // ==================== 安全设置 ====================

    /**
     * 启用/禁用沙盒模式
     * 沙盒模式会禁用危险函数（如 loadfile, dofile, load 等）
     */
    fun enableSandbox(enable: Boolean) {
        checkInitialized()
        nativeEnableSandbox(engineId, enable)
    }

    /**
     * 设置内存限制（MB）
     */
    fun setMemoryLimit(limitMB: Int) {
        checkInitialized()
        nativeSetMemoryLimit(engineId, limitMB)
    }

    /**
     * 启用/禁用编译缓存
     */
    fun enableCache(enable: Boolean) {
        checkInitialized()
        nativeEnableCache(engineId, enable)
    }

    /**
     * 清空编译缓存
     */
    fun clearCache() {
        checkInitialized()
        nativeClearCache(engineId)
    }

    // ==================== 错误处理 ====================

    /**
     * 获取最后的错误信息
     */
    fun getLastError(): String {
        checkInitialized()
        return nativeGetLastError(engineId)
    }

    /**
     * 获取错误发生的行号
     */
    fun getErrorLine(): Int {
        checkInitialized()
        return nativeGetErrorLine(engineId)
    }

    // ==================== 版本信息 ====================

    /**
     * 获取 Lua 版本
     */
    val luaVersion: String
        get() = nativeGetLuaVersion()

    /**
     * 获取引擎版本
     */
    val engineVersion: String
        get() = nativeGetEngineVersion()

    // ==================== 辅助方法 ====================

    /**
     * 检查引擎是否已初始化
     */
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("LuaEngine is not initialized or has been destroyed")
        }
    }

    /**
     * 快速执行 Lua 表达式并获取结果
     */
    fun eval(expression: String): String {
        val result = execute("return $expression")
        return if (result.isSuccess) result.returnValue else throw LuaException(result)
    }

    /**
     * 快速执行 Lua 代码（忽略返回值）
     */
    fun run(code: String) {
        val result = execute(code)
        if (!result.isSuccess) {
            throw LuaException(result)
        }
    }

    // ==================== Native 方法 ====================

    // 引擎生命周期
    private external fun nativeCreateEngine(): Int
    private external fun nativeDestroyEngine(engineId: Int)
    private external fun nativeReset(engineId: Int)

    // 脚本执行
    private external fun nativeExecute(engineId: Int, code: String, name: String): Array<Any>?
    private external fun nativeExecuteFile(engineId: Int, filepath: String): Array<Any>?

    // 全局变量设置
    private external fun nativeSetGlobalInt(engineId: Int, name: String, value: Int)
    private external fun nativeSetGlobalDouble(engineId: Int, name: String, value: Double)
    private external fun nativeSetGlobalString(engineId: Int, name: String, value: String)
    private external fun nativeSetGlobalBoolean(engineId: Int, name: String, value: Boolean)

    // 全局变量获取
    private external fun nativeGetGlobalInt(engineId: Int, name: String): Int
    private external fun nativeGetGlobalDouble(engineId: Int, name: String): Double
    private external fun nativeGetGlobalString(engineId: Int, name: String): String
    private external fun nativeGetGlobalBoolean(engineId: Int, name: String): Boolean

    // 表操作
    private external fun nativeCreateTable(engineId: Int, name: String)
    private external fun nativeSetTableFieldString(engineId: Int, tableName: String, key: String, value: String)
    private external fun nativeSetTableFieldInt(engineId: Int, tableName: String, key: String, value: Int)
    private external fun nativeSetTableFieldDouble(engineId: Int, tableName: String, key: String, value: Double)

    // 安全设置
    private external fun nativeEnableSandbox(engineId: Int, enable: Boolean)
    private external fun nativeSetMemoryLimit(engineId: Int, limitMB: Int)
    private external fun nativeEnableCache(engineId: Int, enable: Boolean)
    private external fun nativeClearCache(engineId: Int)

    // 错误处理
    private external fun nativeGetLastError(engineId: Int): String
    private external fun nativeGetErrorLine(engineId: Int): Int

    // 版本信息
    private external fun nativeGetLuaVersion(): String
    private external fun nativeGetEngineVersion(): String

    companion object {
        /**
         * 加载 Native 库
         */
        init {
            System.loadLibrary("native")
        }
    }
}

/**
 * Lua 异常类
 */
class LuaException(val result: LuaResult) : Exception(
    "Lua Error (${result.status}) at line ${result.lineNumber}: ${result.message}"
)
