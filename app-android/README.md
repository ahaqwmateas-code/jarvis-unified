# JARVIS — native Android app (complete source)

JARVIS as a real Android app, running **entirely on the phone** — no server
required. Every feature is its **own page**: open the app to a home grid and
tap any of the 21 pages.

## The pages

Chat · Time · Calculator · Password · Weather · Search · Wikipedia ·
Translate · Notes · Reminders · Personas (16) · Language (30) · Image ·
Build (AI writes apps) · System · Network · Brain · Video (core) · CLI Hub
(core) · Core (full JARVIS) · Settings (API keys).

- **Chat** — AI brain with auto-failover (Groq → Gemini → OpenRouter →
  Cerebras → Mistral → xAI → DeepSeek → GitHub Models → Custom → Ollama →
  free anonymous brain) + voice input (MIC).
- **Build** — describe an app, the AI writes the full code and saves it to
  the phone's Documents/jarvis folder.
- **Image** — free generation (Gemini if key set, else Pollinations), shown
  inline and saved to Pictures/jarvis.
- **Reminders** — system notifications via AlarmManager.
- **Video / CLI Hub** — these need the full core (ffmpeg + shell on the Kali
  phone); the pages open the core with one tap.
- **Settings** — add free keys, a custom OpenAI-compatible provider, local
  Ollama, and the core URL. No key? Everything still works: skills run
  locally and the brain falls back to the free anonymous model.

## Project layout

```
app/src/main/
├── AndroidManifest.xml
├── java/com/jarvis/assistant/
│   ├── MainActivity.java       home grid (21 pages)
│   ├── ChatActivity.java       AI chat + voice
│   ├── BasePage.java           themed page scaffold
│   ├── Brain.java              multi-provider AI + failover
│   ├── Skills.java             all skills (service layer)
│   ├── Calc.java · Personas.java · NotesDb.java
│   ├── Net.java · ChatAdapter.java · ChatMessage.java
│   ├── ReminderReceiver.java   notification alarms
│   ├── SettingsActivity.java · ServerActivity.java
│   └── (one Activity per feature page)
└── res/                        theme, strings, launcher icons
```

## Build

- **Android Studio:** open the folder, sync (AGP 7.4.2, Gradle 7.6+, JDK 11+),
  press Run.
- **Command line (what produced JARVIS.apk):** `bash build.sh` — needs JDK 11+
  and Android build-tools 34 + platform-34. Zero third-party libraries.
- **CI:** `.github/workflows/build-apk.yml` builds the APK on every push.
