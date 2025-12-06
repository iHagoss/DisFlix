#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>

#define LOG_TAG "StremioCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static int is_initialized = 0;

JNIEXPORT void JNICALL
Java_com_stremio_app_StremioCore_initCore(JNIEnv *env, jobject obj, jobject context) {
    if (is_initialized) {
        LOGI("Stremio Core already initialized");
        return;
    }
    
    LOGI("Initializing Stremio Core");
    is_initialized = 1;
}

JNIEXPORT jstring JNICALL
Java_com_stremio_app_StremioCore_getAddons(JNIEnv *env, jobject obj) {
    LOGI("Getting addons list");
    return (*env)->NewStringUTF(env, "[]");
}

JNIEXPORT jstring JNICALL
Java_com_stremio_app_StremioCore_getLibrary(JNIEnv *env, jobject obj) {
    LOGI("Getting library");
    return (*env)->NewStringUTF(env, "[]");
}

JNIEXPORT jstring JNICALL
Java_com_stremio_app_StremioCore_search(JNIEnv *env, jobject obj, jstring query) {
    const char *queryStr = (*env)->GetStringUTFChars(env, query, NULL);
    LOGI("Searching for: %s", queryStr);
    (*env)->ReleaseStringUTFChars(env, query, queryStr);
    return (*env)->NewStringUTF(env, "[]");
}

JNIEXPORT jstring JNICALL
Java_com_stremio_app_StremioCore_getAddonCatalog(JNIEnv *env, jobject obj, jstring addonId) {
    const char *addonIdStr = (*env)->GetStringUTFChars(env, addonId, NULL);
    LOGI("Getting catalog for addon: %s", addonIdStr);
    (*env)->ReleaseStringUTFChars(env, addonId, addonIdStr);
    return (*env)->NewStringUTF(env, "[]");
}

JNIEXPORT jstring JNICALL
Java_com_stremio_app_StremioCore_invokeAddon(JNIEnv *env, jobject obj, jstring addonId, jstring method, jstring args) {
    const char *addonIdStr = (*env)->GetStringUTFChars(env, addonId, NULL);
    const char *methodStr = (*env)->GetStringUTFChars(env, method, NULL);
    const char *argsStr = (*env)->GetStringUTFChars(env, args, NULL);
    
    LOGI("Invoking addon %s with method %s", addonIdStr, methodStr);
    
    (*env)->ReleaseStringUTFChars(env, addonId, addonIdStr);
    (*env)->ReleaseStringUTFChars(env, method, methodStr);
    (*env)->ReleaseStringUTFChars(env, args, argsStr);
    
    return (*env)->NewStringUTF(env, "{}");
}

JNIEXPORT jstring JNICALL
Java_com_stremio_app_StremioCore_dispatchAction(JNIEnv *env, jobject obj, jstring action, jstring payload) {
    const char *actionStr = (*env)->GetStringUTFChars(env, action, NULL);
    const char *payloadStr = (*env)->GetStringUTFChars(env, payload, NULL);
    
    LOGD("Dispatching action: %s", actionStr);
    
    if (strcmp(actionStr, "Player.SkipIntro") == 0) {
        LOGI("Skip intro action dispatched");
    } else if (strcmp(actionStr, "Player.Seek") == 0) {
        LOGI("Seek action dispatched with payload: %s", payloadStr);
    } else if (strcmp(actionStr, "Player.TimeChanged") == 0) {
        LOGD("Time changed: %s", payloadStr);
    }
    
    (*env)->ReleaseStringUTFChars(env, action, actionStr);
    (*env)->ReleaseStringUTFChars(env, payload, payloadStr);
    
    return (*env)->NewStringUTF(env, "{\"success\":true}");
}

JNIEXPORT jstring JNICALL
Java_com_stremio_app_StremioCore_getSkipIntroData(JNIEnv *env, jobject obj, jstring itemId, jlong duration) {
    const char *itemIdStr = (*env)->GetStringUTFChars(env, itemId, NULL);
    
    LOGI("Getting skip intro data for item: %s, duration: %lld ms", itemIdStr, (long long)duration);
    
    (*env)->ReleaseStringUTFChars(env, itemId, itemIdStr);
    
    char response[512];
    snprintf(response, sizeof(response),
        "{"
            "\"accuracy\":\"byDuration\","
            "\"intros\":{"
                "\"%lld\":{\"from\":0,\"to\":90000},"
                "\"%lld\":{\"from\":0,\"to\":85000}"
            "}"
        "}",
        (long long)duration,
        (long long)(duration - 5000)
    );
    
    LOGD("Returning skip intro response: %s", response);
    
    return (*env)->NewStringUTF(env, response);
}

JNIEXPORT void JNICALL
Java_com_stremio_app_StremioCore_shutdown(JNIEnv *env, jobject obj) {
    LOGI("Shutting down Stremio Core");
    is_initialized = 0;
}
