#!/usr/bin/env python3
import sys, json, os, urllib.request

BASE = os.environ.get("JARVIS_URL", "http://127.0.0.1:8000")


def call(cmd):
    req = urllib.request.Request(
        BASE + "/api/command",
        data=json.dumps({"command": cmd}).encode(),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            d = json.loads(r.read().decode())
        return d.get("reply", json.dumps(d))
    except Exception as e:
        return "JARVIS not reachable at %s - is it running? (%s)" % (BASE, e)


def main():
    args = sys.argv[1:]
    if args:
        print(call(" ".join(args)))
        return
    # interactive REPL
    print("JARVIS CLI. Type commands like 'brain', 'lang set fr', 'ask what is 2+2'.")
    print("'exit' or Ctrl+C to quit.")
    while True:
        try:
            line = input("jarvis> ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            return
        if not line:
            continue
        if line.lower() in ("exit", "quit"):
            return
        print(call(line))


if __name__ == "__main__":
    main()