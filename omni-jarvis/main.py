from fastapi import FastAPI, Header
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from datetime import datetime
import os, platform, shutil, socket, time, json, subprocess, shlex

app = FastAPI(title="OMNI-JARVIS", version="4.0.0")

START_TIME = time.time()

# Change this to something private. Needed only for the "shell" command.
API_TOKEN = "jarvis"

# --- Ollama (local LLM) config -------------------------------------------
OLLAMA_URL = os.environ.get("OLLAMA_URL", "http://127.0.0.1:11434")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "llama3.2")

JARVIS_SYS = (
    "You are JARVIS, a concise and helpful AI assistant running on a Kali Linux "
    "box. Answer briefly and directly, in a dry but friendly tone. Keep answers "
    "short unless detail is asked for."
)


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


def get_memory():
    total = used = 0
    meminfo = read_file("/proc/meminfo")
    if meminfo:
        vals = {}
        for line in meminfo.splitlines():
            parts = line.split()
            if len(parts) >= 2:
                vals[parts[0].rstrip(":")] = int(parts[1]) * 1024
        total = vals.get("MemTotal", 0)
        free = vals.get("MemFree", 0) + vals.get("Buffers", 0) + vals.get("Cached", 0)
        used = total - free
    return total, used


def fmt_bytes(b):
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if b < 1024:
            return f"{b:.1f} {unit}"
        b /= 1024
    return f"{b:.1f} PB"


def shell_run(cmd, timeout=15):
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
        return f"Speaking: {text}"
    except Exception as e:
        return f"TTS error: {e}"


def ollama_models():
    try:
        import requests
        r = requests.get(f"{OLLAMA_URL}/api/tags", timeout=3)
        if r.status_code == 200:
            return [m["name"] for m in r.json().get("models", [])]
    except Exception:
        pass
    return None


def ask_jarvis(prompt, model=None):
    try:
        import requests
        r = requests.post(
            f"{OLLAMA_URL}/api/generate",
            json={
                "model": model or OLLAMA_MODEL,
                "system": JARVIS_SYS,
                "prompt": prompt,
                "stream": False,
            },
            timeout=120,
        )
        if r.status_code == 200:
            return r.json().get("response", "").strip()
    except Exception:
        pass
    return None


HELP = """Available commands:
  help                 - this list
  ask <question>       - ask the local AI (Ollama)
  llm                  - is Ollama online / which models
  status / system      - full system report
  ip / network         - LAN IP and hostname
  time / date          - current date & time
  uptime               - server uptime
  memory               - RAM usage
  disk                 - disk usage
  ping <host>          - ping a host (4 packets)
  ls [path]            - list files
  say <text>           - speak via espeak-ng
  shell <cmd>          - run raw command (requires token)

Anything else is sent to the AI if Ollama is running."""


def handle_command(text):
    t = text.strip()
    low = t.lower()
    if not t:
        return "Type 'help' for commands, or ask me anything."
    if low in ("help", "?"):
        return HELP
    if low in ("status", "system"):
        return json.dumps(system(), indent=2)
    if low in ("ip", "network"):
        return json.dumps(network(), indent=2)
    if low in ("time", "date", "now"):
        return datetime.now().strftime("%A %d %B %Y, %H:%M:%S")
    if low == "uptime":
        return f"Up {int(time.time() - START_TIME)} seconds"
    if low == "memory":
        total, used = get_memory()
        return f"{fmt_bytes(used)} / {fmt_bytes(total)} ({round(used/total*100, 1) if total else 0}%)"
    if low == "disk":
        d = shutil.disk_usage("/")
        return f"used {fmt_bytes(d.used)} / {fmt_bytes(d.total)} (free {fmt_bytes(d.free)})"
    if low == "whoami":
        return shell_run("whoami")
    if low == "hostname":
        return socket.gethostname()
    if low.startswith("ping "):
        host = t[5:].strip()
        return shell_run(f"ping -c 4 -W 3 {shlex.quote(host)}")
    if low == "ls" or low.startswith("ls "):
        path = t[3:].strip() or "~"
        return shell_run(f"ls -lah {shlex.quote(path)}")
    if low.startswith("say "):
        r = say(t[4:].strip())
        return r or "espeak-ng not installed. Run: sudo apt install espeak-ng"
    if low in ("llm", "llm status"):
        models = ollama_models()
        if models is None:
            return "Ollama is OFFLINE. Start it: nohup ollama serve > /tmp/ollama.log 2>&1 &"
        return "Ollama ONLINE. Models: " + ", ".join(models)
    if low.startswith("ask "):
        ans = ask_jarvis(t[4:].strip())
        if ans is None:
            return "Ollama is not reachable. Start it with: nohup ollama serve > /tmp/ollama.log 2>&1 &"
        return ans
    ans = ask_jarvis(t)
    if ans is not None:
        return ans
    return f"Unknown command: {t}\nType 'help' for commands, or start Ollama for free-form answers."


