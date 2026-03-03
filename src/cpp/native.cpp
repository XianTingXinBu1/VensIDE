#include <jni.h>
#include <string>
#include "native.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_venside_x1n_MainActivity_stringFromJNI(
    JNIEnv* env,
    jobject /* this */) {
    std::string hello = "VensIDE Native Module";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_venside_x1n_MainActivity_getVersionInfo(
    JNIEnv* env,
    jobject /* this */) {
    std::string version = "VensIDE v1.0.0 - Native Build";
    return env->NewStringUTF(version.c_str());
}