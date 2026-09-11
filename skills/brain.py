import os, json, time

SKILL = {
    "name": "brain",
    "description": "brain / brain key <key> / brain custom <name> <base> <key> [model] / brain model [provider] <id> / brain base <url> / brain best <prov> / brain drop <prov> / brain local / brain clear",
    "keywords": ["brain", "switch brain", "which model", "best provider", "add provider"],
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


def _next_priority(roster):
    return max([e.get("priority", 99) for e in roster], default=-1) + 1


def run(args, ctx):
    a = args.strip()
    cfg = _load()
    low = a.lower()
    roster = sorted([e for e in (cfg.get("roster") or []) if isinstance(e, dict)],
                    key=lambda e: e.get("priority", 99))

    # --- custom OpenAI-compatible endpoint (Cerebras, Mistral, GitHub Models, ...)
    if low.startswith("custom "):
        parts = a.split(None, 4)  # custom <name> <base> <key> [model]
        if len(parts) < 4:
            return "Usage: brain custom <name> <base-url> <key> [model]"
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
                _save(cfg)
                return "Updated " + name + "."
        roster.append({"provider": name, "key": key, "model": model,
                       "base": base, "priority": _next_priority(roster)})
        cfg["roster"] = roster
        cfg["mode"] = "cloud"
        _save(cfg)
        out = "Added custom provider " + name + "."
        if not model:
            out += " Set its model: brain model " + name + " <model-id>"
        return out

    # --- known provider by key prefix
    if low.startswith("key ") or low.startswith("add "):
        k = a.split(None, 1)[1].strip()
        p = _detect(k)
        base = None
        if p is None and cfg.get("base"):
            p = "experiential"
            base = cfg.get("base")
        if p is None:
            return ("Unknown key prefix. For a custom OpenAI-compatible API use:\n"
                    "brain custom <name> <base-url> <key> [model]")
        for e in roster:
            if e.get("provider") == p:
                e["key"] = k
                if base:
                    e["base"] = base
                break
        else:
            roster.append({"provider": p, "key": k, "model": None,
                           "base": base, "priority": RANK.get(p, _next_priority(roster))})
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
    if not roster:
        k = os.environ.get("BRAIN_API_KEY", "") or cfg.get("key", "")
        p = cfg.get("provider") or _detect(k)
        if p:
            m = cfg.get("model") or DEFAULT_MODEL.get(p, "")
            return "Brain: cloud (" + p + ")\nCloud model: " + m
        return "Brain: local (Ollama)\n(no keys yet - type 'brain key <key>' or 'brain custom ...')"

    lines = ["Brain: cloud (auto-failover)" if cfg.get("mode") != "local" else "Brain: local (Ollama)"]
    for i, e in enumerate(roster):
        p = e.get("provider") or "custom"
        m = e.get("model") or DEFAULT_MODEL.get(p, "?")
        cd = (cfg.get("cooldown") or {}).get(p, 0)
        state = "cooling down" if time.time() < cd else "ready"
        line = "  %d. %s - %s [%s]" % (i + 1, p, m, state)
        if e.get("base") and p not in RANK:
            line += " @ " + e["base"]
        lines.append(line)
    if cfg.get("last"):
        lines.append("Last answer via: " + cfg["last"].get("provider", "?"))
    return "\n".join(lines)
