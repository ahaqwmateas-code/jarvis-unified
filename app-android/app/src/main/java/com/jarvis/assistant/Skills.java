package com.jarvis.assistant;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** All skills as a service layer. handle() routes chat-style commands; the
 *  public methods are used directly by each feature page. */
public class Skills {
    public static class Out {
        public String text;
        public Bitmap image;
        public String imageNote;
    }

    private final Context ctx;
    private final NotesDb db;
    private final SharedPreferences prefs;
    private final Brain brain;

    public Skills(Context c, Brain b) {
        ctx = c;
        db = new NotesDb(c);
        prefs = c.getSharedPreferences("jarvis", Context.MODE_PRIVATE);
        brain = b;
    }

    // ================= chat-style router =================

    public Out handle(String raw) {
        String s = raw == null ? "" : raw.trim();
        String l = s.toLowerCase(Locale.ROOT);
        Out o = new Out();

        if (l.equals("help") || l.equals("?") || l.equals("commands")) { o.text = help(); return o; }

        if (l.equals("time") || l.equals("date") || l.contains("what time") || l.contains("what's the time")
                || l.contains("whats the time") || l.equals("what day is it") || l.contains("today's date")) {
            o.text = "It's " + timeNow();
            return o;
        }
        if (l.startsWith("calc ") || l.startsWith("calculate ") || l.startsWith("math ")) {
            String e = l.replaceFirst("^(calc|calculate|math)\\s+", "");
            String v = Calc.eval(e);
            o.text = (v == null) ? "I couldn't compute that. Try: calc 2+2*10" : (e + " = " + v);
            return o;
        }
        if (l.startsWith("password") || l.contains("generate a password") || l.contains("make a password")) {
            int len = 16;
            Matcher m = Pattern.compile("(\\d{1,3})").matcher(l);
            if (m.find()) { try { len = Integer.parseInt(m.group(1)); } catch (Exception ignored) { } }
            if (len < 4) len = 4;
            if (len > 64) len = 64;
            o.text = "Password (" + len + "): " + password(len);
            return o;
        }
        if (l.startsWith("note add ") || l.startsWith("remember ")) {
            o.text = addNote(s.replaceFirst("(?i)^(note add|remember)\\s+", ""));
            return o;
        }
        if (l.equals("note list") || l.equals("notes") || l.equals("note ls")) { o.text = notesText(); return o; }
        if (l.startsWith("note del ") || l.startsWith("note delete ")) {
            try { int i = Integer.parseInt(l.replaceAll("\\D+", "")); o.text = delNote(i); }
            catch (Exception e) { o.text = "Usage: note del <number>"; }
            return o;
        }
        Matcher rm = Pattern.compile("(?i)remind me in (\\d+) (minute|minutes|min|second|seconds|sec|hour|hours) to (.+)").matcher(l);
        if (rm.find()) {
            long n = Long.parseLong(rm.group(1));
            String unit = rm.group(2);
            String text = rm.group(3).trim();
            long mins = unit.startsWith("sec") ? n / 60.0 > 0 ? (long) Math.ceil(n / 60.0) : 1 : unit.startsWith("hour") ? n * 60 : n;
            o.text = addReminder((int) mins, text);
            return o;
        }
        if (l.equals("reminders") || l.equals("reminder list")) { o.text = reminderList(); return o; }
        if (l.startsWith("reminder del ")) {
            try { long i = Long.parseLong(l.replaceAll("\\D+", "")); o.text = delReminder(i); }
            catch (Exception e) { o.text = "Usage: reminder del <number>"; }
            return o;
        }
        if (l.startsWith("weather ")) { o.text = weather(l.substring(8).trim()); return o; }
        if (l.equals("weather")) { o.text = "Usage: weather <city>  (free, no key - Open-Meteo)"; return o; }
        if (l.startsWith("search ") || l.startsWith("google ")) {
            o.text = search(l.replaceFirst("(?i)^(search|google)\\s+", ""));
            return o;
        }
        if (l.startsWith("wiki ")) { o.text = wiki(l.substring(5).trim()); return o; }
        Matcher tr = Pattern.compile("(?i)^translate (.+?) to ([a-z ]+)$").matcher(l);
        if (tr.find()) { o.text = translate(tr.group(1).trim(), tr.group(2).trim()); return o; }
        if (l.equals("system") || l.equals("sysinfo") || l.startsWith("system info")) { o.text = systemInfo(); return o; }
        if (l.equals("network") || l.equals("net") || l.equals("ip")) { o.text = network(); return o; }
        if (l.equals("lang list") || l.equals("languages")) { o.text = langList(); return o; }
        if (l.startsWith("lang set ")) { o.text = setLang(l.substring(9).trim()); return o; }
        if (l.equals("lang")) { o.text = "Language: " + (currentLang().isEmpty() ? "(auto)" : currentLang()) + "\nTry: lang set spanish"; return o; }
        if (l.equals("persona list") || l.equals("personas")) { o.text = personaList(); return o; }
        if (l.startsWith("persona ")) {
            String p = l.substring(8).trim();
            o.text = p.isEmpty() ? personaList() : setPersona(p);
            return o;
        }
        if (l.equals("brain") || l.equals("brain status")) { o.text = brainStatus(); return o; }
        if (l.startsWith("build ") || l.startsWith("app ") || l.startsWith("make me an app") || l.startsWith("make an app")) {
            String idea = l.replaceFirst("(?i)^(build|app|make me an app|make an app)\\s+", "");
            o.text = buildApp(idea);
            return o;
        }
        if (l.startsWith("video") || l.startsWith("movie")) { o.text = videoGuide(); return o; }
        if (l.startsWith("clihub") || l.startsWith("cli hub")) { o.text = clihubGuide(); return o; }
        if (l.startsWith("image ") || l.startsWith("draw ") || l.startsWith("picture ")) {
            return image(l.replaceFirst("(?i)^(image|draw|picture)\\s+", ""));
        }
        return null;
    }

