import os, json

SKILL = {
    "name": "lang",
    "description": "lang / lang list / lang set <code> / lang auto / lang off",
    "keywords": ["lang", "language", "idioma", "sprache", "langue"],
}

CFG = os.path.expanduser("~/.jarvis/lang.json")

LANGS = {
    "en": "English", "es": "Spanish", "fr": "French", "de": "German", "it": "Italian",
    "pt": "Portuguese", "ru": "Russian", "ar": "Arabic", "hi": "Hindi", "zh": "Chinese",
    "ja": "Japanese", "ko": "Korean", "tr": "Turkish", "nl": "Dutch", "pl": "Polish",
    "sv": "Swedish", "no": "Norwegian", "da": "Danish", "fi": "Finnish", "el": "Greek",
    "cs": "Czech", "ro": "Romanian", "hu": "Hungarian", "th": "Thai", "vi": "Vietnamese",
    "id": "Indonesian", "ms": "Malay", "uk": "Ukrainian", "he": "Hebrew", "fa": "Persian",
}


def run(args, ctx):
    a = args.strip().lower()

    if a in ("", "status", "lang", "language", "idioma", "sprache", "langue"):
        try:
            c = json.load(open(CFG))
            return "Language: " + c.get("name", "auto") + " (JARVIS always replies in this language)"
        except Exception:
            return "Language: auto (JARVIS replies in whatever language you write).\nType 'lang list' for options, 'lang set <code>' to fix one."

    if a == "list":
        lines = ["Supported languages:"]
        for code, name in LANGS.items():
            lines.append("  " + code + " - " + name)
        return "\n".join(lines)

    if a in ("off", "auto"):
        try:
            os.remove(CFG)
        except Exception:
            pass
        return "Language: auto. JARVIS will reply in whatever language you write."

    if a.startswith("set "):
        code = a[4:].strip()
        if code in LANGS:
            json.dump({"code": code, "name": LANGS[code]}, open(CFG, "w"))
            return "Language set to " + LANGS[code] + ". JARVIS will now reply in " + LANGS[code] + "."
        return "Unknown code '" + code + "'. Type 'lang list'."

    if a in LANGS:
        json.dump({"code": a, "name": LANGS[a]}, open(CFG, "w"))
        return "Language set to " + LANGS[a] + "."

    return "Unknown language. Type 'lang list' or 'lang set <code>'."
