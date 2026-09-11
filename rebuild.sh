#!/bin/bash
# Rebuild ~/.jarvis from source (fresh device or after a wipe).
# Usage: bash rebuild.sh
set -e
S="$(cd "$(dirname "$0")" && pwd)"
J="$HOME/.jarvis"
mkdir -p "$J/skills"

echo "== 1) base core + skills =="
cp "$S/core/core.py" "$J/core.py"
cp "$S/skills/"*.py "$J/skills/"
cp "$S/core/update.sh" "$S/core/watchdog.sh" "$J/" 2>/dev/null || true

echo "== 2) services =="
cp "$S/services/voice.py" "$S/services/gen_cert.sh" "$S/services/start.sh" \
   "$S/services/cli.py" "$S/services/ticker.py" "$J/" 2>/dev/null || true
chmod +x "$J/gen_cert.sh" "$J/start.sh" "$J/update.sh" 2>/dev/null || true
ln -sf "$J/cli.py" /usr/local/bin/jarvis 2>/dev/null || true

echo "== 3) patches (v9 → v22) =="
for p in jarvis_v9_brain_patch.py \
         jarvis_v13_experiential_patch.py \
         jarvis_v13_1_base_hotfix.py \
         jarvis_v14_failover_patch.py \
         jarvis_v14_1_custom_patch.py \
         jarvis_v15_lang_patch.py \
         jarvis_v17_media_patch.py \
         jarvis_v19_prompt_patch.py \
         jarvis_v20_video_patch.py \
         jarvis_v21_imagefix_patch.py \
         jarvis_v22_pwa_patch.py \
         jarvis_v23_anonymous_patch.py; do
  echo "   applying $p"
  python3 "$S/patches/$p"
done

echo "== 4) cert (for voice) =="
bash "$J/gen_cert.sh" 2>/dev/null || echo "   (cert step skipped)"

echo "== 5) auto-start on login =="
if ! grep -q 'start.sh' ~/.bashrc 2>/dev/null; then
  printf '\n# JARVIS auto-start\nif [ -f "$HOME/.jarvis/start.sh" ]; then\n  ( bash "$HOME/.jarvis/start.sh" >/dev/null 2>&1 & )\nfi\n' >> ~/.bashrc
fi

echo "== done =="
bash "$J/start.sh"
