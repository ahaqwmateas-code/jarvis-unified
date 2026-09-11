import os, shutil, py_compile

J = os.path.expanduser("~/.jarvis")
CORE = os.path.join(J, "core.py")
BAK = CORE + ".bak3"

CORE_BLOCK = r'''import time

PROVIDER_RANK = {
    "groq": 1,
    "openrouter": 2,
    "experiential": 3,
    "google": 4,
    "openai": 5,
    "anthropic": 6,
}

PROVIDER_BASE = {
    "groq": "https://api.groq.com/openai/v1",
    "openai": "https://api.openai.com/v1",
    "openrouter": "https://openrouter.ai/api/v1",
    "experiential": "https://api.experientiallabs.ai/v1",
}

PROVIDER_MODEL = {
    "groq": "llama-3.3-70b-versatile",
    "openai": "gpt-4o-mini",
    "openrouter": "meta-llama/llama-3.1-8b-instruct:free",
    "experiential": "gpt-4o-mini",
    "anthropic": "claude-3-5-haiku-latest",
    "google": "gemini-2.0-flash",
}


def _roster(cfg):
    out = []
    for e in cfg.get("roster", []) or []:
        if isinstance(e, dict) and e.get("key"):
            out.append(dict(e))
    if not out:
        k = os.environ.get("BRAIN_API_KEY", "") or cfg.get("key", "")
        if k:
            prov = cfg.get("provider") or _detect_provider(k) or "custom"
            out.append({
                "provider": prov,
                "key": k,
                "model": cfg.get("model"),
                "base": cfg.get("base"),
                "priority": PROVIDER_RANK.get(prov, 7),
            })
    out.sort(key=lambda e: e.get("priority", 99))
    return out


def _cool(cfg, provider):
    cd = cfg.get("cooldown") or {}
    return provider in cd and time.time() < cd[provider]


def _cooldown(cfg, provider, secs=90):
    cd = cfg.setdefault("cooldown", {})
    cd[provider] = time.time() + secs
    try:
        json.dump(cfg, open(os.path.join(APP_DIR, "brain.json"), "w"))
    except Exception:
        pass


def _uncool(cfg, provider):
    cd = cfg.setdefault("cooldown", {})
    if cd.pop(provider, None) is not None:
        try:
            json.dump(cfg, open(os.path.join(APP_DIR, "brain.json"), "w"))
        except Exception:
            pass


def _ask_one(entry, prompt):
    provider = entry.get("provider")
    key = entry.get("key", "")
    model = entry.get("model") or PROVIDER_MODEL.get(provider, "gpt-4o-mini")
    base = entry.get("base") or PROVIDER_BASE.get(provider)
    try:
        import requests
        ctx = "\n".join(f"{r}: {t}" for r, t in HISTORY)
    except Exception:
        ctx = ""
    if provider == "anthropic":
        if not key:
            return None
        msgs = []
        if ctx:
            msgs.append({"role": "user", "content": "Recent conversation:\n" + ctx[-2000:]})
        msgs.append({"role": "user", "content": prompt})
        r = requests.post("https://api.anthropic.com/v1/messages",
                          headers={"x-api-key": key, "anthropic-version": "2023-06-01",
                                   "Content-Type": "application/json"},
                          json={"model": model, "max_tokens": 1024, "system": JARVIS_SYS, "messages": msgs},
                          timeout=60)
        if r.status_code == 200:
            return r.json().get("content", [{}])[0].get("text", "").strip()
        return None
    if provider == "google":
        r = requests.post(
            "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + key,
            json={"contents": [{"parts": [{"text": prompt}]}]}, timeout=60)
        if r.status_code == 200:
            return r.json()["candidates"][0]["content"]["parts"][0]["text"].strip()
        return None
    if not base:
        return None
    headers = {"Authorization": "Bearer " + key, "Content-Type": "application/json"}
    if provider == "openrouter":
        headers["HTTP-Referer"] = "http://localhost"
        headers["X-Title"] = "Jarvis"
    msgs = [{"role": "system", "content": JARVIS_SYS}]
    if ctx:
        msgs.append({"role": "user", "content": "Recent conversation:\n" + ctx[-2000:]})
    msgs.append({"role": "user", "content": prompt})
    r = requests.post(base.rstrip("/") + "/chat/completions", headers=headers,
                      json={"model": model, "messages": msgs, "max_tokens": 1024, "temperature": 0.6},
                      timeout=60)
    if r.status_code == 200:
        return r.json()["choices"][0]["message"]["content"].strip()
    return None


def ask_cloud_smart(prompt):
    cfg = _brain_cfg()
    if cfg.get("mode") == "local":
        return None
    for e in _roster(cfg):
        p = e.get("provider") or "custom"
        if _cool(cfg, p):
            continue
        try:
            ans = _ask_one(e, prompt)
        except Exception:
            ans = None
        if ans:
            _uncool(cfg, p)
            try:
                cfg["last"] = {"provider": p, "model": e.get("model"), "at": time.time()}
                json.dump(cfg, open(os.path.join(APP_DIR, "brain.json"), "w"))
            except Exception:
                pass
            return ans
        _cooldown(cfg, p)
    return None
'''

