#ifndef NATIVE_H
#define NATIVE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// JNI 函数声明
JNIEXPORT jstring JNICALL
Java_com_venside_x1n_MainActivity_stringFromJNI(
    JNIEnv* env,
    jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_venside_x1n_MainActivity_getVersionInfo(
    JNIEnv* env,
    jobject thiz);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_H