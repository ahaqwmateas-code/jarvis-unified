#!/bin/bash
# OMNI-JARVIS service manager
APP_DIR=~/omni-jarvis
cd "$APP_DIR" || exit 1
source venv/bin/activate

start() {
  if pgrep -f "python main.py" > /dev/null; then
    echo "OMNI-JARVIS is already running."
  else
    nohup python main.py > /tmp/jarvis.log 2>&1 &
    sleep 1
    echo "Started OMNI-JARVIS (PID $!)"
    curl -s http://127.0.0.1:8000/health && echo
  fi
}

stop() {
  pkill -f "python main.py" && echo "Stopped." || echo "Not running."
}

status() {
  if pgrep -f "python main.py" > /dev/null; then
    echo "RUNNING"
    curl -s http://127.0.0.1:8000/health && echo
  else
    echo "STOPPED"
  fi
}

logs() {
  tail -n 30 /tmp/jarvis.log
}

case "$1" in
  start)   start ;;
  stop)    stop ;;
  restart) stop; sleep 1; start ;;
  status)  status ;;
  logs)    logs ;;
  *) echo "Usage: bash jarvis.sh {start|stop|restart|status|logs}" ;;
esac