BRAIN_SKILL = r'''import os, json, time

SKILL = {
    "name": "brain",
    "description": "brain / brain key <key> / brain model <id> / brain base <url> / brain best <prov> / brain drop <prov> / brain local",
    "keywords": ["brain", "switch brain", "which model", "best provider"],
}

CFG = os.path.expanduser("~/.jarvis/brain.json")

RANK = {"groq": 1, "openrouter": 2, "experiential": 3, "google": 4, "openai": 5, "anthropic": 6}

DEFAULT_MODEL = {
    "groq": "llama-3.3-70b-versatile",
    "openai": "gpt-4o-mini",
    "openrouter": "meta-llama/llama-3.1-8b-instruct:free",
    "experiential": "gpt-4o-mini",
    "anthropic": "claude-3-5-haiku-latest",
    "google": "gemini-2.0-flash",
}


def _detect(key):
    k = (key or "").strip()
    if k.startswith("gsk_"):
        return "groq"
    if k.startswith("sk-ant-"):
        return "anthropic"
    if k.startswith("AIza"):
        return "google"
    if k.startswith("sk-or-"):
        return "openrouter"
    if k.startswith("xpl_"):
        return "experiential"
    if k.startswith("sk-"):
        return "openai"
    return None


def _load():
    try:
        return json.load(open(CFG))
    except Exception:
        return {}


def _save(c):
    try:
        json.dump(c, open(CFG, "w"))
    except Exception:
        pass


def run(args, ctx):
    a = args.strip()
    cfg = _load()
    low = a.lower()
    roster = sorted([e for e in (cfg.get("roster") or []) if isinstance(e, dict)],
                    key=lambda e: e.get("priority", 99))

    if low.startswith("key ") or low.startswith("add "):
        k = a.split(None, 1)[1].strip()
        p = _detect(k)
        base = None
        if p is None and cfg.get("base"):
            p = "experiential"
            base = cfg.get("base")
        if p is None:
            return "Unknown key prefix. If it is a self-hosted gateway key, set 'brain base <url>' first."
        for e in roster:
            if e.get("provider") == p:
                e["key"] = k
                if base:
                    e["base"] = base
                break
        else:
            roster.append({"provider": p, "key": k, "model": None,
                           "base": base, "priority": RANK.get(p, 7)})
        cfg["roster"] = roster
        cfg["mode"] = "cloud"
        _save(cfg)
        return ("Added " + p + ". JARVIS now auto-failovers across "
                + str(len(roster)) + " provider(s), best first.")

    if low in ("local", "ollama"):
        cfg["mode"] = "local"
        _save(cfg)
        return "Brain: local (Ollama). Type 'brain cloud' to re-enable auto-failover."

    if low == "cloud":
        cfg["mode"] = "cloud"
        _save(cfg)
        return "Brain: cloud (auto-failover)."

    if low.startswith("base "):
        cfg["base"] = a[5:].strip()
        _save(cfg)
        return ("Gateway base set to: " + cfg["base"] +
                " (used for self-hosted gateways, e.g. brain base http://127.0.0.1:8080/v1)")

    if low.startswith("best "):
        want = a[5:].strip().lower()
        rest = [e for e in roster if (e.get("provider") or "").lower() != want]
        hit = [e for e in roster if (e.get("provider") or "").lower() == want]
        new = []
        if hit:
            hit[0]["priority"] = 0
            new.append(hit[0])
        for i, e in enumerate(rest):
            e["priority"] = i + 1
            new.append(e)
        cfg["roster"] = new
        _save(cfg)
        if hit:
            return "Best provider set to " + want + ". It will be tried first."
        return "Provider not found: " + want

    if low.startswith("drop ") or low.startswith("remove "):
        want = a.split(None, 1)[1].strip().lower()
        before = len(roster)
        roster = [e for e in roster if (e.get("provider") or "").lower() != want]
        cfg["roster"] = roster
        if not roster:
            cfg["mode"] = "local"
        _save(cfg)
        return ("Removed " + want + "." if len(roster) < before
                else "Provider not found: " + want)

    if low in ("clear", "reset"):
        cfg.pop("cooldown", None)
        cfg.pop("last", None)
        _save(cfg)
        return "Cleared cooldowns and last-answer info."

    if low.startswith("model "):
        m = a[6:].strip()
        if roster:
            roster[0]["model"] = m
            cfg["roster"] = roster
        else:
            cfg["model"] = m
        _save(cfg)
        return "Cloud model set to: " + m

    # status
    if not roster:
        k = os.environ.get("BRAIN_API_KEY", "") or cfg.get("key", "")
        p = cfg.get("provider") or _detect(k)
        if p:
            m = cfg.get("model") or DEFAULT_MODEL.get(p, "")
            return "Brain: cloud (" + p + ")\nCloud model: " + m
        return "Brain: local (Ollama)\n(no keys yet - type 'brain key <key>' to add a provider)"

    lines = ["Brain: cloud (auto-failover)" if cfg.get("mode") != "local" else "Brain: local (Ollama)"]
    for i, e in enumerate(roster):
        p = e.get("provider") or "custom"
        m = e.get("model") or DEFAULT_MODEL.get(p, "?")
        cd = (cfg.get("cooldown") or {}).get(p, 0)
        state = "cooling down" if time.time() < cd else "ready"
        lines.append("  %d. %s - %s [%s]" % (i + 1, p, m, state))
    if cfg.get("last"):
        lines.append("Last answer via: " + cfg["last"].get("provider", "?"))
    return "\n".join(lines)
'''

