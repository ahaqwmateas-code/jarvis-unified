import os, re, subprocess, time, py_compile

J = os.path.expanduser("~/.jarvis")
CORE = os.path.join(J, "core.py")

# Choose the python that has fastapi (the venv one if present)
PY = "python3"
vp = os.path.expanduser("~/omni-jarvis/venv/bin/python")
if os.path.exists(vp):
    PY = vp

src = open(CORE).read()

# ---- 1) insert llm_route before def load_skills (only if not already patched)
llm_route = r'''def llm_route(cmd, skills):
    import json as _json
    try:
        import requests
        listing = "\n".join(n + ": " + m.get("description", "") for n, (m, _f) in skills.items())
        prompt = (
            "You are a router for a personal assistant. Decide whether the user's "
            "request should be handled by one of the skills below, or answered directly.\n\n"
            "SKILLS:\n" + listing + "\n\n"
            "Respond with JSON only, exactly one of these two shapes:\n"
            "{\"skill\": \"<skill name>\", \"args\": \"<argument string>\"}\n"
            "{\"answer\": \"<your direct answer>\"}\n\n"
            "User request: " + cmd
        )
        r = requests.post(OLLAMA_URL + "/api/generate",
                          json={"model": OLLAMA_MODEL, "prompt": prompt, "stream": False, "format": "json"},
                          timeout=60)
        if r.status_code != 200:
            return None
        data = _json.loads(r.json().get("response", "{}").strip())
    except Exception:
        return None
    if isinstance(data, dict) and data.get("skill"):
        name = data["skill"]
        if name in skills:
            meta, fn = skills[name]
            if fn:
                try:
                    return fn(data.get("args", ""), {"app_dir": APP_DIR, "notes_dir": NOTES_DIR, "llm": ask_llm})
                except Exception as e:
                    return "Skill '" + name + "' error: " + str(e)
    if isinstance(data, dict) and data.get("answer"):
        return data["answer"]
    return None
'''

if "def llm_route(" not in src:
    anchor1 = "\n\ndef load_skills():\n"
    assert anchor1 in src, "core.py not recognized (anchor1 missing) - is it JARVIS Core v5?"
    src = src.replace(anchor1, "\n\n" + llm_route + anchor1, 1)

    # ---- 2) pass llm() into skill context
    anchor2 = 'return fn(arg, {"app_dir": APP_DIR, "notes_dir": NOTES_DIR})'
    assert anchor2 in src, "anchor2 missing"
    src = src.replace(anchor2, 'return fn(arg, {"app_dir": APP_DIR, "notes_dir": NOTES_DIR, "llm": ask_llm})')

    # ---- 3) route through the LLM before giving a plain answer
    anchor3 = "    # LLM fallback\n    ans = ask_llm(t)"
    assert anchor3 in src, "anchor3 missing"
    src = src.replace(anchor3, "    # LLM routing fallback\n    ans = llm_route(t, skills)\n    if ans is not None:\n        return ans\n    ans = ask_llm(t)")
    open(CORE, "w").write(src)
    print("core.py patched with LLM router")
else:
    print("core.py already patched - skipping core patch")

# ---- write 3 new skills
skills_dir = os.path.join(J, "skills")
os.makedirs(skills_dir, exist_ok=True)

news = r'''import requests

SKILL = {
    "name": "news",
    "description": "top tech headlines",
    "keywords": ["news", "headlines", "hacker news"],
}


def run(args, ctx):
    try:
        ids = requests.get("https://hacker-news.firebaseio.com/v0/topstories.json", timeout=10).json()[:5]
        out = []
        for i in ids:
            item = requests.get("https://hacker-news.firebaseio.com/v0/item/" + str(i) + ".json", timeout=10).json()
            out.append("- " + item.get("title", "?"))
        return "\n".join(out)
    except Exception as e:
        return "news unavailable: " + str(e)
'''

