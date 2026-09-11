import os, json, time

SKILL = {
    "name": "brain",
    "description": "brain | brain key <k> | brain add <name> <base> <key> <model> | brain custom <name> <base> <key> [model] | brain slim|balanced|max | brain size | brain test [name] | brain off|on <name> | brain best <prov> | brain drop <prov> | brain model [prov] <id> | brain local|cloud | brain clear",
    "keywords": ["brain", "switch brain", "which model", "best provider", "add provider",
                 "make brain smaller", "brain size", "brain slim", "brain max"],
}

CFG = os.path.expanduser("~/.jarvis/brain.json")

RANK = {"groq": 1, "openrouter": 2, "experiential": 3, "google": 4, "openai": 5, "anthropic": 6}

# --- model presets: slim = small & fast, balanced = default, max = biggest ---
SIZE_MODEL = {
    "slim": {
        "groq": "llama-3.1-8b-instant",
        "openai": "gpt-4o-mini",
        "openrouter": "meta-llama/llama-3.1-8b-instruct:free",
        "experiential": "gpt-4o-mini",
        "anthropic": "claude-3-5-haiku-latest",
        "google": "gemini-2.0-flash-lite",
    },
    "balanced": {
        "groq": "llama-3.3-70b-versatile",
        "openai": "gpt-4o-mini",
        "openrouter": "meta-llama/llama-3.1-8b-instruct:free",
        "experiential": "gpt-4o-mini",
        "anthropic": "claude-3-5-haiku-latest",
        "google": "gemini-2.0-flash",
    },
    "max": {
        "groq": "llama-3.3-70b-versatile",
        "openai": "gpt-4o",
        "openrouter": "deepseek/deepseek-chat-v3-0324:free",
        "experiential": "gpt-4o-mini",
        "anthropic": "claude-3-5-sonnet-latest",
        "google": "gemini-2.5-flash",
    },
}

PROVIDER_BASE = {
    "groq": "https://api.groq.com/openai/v1",
    "openai": "https://api.openai.com/v1",
    "openrouter": "https://openrouter.ai/api/v1",
    "experiential": "https://api.experientiallabs.ai/v1",
    "mistral": "https://api.mistral.ai/v1",
    "cerebras": "https://api.cerebras.ai/v1",
    "deepseek": "https://api.deepseek.com",
    "xai": "https://api.x.ai/v1",
    "github": "https://models.inference.ai.azure.com",
}

DEFAULT_MODEL = SIZE_MODEL["balanced"]


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
        d = os.path.dirname(CFG)
        if d:
            os.makedirs(d, exist_ok=True)
        json.dump(c, open(CFG, "w"))
    except Exception:
        pass


def _next_priority(roster):
    return max([e.get("priority", 99) for e in roster], default=-1) + 1


def _size(cfg):
    return cfg.get("size", "balanced")


def _apply_size(cfg, roster):
    """Rewrites every known provider's model to the current size preset.
    Custom providers keep their own model unless they have none."""
    size = _size(cfg)
    preset = SIZE_MODEL.get(size, SIZE_MODEL["balanced"])
    for e in roster:
        p = (e.get("provider") or "").lower()
        if p in preset:
            e["model"] = preset[p]
        elif not e.get("model") and p in DEFAULT_MODEL:
            e["model"] = DEFAULT_MODEL[p]
    return roster


def _test_entry(e):
    """Tiny connectivity ping; returns (ok, detail)."""
    import requests
    p = (e.get("provider") or "").lower()
    key = e.get("key", "")
    model = e.get("model") or DEFAULT_MODEL.get(p, "")
    base = e.get("base") or PROVIDER_BASE.get(p)
    msgs = [{"role": "user", "content": "Reply with the single word OK."}]
    if p == "anthropic":
        r = requests.post("https://api.anthropic.com/v1/messages",
                          headers={"x-api-key": key, "anthropic-version": "2023-06-01",
                                   "Content-Type": "application/json"},
                          json={"model": model, "max_tokens": 8, "messages": msgs}, timeout=20)
        return r.status_code == 200, r.status_code
    if p == "google":
        r = requests.post(
            "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + key,
            json={"contents": [{"parts": [{"text": "Reply with OK"}]}]}, timeout=20)
        return r.status_code == 200, r.status_code
    if not base:
        return False, "no base URL"
    r = requests.post(base.rstrip("/") + "/chat/completions",
                      headers={"Authorization": "Bearer " + key, "Content-Type": "application/json"},
                      json={"model": model, "messages": msgs, "max_tokens": 8}, timeout=20)
    return r.status_code == 200, r.status_code


