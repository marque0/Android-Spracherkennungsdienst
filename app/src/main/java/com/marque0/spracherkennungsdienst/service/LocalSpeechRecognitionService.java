package com.marque0.spracherkennungsdienst.service;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Minimaler alternativer Spracherkennungsdienst.
 * Diese Klasse delegiert aktuell an die Android-eigene On-Device-Spracherkennung,
 * damit der Dienst sofort funktioniert. Du kannst hier später Whisper, Google Cloud
 * Speech-to-Text oder ein selbst gehostetes Modell einbauen.
 */
public class LocalSpeechRecognitionService extends RecognitionService {

    private static final String TAG = "LocalRecService";
    private SpeechRecognizer speechRecognizer;
    private Callback currentCallback;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

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

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    dispatchReadyForSpeech(params);
                }

                @Override
                public void onBeginningOfSpeech() {
                    dispatchBeginningOfSpeech();
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    dispatchRmsChanged(rmsdB);
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    dispatchBufferReceived(buffer);
                }

                @Override
                public void onEndOfSpeech() {
                    dispatchEndOfSpeech();
                }

                @Override
                public void onError(int error) {
                    dispatchError(error, "Fehler: " + error);
                }

                @Override
                public void onResults(Bundle results) {
                    dispatchResults(results);
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    dispatchPartialResults(partialResults);
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    dispatchEvent(eventType, params);
                }
            });
            speechRecognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            Log.e(TAG, "startListening failed", e);
            dispatchError(RecognitionListener.ERROR_CLIENT, e.getMessage());
        }
    }

    @Override
    protected void onStopListening(Callback listener) {
        Log.d(TAG, "onStopListening");
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    @Override
    protected void onCancel(Callback listener) {
        Log.d(TAG, "onCancel");
        destroyRecognizer();
    }

    private void destroyRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        currentCallback = null;
    }

    private void dispatchReadyForSpeech(Bundle params) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    currentCallback.readyForSpeech(params);
                } catch (RemoteException e) {
                    Log.e(TAG, "readyForSpeech failed", e);
                }
            }
        });
    }

    private void dispatchBeginningOfSpeech() {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    currentCallback.beginningOfSpeech();
                } catch (RemoteException e) {
                    Log.e(TAG, "beginningOfSpeech failed", e);
                }
            }
        });
    }

    private void dispatchRmsChanged(float rmsdB) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    currentCallback.rmsChanged(rmsdB);
                } catch (RemoteException e) {
                    Log.e(TAG, "rmsChanged failed", e);
                }
            }
        });
    }

    private void dispatchBufferReceived(byte[] buffer) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    currentCallback.bufferReceived(buffer);
                } catch (RemoteException e) {
                    Log.e(TAG, "bufferReceived failed", e);
                }
            }
        });
    }

    private void dispatchEndOfSpeech() {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    currentCallback.endOfSpeech();
                } catch (RemoteException e) {
                    Log.e(TAG, "endOfSpeech failed", e);
                }
            }
        });
    }

    private void dispatchError(int errorCode, String message) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    Bundle bundle = new Bundle();
                    bundle.putString(SpeechRecognizer.ERROR_MESSAGE, message);
                    currentCallback.error(errorCode);
                } catch (RemoteException e) {
                    Log.e(TAG, "error failed", e);
                }
            }
        });
    }

    private void dispatchResults(Bundle results) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    currentCallback.results(results);
                } catch (RemoteException e) {
                    Log.e(TAG, "results failed", e);
                }
            }
            destroyRecognizer();
        });
    }

    private void dispatchPartialResults(Bundle partialResults) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    currentCallback.partialResults(partialResults);
                } catch (RemoteException e) {
                    Log.e(TAG, "partialResults failed", e);
                }
            }
        });
    }

    private void dispatchEvent(int eventType, Bundle params) {
        uiHandler.post(() -> {
            if (currentCallback != null) {
                try {
                    currentCallback.event(eventType, params);
                } catch (RemoteException e) {
                    Log.e(TAG, "event failed", e);
                }
            }
        });
    }
}
