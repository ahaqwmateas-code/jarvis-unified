import os, subprocess

SKILL = {
    "name": "clihub",
    "description": "clihub setup / list / search <x> / install <tool> / launch <tool> ... (CLI-Anything hub)",
    "keywords": ["clihub", "cli-anything", "cli anything"],
}

BIN = os.environ.get("CLIHUB_BIN", "/usr/local/bin/cli-hub")
VENV = "/opt/cli-hub-venv"

USAGE = ("CLI-Anything Hub (40+ agent-native CLIs). Commands:\n"
         "  clihub setup            - install the hub (one time)\n"
         "  clihub list             - list all available CLIs\n"
         "  clihub search <term>    - e.g. clihub search gimp\n"
         "  clihub install <tool>   - e.g. clihub install comfyui\n"
         "  clihub launch <tool>    - run an installed CLI\n"
         "  clihub info <tool>      - details about a CLI")


def _bootstrap():
    if os.path.exists(BIN):
        return None
    r = subprocess.run(["python3", "-m", "venv", VENV], capture_output=True, text=True)
    if r.returncode != 0:
        return "venv failed (install python3-full first): " + (r.stderr or "")[-200:]
    subprocess.run([VENV + "/bin/pip", "install", "-q", "-U", "pip"],
                   capture_output=True, text=True)
    r = subprocess.run([VENV + "/bin/pip", "install", "cli-anything-hub"],
                       capture_output=True, text=True)
    if r.returncode != 0:
        return "pip install failed: " + (r.stderr or "")[-300:]
    try:
        os.symlink(VENV + "/bin/cli-hub", BIN)
    except FileExistsError:
        pass
    return None


def run(args, ctx):
    a = args.strip()
    low = a.lower()

    if low in ("", "help"):
        return USAGE

    if low in ("setup", "install-hub"):
        err = _bootstrap()
        if err:
            return err
        return "CLI-Hub ready. Now try: clihub list"

    if low in ("list", "search", "info", "can", "matrix", "previews") or \
       low.startswith(("search ", "info ", "can ", "matrix ", "previews ")):
        err = _bootstrap()
        if err:
            return err
        r = subprocess.run([BIN] + a.split(), capture_output=True, text=True, timeout=120)
        return (r.stdout or r.stderr or "(no output)").strip()[-3500:]

    if low.startswith(("install ", "uninstall ", "update ", "launch ")):
        err = _bootstrap()
        if err:
            return err
        r = subprocess.run([BIN] + a.split(), capture_output=True, text=True, timeout=600)
        return (r.stdout or r.stderr or "(no output)").strip()[-3500:]

    return USAGE
