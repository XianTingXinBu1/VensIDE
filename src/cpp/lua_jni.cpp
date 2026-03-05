#include <jni.h>
#include <string>
#include <android/log.h>
#include "lua_engine.h"

#define LOG_TAG "LuaEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace venside::lua;

extern "C" {

// ==================== 引擎生命周期 ====================

JNIEXPORT jint JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeCreateEngine(JNIEnv* env, jobject thiz) {
    int engineId = LuaManager::instance().createEngine();
    LOGI("Created Lua engine with ID: %d", engineId);
    return engineId;
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeDestroyEngine(JNIEnv* env, jobject thiz, jint engineId) {
    LuaManager::instance().destroyEngine(engineId);
    LOGI("Destroyed Lua engine with ID: %d", engineId);
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeReset(JNIEnv* env, jobject thiz, jint engineId) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        engine->reset();
    }
}

// ==================== 脚本执行 ====================

JNIEXPORT jobjectArray JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeExecute(JNIEnv* env, jobject thiz, jint engineId, jstring code, jstring name) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);

    if (!engine) {
        LOGE("Invalid engine ID: %d", engineId);
        return nullptr;
    }

    const char* codeStr = env->GetStringUTFChars(code, nullptr);
    const char* nameStr = env->GetStringUTFChars(name, nullptr);

    LuaResult result = engine->execute(codeStr, nameStr ? nameStr : "chunk");

    env->ReleaseStringUTFChars(code, codeStr);
    env->ReleaseStringUTFChars(name, nameStr);

    // 创建结果数组 [status, message, returnValue, output, lineNumber]
    jobjectArray resultArray = env->NewObjectArray(5, env->FindClass("java/lang/Object"), nullptr);

    // status
    jstring statusStr = env->NewStringUTF(std::to_string(static_cast<int>(result.status)).c_str());
    env->SetObjectArrayElement(resultArray, 0, statusStr);

    // message
    jstring messageStr = env->NewStringUTF(result.message.c_str());
    env->SetObjectArrayElement(resultArray, 1, messageStr);

    // returnValue
    jstring returnStr = env->NewStringUTF(result.returnValue.c_str());
    env->SetObjectArrayElement(resultArray, 2, returnStr);

    // output (print输出的内容)
    jstring outputStr = env->NewStringUTF(result.output.c_str());
    env->SetObjectArrayElement(resultArray, 3, outputStr);

    // lineNumber
    jstring lineStr = env->NewStringUTF(std::to_string(result.lineNumber).c_str());
    env->SetObjectArrayElement(resultArray, 4, lineStr);

    return resultArray;
}

JNIEXPORT jobjectArray JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeExecuteFile(JNIEnv* env, jobject thiz, jint engineId, jstring filepath) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);

    if (!engine) {
        LOGE("Invalid engine ID: %d", engineId);
        return nullptr;
    }

    const char* pathStr = env->GetStringUTFChars(filepath, nullptr);

    LuaResult result = engine->executeFile(pathStr);

    env->ReleaseStringUTFChars(filepath, pathStr);

    // 创建结果数组 [status, message, returnValue, output, lineNumber]
    jobjectArray resultArray = env->NewObjectArray(5, env->FindClass("java/lang/Object"), nullptr);

    jstring statusStr = env->NewStringUTF(std::to_string(static_cast<int>(result.status)).c_str());
    env->SetObjectArrayElement(resultArray, 0, statusStr);

    jstring messageStr = env->NewStringUTF(result.message.c_str());
    env->SetObjectArrayElement(resultArray, 1, messageStr);

    jstring returnStr = env->NewStringUTF(result.returnValue.c_str());
    env->SetObjectArrayElement(resultArray, 2, returnStr);

    // output (print输出的内容)
    jstring outputStr = env->NewStringUTF(result.output.c_str());
    env->SetObjectArrayElement(resultArray, 3, outputStr);

    jstring lineStr = env->NewStringUTF(std::to_string(result.lineNumber).c_str());
    env->SetObjectArrayElement(resultArray, 4, lineStr);

    return resultArray;
}

// ==================== 全局变量设置 ====================

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeSetGlobalInt(JNIEnv* env, jobject thiz, jint engineId, jstring name, jint value) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        engine->setGlobal(nameStr, static_cast<int>(value));
        env->ReleaseStringUTFChars(name, nameStr);
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeSetGlobalDouble(JNIEnv* env, jobject thiz, jint engineId, jstring name, jdouble value) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        engine->setGlobal(nameStr, static_cast<double>(value));
        env->ReleaseStringUTFChars(name, nameStr);
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeSetGlobalString(JNIEnv* env, jobject thiz, jint engineId, jstring name, jstring value) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        const char* valueStr = env->GetStringUTFChars(value, nullptr);
        engine->setGlobal(nameStr, std::string(valueStr));
        env->ReleaseStringUTFChars(name, nameStr);
        env->ReleaseStringUTFChars(value, valueStr);
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeSetGlobalBoolean(JNIEnv* env, jobject thiz, jint engineId, jstring name, jboolean value) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        engine->setGlobal(nameStr, value == JNI_TRUE);
        env->ReleaseStringUTFChars(name, nameStr);
    }
}

