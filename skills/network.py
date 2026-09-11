import socket, subprocess, shlex

SKILL = {
    "name": "network",
    "description": "LAN IP, hostname, ping, DNS lookup",
    "keywords": ["ip", "network", "ping", "hostname", "dns", "lookup"],
}


def lan_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def run(args, ctx):
    a = args.strip()
    if a.startswith("ping "):
        host = a[5:].strip()
        try:
            r = subprocess.run(["ping", "-c", "4", "-W", "3", host],
                               capture_output=True, text=True, timeout=20)
            return (r.stdout + r.stderr).strip()
        except Exception as e:
            return "ping error: " + str(e)
    if a.startswith("dns ") or a.startswith("lookup "):
        host = a.split(" ", 1)[1].strip()
        try:
            return host + " -> " + socket.gethostbyname(host)
        except Exception as e:
            return "lookup error: " + str(e)
    return "LAN IP: " + lan_ip() + "\nHostname: " + socket.gethostname()