def run(args, ctx):
    a = (args or "").strip()
    # the router strips the leading "brain" keyword; be tolerant if it's still there
    if a.lower().startswith("brain "):
        a = a[6:].strip()
    cfg = _load()
    low = a.lower()
    roster = sorted([e for e in (cfg.get("roster") or []) if isinstance(e, dict)],
                    key=lambda e: e.get("priority", 99))

    # --- SIZE MODES (smaller / bigger brain) ---
    if low in ("slim", "fast", "small", "minimal"):
        cfg["size"] = "slim"
        cfg["roster"] = _apply_size(cfg, roster)
        _save(cfg)
        return ("Brain size: SLIM (small, fast models - lowest latency & RAM)\n"
                + _show(cfg))
    if low in ("balanced", "normal"):
        cfg["size"] = "balanced"
        cfg["roster"] = _apply_size(cfg, roster)
        _save(cfg)
        return ("Brain size: BALANCED (default mix)\n" + _show(cfg))
    if low in ("max", "big", "best quality", "quality"):
        cfg["size"] = "max"
        cfg["roster"] = _apply_size(cfg, roster)
        _save(cfg)
        return ("Brain size: MAX (largest models available - highest quality)\n" + _show(cfg))
    if low == "size":
        return ("Brain size: " + _size(cfg).upper() + "\n"
                "slim   = small & fast (llama-3.1-8b-instant etc.)\n"
                "balanced = default (llama-3.3-70b etc.)\n"
                "max    = biggest (deepseek-v3 / gemini-2.5 etc.)\n"
                "change with: brain slim | brain balanced | brain max")

    # --- add a named provider (add = alias of custom) ---
    if low.startswith("add ") or low.startswith("custom "):
        parts = a.split(None, 4)
        if len(parts) < 4:
            return "Usage: brain add <name> <base-url> <key> <model>\n" \
                   "Examples:\n" \
                   "  brain add cerebras https://api.cerebras.ai/v1 <key> llama3.1-8b\n" \
                   "  brain add mistral  https://api.mistral.ai/v1   <key> open-mistral-nemo\n" \
                   "  brain add openai   https://api.openai.com/v1   <key> gpt-4o-mini"
        name = parts[1].strip().lower()
        base = parts[2].strip()
        key = parts[3].strip()
        model = parts[4].strip() if len(parts) > 4 else None
        if not base.startswith("http"):
            return "Base URL must start with http:// or https://"
        for e in roster:
            if (e.get("provider") or "").lower() == name:
                e["key"] = key or e.get("key")
                e["base"] = base or e.get("base")
                if model:
                    e["model"] = model
                e["enabled"] = True
                _save(cfg)
                return "Updated " + name + ".\n" + _show(cfg)
        roster.append({"provider": name, "key": key, "model": model or DEFAULT_MODEL.get(name),
                       "base": base, "priority": _next_priority(roster), "enabled": True})
        cfg["roster"] = roster
        cfg["mode"] = "cloud"
        _save(cfg)
        out = "Added provider " + name + ". It is now in the failover chain."
        if not model and name not in DEFAULT_MODEL:
            out += " (set its model: brain model " + name + " <model-id>)"
        return out + "\n" + _show(cfg)

    # --- known provider by key prefix ---
    if low.startswith("key ") or low == "key":
        rest = a.split(None, 1)
        if len(rest) < 2:
            return "Usage: brain key <key>  (auto-detects Groq/Gemini/OpenRouter/etc.)"
        k = rest[1].strip()
        p = _detect(k)
        base = None
        if p is None and cfg.get("base"):
            p = "experiential"
            base = cfg.get("base")
        if p is None:
            return ("Unknown key prefix. For a custom OpenAI-compatible API use:\n"
                    "brain add <name> <base-url> <key> <model>")
        model = SIZE_MODEL[_size(cfg)].get(p)
        for e in roster:
            if e.get("provider") == p:
                e["key"] = k
                if model:
                    e["model"] = model
                if base:
                    e["base"] = base
                e["enabled"] = True
                break
        else:
            roster.append({"provider": p, "key": k, "model": model,
                           "base": base, "priority": RANK.get(p, _next_priority(roster)),
                           "enabled": True})
        cfg["roster"] = roster
        cfg["mode"] = "cloud"
        _save(cfg)
        return ("Added " + p + ". JARVIS now auto-failovers across "
                + str(len([e for e in roster if e.get("enabled", True)])) + " provider(s).\n" + _show(cfg))

    # --- test connectivity ---
    if low.startswith("test") or low.startswith("ping"):
        rest = a.split(None, 1)
        want = (rest[1].strip().lower() if len(rest) > 1 else "")
        targets = [e for e in roster if not want or (e.get("provider") or "").lower() == want]
        if not targets:
            return "No providers to test. Add one: brain key <key>  or  brain add <name> <base> <key> <model>"
        lines = ["Testing " + str(len(targets)) + " provider(s)..."]
        for e in targets:
            p = e.get("provider")
            try:
                ok, code = _test_entry(e)
                lines.append("  %-12s %s" % (p, "OK" if ok else ("FAIL (HTTP " + str(code) + ")" if isinstance(code, int) else "FAIL (" + str(code) + ")")))
            except Exception as ex:
                lines.append("  %-12s FAIL (%s)" % (p, str(ex)[:60]))
        return "\n".join(lines)

    # --- off / on (disable/enable without deleting the key) ---
    if low.startswith("off ") or low.startswith("disable "):
        want = a.split(None, 1)[1].strip().lower()
        for e in roster:
            if (e.get("provider") or "").lower() == want:
                e["enabled"] = False
                _save(cfg)
                return "Disabled " + want + " (key kept - 'brain on " + want + "' to re-enable)."
        return "Provider not found: " + want
    if low.startswith("on ") or low.startswith("enable "):
        want = a.split(None, 1)[1].strip().lower()
        for e in roster:
            if (e.get("provider") or "").lower() == want:
                e["enabled"] = True
                _save(cfg)
                return "Enabled " + want + "."
        return "Provider not found: " + want

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
                " (for a self-hosted gateway - then 'brain key <its key>')")

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

    # --- model: 'brain model <model>' (best) or 'brain model <provider> <model>'
    if low.startswith("model "):
        rest = a[6:].strip()
        parts = rest.split(None, 1)
        provs = {(e.get("provider") or "").lower() for e in roster}
        if len(parts) == 2 and parts[0].lower() in provs:
            prov = parts[0].lower()
            m = parts[1].strip()
            for e in roster:
                if (e.get("provider") or "").lower() == prov:
                    e["model"] = m
            cfg["roster"] = roster
            _save(cfg)
            return "Model for " + prov + " set to: " + m
        if roster:
            roster[0]["model"] = rest
            cfg["roster"] = roster
        else:
            cfg["model"] = rest
        _save(cfg)
        return "Cloud model set to: " + rest

    # --- status
    return _show(cfg)


