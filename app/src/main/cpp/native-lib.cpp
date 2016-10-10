#include <jni.h>
#include <string>
#include "SpatialFilter.h"
#include "VideoProcessor.h"

extern "C"

jstring
Java_cn_edu_seu_evmhr_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */,
        jstring jFileName) {
    const char* jnamestr = env->GetStringUTFChars(jFileName, NULL);

    VideoProcessor VP;
    VP.setInput(jnamestr);

    std::stringstream ss;
    ss << VP.colorMagnify();

    std::string peaks;
    ss >> peaks;

    return env->NewStringUTF(peaks.c_str());
}
