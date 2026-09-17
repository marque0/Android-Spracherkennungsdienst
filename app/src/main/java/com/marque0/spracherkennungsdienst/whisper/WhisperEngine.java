package com.marque0.spracherkennungsdienst.whisper;

import android.content.Context;
import android.util.Log;

public class WhisperEngine {
    private static final String TAG = "WhisperEngine";
    static {
        System.loadLibrary("whisper-jni");
    }

    private long handle = 0;

    public boolean init(Context context, String modelPath) {
        Log.d(TAG, "Loading model from " + modelPath);
        handle = initModel(modelPath);
        return handle != 0;
    }

    public String transcribe(float[] samples, String language) {
        if (handle == 0) return "";
        return transcribe(handle, samples, language, 0);
    }

    public void release() {
        if (handle != 0) {
            freeModel(handle);
            handle = 0;
        }
    }

    private native long initModel(String modelPath);
    private native String transcribe(long handle, float[] samples, String language, int maxLen);
    private native void freeModel(long handle);
}
