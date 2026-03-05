#include "lua_engine.h"
#include <sstream>
#include <algorithm>
#include <cctype>

namespace venside {
namespace lua {

// ==================== LuaEngine 实现 ====================

LuaEngine::LuaEngine() 
    : m_state(nullptr)
    , m_errorLine(0)
    , m_useCache(true)
    , m_memoryLimit(64) // 默认 64MB
{
    m_state = luaL_newstate();
    if (m_state) {
        // 加载标准库
        luaL_openlibs(m_state);
        // 重写print函数以捕获输出
        setupPrintOutput();
    }
}

LuaEngine::~LuaEngine() {
    if (m_state) {
        lua_close(m_state);
        m_state = nullptr;
    }
    clearCache();
}

void LuaEngine::reset() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_state) {
        lua_close(m_state);
    }
    m_state = luaL_newstate();
    if (m_state) {
        luaL_openlibs(m_state);
        setupPrintOutput();
    }
    clearCache();
    m_callbacks.clear();
    m_lastError.clear();
    m_errorLine = 0;
    m_outputBuffer.clear();
}

LuaResult LuaEngine::execute(const std::string& code, const std::string& name) {
    std::lock_guard<std::mutex> lock(m_mutex);

    LuaResult result;

    if (!m_state) {
        result.status = LuaStatus::MEMORY_ERROR;
        result.message = "Lua state is not initialized";
        return result;
    }

    // 清空输出缓冲区
    m_outputBuffer.clear();

    // 编译并执行
    int status = luaL_loadbuffer(m_state, code.c_str(), code.length(), name.c_str());

    if (status != LUA_OK) {
        return handleError(status);
    }

    // 执行编译后的代码
    status = lua_pcall(m_state, 0, 1, 0);

    if (status != LUA_OK) {
        return handleError(status);
    }

    // 获取返回值（如果有）
    if (lua_gettop(m_state) > 0 && !lua_isnil(m_state, -1)) {
        result.returnValue = luaL_tolstring(m_state, -1, nullptr);
        lua_pop(m_state, 2); // 返回值和 tolstring 的结果
    } else {
        lua_pop(m_state, 1); // 移除 nil 或返回值
    }

    result.status = LuaStatus::OK;
    result.output = m_outputBuffer;
    return result;
}

LuaResult LuaEngine::executeFile(const std::string& filepath) {
    std::lock_guard<std::mutex> lock(m_mutex);

    LuaResult result;

    if (!m_state) {
        result.status = LuaStatus::MEMORY_ERROR;
        result.message = "Lua state is not initialized";
        return result;
    }

    // 清空输出缓冲区
    m_outputBuffer.clear();

    int status = luaL_loadfile(m_state, filepath.c_str());

    if (status != LUA_OK) {
        return handleError(status);
    }

    status = lua_pcall(m_state, 0, 1, 0);

    if (status != LUA_OK) {
        return handleError(status);
    }

    if (lua_gettop(m_state) > 0 && !lua_isnil(m_state, -1)) {
        result.returnValue = luaL_tolstring(m_state, -1, nullptr);
        lua_pop(m_state, 2);
    } else {
        lua_pop(m_state, 1);
    }

    result.status = LuaStatus::OK;
    result.output = m_outputBuffer;
    return result;
}

void LuaEngine::setGlobal(const std::string& name, int value) {
    if (!m_state) return;
    lua_pushinteger(m_state, value);
    lua_setglobal(m_state, name.c_str());
}

void LuaEngine::setGlobal(const std::string& name, double value) {
    if (!m_state) return;
    lua_pushnumber(m_state, value);
    lua_setglobal(m_state, name.c_str());
}

void LuaEngine::setGlobal(const std::string& name, const std::string& value) {
    if (!m_state) return;
    lua_pushstring(m_state, value.c_str());
    lua_setglobal(m_state, name.c_str());
}

void LuaEngine::setGlobal(const std::string& name, bool value) {
    if (!m_state) return;
    lua_pushboolean(m_state, value ? 1 : 0);
    lua_setglobal(m_state, name.c_str());
}

int LuaEngine::getIntGlobal(const std::string& name) {
    if (!m_state) return 0;
    lua_getglobal(m_state, name.c_str());
    int value = (int)luaL_checkinteger(m_state, -1);
    lua_pop(m_state, 1);
    return value;
}

double LuaEngine::getDoubleGlobal(const std::string& name) {
    if (!m_state) return 0.0;
    lua_getglobal(m_state, name.c_str());
    double value = luaL_checknumber(m_state, -1);
    lua_pop(m_state, 1);
    return value;
}