src = open(CORE).read()
shutil.copy(CORE, BAK)

# 1) ensure _brain_cfg exists (idempotent)
if "def _brain_cfg():" not in src:
    cfg_fn = r'''
def _brain_cfg():
    try:
        return json.load(open(os.path.join(APP_DIR, "brain.json")))
    except Exception:
        return {}
'''
    anchor = "\ndef ask_llm("
    if anchor in src:
        src = src.replace(anchor, cfg_fn + "\n" + anchor, 1)
        print("core.py: _brain_cfg added")

# 2) insert failover block (idempotent)
if "def ask_cloud_smart(" not in src:
    src = src.replace("\ndef ask_llm(", "\n" + CORE_BLOCK + "\ndef ask_llm(", 1)
    print("core.py: auto-failover ask_cloud_smart added")

# 3) rewire ask_llm to use the smart router
src2 = src.replace("ans = ask_cloud(prompt)", "ans = ask_cloud_smart(prompt)")
src2 = src2.replace("ans = ask_groq(prompt)", "ans = ask_cloud_smart(prompt)")
if src2 != src:
    print("core.py: ask_llm now uses auto-failover")
    src = src2

open(CORE, "w").write(src)
try:
    py_compile.compile(CORE, doraise=True)
    print("core.py COMPILES OK")
except Exception as e:
    shutil.copy(BAK, CORE)
    print("PATCH FAILED - restored backup:", e)
    raise SystemExit(1)

# 4) rewrite brain skill
os.makedirs(os.path.join(J, "skills"), exist_ok=True)
open(os.path.join(J, "skills", "brain.py"), "w").write(BRAIN_SKILL)
print("brain skill upgraded (roster + auto-failover)")

# 5) restart JARVIS
import time, subprocess, urllib.request
PY = "python3"
vp = os.path.expanduser("~/omni-jarvis/venv/bin/python")
if os.path.exists(vp):
    PY = vp
subprocess.run(["pkill", "-f", "core.py"])
time.sleep(1)
log = open("/tmp/jarvis.log", "w")
subprocess.Popen([PY, CORE], stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
time.sleep(2)
try:
    h = urllib.request.urlopen("http://127.0.0.1:8000/health", timeout=5).read().decode()
    print("HEALTH:", h)
except Exception as e:
    print("CHECK FAILED:", e, "- see /tmp/jarvis.log")
