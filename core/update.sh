#!/bin/bash
# JARVIS Core self-update: pull new core/skills from JARVIS_UPDATE_URL (a raw
# file URL), upgrade pip deps, and restart. Backs up the old core first.
J=~/.jarvis
PY=$(command -v python3)
[ -x ~/omni-jarvis/venv/bin/python ] && PY=~/omni-jarvis/venv/bin/python

if [ -n "$JARVIS_UPDATE_URL" ]; then
  cp "$J/core.py" "$J/core.py.bak" 2>/dev/null
  if curl -fsSL "$JARVIS_UPDATE_URL/core.py" -o "$J/core.py.new"; then
    mv "$J/core.py.new" "$J/core.py"
    echo "core.py updated from $JARVIS_UPDATE_URL"
  else
    echo "update fetch failed - keeping current core.py"
  fi
else
  echo "JARVIS_UPDATE_URL not set - nothing to pull."
fi

# upgrade deps (quiet)
"$PY" -m pip install -q -U requests fastapi uvicorn pydantic 2>/dev/null || true

# restart JARVIS
pkill -f "$J/core.py" 2>/dev/null
sleep 1
cd "$J" && nohup "$PY" core.py > /tmp/jarvis.log 2>&1 &
echo "JARVIS restarted."
