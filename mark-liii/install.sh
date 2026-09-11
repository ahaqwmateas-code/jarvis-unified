#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# MARK LIII (JARVIS 53) — one-command Linux installer  (Kali / XoDos-Ark / PC)
# Voice AI assistant powered by the free Gemini Live API (by FatihMakes).
# License: CC BY-NC 4.0 (free for personal, non-commercial use).
# ─────────────────────────────────────────────────────────────────────────────
set -e
cd "$(dirname "$0")"

echo "══ MARK LIII (JARVIS 53) installer ══"
echo "OS detected: $(uname -s)"

# 1) Python check
if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 not found — install it first (e.g. apt-get install -y python3 python3-venv)"
  exit 1
fi
echo "python: $(python3 --version)"

# 2) Virtual env (keeps the phone's system packages clean)
if [ ! -x venv/bin/python ]; then
  echo "▶ creating virtualenv…"
  python3 -m venv venv
fi
PY=venv/bin/python
"$PY" -m pip install --upgrade pip -q

# 3) Core dependencies (voice, Gemini Live, UI, dashboard, search)
echo "▶ installing core dependencies…"
"$PY" -m pip install -q PyQt6 sounddevice numpy google-genai requests \
  beautifulsoup4 duckduckgo-search pillow psutil fastapi "uvicorn[standard]" \
  cryptography python-multipart qrcode pillow || {
  echo "⚠ pip install hit an issue — retrying one package at a time…"
  for pkg in PyQt6 sounddevice numpy google-genai requests beautifulsoup4 duckduckgo-search pillow psutil fastapi "uvicorn[standard]" cryptography python-multipart qrcode; do
    "$PY" -m pip install -q "$pkg" || echo "  (skipped $pkg)"
  done
}

# 4) Optional extras
if [ "$1" = "--full" ]; then
  echo "▶ installing full extras (browser automation, vision, docs)…"
  "$PY" -m pip install -q playwright pyautogui pyperclip opencv-python mss \
    send2trash youtube-transcript-api python-pptx openpyxl || true
  "$PY" -m playwright install chromium 2>/dev/null || echo "  (playwright browser skipped)"
else
  echo "ℹ skipped heavy extras (browser automation, OCR). Run: bash install.sh --full  to add them."
fi

# 5) Gemini API key
CFG=config/api_keys.json
mkdir -p config
if [ -n "$GEMINI_KEY" ]; then
  "$PY" - "$GEMINI_KEY" << 'PY'
import json, sys
key = sys.argv[1]
p = "config/api_keys.json"
try:
    d = json.load(open(p))
except Exception:
    d = {}
d["gemini_api_key"] = key
json.dump(d, open(p, "w"), indent=2)
print("✔ Gemini key saved to", p)
PY
else
  echo ""
  echo "ℹ Get your FREE Gemini API key:  https://aistudio.google.com/apikey"
  echo "  (sign in → Create API key → copy it)"
  echo ""
  printf "Paste your Gemini key now (or press Enter to set it later in the app): "
  read -r KEY
  if [ -n "$KEY" ]; then
    "$PY" - "$KEY" << 'PY'
import json, sys
key = sys.argv[1]
p = "config/api_keys.json"
try:
    d = json.load(open(p))
except Exception:
    d = {}
d["gemini_api_key"] = key
json.dump(d, open(p, "w"), indent=2)
print("✔ Gemini key saved to", p)
PY
  else
    echo "ℹ no key yet — the app will ask for it on first launch."
  fi
fi

# 6) Native tools note (Linux)
echo ""
echo "ℹ Linux extras used by voice commands (install only what you use):"
echo "    volume    → apt-get install -y pulseaudio-utils      (pactl)"
echo "    brightness→ apt-get install -y brightnessctl"
echo "    open URLs → apt-get install -y xdg-utils             (xdg-open)"
echo ""
echo "✅ Setup complete."
echo ""
echo "   START IT:  venv/bin/python main.py"
echo ""
echo "   Requirements to actually talk:"
echo "   • a display (X11/VNC desktop) — the HUD is a PyQt6 window"
echo "   • a microphone + speaker that Linux can see"
echo "   • (optional) 'Hey Jarvis' wake word: enable in ⚙ → WAKE WORD"
