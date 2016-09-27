#include <jni.h>
#include <string>
#include "SpatialFilter.h"
#include "VideoProcessor.h"
extern "C"

jstring
Java_cn_edu_seu_evmhr_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    VideoProcessor VP;
    bool out = VP.setOutput("output.avi");
    std::string hello = out?"true":"false";

    return env->NewStringUTF(hello.c_str());
}



