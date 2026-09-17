# Android-Spracherkennungsdienst

Minimaler alternativer Android-Spracherkennungsdienst.

Er registriert sich als `android.speech.RecognitionService` und taucht damit in den System-Spracheingabe-Einstellungen als wählbarer Dienst auf.

## Funktion

- Bietet einen Spracherkennungsdienst, den die Samsung-Tastatur, Gboard etc. nutzen können.
- Nutzt intern die Android-Standard-On-Device-Spracherkennung, damit sofort etwas funktioniert.
- Kann später durch Whisper, Google Cloud Speech-to-Text oder ein selbst gehostetes STT-Modell ersetzt werden.

## Bauen

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Verwendung

1. App installieren.
2. Mikrofon-Berechtigung erlauben.
3. In den Android-Spracheingabe-Einstellungen `Lokaler Spracherkennungsdienst` auswählen.
4. In der Samsung-Tastatur auf das Mikrofon tippen.
