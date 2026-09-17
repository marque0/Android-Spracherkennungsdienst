#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#include "whisper.cpp/include/whisper.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_marque0_spracherkennungsdienst_whisper_WhisperEngine_initModel(
        JNIEnv *env,
        jobject /*thiz*/,
        jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);
    if (!ctx) {
        LOGE("Failed to init model");
        return 0;
    }
    LOGI("Model initialized");
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_marque0_spracherkennungsdienst_whisper_WhisperEngine_transcribe(
        JNIEnv *env,
        jobject /*thiz*/,
        jlong handle,
        jfloatArray samples,
        jstring language,
        jint maxLen) {
    if (handle == 0) {
        return env->NewStringUTF("");
    }
    whisper_context *ctx = reinterpret_cast<whisper_context *>(handle);

    jsize len = env->GetArrayLength(samples);
    jfloat *raw = env->GetFloatArrayElements(samples, nullptr);
    std::vector<float> pcmf32(raw, raw + len);
    env->ReleaseFloatArrayElements(samples, raw, 0);

    const char *lang = env->GetStringUTFChars(language, nullptr);

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.language = lang;
    wparams.n_threads = 4;
    wparams.translate = false;
    wparams.print_special = false;
    wparams.print_progress = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;
    wparams.max_len = maxLen;
    wparams.offset_ms = 0;

    if (whisper_full(ctx, wparams, pcmf32.data(), static_cast<int>(pcmf32.size())) != 0) {
        env->ReleaseStringUTFChars(language, lang);
        LOGE("whisper_full failed");
        return env->NewStringUTF("");
    }

    std::string text;
    int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; i++) {
        const char *segment = whisper_full_get_segment_text(ctx, i);
        if (segment) {
            text += segment;
        }
    }

    env->ReleaseStringUTFChars(language, lang);
    return env->NewStringUTF(text.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_marque0_spracherkennungsdienst_whisper_WhisperEngine_freeModel(
        JNIEnv *env,
        jobject /*thiz*/,
        jlong handle) {
    if (handle != 0) {
        whisper_context *ctx = reinterpret_cast<whisper_context *>(handle);
        whisper_free(ctx);
    }
}
