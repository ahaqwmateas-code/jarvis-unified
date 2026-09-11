import os, shutil, platform, socket, time

SKILL = {
    "name": "system",
    "description": "system status: CPU, RAM, disk, load",
    "keywords": ["system", "status", "memory", "ram", "disk", "cpu", "load", "specs"],
}

START = time.time()


def fmt(b):
    for u in ("B", "KB", "MB", "GB", "TB"):
        if b < 1024:
            return f"{b:.1f} {u}"
        b /= 1024
    return f"{b:.1f} PB"


def run(args, ctx):
    total = used = 0
    try:
        with open("/proc/meminfo") as f:
            vals = {}
            for line in f:
                p = line.split()
                if len(p) >= 2:
                    vals[p[0].rstrip(":")] = int(p[1]) * 1024
            total = vals.get("MemTotal", 0)
            free = vals.get("MemFree", 0) + vals.get("Buffers", 0) + vals.get("Cached", 0)
            used = total - free
    except Exception:
        pass
    d = shutil.disk_usage("/")
    load = "?"
    try:
        load = open("/proc/loadavg").read().split()[:3]
    except Exception:
        pass
    return "\n".join([
        "Hostname:  " + socket.gethostname(),
        "Platform:  " + platform.platform(),
        "CPU cores: " + str(os.cpu_count()),
        "Load avg:  " + str(load),
        "Memory:    " + fmt(used) + " / " + fmt(total) + " (" + str(round(used / total * 100, 1) if total else 0) + "%)",
        "Disk:      " + fmt(d.used) + " used / " + fmt(d.total) + " (" + fmt(d.free) + " free)",
        "Uptime:    " + str(int(time.time() - START)) + "s (this process)",
    ])
