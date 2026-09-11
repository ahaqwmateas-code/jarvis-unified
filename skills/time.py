from datetime import datetime

SKILL = {
    "name": "time",
    "description": "current date and time",
    "keywords": ["time", "date", "now", "clock", "today"],
}


def run(args, ctx):
    return datetime.now().strftime("%A %d %B %Y, %H:%M:%S")
