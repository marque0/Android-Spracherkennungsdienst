package com.marque0.spracherkennungsdienst;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.marque0.spracherkennungsdienst.whisper.ModelManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MIC = 1;
    private static final String PREF_NAME = "whisper_prefs";
    private static final String KEY_MODEL = "model_name";
    private static final String DEFAULT_MODEL = "base";

    private Spinner modelSpinner;
    private TextView statusText;
    private TextView modelStatusText;
    private ProgressBar progressBar;
    private Button downloadButton;
    private Button openSettingsButton;

    private String[] availableModels;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        availableModels = getResources().getStringArray(R.array.model_names);

        statusText = findViewById(R.id.statusText);
        modelStatusText = findViewById(R.id.modelStatusText);
        progressBar = findViewById(R.id.progressBar);
        modelSpinner = findViewById(R.id.modelSpinner);
        downloadButton = findViewById(R.id.downloadButton);
        openSettingsButton = findViewById(R.id.openSettingsButton);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, availableModels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);

        String selectedModel = prefs.getString(KEY_MODEL, DEFAULT_MODEL);
        for (int i = 0; i < availableModels.length; i++) {
            if (availableModels[i].equals(selectedModel)) {
                modelSpinner.setSelection(i);
                break;
            }
        }

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String model = availableModels[position];
                prefs.edit().putString(KEY_MODEL, model).apply();
                updateModelStatus(model);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        downloadButton.setOnClickListener(v -> downloadSelectedModel());
        openSettingsButton.setOnClickListener(v -> {
            boolean launched = false;

            String[] samsungIntents = {
                    // Samsung Settings > General management > Keyboard > Samsung Keyboard > Voice input
                    "com.android.settings/com.android.settings.Settings$KeyboardLayoutPickerActivity",
                    "com.android.settings/com.samsung.android.settings.inputmethod.SamsungKeyboardSettingsActivity",
                    "com.android.settings/com.samsung.android.settings.inputmethod.VoiceInputControlActivity",
                    "com.android.settings/com.samsung.android.settings.language.VoiceInputControlActivity",
                    // Honeyboard / Samsung Keyboard app
                    "com.samsung.android.honeyboard/.settings.HoneyBoardSettingsActivity",
                    "com.samsung.android.honeyboard/.settings.VoiceInputSettingsActivity",
                    "com.samsung.android.honeyboard/.settings.VoiceInputControlActivity",
                    // Samsung OneUI settings
                    "com.samsung.android.settings/.inputmethod.SamsungKeyboardSettingsActivity",
                    "com.samsung.android.settings/.inputmethod.VoiceInputControlActivity",
                    // Fallbacks
                    "com.android.settings/com.android.settings.language.VoiceInputControlActivity",
            };

            for (String className : samsungIntents) {
                if (launched) break;
                try {
                    String[] parts = className.split("/");
                    Intent intent = new Intent();
                    intent.setClassName(parts[0], parts[1]);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        launched = true;
                    }
                } catch (Exception ignored) {}
            }

            if (!launched) {
                try {
                    Intent intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        launched = true;
                    }
                } catch (Exception ignored) {}
            }

            if (!launched) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Manuell einrichten")
                        .setMessage("Gehe zu:\n\nEinstellungen > Allgemeine Verwaltung > Tastatur > Samsung-Tastatur > Spracheingabe\n\nWähle dort \"Lokaler Whisper-Spracherkennungsdienst\" aus.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        checkOrRequestMicPermission();
        updateStatus();
        updateModelStatus(selectedModel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        updateModelStatus(prefs.getString(KEY_MODEL, DEFAULT_MODEL));
    }

    private void checkOrRequestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.microphone_permission_title)
                    .setMessage(R.string.microphone_permission_message)
                    .setPositiveButton(R.string.grant_permission,
                            (d, w) -> ActivityCompat.requestPermissions(
                                    this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    REQUEST_MIC))
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC) {
            updateStatus();
        }
    }

    private void updateStatus() {
        boolean available = SpeechRecognizer.isRecognitionAvailable(this);
        String text = available
                ? getString(R.string.status_ready)
                : "Spracherkennung nicht verfügbar.";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            text += "\n" + getString(R.string.status_listening);
        }
        statusText.setText(text);
    }

    private void updateModelStatus(String modelName) {
        boolean available = ModelManager.isModelAvailable(this, modelName);
        modelStatusText.setText(available
                ? getString(R.string.model_available) + ": " + modelName
                : getString(R.string.model_missing) + ": " + modelName);
    }

    private void downloadSelectedModel() {
        String modelName = availableModels[modelSpinner.getSelectedItemPosition()];
        downloadButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        ModelManager.downloadModel(this, modelName, new ModelManager.DownloadListener() {
            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> progressBar.setProgress(percent));
            }

            @Override
            public void onComplete(boolean success, String message) {
                runOnUiThread(() -> {
                    downloadButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    updateModelStatus(modelName);
                    statusText.setText(success
                            ? getString(R.string.download_complete)
                            : getString(R.string.download_failed) + " " + message);
                });
            }
        });
    }
}
