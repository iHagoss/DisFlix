LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := stremio_core
LOCAL_SRC_FILES := stremio_core.c
LOCAL_LDLIBS := -llog
LOCAL_CFLAGS := -Wall -Wextra

include $(BUILD_SHARED_LIBRARY)
