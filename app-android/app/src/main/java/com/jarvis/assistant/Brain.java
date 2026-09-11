package com.jarvis.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.List;

/**
 * Multi-provider AI brain with automatic failover.
 * Tries, in order, every provider the user configured in Settings,
 * then falls back to the anonymous free brain (Pollinations) so the app
 * always answers even with no API key.
 */
public class Brain {
    public static class Result { public String text; public String provider; }

    private final SharedPreferences prefs;

    public Brain(Context c) { prefs = c.getSharedPreferences("jarvis", Context.MODE_PRIVATE); }

    public Result ask(List<ChatMessage> history, String persona) {
        String system = Personas.systemFor(persona);

        String gk = prefs.getString("groq_key", "");
        if (!gk.isEmpty()) {
            Result r = openai("https://api.groq.com/openai/v1/chat/completions",
                    "llama-3.3-70b-versatile", "Bearer " + gk, system, history);
            if (r != null) { r.provider = "Groq"; return r; }
        }

        String gem = prefs.getString("gemini_key", "");
        if (!gem.isEmpty()) {
            Result r = gemini(system, history, gem);
            if (r != null) { r.provider = "Gemini"; return r; }
        }

        String or = prefs.getString("openrouter_key", "");
        if (!or.isEmpty()) {
            Result r = openai("https://openrouter.ai/api/v1/chat/completions",
                    "deepseek/deepseek-chat-v3-0324:free", "Bearer " + or, system, history);
            if (r != null) { r.provider = "OpenRouter"; return r; }
        }

        String ce = prefs.getString("cerebras_key", "");
        if (!ce.isEmpty()) {
            Result r = openai("https://api.cerebras.ai/v1/chat/completions",
                    "llama3.1-8b", "Bearer " + ce, system, history);
            if (r != null) { r.provider = "Cerebras"; return r; }
        }

        String mi = prefs.getString("mistral_key", "");
        if (!mi.isEmpty()) {
            Result r = openai("https://api.mistral.ai/v1/chat/completions",
                    "open-mistral-nemo", "Bearer " + mi, system, history);
            if (r != null) { r.provider = "Mistral"; return r; }
        }

        String ol = prefs.getString("ollama_url", "");
        if (!ol.isEmpty()) {
            Result r = ollama(ol, system, history);
            if (r != null) { r.provider = "Ollama"; return r; }
        }

        Result r = pollinations(system, history);
        if (r != null) { r.provider = "Pollinations (free)"; return r; }
        return null;
    }

    private Result openai(String url, String model, String auth, String system, List<ChatMessage> history) {
        try {
            JSONArray msgs = new JSONArray();
            msgs.put(new JSONObject().put("role", "system").put("content", system));
            int from = Math.max(0, history.size() - 12);
            String last = null;
            for (int i = from; i < history.size(); i++) {
                ChatMessage m = history.get(i);
                if (m.text == null || m.text.equals("…") || m.role == ChatMessage.SYSTEM) continue;
                String role = m.role == ChatMessage.USER ? "user" : "assistant";
                if (role.equals(last)) continue;
                msgs.put(new JSONObject().put("role", role).put("content", m.text));
                last = role;
            }
            JSONObject body = new JSONObject().put("model", model).put("messages", msgs).put("temperature", 0.7);
            String resp = Net.postJson(url, body.toString(), auth, 45000);
            String text = new JSONObject(resp).getJSONArray("choices").getJSONObject(0)
                    .optJSONObject("message").optString("content");
            if (text != null && !text.trim().isEmpty()) {
                Result r = new Result();
                r.text = text.trim();
                return r;
            }
        } catch (Exception e) { /* try next provider */ }
        return null;
    }

    private Result gemini(String system, List<ChatMessage> history, String key) {
        try {
            JSONArray contents = new JSONArray();
            int from = Math.max(0, history.size() - 12);
            String last = null;
            for (int i = from; i < history.size(); i++) {
                ChatMessage m = history.get(i);
                if (m.text == null || m.text.equals("…") || m.role == ChatMessage.SYSTEM) continue;
                String role = m.role == ChatMessage.USER ? "user" : "model";
                if (role.equals(last)) continue;
                contents.put(new JSONObject().put("role", role).put("parts",
                        new JSONArray().put(new JSONObject().put("text", m.text))));
                last = role;
            }
            if (contents.length() == 0) return null;
            JSONObject body = new JSONObject();
            body.put("systemInstruction", new JSONObject().put("parts",
                    new JSONArray().put(new JSONObject().put("text", system))));
            body.put("contents", contents);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                    + URLEncoder.encode(key, "UTF-8");
            String resp = Net.postJson(url, body.toString(), null, 45000);
            String text = new JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
                    .optJSONObject("content").optJSONArray("parts").optJSONObject(0).optString("text");
            if (text != null && !text.trim().isEmpty()) {
                Result r = new Result();
                r.text = text.trim();
                return r;
            }
        } catch (Exception e) { /* try next provider */ }
        return null;
    }

    private Result ollama(String base, String system, List<ChatMessage> history) {
        try {
            JSONArray msgs = new JSONArray();
            msgs.put(new JSONObject().put("role", "system").put("content", system));
            int from = Math.max(0, history.size() - 12);
            for (int i = from; i < history.size(); i++) {
                ChatMessage m = history.get(i);
                if (m.text == null || m.text.equals("…") || m.role == ChatMessage.SYSTEM) continue;
                msgs.put(new JSONObject().put("role", m.role == ChatMessage.USER ? "user" : "assistant")
                        .put("content", m.text));
            }
            String url = base.endsWith("/") ? base : base + "/";
            JSONObject body = new JSONObject().put("model", "llama3.1").put("stream", false).put("messages", msgs);
            String resp = Net.postJson(url + "api/chat", body.toString(), null, 45000);
            String text = new JSONObject(resp).optJSONObject("message").optString("content");
            if (text != null && !text.trim().isEmpty()) {
                Result r = new Result();
                r.text = text.trim();
                return r;
            }
        } catch (Exception e) { }
        return null;
    }

    private Result pollinations(String system, List<ChatMessage> history) {
        try {
            StringBuilder p = new StringBuilder();
            p.append(system).append("\n\n");
            int from = Math.max(0, history.size() - 8);
            for (int i = from; i < history.size(); i++) {
                ChatMessage m = history.get(i);
                if (m.text == null || m.text.equals("…") || m.role == ChatMessage.SYSTEM) continue;
                p.append(m.role == ChatMessage.USER ? "User: " : "JARVIS: ").append(m.text).append("\n");
            }
            p.append("JARVIS: ");
            String url = "https://text.pollinations.ai/" + URLEncoder.encode(p.toString(), "UTF-8") + "?model=openai";
            String resp = Net.get(url, 60000);
            if (resp != null && !resp.trim().isEmpty()) {
                Result r = new Result();
                r.text = resp.trim();
                return r;
            }
        } catch (Exception e) { }
        return null;
    }
}
