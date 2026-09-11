import os, json, time, shutil, socket, subprocess, shlex, platform
from datetime import datetime
from collections import deque

from fastapi import FastAPI, Header
from fastapi.responses import HTMLResponse
from pydantic import BaseModel

APP_DIR = os.path.dirname(os.path.abspath(__file__))
SKILLS_DIR = os.path.join(APP_DIR, "skills")
MEMORY_FILE = os.path.join(APP_DIR, "memory.jsonl")
NOTES_DIR = os.path.join(APP_DIR, "notes")
os.makedirs(NOTES_DIR, exist_ok=True)

API_TOKEN = os.environ.get("JARVIS_TOKEN", "jarvis")
OLLAMA_URL = os.environ.get("OLLAMA_URL", "http://127.0.0.1:11434")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "llama3.2")

JARVIS_SYS = (
    "You are JARVIS, a concise, helpful AI assistant on a Kali Linux box. "
    "Answer briefly and directly with a dry, friendly tone. Keep answers short "
    "unless detail is asked for."
)

app = FastAPI(title="JARVIS Core", version="5.0.0")
START = time.time()
HISTORY = deque(maxlen=40)


def load_memory():
    if not os.path.exists(MEMORY_FILE):
        return
    try:
        with open(MEMORY_FILE) as f:
            for line in f:
                try:
                    d = json.loads(line)
                    HISTORY.append((d.get("role", "?"), d.get("text", "")))
                except Exception:
                    pass
    except Exception:
        pass


def save_memory(role, text):
    try:
        with open(MEMORY_FILE, "a") as f:
            f.write(json.dumps({"role": role, "text": text}) + "\n")
    except Exception:
        pass


def get_lan_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def read_file(path):
    try:
        with open(path) as f:
            return f.read().strip()
    except Exception:
        return None


def fmt_bytes(b):
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if b < 1024:
            return f"{b:.1f} {unit}"
        b /= 1024
    return f"{b:.1f} PB"


def shell_run(cmd, timeout=20):
    try:
        r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
        out = (r.stdout or "").strip()
        err = (r.stderr or "").strip()
        return (out + ("\n" + err if err else "")).strip() or "(no output)"
    except subprocess.TimeoutExpired:
        return f"Timed out after {timeout}s"
    except Exception as e:
        return f"Error: {e}"