// ==================== 全局变量获取 ====================

JNIEXPORT jint JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeGetGlobalInt(JNIEnv* env, jobject thiz, jint engineId, jstring name) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        int value = engine->getIntGlobal(nameStr);
        env->ReleaseStringUTFChars(name, nameStr);
        return value;
    }
    return 0;
}

JNIEXPORT jdouble JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeGetGlobalDouble(JNIEnv* env, jobject thiz, jint engineId, jstring name) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        double value = engine->getDoubleGlobal(nameStr);
        env->ReleaseStringUTFChars(name, nameStr);
        return value;
    }
    return 0.0;
}

JNIEXPORT jstring JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeGetGlobalString(JNIEnv* env, jobject thiz, jint engineId, jstring name) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        std::string value = engine->getStringGlobal(nameStr);
        env->ReleaseStringUTFChars(name, nameStr);
        return env->NewStringUTF(value.c_str());
    }
    return env->NewStringUTF("");
}

JNIEXPORT jboolean JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeGetGlobalBoolean(JNIEnv* env, jobject thiz, jint engineId, jstring name) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        bool value = engine->getBoolGlobal(nameStr);
        env->ReleaseStringUTFChars(name, nameStr);
        return value ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

// ==================== 表操作 ====================

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeCreateTable(JNIEnv* env, jobject thiz, jint engineId, jstring name) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* nameStr = env->GetStringUTFChars(name, nullptr);
        engine->createTable(nameStr);
        env->ReleaseStringUTFChars(name, nameStr);
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeSetTableFieldString(JNIEnv* env, jobject thiz, jint engineId, jstring tableName, jstring key, jstring value) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* tableStr = env->GetStringUTFChars(tableName, nullptr);
        const char* keyStr = env->GetStringUTFChars(key, nullptr);
        const char* valueStr = env->GetStringUTFChars(value, nullptr);
        engine->setTableField(tableStr, keyStr, std::string(valueStr));
        env->ReleaseStringUTFChars(tableName, tableStr);
        env->ReleaseStringUTFChars(key, keyStr);
        env->ReleaseStringUTFChars(value, valueStr);
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeSetTableFieldInt(JNIEnv* env, jobject thiz, jint engineId, jstring tableName, jstring key, jint value) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* tableStr = env->GetStringUTFChars(tableName, nullptr);
        const char* keyStr = env->GetStringUTFChars(key, nullptr);
        engine->setTableField(tableStr, keyStr, static_cast<int>(value));
        env->ReleaseStringUTFChars(tableName, tableStr);
        env->ReleaseStringUTFChars(key, keyStr);
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeSetTableFieldDouble(JNIEnv* env, jobject thiz, jint engineId, jstring tableName, jstring key, jdouble value) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        const char* tableStr = env->GetStringUTFChars(tableName, nullptr);
        const char* keyStr = env->GetStringUTFChars(key, nullptr);
        engine->setTableField(tableStr, keyStr, static_cast<double>(value));
        env->ReleaseStringUTFChars(tableName, tableStr);
        env->ReleaseStringUTFChars(key, keyStr);
    }
}

// ==================== 安全设置 ====================

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeEnableSandbox(JNIEnv* env, jobject thiz, jint engineId, jboolean enable) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        engine->enableSandbox(enable == JNI_TRUE);
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeSetMemoryLimit(JNIEnv* env, jobject thiz, jint engineId, jint limitMB) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        engine->setMemoryLimit(static_cast<size_t>(limitMB));
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeEnableCache(JNIEnv* env, jobject thiz, jint engineId, jboolean enable) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        engine->enableCache(enable == JNI_TRUE);
    }
}

JNIEXPORT void JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeClearCache(JNIEnv* env, jobject thiz, jint engineId) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        engine->clearCache();
    }
}

// ==================== 错误处理 ====================

JNIEXPORT jstring JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeGetLastError(JNIEnv* env, jobject thiz, jint engineId) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        return env->NewStringUTF(engine->getLastError().c_str());
    }
    return env->NewStringUTF("");
}

JNIEXPORT jint JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeGetErrorLine(JNIEnv* env, jobject thiz, jint engineId) {
    LuaEngine* engine = LuaManager::instance().getEngine(engineId);
    if (engine) {
        return engine->getErrorLine();
    }
    return 0;
}

// ==================== 版本信息 ====================

JNIEXPORT jstring JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeGetLuaVersion(JNIEnv* env, jobject thiz) {
    return env->NewStringUTF(LUA_VERSION);
}

JNIEXPORT jstring JNICALL
Java_com_venside_x1n_lua_LuaEngine_nativeGetEngineVersion(JNIEnv* env, jobject thiz) {
    return env->NewStringUTF("VensIDE Lua Engine v1.0.0");
}

} // extern "C"
