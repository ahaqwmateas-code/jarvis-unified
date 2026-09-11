import os, shutil, py_compile

J = os.path.expanduser("~/.jarvis")
CORE = os.path.join(J, "core.py")

IMAGE_SKILL = r'''import os, json, time, base64, socket, urllib.request, urllib.parse

SKILL = {
    "name": "image",
    "description": "image <prompt> - generate an image (Gemini if key set, else Pollinations)",
    "keywords": ["image", "imagine", "draw", "generate image", "picture", "photo"],
}

GALLERY = os.path.expanduser("~/.jarvis/gallery")


def _is_image(b):
    return bool(b) and ((b[:3] == b"\xff\xd8\xff") or                 # JPEG
                        (b[:8] == b"\x89PNG\r\n\x1a\n") or           # PNG
                        (b[:4] == b"RIFF" and b[8:12] == b"WEBP"))   # WebP


def _my_ip():
    try:
        import subprocess
        out = subprocess.run(["ip", "-o", "addr", "show"], capture_output=True, text=True,
                             timeout=10).stdout
        for line in out.splitlines():
            if "inet " in line and "127.0.0.1" not in line:
                ip = line.split()[3].split("/")[0]
                if ip.startswith(("192.168.", "10.", "172.16", "172.17", "172.18",
                                  "172.19", "172.2", "172.30", "172.31")):
                    return ip
    except Exception:
        pass
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        if not ip.startswith("169.254."):
            return ip
    except Exception:
        pass
    return "192.168.1.109"


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
    with urllib.request.urlopen(req, timeout=180) as r:
        data = r.read()
    if not _is_image(data):
        raise Exception("provider returned a non-image page")
    return data


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
            data = base64.b64decode(part["inlineData"]["data"])
            if _is_image(data):
                return data
    raise Exception("no image in response")


def run(args, ctx):
    prompt = args.strip()
    if not prompt:
        return "Usage: image <prompt>  - e.g. image a red fox in snowy mountains"

    key = _gemini_key()
    if key:
        try:
            data = _gemini(prompt, key)
            ext, engine = "png", "gemini"
        except Exception:
            key = None

    if not key:
        data = None
        last = "unknown"
        for attempt in range(2):
            try:
                data = _polli(prompt, 1024)
                ext, engine = "jpg", "pollinations"
                break
            except Exception as e:
                last = str(e)
                time.sleep(2)
        if data is None:
            return ("Image generation failed: " + last +
                    "\nTip: add a free Gemini key for accurate images: jarvis brain key AIza...")

    os.makedirs(GALLERY, exist_ok=True)
    name = "img_" + time.strftime("%Y%m%d_%H%M%S") + "_" + str(int(time.time() * 1000) % 1000) + "." + ext
    p = os.path.join(GALLERY, name)
    with open(p, "wb") as f:
        f.write(data)
    return ("Image ready (" + engine + "). Saved: " + p +
            "\nView: http://" + _my_ip() + ":8000/files/" + name)
'''

# skill-only change: no core edit needed, skills reload on every request
os.makedirs(os.path.join(J, "skills"), exist_ok=True)
open(os.path.join(J, "skills", "image.py"), "w").write(IMAGE_SKILL)
print("image skill upgraded (verifies real image bytes, retries, auto-IP link)")

try:
    py_compile.compile(os.path.join(J, "skills", "image.py"), doraise=True)
    print("image.py COMPILES OK")
except Exception as e:
    print("PATCH FAILED:", e)
    raise SystemExit(1)

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
