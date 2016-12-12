LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OpenCV_INSTALL_MODULES := on
OpenCV_CAMERA_MODULES := off

#opencv编译成静态库
OPENCV_LIB_TYPE := STATIC

#设定OpenCV.mk文件的地址
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
include E:\project\JAVA_soft\EVMHR\native\jni\OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif

#自己编译出的动态库名字
LOCAL_MODULE := native-lib

#替换成自己程序源文件的地址
LOCAL_SRC_FILES :=  native-lib.cpp\
                    SpatialFilter.cpp\
                    VideoProcessor.cpp

#链接库
LOCAL_LDLIBS +=  -lm -llog -ljnigraphics

#编译为动态链接库
include $(BUILD_SHARED_LIBRARY)
