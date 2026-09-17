package com.marque0.spracherkennungsdienst.whisper;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ModelManager {

    private static final String TAG = "ModelManager";
    private static final String BASE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/";

    public static File getModelDir(Context context) {
        File dir = new File(context.getFilesDir(), "models");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getModelFile(Context context, String modelName) {
        return new File(getModelDir(context), "ggml-" + modelName + ".bin");
    }

    public static boolean isModelAvailable(Context context, String modelName) {
        return getModelFile(context, modelName).exists();
    }

    public static void downloadModel(Context context, String modelName, DownloadListener listener) {
        File outFile = getModelFile(context, modelName);
        String url = BASE_URL + outFile.getName();
        Log.d(TAG, "Downloading " + url);

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL modelUrl = new URL(url);
                connection = (HttpURLConnection) modelUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    throw new IOException("HTTP " + responseCode);
                }

                long totalBytes = connection.getContentLengthLong();
                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (listener != null && totalBytes > 0) {
                            listener.onProgress((int) (100 * downloaded / totalBytes));
                        }
                    }
                }
                if (listener != null) listener.onComplete(true, outFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                if (outFile.exists()) outFile.delete();
                if (listener != null) listener.onComplete(false, e.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    public interface DownloadListener {
        void onProgress(int percent);
        void onComplete(boolean success, String message);
    }
}
