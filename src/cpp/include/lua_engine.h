#ifndef LUA_ENGINE_H
#define LUA_ENGINE_H

// Android NDK workaround: 先包含 C 头文件
#include <math.h>
#include <stdlib.h>
#include <string.h>

#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include <functional>
#include <mutex>
#include <memory>

extern "C" {
#include "lua.h"
#include "lauxlib.h"
#include "lualib.h"
}

namespace venside {
namespace lua {

// 执行结果状态
enum class LuaStatus {
    OK = 0,
    SYNTAX_ERROR = 1,
    RUNTIME_ERROR = 2,
    MEMORY_ERROR = 3,
    FILE_ERROR = 4
};

// 执行结果
struct LuaResult {
    LuaStatus status;
    std::string message;
    std::string returnValue;
    std::string output;  // print输出的内容
    int lineNumber;
    
    LuaResult() : status(LuaStatus::OK), lineNumber(0) {}
    
    bool isSuccess() const { return status == LuaStatus::OK; }
};

// 回调类型
using LuaCallback = std::function<int(lua_State*)>;

// Lua 引擎核心类
class LuaEngine {
public:
    LuaEngine();
    ~LuaEngine();
    
    // 禁止拷贝
    LuaEngine(const LuaEngine&) = delete;
    LuaEngine& operator=(const LuaEngine&) = delete;
    
    // 状态管理
    bool isValid() const { return m_state != nullptr; }
    void reset();
    
    // 脚本执行
    LuaResult execute(const std::string& code, const std::string& name = "chunk");
    LuaResult executeFile(const std::string& filepath);
    
    // 变量操作
    void setGlobal(const std::string& name, int value);
    void setGlobal(const std::string& name, double value);
    void setGlobal(const std::string& name, const std::string& value);
    void setGlobal(const std::string& name, bool value);
    
    int getIntGlobal(const std::string& name);
    double getDoubleGlobal(const std::string& name);
    std::string getStringGlobal(const std::string& name);
    bool getBoolGlobal(const std::string& name);
    
    // 函数注册
    void registerFunction(const std::string& name, lua_CFunction func);
    void registerFunction(const std::string& name, LuaCallback callback);
    
    // 表操作
    void createTable(const std::string& name);
    void setTableField(const std::string& tableName, const std::string& key, const std::string& value);
    void setTableField(const std::string& tableName, const std::string& key, int value);
    void setTableField(const std::string& tableName, const std::string& key, double value);
    
    // 错误处理
    std::string getLastError() const { return m_lastError; }
    int getErrorLine() const { return m_errorLine; }
    
    // 输出处理
    std::string getOutput() const { return m_outputBuffer; }
    void clearOutput() { m_outputBuffer.clear(); }
    
    // 获取原始 Lua state（高级用法）
    lua_State* getState() { return m_state; }
    
    // 编译缓存
    void enableCache(bool enable) { m_useCache = enable; }
    void clearCache();
    
    // 安全设置
    void setMemoryLimit(size_t limitMB);
    void enableSandbox(bool enable);
    
private:
    lua_State* m_state;
    std::string m_lastError;
    int m_errorLine;
    bool m_useCache;
    size_t m_memoryLimit;
    std::string m_outputBuffer;  // 输出缓冲区（捕获print输出）
    
    // 编译缓存
    std::map<std::string, std::string> m_compiledCache;
    
    // 回调存储
    std::map<std::string, std::unique_ptr<LuaCallback>> m_callbacks;
    
    // 线程安全
    std::mutex m_mutex;
    
    // 内部方法
    LuaResult handleError(int status);
    std::string extractErrorMessage(const std::string& error);
    int extractLineNumber(const std::string& error);
    
    // Lua 分配器（用于内存限制）
    static void* luaAllocator(void* ud, void* ptr, size_t osize, size_t nsize);
    
    // print回调函数
    static int luaPrint(lua_State* L);
    void setupPrintOutput();  // 设置print输出重定向
};

// Lua 管理器（多实例管理）
class LuaManager {
public:
    static LuaManager& instance();
    
    // 创建/销毁引擎实例
    int createEngine();
    void destroyEngine(int engineId);
    LuaEngine* getEngine(int engineId);
    
    // 全局回调
    void setPrintCallback(std::function<void(const std::string&)> callback);
    void setErrorCallback(std::function<void(const std::string&)> callback);
    
private:
    LuaManager();
    ~LuaManager();
    
    std::map<int, std::unique_ptr<LuaEngine>> m_engines;
    int m_nextId;
    std::mutex m_mutex;
    
    std::function<void(const std::string&)> m_printCallback;
    std::function<void(const std::string&)> m_errorCallback;
};

} // namespace lua
} // namespace venside

#endif // LUA_ENGINE_H
