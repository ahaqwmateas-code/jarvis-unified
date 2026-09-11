import requests, re, html

SKILL = {
    "name": "search",
    "description": "search <query> - web search",
    "keywords": ["search", "google", "find", "web"],
}

UA = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"}


def run(args, ctx):
    q = args.strip()
    if not q:
        return "Usage: search <query>"
    links = []
    try:
        r = requests.post("https://html.duckduckgo.com/html/", data={"q": q},
                          headers=UA, timeout=12)
        if r.status_code == 200:
            links = re.findall(r'class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>', r.text)
    except Exception as e:
        return "search failed: " + str(e)
    out = []
    for i, (u, t) in enumerate(links[:5], 1):
        t = html.unescape(re.sub("<[^>]+>", "", t)).strip()
        if not u.startswith("http"):
            continue
        out.append(str(i) + ". " + t + "\n   " + u)
    return "\n".join(out) if out else "No results found for: " + q
