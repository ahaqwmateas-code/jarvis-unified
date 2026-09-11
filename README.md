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
A complete Java app, zero third-party libraries, no server needed:

- **Chat** with the multi-provider AI brain (Groq → Gemini → OpenRouter →
  Cerebras → Mistral → Ollama → free anonymous brain), with auto-failover.
- **Voice input** (Android speech recognition).
- **On-device skills:** `calc`, `password`, `note add/list/del`,
  `remind me in 10 minutes to …` (system notifications), `weather <city>`,
  `search`, `wiki`, `translate`, `persona`, `image <desc>`.
- **Personas** — 16 bundled, curated from prompts.chat.
- **CORE button** — opens the full JARVIS core (below) for the heavy skills.

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
