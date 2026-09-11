package com.jarvis.assistant;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Minimal MCP (Model Context Protocol) client over Streamable HTTP.
 *  Talks to Composio's hosted MCP endpoint: https://connect.composio.dev/mcp */
public class Mcp {
    public static final String URL_MCP = "https://connect.composio.dev/mcp";

    public static class Tool { public String name; public String description; }

    private final String key;

    public Mcp(String key) { this.key = key == null ? "" : key.trim(); }

    public String getKey() { return key; }

    private static class Resp { JSONObject json; String sid; }

    private Resp post(JSONObject payload, String sid) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(URL_MCP).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(90000);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json, text/event-stream");
        if (!key.isEmpty()) {
            c.setRequestProperty("Authorization", "Bearer " + key);
            c.setRequestProperty("x-consumer-api-key", key);
        }
        if (sid != null) c.setRequestProperty("Mcp-Session-Id", sid);
        c.setDoOutput(true);
        OutputStream os = c.getOutputStream();
        os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        os.close();

        int code = c.getResponseCode();
        InputStream in = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
        String body = "";
        if (in != null) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            in.close();
            body = new String(b.toByteArray(), StandardCharsets.UTF_8);
        }
        Resp r = new Resp();
        r.sid = c.getHeaderField("Mcp-Session-Id");
        r.json = parseBody(body, c.getContentType());
        return r;
    }

    private static JSONObject parseBody(String body, String ctype) {
        if (ctype != null && ctype.contains("event-stream")) {
            for (String line : body.split("\n")) {
                if (line.startsWith("data:")) {
                    try { return new JSONObject(line.substring(5).trim()); }
                    catch (Exception e) { }
                }
            }
            return new JSONObject();
        }
        try { return new JSONObject(body); }
        catch (Exception e) {
            JSONObject err = new JSONObject();
            try { err.put("error", body == null ? "empty" : body.substring(0, Math.min(200, body.length()))); }
            catch (Exception e2) { }
            return err;
        }
    }

    private JSONObject rpc(String method, JSONObject params, int id) throws Exception {
        JSONObject init = new JSONObject()
                .put("jsonrpc", "2.0").put("id", 1).put("method", "initialize")
                .put("params", new JSONObject()
                        .put("protocolVersion", "2024-11-05")
                        .put("capabilities", new JSONObject())
                        .put("clientInfo", new JSONObject().put("name", "jarvis").put("version", "1.0")));
        Resp r1 = post(init, null);
        try {
            post(new JSONObject().put("jsonrpc", "2.0").put("method", "notifications/initialized"), r1.sid);
        } catch (Exception ignored) { }
        JSONObject call = new JSONObject().put("jsonrpc", "2.0").put("id", id).put("method", method);
        if (params != null) call.put("params", params); else call.put("params", new JSONObject());
        Resp r2 = post(call, r1.sid);
        return r2.json;
    }

    /** Lists tools; on any failure returns null. */
    public List<Tool> listTools() {
        List<Tool> out = new ArrayList<Tool>();
        try {
            JSONObject r = rpc("tools/list", new JSONObject(), 2);
            if (r.has("error")) return null;
            JSONArray tools = r.optJSONObject("result") == null ? null : r.optJSONObject("result").optJSONArray("tools");
            if (tools == null) return null;
            for (int i = 0; i < tools.length(); i++) {
                JSONObject t = tools.optJSONObject(i);
                if (t == null) continue;
                Tool tool = new Tool();
                tool.name = t.optString("name", "");
                tool.description = t.optString("description", "");
                out.add(tool);
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    /** Calls a tool; returns the text result or an error string. */
    public String callTool(String name, String argsJson) {
        try {
            JSONObject arguments;
            try { arguments = new JSONObject(argsJson); }
            catch (Exception e) { arguments = new JSONObject().put("_raw", argsJson); }
            JSONObject r = rpc("tools/call", new JSONObject().put("name", name).put("arguments", arguments), 3);
            if (r.has("error")) return "MCP error: " + r.opt("error");
            JSONObject result = r.optJSONObject("result");
            if (result == null) return "No result from Composio.";
            if (result.optBoolean("isError", false)) return "Tool error: " + result.optString("content");
            JSONArray content = result.optJSONArray("content");
            StringBuilder sb = new StringBuilder();
            if (content != null) {
                for (int i = 0; i < content.length(); i++) {
                    Object item = content.opt(i);
                    if (item instanceof JSONObject && ((JSONObject) item).optString("type", "").equals("text")) {
                        sb.append(((JSONObject) item).optString("text", "")).append("\n");
                    } else if (item instanceof String) {
                        sb.append((String) item).append("\n");
                    }
                }
            }
            String s = sb.toString().trim();
            return s.isEmpty() ? result.toString() : s;
        } catch (Exception e) {
            return "Call failed: " + e.getMessage();
        }
    }
}
