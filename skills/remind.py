import os, json, re
from datetime import datetime, timedelta

SKILL = {
    "name": "remind",
    "description": "remind me in 5 minutes to X / at 15:30 to X",
    "keywords": ["remind", "reminder", "alarm", "remind me"],
}

FILE = os.path.expanduser("~/.jarvis/reminders.json")


def parse(a):
    now = datetime.now()
    due, text = None, a
    m = re.search(r"(?i)\bin\s+(\d+)\s*(seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|h)\b", a)
    if m:
        n = int(m.group(1))
        unit = m.group(2).lower()[0]
        mult = {"s": 1, "m": 60, "h": 3600}[unit]
        due = now + timedelta(seconds=n * mult)
        text = re.sub(m.group(0), "", a, count=1)
    m2 = re.search(r"(?i)\bat\s+(\d{1,2}):(\d{2})\b", a)
    if m2 and due is None:
        hh, mm = int(m2.group(1)), int(m2.group(2))
        due = now.replace(hour=hh, minute=mm, second=0, microsecond=0)
        if due <= now:
            due += timedelta(days=1)
        text = re.sub(m2.group(0), "", a, count=1)
    if "tomorrow" in a.lower() and due is None:
        due = now + timedelta(days=1)
        text = a.lower().replace("tomorrow", "")
    text = re.sub(r"(?i)^(remind\s+me|remind|me)\b", "", text)
    text = text.strip(" ,.:")
    if text.lower().startswith("to "):
        text = text[3:].strip()
    return due, text


def run(args, ctx):
    due, text = parse(args)
    if not due or not text:
        return "Usage: remind me in 5 minutes to drink water   |   remind me at 15:30 to call mom"
    try:
        data = json.load(open(FILE)) if os.path.exists(FILE) else []
    except Exception:
        data = []
    data.append({"due": due.isoformat(), "text": text, "done": False})
    json.dump(data, open(FILE, "w"))
    return "Reminder set for " + due.strftime("%H:%M") + ": " + text
