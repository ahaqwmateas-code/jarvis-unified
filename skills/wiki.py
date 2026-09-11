import requests

SKILL = {
    "name": "wiki",
    "description": "wiki <topic> - Wikipedia summary",
    "keywords": ["wiki", "wikipedia", "who was", "define"],
}

UA = {"User-Agent": "jarvis/1.0"}


def run(args, ctx):
    t = args.strip().replace(" ", "_")
    if not t:
        return "Usage: wiki <topic>"
    try:
        r = requests.get("https://en.wikipedia.org/api/rest_v1/page/summary/" + t,
                         headers=UA, timeout=12)
        if r.status_code != 200:
            return "No Wikipedia article found for: " + args.strip()
        d = r.json()
        ext = d.get("extract", "").strip()
        url = d.get("content_urls", {}).get("desktop", {}).get("page", "")
        return ext + ("\n" + url if url else "")
    except Exception as e:
        return "wiki failed: " + str(e)
