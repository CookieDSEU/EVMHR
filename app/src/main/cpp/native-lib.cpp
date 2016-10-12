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
        
    double length = VP.getLengthMS() / 1000;
    int peaks = VP.colorMagnify();

    std::stringstream ss;
    ss << std::floor(60 / length * peaks);

    std::string heartRate;
    ss >> heartRate;

    return env->NewStringUTF(heartRate.c_str());
}
