cmake_minimum_required(VERSION 3.10)
project(whisper-jni VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Path to whisper.cpp submodule
set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/whisper.cpp)

# Build whisper.cpp as static library
set(BUILD_SHARED_LIBS_DEFAULT OFF)

add_subdirectory(${WHISPER_DIR} whisper_build)

add_library(
    whisper-jni
    SHARED
    whisper-jni.cpp
)

target_link_libraries(
    whisper-jni
    whisper
    log
)