crypto = r'''import requests

SKILL = {
    "name": "crypto",
    "description": "crypto prices: crypto bitcoin,ethereum",
    "keywords": ["crypto", "bitcoin", "btc", "ethereum", "eth", "price"],
}


def run(args, ctx):
    coins = args.strip().replace(" ", "") or "bitcoin"
    try:
        r = requests.get("https://api.coingecko.com/api/v3/simple/price",
                         params={"ids": coins, "vs_currencies": "usd"}, timeout=10)
        d = r.json()
        if not d:
            return "unknown coin(s). Try: crypto bitcoin,ethereum"
        return "\n".join(k + ": $" + format(v.get("usd", 0), ",") for k, v in d.items())
    except Exception as e:
        return "crypto unavailable: " + str(e)
'''

make = r'''import os, re

SKILL = {
    "name": "make",
    "description": "make <what to build> - AI writes a new skill",
    "keywords": ["make", "create", "build", "new skill"],
}


def run(args, ctx):
    desc = args.strip()
    if not desc:
        return "Usage: make <what should the new skill do?>"
    llm = ctx.get("llm")
    if not llm:
        return "LLM not available - start Ollama first."
    prompt = (
        "Write a small Python skill module for a personal assistant. "
        "It must define SKILL (a dict with keys name, description, keywords "
        "as a list of trigger words) and a function run(args, ctx) returning a "
        "string. Use only the Python standard library plus 'requests'. "
        "Output raw Python code only, no markdown, no backticks.\n"
        "Skill to create: " + desc
    )
    code = llm(prompt)
    if not code:
        return "LLM offline - could not generate."
    code = code.strip()
    code = re.sub(r"^```[a-zA-Z]*\n?", "", code)
    code = re.sub(r"\n?```$", "", code).strip()
    name = re.sub(r"[^a-z0-9_]", "", (desc.lower().split() or ["custom"])[0])[:20] or "custom"
    skills_dir = os.path.join(ctx["app_dir"], "skills")
    path = os.path.join(skills_dir, name + ".py")
    if os.path.exists(path):
        name = name + "2"
        path = os.path.join(skills_dir, name + ".py")
    try:
        compile(code, name + ".py", "exec")
    except Exception as e:
        return "Generated code failed to compile, NOT installed: " + str(e)
    with open(path, "w") as f:
        f.write(code)
    return "Skill '" + name + "' created. Try typing: " + name
'''

open(os.path.join(skills_dir, "news.py"), "w").write(news)
open(os.path.join(skills_dir, "crypto.py"), "w").write(crypto)
open(os.path.join(skills_dir, "make.py"), "w").write(make)
print("skills written: news, crypto, make")

# ---- compile check
py_compile.compile(CORE, doraise=True)
print("CORE COMPILES OK (v6)")

# ---- restart JARVIS
subprocess.run(["pkill", "-f", os.path.join(J, "core.py")])
time.sleep(1)
log = open("/tmp/jarvis.log", "w")
subprocess.Popen([PY, CORE], stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
time.sleep(2)

# ---- ensure ollama is up
import shutil as _sh
if subprocess.run(["pgrep", "-x", "ollama"]).returncode != 0:
    oll = _sh.which("ollama")
    if oll:
        olog = open("/tmp/ollama.log", "w")
        subprocess.Popen([oll, "serve"], stdout=olog, stderr=subprocess.STDOUT, start_new_session=True)
    else:
        print("note: ollama binary not found on PATH - start it manually")

# ---- health + skill list check
import urllib.request
try:
    h = urllib.request.urlopen("http://127.0.0.1:8000/health", timeout=5).read().decode()
    print("HEALTH:", h)
    s = urllib.request.urlopen("http://127.0.0.1:8000/api/skills", timeout=5).read().decode()
    print("SKILLS:", s)
except Exception as e:
    print("CHECK FAILED:", e, "- see /tmp/jarvis.log")
