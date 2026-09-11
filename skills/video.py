import os, json, time, base64, subprocess, shutil, urllib.request, urllib.parse

SKILL = {
    "name": "video",
    "description": "video make <prompt> - generate a real MP4 (free frames + Ken Burns animation)",
    "keywords": ["video", "veo", "film", "clip", "animate", "movie"],
}

GALLERY = os.path.expanduser("~/.jarvis/gallery")
FFMPEG = os.environ.get("FFMPEG_BIN", "ffmpeg")

PAID = ("\nPaid text-to-video models (Veo 3.1, Runway, Kling) have no free API.\n"
        "Ready-to-wire CLIs: clihub install generate-veo-video / jimeng / vivideo\n"
        "But this command already makes real videos free:\n"
        "    video make <prompt>")


def _gemini_key():
    try:
        cfg = json.load(open(os.path.expanduser("~/.jarvis/brain.json")))
        for e in cfg.get("roster", []) or []:
            k = (e.get("key") or "").strip()
            if k.startswith("AIza"):
                return k
    except Exception:
        pass
    return None


def _polli(prompt, size):
    url = ("https://image.pollinations.ai/prompt/" + urllib.parse.quote(prompt)
           + "?width=" + str(size) + "&height=" + str(size) + "&nologo=true")
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    return urllib.request.urlopen(req, timeout=180).read()


def _gemini(prompt, key):
    base = os.environ.get("GEMINI_IMAGE_BASE",
                          "https://generativelanguage.googleapis.com/v1beta")
    url = base + "/models/gemini-2.5-flash-image:generateContent?key=" + key
    body = json.dumps({"contents": [{"parts": [{"text": prompt}]}],
                       "generationConfig": {"responseModalities": ["IMAGE"]}}).encode()
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    r = json.loads(urllib.request.urlopen(req, timeout=180).read().decode())
    for part in r.get("candidates", [{}])[0].get("content", {}).get("parts", []):
        if "inlineData" in part:
            return base64.b64decode(part["inlineData"]["data"])
    raise Exception("no image in response")


def _frame(prompt, i, n):
    p = prompt + (" (shot %d of %d, cinematic, natural variation)" % (i + 1, n))
    key = _gemini_key()
    if key:
        try:
            return _gemini(p, key)
        except Exception:
            pass
    return _polli(p, 1024)


def _make(prompt):
    try:
        r = subprocess.run([FFMPEG, "-version"], capture_output=True, text=True, timeout=30)
        if r.returncode != 0:
            raise Exception("ffmpeg not usable")
    except FileNotFoundError:
        return ("ffmpeg is missing. Install it once:\n"
                "    apt-get install -y ffmpeg\n"
                "then re-run: video make <prompt>")

    os.makedirs(GALLERY, exist_ok=True)
    stamp = time.strftime("%Y%m%d_%H%M%S") + "_" + str(int(time.time() * 1000) % 1000)
    tmp = os.path.join(GALLERY, "tmp_" + stamp)
    os.makedirs(tmp, exist_ok=True)

    n = 3
    frames = []
    for i in range(n):
        try:
            data = _frame(prompt, i, n)
        except Exception as e:
            shutil.rmtree(tmp, ignore_errors=True)
            return "Frame %d failed: %s" % (i + 1, e)
        fp = os.path.join(tmp, "frame%d.jpg" % i)
        with open(fp, "wb") as f:
            f.write(data)
        frames.append(fp)

    vf = ("scale=1280:720:force_original_aspect_ratio=increase,crop=1280:720,"
          "zoompan=z='min(zoom+0.0015,1.25)':d=72:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':"
          "s=1280x720:fps=24")
    segs = []
    for i, fp in enumerate(frames):
        seg = os.path.join(tmp, "seg%d.mp4" % i)
        subprocess.run([FFMPEG, "-y", "-i", fp, "-vf", vf, "-frames:v", "72",
                        "-c:v", "libx264", "-pix_fmt", "yuv420p", seg],
                       capture_output=True, timeout=300)
        if not os.path.exists(seg):
            shutil.rmtree(tmp, ignore_errors=True)
            return "ffmpeg failed to render frame %d" % (i + 1)
        segs.append(seg)

    lst = os.path.join(tmp, "list.txt")
    with open(lst, "w") as f:
        for seg in segs:
            f.write("file '%s'\n" % os.path.basename(seg))

    out = os.path.join(GALLERY, "vid_" + stamp + ".mp4")
    subprocess.run([FFMPEG, "-y", "-f", "concat", "-safe", "0", "-i", lst,
                    "-c", "copy", out], capture_output=True, timeout=300)
    shutil.rmtree(tmp, ignore_errors=True)

    if not os.path.exists(out):
        return "Video assembly failed - see logs."
    size = os.path.getsize(out)
    return ("Video ready: %s (%.1f MB, %ds)\nView/play: http://YOUR_IP:8000/files/%s"
            % (out, size / 1048576.0, n * 3, os.path.basename(out)))


def run(args, ctx):
    a = args.strip()
    low = a.lower()
    if low.startswith("make "):
        prompt = a[5:].strip()
        if not prompt:
            return "Usage: video make <prompt>  - e.g. video make a dragon flying over a city"
        return _make(prompt)
    return ("JARVIS video modes:\n"
            "  video make <prompt>   - generate a REAL MP4 now (free: image frames + Ken Burns animation)\n"
            + PAID)