def _show(cfg):
    roster = sorted([e for e in (cfg.get("roster") or []) if isinstance(e, dict)],
                    key=lambda e: e.get("priority", 99))
    size = _size(cfg)
    mode = "local (Ollama)" if cfg.get("mode") == "local" else "cloud (auto-failover)"
    if not roster:
        return ("Brain: " + mode + " | size: " + size.upper() + "\n"
                "(no providers yet)\n"
                "Add one:\n"
                "  brain key <key>                          auto-detect\n"
                "  brain add <name> <base> <key> <model>   any OpenAI-compatible API\n"
                "  brain slim | balanced | max              choose size")
    lines = ["Brain: " + mode + " | size: " + size.upper()]
    n = 0
    for e in roster:
        n += 1
        p = e.get("provider") or "custom"
        m = e.get("model") or DEFAULT_MODEL.get(p, "?")
        enabled = e.get("enabled", True)
        state = "OFF" if not enabled else "ready"
        cd = (cfg.get("cooldown") or {}).get(p, 0)
        if enabled and time.time() < cd:
            state = "cooling down"
        line = "  %d. %s - %s [%s]" % (n, p, m, state)
        if e.get("base") and p not in RANK:
            line += " @ " + e["base"]
        lines.append(line)
    lines.append("commands: brain slim|balanced|max · add · test · off/on · best · drop · model")
    if cfg.get("last"):
        lines.append("Last answer via: " + cfg["last"].get("provider", "?"))
    return "\n".join(lines)
