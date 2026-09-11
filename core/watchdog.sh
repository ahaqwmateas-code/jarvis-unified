#!/bin/bash
# JARVIS Core watchdog: keeps Ollama + JARVIS alive, auto-restarts on crash
J=~/.jarvis
PY=$(command -v python3)
[ -x ~/omni-jarvis/venv/bin/python ] && PY=~/omni-jarvis/venv/bin/python

while true; do
  # keep Ollama alive
  pgrep -x ollama >/dev/null 2>&1 || nohup ollama serve > /tmp/ollama.log 2>&1 &

  # keep JARVIS alive
  if ! curl -s --max-time 3 http://127.0.0.1:8000/health >/dev/null 2>&1; then
    echo "$(date '+%F %T') JARVIS down - restarting" >> "$J/watchdog.log"
    pkill -f "$J/core.py" 2>/dev/null
    cd "$J" && nohup "$PY" core.py > /tmp/jarvis.log 2>&1 &
  fi

  sleep 30
done
