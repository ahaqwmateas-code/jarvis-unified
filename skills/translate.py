import requests, re

SKILL = {
    "name": "translate",
    "description": "translate <text> to <language>",
    "keywords": ["translate", "translation", "in french", "in spanish"],
}

CODES = {
    "spanish": "es", "french": "fr", "german": "de", "italian": "it",
    "chinese": "zh-CN", "japanese": "ja", "korean": "ko", "arabic": "ar",
    "hindi": "hi", "portuguese": "pt", "russian": "ru", "dutch": "nl",
    "english": "en", "turkish": "tr", "greek": "el", "polish": "pl",
}


def run(args, ctx):
    a = args.strip()
    m = re.search(r"(?i)^(.+?)\s+to\s+([a-zA-Z]+)$", a)
    if not m:
        return "Usage: translate hello to spanish"
    text, lang = m.group(1).strip(), m.group(2).strip().lower()
    tgt = CODES.get(lang, lang[:2])
    try:
        r = requests.get("https://api.mymemory.translated.net/get",
                         params={"q": text, "langpair": "en|" + tgt}, timeout=12)
        d = r.json()
        return d.get("responseData", {}).get("translatedText") or "translation failed"
    except Exception as e:
        return "translate failed: " + str(e)
