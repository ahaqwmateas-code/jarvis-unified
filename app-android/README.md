# JARVIS — native Android app (complete source)

JARVIS as a real Android app. It runs **entirely on the phone** — no server, no
Kali, no Python required. The chat UI, voice input, multi-provider AI brain and
the skills are all native code in this project.

## What it does

- **Chat with the AI brain** — talks directly to free AI APIs with automatic
  failover: Groq → Gemini → OpenRouter → Cerebras → Mistral → local Ollama →
  anonymous free brain (Pollinations). Add your free keys in ⚙ Settings.
- **Voice input** — tap 🎤 and speak (Android SpeechRecognizer).
- **Built-in skills** (work even with no API key):
  `time`, `calc 2+2*10`, `password 16`, `note add/list/del`,
  `remind me in 10 minutes to …`, `weather <city>`, `search <q>`, `wiki <topic>`,
  `translate <text> to <lang>`, `persona <name>`, `image <description>`.
- **Persona library** — 16 bundled personas (curated subset of prompts.chat).
- **Image generation** — Gemini (if key set) or free Pollinations, shown inline.
- **Reminders** — system notifications via AlarmManager.
- **CORE button** — opens the full JARVIS core (your Kali phone's web UI) for
  the heavy server-side skills (app-building, video, voice web UI).

## Project layout

```
jarvis-android/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/jarvis/assistant/
│   │   ├── MainActivity.java      chat screen + voice
│   │   ├── ChatAdapter.java       chat bubbles
│   │   ├── ChatMessage.java       message model
│   │   ├── Brain.java             multi-provider AI + failover
│   │   ├── Skills.java            all local skills
│   │   ├── Calc.java              safe expression evaluator
│   │   ├── Personas.java          bundled personas
│   │   ├── NotesDb.java           SQLite notes/reminders
│   │   ├── ReminderReceiver.java  notification alarms
│   │   ├── SettingsActivity.java  keys + server URL
│   │   ├── ServerActivity.java    JARVIS core web view
│   │   └── Net.java               HTTP + JSON helper
│   └── res/                       theme, strings, launcher icons
├── build.gradle · settings.gradle · gradle.properties   (Android Studio)
├── build.sh                        command-line build (no Gradle)
├── jarvis.keystore                 signing key (password: jarvis123)
└── JARVIS.apk                      the built, signed app
```

## Build it yourself

**Option A — Android Studio:** open this folder, let Gradle sync (AGP 7.4.2,
Gradle 7.6+, JDK 11+), press Run. If Android Studio's AGP complains about the
`package` attribute in AndroidManifest.xml, delete that one attribute (the
`namespace` in app/build.gradle covers it).

**Option B — command line (what produced JARVIS.apk):**

```bash
bash build.sh
```

Requirements: JDK 11+, Android SDK build-tools 34 (aapt2/d8/apksigner/zipalign)
and platform-34 (android.jar). No third-party libraries — the app uses only the
Android framework (HttpURLConnection, org.json, SpeechRecognizer, SQLite).

## Free API keys (optional, in ⚙ Settings)

| Provider | Get a key at | Model used |
|---|---|---|
| Groq | groq.com | llama-3.3-70b-versatile |
| Google Gemini | aistudio.google.com | gemini-2.0-flash |
| OpenRouter | openrouter.ai | deepseek-chat-v3 (free) |
| Cerebras | cloud.cerebras.ai | llama3.1-8b |
| Mistral | console.mistral.ai | open-mistral-nemo |
| Ollama (local) | ollama.com | llama3.1 |

No key? The app still answers via the free anonymous brain, and every skill
above still works.
