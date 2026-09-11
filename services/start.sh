#!/bin/bash
# JARVIS MASTER BOOT - starts everything, safe to re-run any time
J="$HOME/.jarvis"
PY=$(command -v python3)
[ -x "$HOME/omni-jarvis/venv/bin/python" ] && PY="$HOME/omni-jarvis/venv/bin/python"

up() { timeout 2 bash -c "exec 3<>/dev/tcp/127.0.0.1/$1" 2>/dev/null; }

# 1) Ollama
if command -v ollama >/dev/null 2>&1 && ! curl -s --max-time 2 http://127.0.0.1:11434/api/version >/dev/null 2>&1; then
  (nohup ollama serve >/tmp/ollama.log 2>&1 &)
fi

# 2) Core (8000)
if ! up 8000; then
  (nohup "$PY" "$J/core.py" >/tmp/jarvis.log 2>&1 &)
fi

# 3) Voice (8443)
if ! up 8443; then
  if [ -f "$J/certs/cert.pem" ] && [ -f "$J/voice.py" ]; then
    (nohup "$PY" "$J/voice.py" >/tmp/voice.log 2>&1 &)
  fi
fi

# 4) Ticker
if [ -f "$J/ticker.py" ] && ! pgrep -f "$J/ticker.py" >/dev/null 2>&1; then
  (nohup "$PY" "$J/ticker.py" >/tmp/ticker.log 2>&1 &)
fi

# 5) Watchdog
if [ -f "$J/watchdog.sh" ] && ! pgrep -f "watchdog.sh" >/dev/null 2>&1; then
  (nohup bash "$J/watchdog.sh" >/tmp/watchdog.log 2>&1 &)
fi

sleep 3
echo "=== JARVIS BOOT STATUS ==="
curl -s --max-time 3 http://127.0.0.1:8000/health >/dev/null 2>&1 && echo "CORE   (8000)  : UP" || echo "CORE   (8000)  : DOWN - see /tmp/jarvis.log"
curl -sk --max-time 3 https://127.0.0.1:8443/health >/dev/null 2>&1 && echo "VOICE  (8443)  : UP" || echo "VOICE  (8443)  : DOWN - run: bash ~/.jarvis/gen_cert.sh"
curl -s --max-time 3 http://127.0.0.1:11434/api/version >/dev/null 2>&1 && echo "OLLAMA (11434) : UP" || echo "OLLAMA (11434) : not running"
pgrep -f ticker.py >/dev/null 2>&1 && echo "TICKER         : UP" || echo "TICKER         : n/a (no ticker.py)"
pgrep -f watchdog.sh >/dev/null 2>&1 && echo "WATCHDOG       : UP" || echo "WATCHDOG       : n/a"
IP=$(ip addr show 2>/dev/null | grep -w inet | grep -v 127.0.0.1 | awk '{print $2}' | cut -d/ -f1 | head -1)
[ -z "$IP" ] && IP="192.168.1.109"
echo "-----------------------------------"
echo "Web UI : http://$IP:8000"
echo "Voice  : https://$IP:8443"
