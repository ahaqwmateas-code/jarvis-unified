import os, json, csv, random, urllib.request

SKILL = {
    "name": "prompt",
    "description": "prompt search <term> / use <act> / show <act> / random / list / update / off",
    "keywords": ["prompt", "persona", "act as", "roleplay"],
}

CSV_URL = "https://raw.githubusercontent.com/f/awesome-chatgpt-prompts/main/prompts.csv"
CSV = os.path.expanduser("~/.jarvis/prompts.csv")
IDX = os.path.expanduser("~/.jarvis/prompts_index.json")
PERSONA = os.path.expanduser("~/.jarvis/persona.json")


def _build_index():
    csv.field_size_limit(10 ** 7)
    idx = {}
    with open(CSV, encoding="utf-8", newline="") as f:
        for row in csv.reader(f):
            if len(row) < 2:
                continue
            act = row[0].strip()
            if not act or act.lower() == "act":
                continue
            idx.setdefault(act.lower(), {"act": act, "prompt": row[1].strip()})
    json.dump(idx, open(IDX, "w"))
    return idx


def _index():
    if os.path.exists(IDX):
        try:
            return json.load(open(IDX))
        except Exception:
            pass
    if not os.path.exists(CSV):
        return None
    return _build_index()


def _update():
    urllib.request.urlretrieve(CSV_URL, CSV)
    return _build_index()


def run(args, ctx):
    a = args.strip()
    low = a.lower()

    if low == "update":
        try:
            idx = _update()
            return "Updated: %d prompt templates from prompts.chat." % len(idx)
        except Exception as e:
            return "Update failed: " + str(e)

    if low == "off":
        try:
            os.remove(PERSONA)
        except Exception:
            pass
        return "Persona cleared. JARVIS is back to normal."

    idx = _index()
    if idx is None:
        try:
            idx = _update()
        except Exception as e:
            return "Could not download prompts.chat library: " + str(e)

    if low == "list":
        names = sorted(idx.keys())
        return ("prompts.chat has %d templates. First 60:\n%s\nUse: prompt search <term>"
                % (len(names), "\n".join("- " + n for n in names[:60])))

    if low == "random":
        k = random.choice(list(idx.keys()))
        return "Random prompt: %s\n\n%s" % (idx[k]["act"], idx[k]["prompt"][:1500])

    if low.startswith("search "):
        term = a[7:].strip().lower()
        hits = [k for k in idx if term in k][:20]
        if not hits:
            return "No matches for '%s'." % term
        return ("Matches for '%s':\n%s\nUse: prompt show <act> or prompt use <act>"
                % (term, "\n".join("- " + k for k in hits)))

    if low.startswith("show "):
        term = a[5:].strip().lower()
        k = term if term in idx else next((x for x in idx if term in x), None)
        if not k:
            return "No prompt matching '%s'. Try: prompt search %s" % (term, term)
        return "%s:\n%s" % (idx[k]["act"], idx[k]["prompt"][:2500])

    if low.startswith("use "):
        term = a[4:].strip().lower()
        k = term if term in idx else next((x for x in idx if term in x), None)
        if not k:
            return "No prompt matching '%s'. Try: prompt search %s" % (term, term)
        json.dump({"act": idx[k]["act"], "prompt": idx[k]["prompt"]}, open(PERSONA, "w"))
        return "Persona set: %s\nJARVIS now answers as this. 'prompt off' to stop." % idx[k]["act"]

    return ("prompts.chat (%d templates). Commands:\n" % len(idx) +
            "  prompt search <term>   - find a persona\n" +
            "  prompt use <act>       - become it\n" +
            "  prompt show <act>      - read it\n" +
            "  prompt random          - surprise me\n" +
            "  prompt list            - list acts\n" +
            "  prompt update          - refresh library\n" +
            "  prompt off             - back to normal")