std::string LuaEngine::getStringGlobal(const std::string& name) {
    if (!m_state) return "";
    lua_getglobal(m_state, name.c_str());
    const char* value = luaL_checkstring(m_state, -1);
    std::string result(value ? value : "");
    lua_pop(m_state, 1);
    return result;
}

bool LuaEngine::getBoolGlobal(const std::string& name) {
    if (!m_state) return false;
    lua_getglobal(m_state, name.c_str());
    bool value = lua_toboolean(m_state, -1) != 0;
    lua_pop(m_state, 1);
    return value;
}

void LuaEngine::registerFunction(const std::string& name, lua_CFunction func) {
    if (!m_state) return;
    lua_register(m_state, name.c_str(), func);
}

// 回调包装器
static int callbackWrapper(lua_State* L) {
    // 从注册表中获取回调
    lua_pushlightuserdata(L, (void*)L);
    lua_gettable(L, LUA_REGISTRYINDEX);
    
    auto* callback = static_cast<LuaCallback*>(lua_touserdata(L, -1));
    lua_pop(L, 1);
    
    if (callback) {
        return (*callback)(L);
    }
    return 0;
}

void LuaEngine::registerFunction(const std::string& name, LuaCallback callback) {
    if (!m_state) return;
    
    // 存储回调
    m_callbacks[name] = std::make_unique<LuaCallback>(std::move(callback));
    
    // 注册包装函数
    lua_pushlightuserdata(m_state, m_callbacks[name].get());
    lua_pushlightuserdata(m_state, m_state);
    lua_settable(m_state, LUA_REGISTRYINDEX);
    
    lua_register(m_state, name.c_str(), callbackWrapper);
}

void LuaEngine::createTable(const std::string& name) {
    if (!m_state) return;
    lua_newtable(m_state);
    lua_setglobal(m_state, name.c_str());
}

void LuaEngine::setTableField(const std::string& tableName, const std::string& key, const std::string& value) {
    if (!m_state) return;
    lua_getglobal(m_state, tableName.c_str());
    if (lua_istable(m_state, -1)) {
        lua_pushstring(m_state, value.c_str());
        lua_setfield(m_state, -2, key.c_str());
    }
    lua_pop(m_state, 1);
}

void LuaEngine::setTableField(const std::string& tableName, const std::string& key, int value) {
    if (!m_state) return;
    lua_getglobal(m_state, tableName.c_str());
    if (lua_istable(m_state, -1)) {
        lua_pushinteger(m_state, value);
        lua_setfield(m_state, -2, key.c_str());
    }
    lua_pop(m_state, 1);
}

void LuaEngine::setTableField(const std::string& tableName, const std::string& key, double value) {
    if (!m_state) return;
    lua_getglobal(m_state, tableName.c_str());
    if (lua_istable(m_state, -1)) {
        lua_pushnumber(m_state, value);
        lua_setfield(m_state, -2, key.c_str());
    }
    lua_pop(m_state, 1);
}

LuaResult LuaEngine::handleError(int status) {
    LuaResult result;
    
    const char* msg = lua_tostring(m_state, -1);
    std::string error = msg ? msg : "Unknown error";
    
    m_lastError = error;
    m_errorLine = extractLineNumber(error);
    
    result.message = extractErrorMessage(error);
    result.lineNumber = m_errorLine;
    
    switch (status) {
        case LUA_ERRSYNTAX:
            result.status = LuaStatus::SYNTAX_ERROR;
            break;
        case LUA_ERRRUN:
            result.status = LuaStatus::RUNTIME_ERROR;
            break;
        case LUA_ERRMEM:
            result.status = LuaStatus::MEMORY_ERROR;
            break;
        case LUA_ERRFILE:
            result.status = LuaStatus::FILE_ERROR;
            break;
        default:
            result.status = LuaStatus::RUNTIME_ERROR;
    }
    
    lua_pop(m_state, 1);
    return result;
}

std::string LuaEngine::extractErrorMessage(const std::string& error) {
    // 提取错误消息（去掉行号前缀）
    size_t colonPos = error.find(':');
    size_t secondColonPos = error.find(':', colonPos + 1);
    
    if (secondColonPos != std::string::npos) {
        std::string msg = error.substr(secondColonPos + 1);
        // 去掉前导空格
        size_t start = msg.find_first_not_of(" \t");
        if (start != std::string::npos) {
            return msg.substr(start);
        }
        return msg;
    }
    return error;
}