    // ================= public skill API (used by pages) =================

    public String help() {
        return "JARVIS commands:\n"
                + "time | calc 2+2*10 | password 16\n"
                + "note add <t> | note list | note del <n>\n"
                + "remind me in 10 minutes to <t> | reminders\n"
                + "weather <city> | search <q> | wiki <t>\n"
                + "translate <text> to <lang>\n"
                + "system | network | brain status\n"
                + "lang set spanish | lang list\n"
                + "persona <name> | persona list\n"
                + "build <idea> | image <desc>\n"
                + "video | clihub | anything else -> AI brain";
    }

    public String timeNow() {
        SimpleDateFormat f = new SimpleDateFormat("EEEE, d MMMM yyyy - HH:mm:ss", Locale.US);
        return f.format(new Date());
    }

    public String password(int len) {
        if (len < 4) len = 4;
        if (len > 64) len = 64;
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(CHARS[rnd.nextInt(CHARS.length)]);
        return sb.toString();
    }

    public String addNote(String t) {
        db.addNote(t);
        return "Saved note: " + t;
    }

    public String notesText() {
        List<String> n = db.notes();
        return n.isEmpty() ? "No notes yet." : "Notes:\n" + join(n);
    }

    public List<String> notes() {
        return db.notes();
    }

    public String delNote(int id) {
        db.delNote(id);
        return "Note " + id + " deleted.";
    }

    public String addReminder(int minutes, String text) {
        long when = System.currentTimeMillis() + (long) minutes * 60000L;
        long id = db.addReminder(when, text);
        schedule(when, text, (int) id);
        return "OK - I'll remind you to \"" + text + "\" in " + minutes + " min.";
    }

    public String reminderList() {
        List<Object[]> r = db.reminders();
        if (r.isEmpty()) return "No reminders.";
        StringBuilder sb = new StringBuilder("Reminders:\n");
        long now = System.currentTimeMillis();
        for (Object[] it : r) {
            long mins = Math.max(0, (((Long) it[1]) - now) / 60000L);
            sb.append("- #").append(it[0]).append(" in ").append(mins).append(" min: ").append(it[2]).append("\n");
        }
        return sb.toString().trim();
    }

    public String delReminder(long id) {
        db.delReminder(id);
        return "Reminder " + id + " deleted.";
    }

