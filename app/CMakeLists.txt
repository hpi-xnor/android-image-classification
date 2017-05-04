cmake_minimum_required(VERSION 3.4.1)

set(SourceLocation src/main/native-image-utils)

add_library(native-image-utils SHARED "${SourceLocation}/imageutils_jni.cc" "${SourceLocation}/yuv2rgb.cc")
include_directories(${SOURCE})
