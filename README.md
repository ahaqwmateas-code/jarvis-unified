# JARVIS — your personal AI, on your phone

A self-hosted, **free** personal AI assistant that lives on an Android phone
(with Kali Linux / XoDos-Ark) — and now also as a **native Android app** that
runs on *any* phone with no server at all.

Chat, voice, image & video generation, app-building, 18 skills, 2,165 personas
— all through free AI APIs with automatic failover, falling back to a local
brain so it is never silent.

---

## Two ways to run JARVIS

### 1. 📱 Native Android app — `app-android/` (works on ANY phone)
A complete Java app, zero third-party libraries, no server needed. Every
feature is its **own page** — the app opens to a home grid of 21 pages:

Chat · Time · Calculator · Password · Weather · Search · Wikipedia ·
Translate · Notes · Reminders · Personas (16) · Language (30) · Image ·
Build (AI writes apps) · System · Network · Brain · Video (core) · CLI Hub
(core) · Composio (1,500+ app tools) · Core (full JARVIS) · Settings.

- **Chat** — multi-provider brain with auto-failover (Groq → Gemini →
  OpenRouter → Cerebras → Mistral → xAI → DeepSeek → GitHub Models → Custom
  → Ollama → free anonymous brain) + voice input.
- **Build** — the AI writes a whole app and saves the code to the phone.
- **Image** — free generation (Gemini/Pollinations), shown inline + saved.
- **Reminders** — system notifications via AlarmManager.
- **Video / CLI Hub** — need the full core (ffmpeg + shell); one-tap open.

Download the ready-to-install APK: [`JARVIS.apk`](JARVIS.apk) (dev-signed).
Rebuild it with `bash app-android/build.sh`, or open `app-android/` in Android
Studio. GitHub Actions also builds it on every push (see `.github/workflows`).

### 2. 🐍 Full JARVIS core — Python (the XoDos-Ark / Kali phone)
The complete assistant service (FastAPI on `:8000`, voice on `:8443`), 18
skills, auto-start on login, installable web app (PWA).

- `core/` — the main service (`core.py`, `update.sh`, `watchdog.sh`)
- `skills/` — all 18 skills
- `services/` — voice, CLI (`jarvis`), cert, start script
- `patches/` — the upgrade patches (v6 → v22), applied in order
- `installers/` — one-command installers (v5 → v22)
- `omni-jarvis/` — the original OMNI-JARVIS service
- `app-pwa/` — installable web app (manifest + service worker + icons)

Fresh device? One command: `bash rebuild.sh` (then `bash ~/.jarvis/start.sh`).

---

## Skill list

| Skill | What it does |
|---|---|
| brain | multi-provider AI with auto-failover |
| build / app | AI writes a whole working app |
| calc | safe math |
| clihub | CLI-Anything hub (40+ agent CLIs) |
| image | generate images (Gemini / Pollinations) |
| lang | 30 languages |
| network | IP, ping, DNS |
| notes | save / read notes |
| password | strong passwords |
| prompt | 2,165 personas (prompts.chat) |
| remind | reminders |
| search | web search |
| system | CPU / RAM / disk |
| time | date & time |
| translate | translate text |
| video | real MP4 generation (frames + Ken Burns) |
| weather | forecast |
| wiki | Wikipedia summary |

## The free brain stack

| Provider | Free tier | Add with |
|---|---|---|
| Groq | ~14k req/day | `brain key gsk_…` |
| Google Gemini | ~1.5k req/day | `brain key AIza…` |
| OpenRouter | 25+ free models | `brain key sk-or-…` |
| Cerebras / Mistral / GitHub | free tiers | `brain custom …` |
| Ollama (local) | free, offline | automatic |

JARVIS tries the best brain first and auto-switches on rate limits or errors.

## Talk to JARVIS — 3 ways

1. Terminal: `jarvis <words>`
2. Web: `http://<phone-ip>:8000`
3. Voice: `https://<phone-ip>:8443`

## Honest notes

- GPT-6 Astra, MiniMax M3, and true text-to-video (Veo/Runway/Kling) have
  **no free API** — they're wired in for when a paid key is available.
- Free video uses AI frames + Ken Burns motion (real MP4, not true motion video).

## License

MIT — see [LICENSE](LICENSE).


## Composio integration (MCP)

JARVIS can act as an MCP client against Composio's hosted endpoint
`https://connect.composio.dev/mcp`, giving it tools across 1,500+ apps
(Gmail, GitHub, Google Calendar, Notion, Slack...).

- **App:** the Composio page (add your `ck_...` API key in Settings).
- **Core:** the `composio` skill — `composio key <ck_...>`, `composio status`,
  `composio tools`, `composio run <TOOL> <json>`.
- Get a key and connect apps at dashboard.composio.dev.