    public String systemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("android: ").append(Build.VERSION.RELEASE).append("\n");
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            sb.append("RAM: ").append((mi.totalMem - mi.availMem) / 1048576L).append(" / ")
              .append(mi.totalMem / 1048576L).append(" MB used\n");
        } catch (Exception ignored) { }
        sb.append("uptime: ").append(fmt(SystemClock.elapsedRealtime() / 1000L)).append("\n");
        try {
            IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent bstat = ctx.registerReceiver(null, f);
            if (bstat != null) {
                int level = bstat.getIntExtra("level", -1);
                int scale = bstat.getIntExtra("scale", -1);
                int pct = (scale > 0) ? (level * 100 / scale) : -1;
                int status = bstat.getIntExtra("status", -1);
                String st = status == 2 ? "charging" : status == 5 ? "full" : "on battery";
                sb.append("battery: ").append(pct).append("% (").append(st).append(")");
            }
        } catch (Exception ignored) { }
        return sb.toString().trim();
    }

    public String network() {
        StringBuilder sb = new StringBuilder();
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            sb.append("connection: ").append(ni != null ? ni.getTypeName() : "none").append("\n");
        } catch (Exception ignored) { }
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            sb.append("local IP:");
            boolean any = false;
            while (e.hasMoreElements()) {
                NetworkInterface n = e.nextElement();
                Enumeration<InetAddress> a = n.getInetAddresses();
                while (a.hasMoreElements()) {
                    InetAddress ia = a.nextElement();
                    if (!ia.isLoopbackAddress() && ia instanceof Inet4Address) {
                        sb.append(" ").append(ia.getHostAddress());
                        any = true;
                    }
                }
            }
            if (!any) sb.append(" (none)");
            sb.append("\n");
        } catch (Exception ignored) { }
        try {
            String pub = Net.get("https://api.ipify.org", 8000).trim();
            if (!pub.isEmpty()) sb.append("public IP: ").append(pub).append("\n");
        } catch (Exception ignored) { }
        try {
            sb.append("DNS: google.com -> ").append(InetAddress.getByName("google.com").getHostAddress());
        } catch (Exception ignored) { }
        return sb.toString().trim();
    }

    public String brainStatus() {
        List<String> have = new ArrayList<String>();
        if (!prefs.getString("groq_key", "").isEmpty()) have.add("Groq");
        if (!prefs.getString("gemini_key", "").isEmpty()) have.add("Gemini");
        if (!prefs.getString("openrouter_key", "").isEmpty()) have.add("OpenRouter");
        if (!prefs.getString("cerebras_key", "").isEmpty()) have.add("Cerebras");
        if (!prefs.getString("mistral_key", "").isEmpty()) have.add("Mistral");
        if (!prefs.getString("xai_key", "").isEmpty()) have.add("xAI");
        if (!prefs.getString("deepseek_key", "").isEmpty()) have.add("DeepSeek");
        if (!prefs.getString("github_key", "").isEmpty()) have.add("GitHub Models");
        if (!prefs.getString("custom_key", "").isEmpty()) have.add("Custom");
        if (!prefs.getString("ollama_url", "").isEmpty()) have.add("Ollama (local)");
        return "persona: " + currentPersona() + "\n"
                + "language: " + (currentLang().isEmpty() ? "(auto)" : currentLang()) + "\n"
                + "size: " + brain.brainSize().toUpperCase(java.util.Locale.US) + "  (change above)\n"
                + "providers: " + (have.isEmpty() ? "none set - free anonymous brain (Pollinations)" : joinInline(have)) + "\n"
                + "fallback: Ollama then free brain\n"
                + "add keys in Settings";
    }

    public String currentPersona() {
        return prefs.getString("persona", "assistant");
    }

    public String setPersona(String p) {
        prefs.edit().putString("persona", p).apply();
        return "Persona set to \"" + p + "\".\n\n" + Personas.systemFor(p);
    }

    public String personaList() {
        return "Current: " + currentPersona() + "\n\n" + Personas.list();
    }

    public String currentLang() {
        return prefs.getString("lang", "");
    }

    public String setLang(String lg) {
        prefs.edit().putString("lang", lg).apply();
        return "Language set to " + lg + ". I'll answer in " + lg + " from now on.";
    }

    public String langList() {
        return "english, spanish, french, german, italian, portuguese, russian, dutch,\n"
                + "polish, turkish, arabic, hindi, japanese, korean, chinese, indonesian,\n"
                + "vietnamese, thai, swedish, norwegian, danish, finnish, greek, czech,\n"
                + "romanian, hungarian, hebrew, ukrainian, malay, filipino";
    }

    public String weather(String city) {
        try {
            String g = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(city, "UTF-8")
                    + "&count=1&language=en&format=json";
            String gr = Net.get(g, 15000);
            JSONArray res = new JSONObject(gr).optJSONArray("results");
            if (res == null || res.length() == 0) return "City not found: " + city;
            JSONObject p = res.getJSONObject(0);
            double lat = p.getDouble("latitude"), lon = p.getDouble("longitude");
            String name = p.optString("name"), country = p.optString("country");
            String f = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m";
            String fr = Net.get(f, 15000);
            JSONObject cur = new JSONObject(fr).getJSONObject("current");
            double t = cur.getDouble("temperature_2m");
            int code = cur.getInt("weather_code");
            double wind = cur.getDouble("wind_speed_10m");
            int hum = cur.getInt("relative_humidity_2m");
            return name + ", " + country + ": " + t + " C, " + wdesc(code)
                    + ", wind " + wind + " km/h, humidity " + hum + "%";
        } catch (Exception e) {
            return "Weather lookup failed for \"" + city + "\" - check your connection.";
        }
    }

    public String search(String q) {
        try {
            String u = "https://api.duckduckgo.com/?q=" + URLEncoder.encode(q, "UTF-8")
                    + "&format=json&no_html=1&skip_disambig=1";
            String resp = Net.get(u, 15000);
            JSONObject j = new JSONObject(resp);
            String abs = j.optString("AbstractText");
            if (abs != null && !abs.trim().isEmpty()) {
                String src = j.optString("AbstractURL");
                return abs.trim() + (src == null || src.isEmpty() ? "" : "\n" + src);
            }
            JSONArray rt = j.optJSONArray("RelatedTopics");
            if (rt != null && rt.length() > 0) {
                String t = rt.optJSONObject(0).optString("Text");
                if (t != null && !t.trim().isEmpty()) return t.trim();
            }
        } catch (Exception e) { }
        String w = wiki(q);
        return w != null ? w : "No results for \"" + q + "\"";
    }

    public String wiki(String title) {
        try {
            String u = "https://en.wikipedia.org/api/rest_v1/page/summary/"
                    + URLEncoder.encode(title, "UTF-8").replace("+", "%20");
            String resp = Net.get(u, 20000);
            String e = new JSONObject(resp).optString("extract");
            if (e == null || e.trim().isEmpty()) return null;
            if (e.length() > 900) e = e.substring(0, 900) + "...";
            return e.trim();
        } catch (Exception ex) {
            return null;
        }
    }

    public String translate(String text, String to) {
        String code = langCode(to);
        try {
            String u = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + code
                    + "&dt=t&q=" + URLEncoder.encode(text, "UTF-8");
            String resp = Net.get(u, 15000);
            JSONArray arr = new JSONArray(resp);
            JSONArray seg = arr.getJSONArray(0);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < seg.length(); i++) {
                JSONArray piece = seg.optJSONArray(i);
                if (piece != null) sb.append(piece.optString(0));
            }
            String out = sb.toString().trim();
            if (!out.isEmpty()) return out + "  (-> " + code + ")";
        } catch (Exception e) { }
        try {
            String u = "https://api.mymemory.translated.net/get?q=" + URLEncoder.encode(text, "UTF-8")
                    + "&langpair=en|" + code;
            String resp = Net.get(u, 15000);
            String out = new JSONObject(resp).optJSONObject("responseData").optString("translatedText");
            if (out != null && !out.trim().isEmpty() && !out.equalsIgnoreCase(text)) {
                return out.trim() + "  (-> " + code + ")";
            }
        } catch (Exception e) { }
        return "Couldn't translate to \"" + code + "\".";
    }

    public String buildApp(String idea) {
        if (idea.isEmpty()) return "Usage: build <idea>  e.g. build coin flip game";
        Brain.Result r = brain.askBuild(idea);
        if (r == null) return "No brain available - add a free key in Settings (Groq/Gemini), or check your connection.";
        String ext = "txt";
        String low = idea.toLowerCase(Locale.ROOT);
        if (low.contains("website") || low.contains("html") || low.contains("web") || low.contains("page")) ext = "html";
        else if (low.contains("python") || low.contains("script") || low.contains("cli")) ext = "py";
        String path = saveText(r.text, "build_" + System.currentTimeMillis() + "." + ext);
        return "Built: " + idea + "  (via " + r.provider + ")\n"
                + "Saved: " + (path == null ? "(couldn't save)" : path) + "\n\n" + r.text;
    }

    public String videoGuide() {
        return "VIDEO - real MP4 generation runs on the full core (ffmpeg + AI frames + Ken Burns motion).\n\n"
                + "1. Tap CORE on the home screen\n"
                + "2. In the core, run: video make <prompt>\n\n"
                + "For an instant picture of a scene, use the Image page instead.";
    }

    public String clihubGuide() {
        return "CLI-HUB (CLI-Anything) - a registry of 40+ agent-native CLIs:\n"
                + "Claude Code, OpenCode, OpenClaw, Codex, Qodercli, Goose, GitHub Copilot.\n\n"
                + "It runs on the Kali core - tap CORE on the home screen, then:\n"
                + "clihub list   or   clihub install <name>\n\n"
                + "On this phone, use the Build page to create anything directly.";
    }

    public Out image(String prompt) {
        Out o = new Out();
        String gk = prefs.getString("gemini_key", "");
        if (!gk.isEmpty()) {
            try {
                JSONObject req = new JSONObject();
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", "Generate an image of: " + prompt));
                req.put("contents", new JSONArray().put(new JSONObject().put("parts", parts)));
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key="
                        + URLEncoder.encode(gk, "UTF-8");
                String resp = Net.postJson(url, req.toString(), null, 60000);
                JSONArray cands = new JSONObject(resp).getJSONArray("candidates");
                for (int i = 0; i < cands.length(); i++) {
                    JSONArray ps = cands.getJSONObject(i).optJSONObject("content").optJSONArray("parts");
                    if (ps == null) continue;
                    for (int k = 0; k < ps.length(); k++) {
                        JSONObject inline = ps.optJSONObject(k).optJSONObject("inlineData");
                        if (inline != null && inline.has("data")) {
                            byte[] bytes = Base64.decode(inline.getString("data"), Base64.DEFAULT);
                            Bitmap b = decode(bytes);
                            if (b != null) {
                                o.image = b;
                                o.imageNote = saveImage(bytes);
                                o.text = "Generated with Gemini.";
                                return o;
                            }
                        }
                    }
                }
            } catch (Exception ignored) { }
        }
        try {
            String url = "https://image.pollinations.ai/prompt/" + URLEncoder.encode(prompt, "UTF-8")
                    + "?width=1024&height=1024&nologo=true";
            byte[] bytes = Net.getBytes(url, 90000);
            if (bytes != null && bytes.length > 1000) {
                Bitmap b = decode(bytes);
                if (b != null) {
                    o.image = b;
                    o.imageNote = saveImage(bytes);
                    o.text = "Generated free with Pollinations.";
                    return o;
                }
            }
        } catch (Exception ignored) { }
        o.text = "Image generation failed - check connection, or add a free Gemini key in Settings.";
        return o;
    }

    // ================= internals =================

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String it : items) sb.append("- ").append(it).append("\n");
        return sb.toString().trim();
    }

    private static String joinInline(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private String fmt(long secs) {
        long d = secs / 86400, h = (secs % 86400) / 3600, m = (secs % 3600) / 60;
        return (d > 0 ? d + "d " : "") + h + "h " + m + "m";
    }

    private void schedule(long whenMs, String text, int id) {
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra("text", text);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, id, intent, flags);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        try { am.set(AlarmManager.RTC_WAKEUP, whenMs, pi); } catch (Exception ignored) { }
    }

    private static String wdesc(int c) {
        if (c == 0) return "clear sky";
        if (c <= 3) return "partly cloudy";
        if (c == 45 || c == 48) return "foggy";
        if (c <= 57) return "drizzle";
        if (c <= 67) return "rain";
        if (c <= 77) return "snow";
        if (c <= 82) return "rain showers";
        if (c <= 86) return "snow showers";
        return "thunderstorm";
    }

    private static String langCode(String name) {
        String n = name.toLowerCase(Locale.ROOT).trim();
        if (n.contains("spanish") || n.equals("es")) return "es";
        if (n.contains("french") || n.equals("fr")) return "fr";
        if (n.contains("german") || n.equals("de")) return "de";
        if (n.contains("italian") || n.equals("it")) return "it";
        if (n.contains("portuguese") || n.equals("pt")) return "pt";
        if (n.contains("russian") || n.equals("ru")) return "ru";
        if (n.contains("japanese") || n.equals("ja")) return "ja";
        if (n.contains("korean") || n.equals("ko")) return "ko";
        if (n.contains("chinese") || n.equals("zh")) return "zh-CN";
        if (n.contains("arabic") || n.equals("ar")) return "ar";
        if (n.contains("hindi") || n.equals("hi")) return "hi";
        if (n.contains("dutch") || n.equals("nl")) return "nl";
        if (n.contains("polish") || n.equals("pl")) return "pl";
        if (n.contains("turkish") || n.equals("tr")) return "tr";
        return n.length() == 2 ? n : "es";
    }

    private static Bitmap decode(byte[] bytes) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            int w = opts.outWidth, h = opts.outHeight;
            int sample = 1;
            while (w / sample > 1024 || h / sample > 1024) sample *= 2;
            opts.inSampleSize = sample;
            opts.inJustDecodeBounds = false;
            Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            if (b == null || b.getWidth() < 2 || b.getHeight() < 2) return null;
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private String saveImage(byte[] bytes) {
        try {
            File dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "jarvis");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "img_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes);
            fos.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private String saveText(String content, String name) {
        try {
            File dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "jarvis");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, name);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*_-+=".toCharArray();
}