int LuaEngine::extractLineNumber(const std::string& error) {
    // 从错误消息中提取行号（格式：chunk:行号:消息）
    size_t colonPos = error.find(':');
    if (colonPos != std::string::npos) {
        size_t secondColonPos = error.find(':', colonPos + 1);
        if (secondColonPos != std::string::npos) {
            std::string lineNumStr = error.substr(colonPos + 1, secondColonPos - colonPos - 1);
            try {
                return std::stoi(lineNumStr);
            } catch (...) {
                return 0;
            }
        }
    }
    return 0;
}

void LuaEngine::clearCache() {
    m_compiledCache.clear();
}

void LuaEngine::setMemoryLimit(size_t limitMB) {
    m_memoryLimit = limitMB;
    // Lua 5.4 的内存限制需要在创建 state 时设置
    // 这里只记录限制值，实际限制需要重新创建 state
}

void LuaEngine::enableSandbox(bool enable) {
    if (!m_state) return;
    
    if (enable) {
        // 移除危险的函数
        lua_pushnil(m_state);
        lua_setglobal(m_state, "loadfile");
        lua_pushnil(m_state);
        lua_setglobal(m_state, "dofile");
        lua_pushnil(m_state);
        lua_setglobal(m_state, "load");
        lua_pushnil(m_state);
        lua_setglobal(m_state, "loadstring");
        
        // 可以继续移除更多危险函数，如 os.execute, io.open 等
    }
}

void* LuaEngine::luaAllocator(void* ud, void* ptr, size_t osize, size_t nsize) {
    LuaEngine* engine = static_cast<LuaEngine*>(ud);

    if (nsize == 0) {
        free(ptr);
        return nullptr;
    }

    // 检查内存限制
    // 简化实现，实际需要跟踪总分配量

    void* newPtr = realloc(ptr, nsize);
    return newPtr;
}

// ==================== print函数重写 ====================

int LuaEngine::luaPrint(lua_State* L) {
    // 从注册表中获取引擎实例
    lua_pushlightuserdata(L, (void*)L);
    lua_gettable(L, LUA_REGISTRYINDEX);

    LuaEngine* engine = static_cast<LuaEngine*>(lua_touserdata(L, -1));
    lua_pop(L, 1); // 移除引擎指针

    if (!engine) {
        // 如果无法获取引擎实例，回退到原始print
        int n = lua_gettop(L);
        lua_getglobal(L, "io");
        lua_getfield(L, -1, "write");
        lua_getglobal(L, "io");
        lua_getfield(L, -1, "stdout");

        for (int i = 1; i <= n; i++) {
            const char* s = luaL_tolstring(L, i, nullptr);
            lua_pushvalue(L, -3); // io.write
            lua_pushvalue(L, -3); // io.stdout
            lua_pushstring(L, s);
            lua_call(L, 2, 0);
            lua_pop(L, 1); // 移除 tostring 的结果
        }
        lua_pushstring(L, "\n");
        lua_pushvalue(L, -3); // io.write
        lua_pushvalue(L, -3); // io.stdout
        lua_call(L, 2, 0);
        lua_pop(L, 3); // 清理栈
        return 0;
    }

    int n = lua_gettop(L);
    for (int i = 1; i <= n; i++) {
        if (i > 1) {
            engine->m_outputBuffer += "\t";
        }
        const char* s = luaL_tolstring(L, i, nullptr);
        engine->m_outputBuffer += s;
        lua_pop(L, 1); // 移除 tostring 的结果
    }
    engine->m_outputBuffer += "\n";
    return 0;
}

void LuaEngine::setupPrintOutput() {
    // 将引擎实例保存到注册表
    lua_pushlightuserdata(m_state, (void*)m_state);
    lua_pushlightuserdata(m_state, this);
    lua_settable(m_state, LUA_REGISTRYINDEX);

    // 注册新的print函数
    lua_register(m_state, "print", luaPrint);
}

// ==================== LuaManager 实现 ====================

LuaManager& LuaManager::instance() {
    static LuaManager instance;
    return instance;
}

LuaManager::LuaManager() : m_nextId(1) {}

LuaManager::~LuaManager() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_engines.clear();
}

int LuaManager::createEngine() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    int id = m_nextId++;
    m_engines[id] = std::make_unique<LuaEngine>();
    
    return id;
}

void LuaManager::destroyEngine(int engineId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_engines.erase(engineId);
}

LuaEngine* LuaManager::getEngine(int engineId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_engines.find(engineId);
    if (it != m_engines.end()) {
        return it->second.get();
    }
    return nullptr;
}

void LuaManager::setPrintCallback(std::function<void(const std::string&)> callback) {
    m_printCallback = std::move(callback);
}

void LuaManager::setErrorCallback(std::function<void(const std::string&)> callback) {
    m_errorCallback = std::move(callback);
}

} // namespace lua
} // namespace venside
