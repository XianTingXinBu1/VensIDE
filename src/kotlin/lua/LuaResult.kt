package com.venside.x1n.lua

/**
 * Lua 执行状态枚举
 */
enum class LuaStatus(val code: Int) {
    OK(0),
    SYNTAX_ERROR(1),
    RUNTIME_ERROR(2),
    MEMORY_ERROR(3),
    FILE_ERROR(4);

    companion object {
        fun fromCode(code: Int): LuaStatus {
            return entries.find { it.code == code } ?: RUNTIME_ERROR
        }
    }
}

/**
 * Lua 执行结果封装
 */
data class LuaResult(
    val status: LuaStatus,
    val message: String,
    val returnValue: String,
    val output: String = "",  // print输出的内容
    val lineNumber: Int
) {
    val isSuccess: Boolean
        get() = status == LuaStatus.OK

    val isError: Boolean
        get() = status != LuaStatus.OK

    companion object {
        fun success(returnValue: String = "", output: String = ""): LuaResult {
            return LuaResult(
                status = LuaStatus.OK,
                message = "",
                returnValue = returnValue,
                output = output,
                lineNumber = 0
            )
        }

        fun error(status: LuaStatus, message: String, lineNumber: Int = 0): LuaResult {
            return LuaResult(
                status = status,
                message = message,
                returnValue = "",
                output = "",
                lineNumber = lineNumber
            )
        }

        /**
         * 从 JNI 返回的数组解析结果
         */
        internal fun fromArray(result: Array<Any>?): LuaResult {
            if (result == null || result.size < 5) {
                return error(LuaStatus.RUNTIME_ERROR, "Invalid result from native layer")
            }

            return LuaResult(
                status = LuaStatus.fromCode((result[0] as String).toInt()),
                message = result[1] as String,
                returnValue = result[2] as String,
                output = result[3] as String,
                lineNumber = (result[4] as String).toInt()
            )
        }
    }

    override fun toString(): String {
        return if (isSuccess) {
            "LuaResult(OK, returnValue=\"$returnValue\", output=\"$output\")"
        } else {
            "LuaResult($status, line=$lineNumber, message=\"$message\")"
        }
    }
}
