import os
from datetime import datetime

SKILL = {
    "name": "notes",
    "description": "save and read notes (note add <text> / notes)",
    "keywords": ["note", "notes", "remember", "memo"],
}


def run(args, ctx):
    notes_dir = ctx.get("notes_dir", os.path.expanduser("~/.jarvis/notes"))
    os.makedirs(notes_dir, exist_ok=True)
    fpath = os.path.join(notes_dir, "notes.txt")
    a = args.strip()
    if a.startswith("add "):
        text = a[4:].strip()
        if not text:
            return "Usage: note add <text>"
        with open(fpath, "a") as f:
            f.write(datetime.now().strftime("%Y-%m-%d %H:%M") + " | " + text + "\n")
        return "Saved note: " + text
    if not os.path.exists(fpath):
        return "No notes yet. Try: note add buy milk"
    lines = open(fpath).read().strip().splitlines()
    return "\n".join(lines[-20:]) or "No notes yet."
