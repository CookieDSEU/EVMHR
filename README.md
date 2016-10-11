# EVMHR

## Introduction

EVMHR is an Android app for detecting human heart rate by analysing the video taken from the front camera of the phone, it is based on the theory of *Eulerian Video Magnification* raised by Wu, H.-Y., et al in 2012. And the implementation of the core function is provided by Weizhou Pan in his blog article: [Eulerian Video Magnification](http://www.hahack.com/codes/eulerian-video-magnification/#源码和程序).

Here is a simple *demo*:

## How to use

### Android Studio

This project contains a large amount of  C++ code, so we use NDK to build the native library, which will be called by android interface. Android Studio 2.2 provides a good compiling compatibility with C++ code, we are not sure about the success of compilation in previous version of Android Studio.

### FFmpeg

The video recorded from a phone camera was compressed by the system, while OpenCV cannot read the video codec used by Android, which typically is avc or h.264. So we use a compiled version of FFmpeg: [ffmpeg-android-java](https://github.com/WritingMinds/ffmpeg-android-java).

To use this library, make sure to have the following line in your app's build.gradle:

```
dependencies {
	...
	compile 'com.writingminds:FFmpegAndroid:0.3.2'
	...
}
```

### OpenCV

This project is based on OpenCV4Android 3.1.0, you can download it from [here](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/). We only need the native folder, so copy it to the same level beside your app folder.

### NDK

We use NDK 12, just download it from the manager in Android Studio.

In app/src/main/cpp fold, edit the Android.mk file and adapt the path of openCV.mk to your own

`include PathTo \native\jni\OpenCV.mk`

If you add your own .cpp files, don't forget to add them into `LOCAL_SRC_FILES`  

If you want to change the native interface function in C++, make sure you follow the naming rule:

`Java_ + PackageName + ClassName+ InterfaceName`

and use `InterfaceName` in your java code.

## Lincese

MIT License
