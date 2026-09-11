SKILL = {
    "name": "weather",
    "description": "weather: weather <city> (default Sydney)",
    "keywords": ["weather", "forecast", "temp", "temperature"],
}


def run(args, ctx):
    city = args.strip() or "Sydney"
    try:
        import requests
        r = requests.get("https://wttr.in/" + city + "?format=3", timeout=10)
        if r.status_code == 200:
            return r.text.strip()
        return "Weather service error: HTTP " + str(r.status_code)
    except Exception as e:
        return "Weather unavailable (no internet?): " + str(e)
