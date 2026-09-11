import json, os

import requests


def detect(k):
    k = k.strip()
    if k.startswith("gsk_"):
        return "groq"
    if k.startswith("sk-ant-"):
        return "anthropic"
    if k.startswith("AIza"):
        return "google"
    if k.startswith("sk-or-"):
        return "openrouter"
    if k.startswith("sk-"):
        return "openai"
    return None


def test(k):
    p = detect(k)
    if not p:
        return None, "unknown-prefix"
    try:
        if p == "groq":
            r = requests.get("https://api.groq.com/openai/v1/models",
                             headers={"Authorization": "Bearer " + k}, timeout=20)
        elif p == "openai":
            r = requests.get("https://api.openai.com/v1/models",
                             headers={"Authorization": "Bearer " + k}, timeout=20)
        elif p == "openrouter":
            # /models is public - must test an authenticated call instead
            r = requests.post("https://openrouter.ai/api/v1/chat/completions",
                              headers={"Authorization": "Bearer " + k},
                              json={"model": "meta-llama/llama-3.1-8b-instruct:free",
                                    "messages": [{"role": "user", "content": "hi"}],
                                    "max_tokens": 1},
                              timeout=25)
        elif p == "anthropic":
            r = requests.get("https://api.anthropic.com/v1/models",
                             headers={"x-api-key": k, "anthropic-version": "2023-06-01"}, timeout=20)
        elif p == "google":
            r = requests.get("https://generativelanguage.googleapis.com/v1beta/models",
                             params={"key": k}, timeout=20)
        return p, r.status_code
    except Exception as e:
        return p, "ERR:" + str(e)[:40]


def main():
    kpath = "/tmp/keys.txt"
    if not os.path.exists(kpath):
        print("No /tmp/keys.txt found.")
        return
    keys = [l.strip() for l in open(kpath) if l.strip() and not l.strip().startswith("#")]
    if not keys:
        print("No keys in /tmp/keys.txt.")
        return

    print("Testing %d key(s)...\n" % len(keys))
    valid = {}
    for k in keys:
        p, status = test(k)
        masked = (k[:10] + "..." + k[-4:]) if len(k) > 16 else "***hidden***"
        if status == 200:
            valid[p] = k
            print("  [OK]  %-24s -> %-11s VALID" % (masked, p))
        else:
            print("  [NO]  %-24s -> %-11s %s" % (masked, p or "?", status))

    cfg_path = os.path.expanduser("~/.jarvis/brain.json")
    cfg = {}
    try:
        cfg = json.load(open(cfg_path))
    except Exception:
        cfg = {}

    if valid:
        cfg["keys"] = valid
        first_p, first_k = list(valid.items())[0]
        cfg["key"] = first_k
        cfg["provider"] = first_p
        cfg["mode"] = "cloud"
        try:
            os.makedirs(os.path.dirname(cfg_path), exist_ok=True)
            json.dump(cfg, open(cfg_path, "w"))
        except Exception as e:
            print("Could not save brain.json:", e)
        print("\nACTIVATED: %s  (%s...)" % (first_p, first_k[:10]))
        if len(valid) > 1:
            print("Other valid keys saved too. Switch anytime: brain key <another key>")
    else:
        print("\nNo valid keys. Double-check they are real and not revoked/expired.")


if __name__ == "__main__":
    main()
