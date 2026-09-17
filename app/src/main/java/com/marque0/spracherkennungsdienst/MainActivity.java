package com.marque0.spracherkennungsdienst;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MIC = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusText = findViewById(R.id.statusText);
        Button openSettingsButton = findViewById(R.id.openSettingsButton);

        checkOrRequestMicPermission();
        updateStatus(statusText);

        openSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });
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
            updateStatus(findViewById(R.id.statusText));
        }
    }

    private void updateStatus(TextView statusText) {
        boolean available = SpeechRecognizer.isRecognitionAvailable(this);
        boolean listening = false;

        Context context = getApplicationContext();
        if (available) {
            SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            listening = recognizer != null;
            if (recognizer != null) {
                recognizer.destroy();
            }
        }

        String text = available
                ? getString(R.string.status_ready)
                : "Spracherkennung nicht verfügbar.";
        if (listening) {
            text += "\n" + getString(R.string.status_listening);
        }
        statusText.setText(text);
    }
}
