package com.marque0.spracherkennungsdienst.whisper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class AudioRecorder {

    private static final String TAG = "AudioRecorder";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private final AudioBuffer buffer;

    public AudioRecorder() {
        this.buffer = new AudioBuffer();
    }

    public boolean startRecording() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (minBufferSize < 0) {
            Log.e(TAG, "Invalid min buffer size");
            return false;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                Math.max(minBufferSize, SAMPLE_RATE)
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized");
            return false;
        }

        buffer.clear();
        isRecording = true;
        audioRecord.startRecording();

        recordingThread = new Thread(() -> {
            short[] temp = new short[1600]; // 100ms bei 16kHz
            while (isRecording) {
                int read = audioRecord.read(temp, 0, temp.length);
                if (read > 0) {
                    buffer.append(temp, read);
                }
            }
        });
        recordingThread.start();
        return true;
    }

    public float[] stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        try {
            if (recordingThread != null) {
                recordingThread.join(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return buffer.toFloatArray();
    }

    public boolean isRecording() {
        return isRecording;
    }

    private static class AudioBuffer {
        private short[] data = new short[SAMPLE_RATE * 5]; // 5 Sekunden initial
        private int size = 0;

        synchronized void clear() {
            size = 0;
        }

        synchronized void append(short[] src, int len) {
            if (size + len > data.length) {
                int newCapacity = Math.max(data.length * 2, size + len);
                short[] newData = new short[newCapacity];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(src, 0, data, size, len);
            size += len;
        }

        synchronized float[] toFloatArray() {
            float[] result = new float[size];
            for (int i = 0; i < size; i++) {
                result[i] = data[i] / 32768.0f;
            }
            return result;
        }
    }
}
