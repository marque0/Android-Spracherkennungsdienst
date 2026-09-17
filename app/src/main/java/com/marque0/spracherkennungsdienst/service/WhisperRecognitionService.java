package com.marque0.spracherkennungsdienst.service;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.marque0.spracherkennungsdienst.whisper.AudioRecorder;
import com.marque0.spracherkennungsdienst.whisper.ModelManager;
import com.marque0.spracherkennungsdienst.whisper.WhisperEngine;

import java.util.ArrayList;

public class WhisperRecognitionService extends RecognitionService {

    private static final String TAG = "WhisperRecService";
    private static final String PREF_NAME = "whisper_prefs";
    private static final String KEY_MODEL = "model_name";
    private static final String KEY_LANGUAGE = "language";
    private static final String DEFAULT_MODEL = "base";
    private static final String DEFAULT_LANGUAGE = "de";

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final AudioRecorder recorder = new AudioRecorder();
    private WhisperEngine engine;
    private Callback currentCallback;
    private boolean isInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        engine = new WhisperEngine();
        initModelAsync();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (engine != null) engine.release();
    }

    private void initModelAsync() {
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String modelName = prefs.getString(KEY_MODEL, DEFAULT_MODEL);

            if (!ModelManager.isModelAvailable(this, modelName)) {
                Log.d(TAG, "Model not available yet");
                isInitialized = false;
                return;
            }

            String modelPath = ModelManager.getModelFile(this, modelName).getAbsolutePath();
            isInitialized = engine.init(this, modelPath);
            Log.d(TAG, "Model init result: " + isInitialized);
        }).start();
    }

    @Override
    protected void onStartListening(Intent recognizerIntent, Callback listener) {
        Log.d(TAG, "onStartListening");
        this.currentCallback = listener;

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            dispatchError(RecognitionListener.ERROR_INSUFFICIENT_PERMISSIONS,
                    "Mikrofon-Berechtigung fehlt.");
            return;
        }

        if (!isInitialized) {
            dispatchError(RecognitionListener.ERROR_CLIENT,
                    "Whisper-Modell noch nicht bereit. Öffne die App und lade das Modell.");
            return;
        }

        dispatchReadyForSpeech(new Bundle());
        if (!recorder.startRecording()) {
            dispatchError(RecognitionListener.ERROR_AUDIO, "Audioaufnahme konnte nicht starten.");
            return;
        }
        dispatchBeginningOfSpeech();
    }

    @Override
    protected void onStopListening(Callback listener) {
        Log.d(TAG, "onStopListening");
        float[] samples = recorder.stopRecording();
        dispatchEndOfSpeech();
        dispatchRmsChanged(0.0f);

        transcribeAsync(samples);
    }

    @Override
    protected void onCancel(Callback listener) {
        Log.d(TAG, "onCancel");
        recorder.stopRecording();
        currentCallback = null;
    }

    private void transcribeAsync(float[] samples) {
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String language = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
            String text = engine.transcribe(samples, language);

            Bundle results = new Bundle();
            ArrayList<String> texts = new ArrayList<>();
            texts.add(text.trim());
            results.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, texts);

            dispatchResults(results);
        }).start();
    }

    private void dispatchReadyForSpeech(Bundle params) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try { currentCallback.readyForSpeech(params); }
                catch (RemoteException e) { Log.e(TAG, "readyForSpeech", e); }
            }
        });
    }

    private void dispatchBeginningOfSpeech() {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try { currentCallback.beginningOfSpeech(); }
                catch (RemoteException e) { Log.e(TAG, "beginningOfSpeech", e); }
            }
        });
    }

    private void dispatchRmsChanged(float rmsdB) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try { currentCallback.rmsChanged(rmsdB); }
                catch (RemoteException e) { Log.e(TAG, "rmsChanged", e); }
            }
        });
    }

    private void dispatchEndOfSpeech() {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try { currentCallback.endOfSpeech(); }
                catch (RemoteException e) { Log.e(TAG, "endOfSpeech", e); }
            }
        });
    }

    private void dispatchError(int errorCode, String message) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try { currentCallback.error(errorCode); }
                catch (RemoteException e) { Log.e(TAG, "error", e); }
            }
        });
    }

    private void dispatchResults(Bundle results) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try { currentCallback.results(results); }
                catch (RemoteException e) { Log.e(TAG, "results", e); }
            }
            currentCallback = null;
        });
    }
}
