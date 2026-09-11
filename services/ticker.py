import os, json, time, subprocess, shutil
from datetime import datetime

FILE = os.path.expanduser("~/.jarvis/reminders.json")


def speak(t):
    es = shutil.which("espeak-ng") or shutil.which("espeak")
    if es:
        try:
            subprocess.Popen([es, "-s", "150", t],
                             stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception:
            pass
    print(time.strftime("%H:%M:%S"), "REMINDER:", t, flush=True)


while True:
    try:
        if os.path.exists(FILE):
            data = json.load(open(FILE))
            changed = False
            for d in data:
                if not d.get("done"):
                    try:
                        due = datetime.fromisoformat(d["due"])
                    except Exception:
                        due = None
                    if due and due <= datetime.now():
                        speak("Reminder: " + d["text"])
                        d["done"] = True
                        changed = True
            if changed:
                json.dump(data, open(FILE, "w"))
    except Exception as e:
        print("ticker error:", e, flush=True)
    time.sleep(20)