def say(text):
    es = shutil.which("espeak-ng") or shutil.which("espeak")
    if not es:
        return None
    try:
        subprocess.Popen([es, "-s", "150", text], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return "Speaking: " + text
    except Exception as e:
        return "TTS error: " + str(e)


def ollama_models():
    try:
        import requests
        r = requests.get(f"{OLLAMA_URL}/api/tags", timeout=3)
        if r.status_code == 200:
            return [m["name"] for m in r.json().get("models", [])]
    except Exception:
        pass
    return None


def ask_llm(prompt):
    try:
        import requests
        ctx = "\n".join(f"{r}: {t}" for r, t in HISTORY)
        ctx = ctx[-3000:]
        full = JARVIS_SYS + ("\n\nRecent conversation:\n" + ctx if ctx else "") + "\n\nUser: " + prompt
        r = requests.post(
            f"{OLLAMA_URL}/api/generate",
            json={"model": OLLAMA_MODEL, "system": JARVIS_SYS, "prompt": full, "stream": False},
            timeout=180,
        )
        if r.status_code == 200:
            return r.json().get("response", "").strip()
    except Exception:
        pass
    return None


def ask_cloud(prompt):
    """Anonymous free cloud fallback (Pollinations) when Ollama is offline."""
    try:
        import requests
        ctx = "\n".join(f"{r}: {t}" for r, t in HISTORY)
        ctx = ctx[-3000:]
        full = JARVIS_SYS + ("\n\nRecent conversation:\n" + ctx if ctx else "") + "\n\nUser: " + prompt
        r = requests.post(
            "https://text.pollinations.ai/openai",
            json={"model": "openai", "messages": [
                {"role": "system", "content": JARVIS_SYS},
                {"role": "user", "content": full},
            ]},
            timeout=90,
        )
        if r.status_code == 200:
            j = r.json()
            c = j.get("choices") or []
            if c and c[0].get("message", {}).get("content"):
                return c[0]["message"]["content"].strip()
    except Exception:
        pass
    return None


def load_skills():
    skills = {}
    if not os.path.isdir(SKILLS_DIR):
        return skills
    for fn in sorted(os.listdir(SKILLS_DIR)):
        if not fn.endswith(".py") or fn.startswith("_"):
            continue
        name = fn[:-3]
        path = os.path.join(SKILLS_DIR, fn)
        try:
            ns = {}
            exec(compile(open(path).read(), fn, "exec"), ns)
            skills[name] = (ns.get("SKILL", {"name": name, "description": "no description"}), ns.get("run"))
        except Exception as e:
            skills[name] = ({"name": name, "description": "ERROR: " + str(e)}, None)
    return skills


def help_text():
    lines = ["JARVIS CORE v5 - type a keyword or just talk to me.", ""]
    for name, (meta, fn) in sorted(load_skills().items()):
        lines.append("  " + name.ljust(10) + meta.get("description", ""))
    lines.append("  " + "shell".ljust(10) + "raw command (token required)")
    lines.append("  " + "update".ljust(10) + "self-update + restart (token)")
    lines.append("  " + "llm".ljust(10) + "Ollama status / models")
    lines.append("")
    lines.append("Anything else is sent to the local AI (Ollama).")
    return "\n".join(lines)


def route(cmd):
    t = cmd.strip()
    low = t.lower()
    if not t:
        return "Say 'help' or ask me anything."
    if low in ("help", "?"):
        return help_text()
    if low in ("llm", "llm status"):
        models = ollama_models()
        if models is None:
            return "Ollama OFFLINE. Start: nohup ollama serve > /tmp/ollama.log 2>&1 &"
        return "Ollama ONLINE. Models: " + ", ".join(models)
    if low == "update":
        return "Run 'shell bash ~/.jarvis/update.sh' (token required) or POST /api/update with token."
    # skill keyword routing
    skills = load_skills()
    for name, (meta, fn) in sorted(skills.items(), key=lambda kv: -len(str(kv[1][0].get("keywords", "")))):
        if not fn:
            continue
        for kw in meta.get("keywords", []):
            kwl = kw.lower()
            if low == kwl or low.startswith(kwl + " ") or (" " + kwl) in (" " + low):
                arg = t[len(kw):].strip() if low.startswith(kwl) else t
                try:
                    return fn(arg, {"app_dir": APP_DIR, "notes_dir": NOTES_DIR})
                except Exception as e:
                    return f"Skill '{name}' error: {e}"
    # LLM fallback
    ans = ask_llm(t)
    if ans is None:
        ans = ask_cloud(t)
    if ans is not None:
        return ans
    return "No AI online and no skill matched. Say 'help'."


@app.get("/")
def home():
    return HTMLResponse(UI)


@app.get("/health")
def health():
    return {"healthy": True, "uptime_seconds": int(time.time() - START), "version": app.version}


@app.get("/api/system")
def system():
    total = used = 0
    meminfo = read_file("/proc/meminfo")
    if meminfo:
        vals = {}
        for line in meminfo.splitlines():
            p = line.split()
            if len(p) >= 2:
                vals[p[0].rstrip(":")] = int(p[1]) * 1024
        total = vals.get("MemTotal", 0)
        free = vals.get("MemFree", 0) + vals.get("Buffers", 0) + vals.get("Cached", 0)
        used = total - free
    disk = shutil.disk_usage("/")
    return {
        "hostname": socket.gethostname(),
        "platform": platform.platform(),
        "cpu_cores": os.cpu_count(),
        "load_avg": read_file("/proc/loadavg"),
        "uptime_seconds": int(time.time() - START),
        "memory": {"total": fmt_bytes(total), "used": fmt_bytes(used),
                   "percent": round(used / total * 100, 1) if total else 0},
        "disk": {"total": fmt_bytes(disk.total), "used": fmt_bytes(disk.used), "free": fmt_bytes(disk.free)},
        "timestamp": datetime.now().isoformat(),
    }


@app.get("/api/network")
def network():
    return {"lan_ip": get_lan_ip(), "hostname": socket.gethostname()}


@app.get("/api/skills")
def skills_list():
    return [{"name": n, "description": m.get("description", ""), "keywords": m.get("keywords", [])}
            for n, (m, fn) in load_skills().items()]


class CommandBody(BaseModel):
    command: str
    token: str = ""


class ChatBody(BaseModel):
    message: str
    token: str = ""


def has_token(body_token, header_token):
    return (body_token or header_token or "") == API_TOKEN


@app.post("/api/command")
def command(body: CommandBody, x_token: str = Header(None)):
    t = body.command.strip()
    low = t.lower()
    if low.startswith("shell "):
        if not has_token(body.token, x_token):
            return {"ok": False, "reply": "Invalid token for shell access."}
        return {"ok": True, "reply": shell_run(t[6:].strip())}
    if low.startswith("say "):
        r = say(t[4:].strip())
        return {"ok": True, "reply": r or "espeak-ng not installed"}
    reply = route(t)
    return {"ok": True, "reply": reply}


@app.post("/api/chat")
def chat(body: ChatBody):
    msg = body.message.strip()
    if not msg:
        return {"ok": False, "reply": "Empty message."}
    HISTORY.append(("user", msg))
    save_memory("user", msg)
    ans = ask_llm(msg)
    if ans is None:
        ans = "Ollama is offline. Start it with: nohup ollama serve > /tmp/ollama.log 2>&1 &"
    HISTORY.append(("jarvis", ans))
    save_memory("jarvis", ans)
    return {"ok": True, "reply": ans}


@app.post("/api/update")
def update(body: ChatBody, x_token: str = Header(None)):
    if not has_token(body.token, x_token):
        return {"ok": False, "reply": "Invalid token."}
    out = shell_run("bash " + os.path.join(APP_DIR, "update.sh"), timeout=120)
    return {"ok": True, "reply": out}


UI = r"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>JARVIS Core</title>
<style>
  * { box-sizing:border-box; margin:0; padding:0; }
  body { background:#05080c; color:#33d17a; font-family:ui-monospace,Menlo,Consolas,monospace; height:100vh; display:flex; flex-direction:column; }
  #top { display:flex; align-items:center; gap:10px; padding:10px 14px; border-bottom:1px solid #123; background:#071019; }
  #top .title { font-weight:bold; letter-spacing:2px; font-size:13px; }
  .tbtn { border:1px solid #155; background:transparent; color:#66e0ff; border-radius:12px; padding:5px 12px; font-family:inherit; font-size:12px; cursor:pointer; }
  .tbtn.on { background:#0f2; color:#002; border-color:#0f2; font-weight:bold; }
  .grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:8px; padding:10px 14px; }
  .card { background:#0b141c; border:1px solid #123; border-radius:8px; padding:10px; }
  .card h2 { font-size:10px; color:#3d7a5a; text-transform:uppercase; letter-spacing:1px; }
  .card .val { font-size:14px; margin-top:4px; }
  #skills { display:flex; flex-wrap:wrap; gap:6px; padding:4px 14px 8px; }
  .chip { border:1px solid #155; border-radius:12px; padding:3px 10px; font-size:11px; color:#66e0ff; cursor:pointer; background:transparent; font-family:inherit; }
  #log { flex:1; overflow-y:auto; padding:12px 16px; white-space:pre-wrap; font-size:13px; line-height:1.5; }
  .line { margin-bottom:6px; }
  .u { color:#66e0ff; }
  .j { color:#33d17a; }
  .e { color:#ff6b6b; }
  #bar { display:flex; border-top:1px solid #123; background:#071019; }
  #inp { flex:1; background:transparent; border:none; outline:none; color:#33d17a; font-family:inherit; font-size:14px; padding:14px; }
  #btn { background:#0f2; color:#002; border:none; padding:0 18px; font-family:inherit; font-weight:bold; font-size:13px; }
</style>
</head>
<body>
<div id="top">
  <span class="title">JARVIS CORE v5</span>
  <button class="tbtn" id="speak">voice: OFF</button>
  <button class="tbtn" id="upd">update</button>
</div>
<div class="grid">
  <div class="card"><h2>Status</h2><div class="val" id="st">...</div></div>
  <div class="card"><h2>Uptime</h2><div class="val" id="up">...</div></div>
  <div class="card"><h2>Memory</h2><div class="val" id="mem">...</div></div>
  <div class="card"><h2>Disk</h2><div class="val" id="dsk">...</div></div>
  <div class="card"><h2>IP</h2><div class="val" id="ip">...</div></div>
</div>
<div id="skills"></div>
<div id="log"><div class="line j">JARVIS Core online. Type 'help' or just talk.</div></div>
<div id="bar">
  <input id="inp" placeholder="jarvis> " autocomplete="off" />
  <button id="btn" onclick="submit()">SEND</button>
</div>
<script>
var log = document.getElementById('log');
var inp = document.getElementById('inp');
var speakOn = false;
function print(cls, text){
  var d = document.createElement('div');
  d.className = 'line ' + cls;
  d.textContent = text;
  log.appendChild(d);
  log.scrollTop = log.scrollHeight;
}
function speak(text){
  if(!('speechSynthesis' in window)) return;
  try{
    window.speechSynthesis.cancel();
    var u = new SpeechSynthesisUtterance(text.replace(/[#*_`>]/g, ''));
    u.rate = 1.05;
    window.speechSynthesis.speak(u);
  }catch(e){}
}
function send(cmd){
  print('u', 'you> ' + cmd);
  fetch('/api/command', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({command: cmd, token: localStorage.getItem('token') || ''})
  })
  .then(function(r){ return r.json(); })
  .then(function(d){
    var reply = d.reply || JSON.stringify(d);
    print('j', 'jarvis> ' + reply);
    if(speakOn) speak(reply);
  })
  .catch(function(e){ print('e', 'error: ' + e); });
}
function submit(){
  var v = inp.value.trim();
  if(!v) return;
  inp.value = '';
  send(v);
}
inp.addEventListener('keydown', function(e){ if(e.key === 'Enter') submit(); });
document.getElementById('speak').addEventListener('click', function(){
  speakOn = !speakOn;
  this.textContent = speakOn ? 'voice: ON' : 'voice: OFF';
  this.classList.toggle('on', speakOn);
  if(speakOn) speak('Voice enabled.');
});
document.getElementById('upd').addEventListener('click', function(){
  var tk = prompt('Update token:') || '';
  fetch('/api/update', {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({message:'', token: tk})})
    .then(function(r){ return r.json(); })
    .then(function(d){ print('j', 'update> ' + d.reply); });
});
function refresh(){
  fetch('/api/system').then(function(r){ return r.json(); }).then(function(s){
    document.getElementById('st').textContent = 'ONLINE';
    document.getElementById('up').textContent = s.uptime_seconds + 's';
    document.getElementById('mem').textContent = s.memory.percent + '%';
    document.getElementById('dsk').textContent = s.disk.used + ' / ' + s.disk.total;
  }).catch(function(){ document.getElementById('st').textContent = 'OFFLINE'; });
  fetch('/api/network').then(function(r){ return r.json(); }).then(function(n){
    document.getElementById('ip').textContent = n.lan_ip;
  }).catch(function(){});
}
fetch('/api/skills').then(function(r){ return r.json(); }).then(function(list){
  var box = document.getElementById('skills');
  list.forEach(function(s){
    var b = document.createElement('button');
    b.className = 'chip';
    b.textContent = s.name;
    b.title = s.description;
    b.onclick = function(){ send(s.name); };
    box.appendChild(b);
  });
});
refresh();
setInterval(refresh, 5000);
</script>
</body>
</html>
"""


if __name__ == "__main__":
    import uvicorn
    load_memory()
    uvicorn.run(app, host="0.0.0.0", port=8000)