@app.get("/")
def root():
    return {
        "status": "online",
        "service": "OMNI-JARVIS",
        "version": app.version,
        "timestamp": datetime.now().isoformat(),
    }


@app.get("/health")
def health():
    return {"healthy": True, "uptime_seconds": int(time.time() - START_TIME)}


@app.get("/status")
def status():
    return {
        "system": "operational",
        "api": "ready",
        "endpoints": ["/", "/health", "/status", "/system", "/network", "/dashboard", "/jarvis", "/command", "/chat"],
    }


@app.get("/system")
def system():
    total_mem, used_mem = get_memory()
    load = read_file("/proc/loadavg")
    disk = shutil.disk_usage("/")
    return {
        "hostname": socket.gethostname(),
        "platform": platform.platform(),
        "python": platform.python_version(),
        "cpu_cores": os.cpu_count(),
        "load_avg": load,
        "uptime_seconds": int(time.time() - START_TIME),
        "memory": {
            "total": fmt_bytes(total_mem),
            "used": fmt_bytes(used_mem),
            "percent": round(used_mem / total_mem * 100, 1) if total_mem else 0,
        },
        "disk": {
            "total": fmt_bytes(disk.total),
            "used": fmt_bytes(disk.used),
            "free": fmt_bytes(disk.free),
        },
        "timestamp": datetime.now().isoformat(),
    }


@app.get("/network")
def network():
    return {
        "lan_ip": get_lan_ip(),
        "hostname": socket.gethostname(),
    }


class CommandBody(BaseModel):
    command: str
    token: str = ""


class ChatBody(BaseModel):
    prompt: str
    model: str = ""


@app.post("/command")
def command(body: CommandBody, x_token: str = Header(None)):
    t = body.command.strip()
    if t.lower().startswith("shell "):
        if (body.token or x_token or "") != API_TOKEN:
            return {"ok": False, "reply": "Invalid token for shell access."}
        return {"ok": True, "reply": shell_run(t[6:].strip())}
    return {"ok": True, "reply": handle_command(t)}


@app.post("/chat")
def chat(body: ChatBody):
    ans = ask_jarvis(body.prompt.strip(), body.model or None)
    if ans is None:
        return {"ok": False, "reply": "Ollama unreachable. Run: nohup ollama serve > /tmp/ollama.log 2>&1 &"}
    return {"ok": True, "reply": ans}


