import os, json, ssl, urllib.request

SKILL = {
    "name": "composio",
    "description": "composio key <ck_...> | composio status | composio tools | composio run <TOOL> <json>",
    "keywords": ["composio", "mcp", "integrations", "connected apps", "composio tools"],
}

MCP_URL = "https://connect.composio.dev/mcp"
CFG = os.path.expanduser("~/.jarvis/composio.json")

CLIENT_INFO = {"name": "jarvis", "version": "1.0"}
PROTO = "2024-11-05"


def _load():
    try:
        with open(CFG) as f:
            return json.load(f)
    except Exception:
        return {}


def _save(d):
    try:
        with open(CFG, "w") as f:
            json.dump(d, f)
    except Exception:
        pass


def _post(payload, key, session_id=None):
    req = urllib.request.Request(MCP_URL, data=json.dumps(payload).encode("utf-8"), method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json, text/event-stream")
    if key:
        req.add_header("Authorization", "Bearer " + key)
        req.add_header("x-consumer-api-key", key)
    if session_id:
        req.add_header("Mcp-Session-Id", session_id)
    ctx = ssl.create_default_context()
    with urllib.request.urlopen(req, timeout=90, context=ctx) as r:
        body = r.read().decode("utf-8", "replace")
        return body, r.headers.get("Mcp-Session-Id"), r.headers.get("Content-Type", "")


def _parse(body, ctype):
    if "event-stream" in ctype:
        for line in body.splitlines():
            if line.startswith("data:"):
                try:
                    return json.loads(line[5:].strip())
                except Exception:
                    pass
        return None
    try:
        return json.loads(body)
    except Exception:
        return {"error": body[:300]}


def _rpc(key, method, params, rid=2):
    # initialize handshake (streamable HTTP)
    body, sid, ctype = _post({
        "jsonrpc": "2.0", "id": 1, "method": "initialize",
        "params": {"protocolVersion": PROTO, "capabilities": {}, "clientInfo": CLIENT_INFO},
    }, key, None)
    _parse(body, ctype)
    # initialized notification
    try:
        _post({"jsonrpc": "2.0", "method": "notifications/initialized"}, key, sid)
    except Exception:
        pass
    body2, sid2, ctype2 = _post({"jsonrpc": "2.0", "id": rid, "method": method, "params": params or {}}, key, sid or sid2)
    return _parse(body2, ctype2)


def run(args, ctx):
    args = (args or "").strip()
    parts = args.split(None, 2)
    cmd = parts[0].lower() if parts else ""
    rest = parts[1] if len(parts) > 1 else ""

    if not args:
        return ("Composio - 1,500+ app tools (Gmail, GitHub, Calendar, Notion, Slack...) via MCP.\n"
                "Setup:\n"
                "  1. composio key <ck_...>   (get it at dashboard.composio.dev)\n"
                "  2. connect apps on the dashboard\n"
                "  3. composio status | composio tools | composio run <TOOL> <json>")

    if cmd == "key":
        k = rest.strip()
        if not k:
            return "Usage: composio key <ck_...>   (from dashboard.composio.dev)"
        _save({"key": k})
        return "Composio key saved. Checking connection...\n" + status()

    key = _load().get("key", "")
    if not key:
        return "No Composio key set.\nRun: composio key <ck_...>  (dashboard.composio.dev)"

    if cmd == "status":
        return status(key)
    if cmd == "tools":
        return tools(key)
    if cmd == "run":
        tool = parts[1] if len(parts) > 1 else ""
        payload = parts[2] if len(parts) > 2 else "{}"
        if not tool:
            return "Usage: composio run <TOOL_NAME> <json-args>   (see composio tools)"
        return call_tool(key, tool, payload)
    if cmd == "help":
        return ("composio key <ck_...>   save key\n"
                "composio status          apps + tool count\n"
                "composio tools           list tool names\n"
                "composio run <TOOL> <json>  run a tool, e.g. composio run GITHUB_SEARCH_REPOSITORIES {\"query\":\"jarvis\"}")
    return "Unknown composio command. Try: composio help"


def status(key=None):
    key = key or _load().get("key", "")
    r = _rpc(key, "tools/list", {})
    if "error" in (r or {}):
        return "Composio connection failed: " + str(r.get("error"))[:200]
    tools = ((r or {}).get("result") or {}).get("tools") or []
    apps = {}
    for t in tools:
        name = t.get("name", "")
        app = name.split("_")[0].lower() if name else "?"
        apps[app] = apps.get(app, 0) + 1
    line = ", ".join(sorted(apps)) if apps else "(none)"
    return ("Composio ONLINE\n"
            "tools: %d\napps: %s\n"
            "connect more apps at dashboard.composio.dev" % (len(tools), line))


def tools(key=None):
    key = key or _load().get("key", "")
    r = _rpc(key, "tools/list", {})
    if "error" in (r or {}):
        return "Connection failed: " + str(r.get("error"))[:200]
    ts = ((r or {}).get("result") or {}).get("tools") or []
    if not ts:
        return "No tools yet - connect an app at dashboard.composio.dev first."
    out = []
    for t in ts[:80]:
        desc = (t.get("description") or "").replace("\n", " ")[:70]
        out.append(t.get("name", "") + "  -  " + desc)
    return "\n".join(out)


def call_tool(key, tool, payload):
    try:
        arguments = json.loads(payload)
    except Exception:
        arguments = {"_raw": payload}
    r = _rpc(key, "tools/call", {"name": tool, "arguments": arguments}, rid=3)
    if not r:
        return "No response from Composio."
    if r.get("error"):
        return "MCP error: " + str(r.get("error"))[:300]
    result = r.get("result") or {}
    if result.get("isError"):
        return "Tool error: " + json.dumps(result.get("content", result))[:500]
    content = result.get("content") or []
    texts = []
    for item in content:
        if isinstance(item, dict) and item.get("type") == "text":
            texts.append(item.get("text", ""))
        elif isinstance(item, str):
            texts.append(item)
    out = "\n".join(t for t in texts if t)
    return out if out.strip() else json.dumps(result, indent=2)[:800]