DASHBOARD = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>OMNI-JARVIS</title>
<style>
  :root { --bg:#0b0f14; --card:#151b23; --line:#232b36; --txt:#d7dee8; --dim:#7d8898; --acc:#33d17a; }
  * { box-sizing:border-box; margin:0; padding:0; }
  body { background:var(--bg); color:var(--txt); font-family:ui-monospace,Menlo,Consolas,monospace; padding:24px; }
  header { display:flex; align-items:center; gap:12px; margin-bottom:20px; }
  .dot { width:10px; height:10px; border-radius:50%; background:var(--acc); box-shadow:0 0 12px var(--acc); animation:pulse 2s infinite; }
  @keyframes pulse { 50% { opacity:.4; } }
  h1 { font-size:20px; letter-spacing:2px; }
  .sub { color:var(--dim); font-size:12px; }
  .grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:14px; }
  .card { background:var(--card); border:1px solid var(--line); border-radius:10px; padding:16px; }
  .card h2 { font-size:11px; color:var(--dim); text-transform:uppercase; letter-spacing:1px; margin-bottom:10px; }
  .card .val { font-size:18px; }
  .bar { background:#0e141c; border-radius:6px; height:8px; margin-top:10px; overflow:hidden; }
  .bar > span { display:block; height:100%; background:var(--acc); }
  table { width:100%; border-collapse:collapse; margin-top:14px; }
  td { padding:8px 10px; border-bottom:1px solid var(--line); font-size:13px; }
  td:first-child { color:var(--dim); width:160px; }
  .foot { color:var(--dim); font-size:11px; margin-top:18px; }
  .nav a { color:var(--acc); margin-right:14px; font-size:13px; }
</style>
</head>
<body>
<header><span class="dot"></span><h1>OMNI-JARVIS</h1><span class="sub">local control panel</span></header>
<div class="nav"><a href="/jarvis">Jarvis terminal →</a></div>
<div class="grid">
  <div class="card"><h2>Status</h2><div class="val" id="status">...</div></div>
  <div class="card"><h2>Uptime</h2><div class="val" id="uptime">...</div></div>
  <div class="card"><h2>Memory</h2><div class="val" id="mem">...</div><div class="bar"><span id="membar" style="width:0%"></span></div></div>
  <div class="card"><h2>Disk</h2><div class="val" id="disk">...</div><div class="bar"><span id="diskbar" style="width:0%"></span></div></div>
  <div class="card"><h2>CPU Cores</h2><div class="val" id="cpu">...</div></div>
  <div class="card"><h2>Load Avg</h2><div class="val" id="load">...</div></div>
</div>
<table id="meta"></table>
<div class="foot">OMNI-JARVIS v4.0 &middot; served from FastAPI on <span id="ip">...</span></div>
<script>
async function refresh(){
  try{
    const s = await (await fetch('/system')).json();
    document.getElementById('status').textContent = 'ONLINE';
    document.getElementById('uptime').textContent = s.uptime_seconds + ' s';
    document.getElementById('mem').textContent = s.memory.used + ' / ' + s.memory.total + ' (' + s.memory.percent + '%)';
    document.getElementById('membar').style.width = s.memory.percent + '%';
    const du = s.disk;
    const dp = Math.round(du.used / ((du.used + du.free) || 1) * 100);
    document.getElementById('disk').textContent = du.used + ' used / ' + du.total;
    document.getElementById('diskbar').style.width = dp + '%';
    document.getElementById('cpu').textContent = s.cpu_cores;
    document.getElementById('load').textContent = s.load_avg;
    document.getElementById('meta').innerHTML =
      '<tr><td>Hostname</td><td>'+s.hostname+'</td></tr>'+
      '<tr><td>Platform</td><td>'+s.platform+'</td></tr>'+
      '<tr><td>Python</td><td>'+s.python+'</td></tr>'+
      '<tr><td>Updated</td><td>'+s.timestamp+'</td></tr>';
  }catch(e){ document.getElementById('status').textContent = 'OFFLINE'; }
  try{
    const n = await (await fetch('/network')).json();
    document.getElementById('ip').textContent = n.lan_ip;
  }catch(e){}
}
refresh();
setInterval(refresh, 3000);
</script>
</body>
</html>
"""


JARVIS_UI = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>JARVIS Terminal</title>
<style>
  * { box-sizing:border-box; margin:0; padding:0; }
  body { background:#05080c; color:#33d17a; font-family:ui-monospace,Menlo,Consolas,monospace; height:100vh; display:flex; flex-direction:column; }
  #top { display:flex; align-items:center; gap:10px; padding:10px 14px; border-bottom:1px solid #123; background:#071019; }
  #top .title { font-weight:bold; letter-spacing:1px; font-size:13px; }
  .tbtn { border:1px solid #155; background:transparent; color:#66e0ff; border-radius:12px; padding:5px 12px; font-family:inherit; font-size:12px; cursor:pointer; }
  .tbtn.on { background:#0f2; color:#002; border-color:#0f2; font-weight:bold; }
  .tbtn.off { color:#4a5a68; border-color:#1c2a35; }
  #log { flex:1; overflow-y:auto; padding:18px; white-space:pre-wrap; font-size:13px; line-height:1.5; }
  .line { margin-bottom:8px; }
  .u { color:#66e0ff; }
  .j { color:#33d17a; }
  #bar { display:flex; border-top:1px solid #123; background:#071019; }
  #inp { flex:1; background:transparent; border:none; outline:none; color:#33d17a; font-family:inherit; font-size:14px; padding:14px; }
  #btn { background:#0f2; color:#002; border:none; padding:0 18px; font-family:inherit; font-weight:bold; font-size:13px; }
  .chip { display:inline-block; border:1px solid #155; border-radius:12px; padding:3px 10px; margin:0 6px 6px 0; font-size:11px; color:#66e0ff; cursor:pointer; }
  #chips { padding:8px 16px; border-bottom:1px solid #123; }
  .dim { color:#3d7a5a; }
</style>
</head>
<body>
<div id="top">
  <span class="title">JARVIS</span>
  <button class="tbtn" id="speak" title="Read replies aloud">voice: OFF</button>
  <button class="tbtn off" id="mic" title="Speak a command">&#127908; mic</button>
</div>
<div id="log">
  <div class="line dim">OMNI-JARVIS v4.0 &middot; type a command or just ask me anything.</div>
</div>
<div id="chips">
  <span class="chip" onclick="send('status')">status</span>
  <span class="chip" onclick="send('ip')">ip</span>
  <span class="chip" onclick="send('memory')">memory</span>
  <span class="chip" onclick="send('time')">time</span>
  <span class="chip" onclick="send('llm')">llm</span>
  <span class="chip" onclick="send('ask write a haiku about a router')">ask: haiku</span>
  <span class="chip" onclick="send('help')">help</span>
</div>
<div id="bar">
  <input id="inp" placeholder="jarvis> ask me anything" autocomplete="off" />
  <button id="btn" onclick="submit()">SEND</button>
</div>
<script>
const log = document.getElementById('log');
const inp = document.getElementById('inp');
let speakOn = false;

function print(cls, text){
  const d = document.createElement('div');
  d.className = 'line ' + cls;
  d.textContent = text;
  log.appendChild(d);
  log.scrollTop = log.scrollHeight;
}

function speak(text){
  if(!('speechSynthesis' in window)) return;
  try{
    window.speechSynthesis.cancel();
    const clean = text.replace(/[#*_`>]/g, '');
    const u = new SpeechSynthesisUtterance(clean);
    u.rate = 1.05;
    window.speechSynthesis.speak(u);
  }catch(e){}
}

function send(cmd){
  print('u', 'you> ' + cmd);
  fetch('/command', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({command: cmd, token: localStorage.getItem('token') || ''})
  })
  .then(r => r.json())
  .then(d => {
    const reply = d.reply || JSON.stringify(d);
    print('j', 'jarvis> ' + reply);
    if(speakOn) speak(reply);
  })
  .catch(e => print('j', 'jarvis> error: ' + e));
}

function submit(){
  const v = inp.value.trim();
  if(!v) return;
  inp.value = '';
  send(v);
}

inp.addEventListener('keydown', e => { if(e.key === 'Enter') submit(); });

const speakBtn = document.getElementById('speak');
speakBtn.addEventListener('click', () => {
  speakOn = !speakOn;
  speakBtn.textContent = speakOn ? 'voice: ON' : 'voice: OFF';
  speakBtn.classList.toggle('on', speakOn);
  if(speakOn) speak('Voice enabled.');
});

const micBtn = document.getElementById('mic');
const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
if(!SR || !window.isSecureContext){
  micBtn.title = 'Mic needs HTTPS (browsers block mic on http:// LAN addresses)';
  micBtn.onclick = () => alert('Voice input needs a secure HTTPS connection.\\nAsk to enable HTTPS for JARVIS, or type instead.');
} else {
  micBtn.classList.remove('off');
  const rec = new SR();
  rec.lang = 'en-US';
  rec.interimResults = false;
  rec.onresult = (e) => { inp.value = e.results[0][0].transcript; submit(); };
  rec.onend = () => micBtn.classList.remove('on');
  micBtn.onclick = () => { micBtn.classList.add('on'); try { rec.start(); } catch(e){} };
}
</script>
</body>
</html>
"""


@app.get("/dashboard", response_class=HTMLResponse)
def dashboard():
    return DASHBOARD


@app.get("/jarvis", response_class=HTMLResponse)
def jarvis():
    return JARVIS_UI


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
