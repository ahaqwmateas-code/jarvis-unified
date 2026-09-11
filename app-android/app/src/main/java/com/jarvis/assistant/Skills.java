package com.jarvis.assistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Local command skills that run on-device (no server, no key needed for most). */
public class Skills {
    public static class Out {
        public String text;
        public Bitmap image;
        public String imageNote;
    }

    private final Context ctx;
    private final NotesDb db;
    private final SharedPreferences prefs;

    public Skills(Context c) {
        ctx = c;
        db = new NotesDb(c);
        prefs = c.getSharedPreferences("jarvis", Context.MODE_PRIVATE);
    }

    /** Returns null if the input isn't a skill command (→ fall through to the brain). */
    public Out handle(String raw) {
        String s = raw == null ? "" : raw.trim();
        String l = s.toLowerCase(Locale.ROOT);
        Out o = new Out();

        if (l.equals("help") || l.equals("?") || l.equals("commands")) { o.text = help(); return o; }

        if (l.equals("time") || l.equals("date") || l.contains("what time") || l.contains("what's the time")
                || l.contains("whats the time") || l.equals("what day is it") || l.contains("today's date")) {
            SimpleDateFormat f = new SimpleDateFormat("EEEE, d MMMM yyyy · HH:mm:ss", Locale.US);
            o.text = "It's " + f.format(new Date());
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
            String t = s.replaceFirst("(?i)^(note add|remember)\\s+", "");
            db.addNote(t);
            o.text = "Saved note: " + t;
            return o;
        }
        if (l.equals("note list") || l.equals("notes") || l.equals("note ls")) {
            List<String> n = db.notes();
            o.text = n.isEmpty() ? "No notes yet. Try: note add buy milk" : "Notes:\n" + join(n);
            return o;
        }
        if (l.startsWith("note del ") || l.startsWith("note delete ")) {
            try { int i = Integer.parseInt(l.replaceAll("\\D+", "")); db.delNote(i); o.text = "Note " + i + " deleted."; }
            catch (Exception e) { o.text = "Usage: note del <number>"; }
            return o;
        }

        Matcher rm = Pattern.compile("(?i)remind me in (\\d+) (minute|minutes|min|second|seconds|sec|hour|hours) to (.+)").matcher(l);
        if (rm.find()) {
            long n = Long.parseLong(rm.group(1));
            String unit = rm.group(2);
            String text = rm.group(3).trim();
            long ms = unit.startsWith("sec") ? n * 1000L : unit.startsWith("hour") ? n * 3600000L : n * 60000L;
            long when = System.currentTimeMillis() + ms;
            long id = db.addReminder(when, text);
            schedule(when, text, (int) id);
            o.text = "OK — I'll remind you to \"" + text + "\" in " + n + " " + unit + ".";
            return o;
        }
        if (l.equals("reminders") || l.equals("reminder list")) { o.text = reminderList(); return o; }
        if (l.startsWith("reminder del ")) {
            try { long i = Long.parseLong(l.replaceAll("\\D+", "")); db.delReminder(i); o.text = "Reminder " + i + " deleted."; }
            catch (Exception e) { o.text = "Usage: reminder del <number>"; }
            return o;
        }

        if (l.startsWith("weather ")) { o.text = weather(l.substring(8).trim()); return o; }
        if (l.equals("weather")) { o.text = "Usage: weather <city>  (free, no key — Open-Meteo)"; return o; }

        if (l.startsWith("search ") || l.startsWith("google ")) {
            o.text = search(l.replaceFirst("(?i)^(search|google)\\s+", ""));
            return o;
        }
        if (l.startsWith("wiki ")) { o.text = wiki(l.substring(5).trim()); return o; }

        Matcher tr = Pattern.compile("(?i)^translate (.+?) to ([a-z ]+)$").matcher(l);
        if (tr.find()) { o.text = translate(tr.group(1).trim(), tr.group(2).trim()); return o; }

        if (l.equals("persona list") || l.equals("personas")) { o.text = Personas.list(); return o; }
        if (l.startsWith("persona ")) {
            String p = l.substring(8).trim();
            if (p.isEmpty()) {
                o.text = "Current persona: " + prefs.getString("persona", "assistant") + "\n\n" + Personas.list();
            } else {
                prefs.edit().putString("persona", p).apply();
                o.text = "Persona set to \"" + p + "\".\n\n" + Personas.systemFor(p);
            }
            return o;
        }

        if (l.startsWith("image ") || l.startsWith("draw ") || l.startsWith("picture ")) {
            String p = l.replaceFirst("(?i)^(image|draw|picture)\\s+", "");
            return image(p);
        }

        return null;
    }

    private String help() {
        return "JARVIS commands (all run on this phone):\n"
                + "• time / date — the time now\n"
                + "• calc 2+2*10 — safe math\n"
                + "• password 16 — strong password\n"
                + "• note add <text> · note list · note del <n>\n"
                + "• remind me in 10 minutes to <text> · reminders\n"
                + "• weather <city> — free, no key\n"
                + "• search <query> · wiki <topic>\n"
                + "• translate <text> to <language>\n"
                + "• persona <name> · persona list\n"
                + "• image <description> — free\n"
                + "• anything else → the AI brain";
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String it : items) sb.append("• ").append(it).append("\n");
        return sb.toString().trim();
    }

    private String reminderList() {
        List<Object[]> r = db.reminders();
        if (r.isEmpty()) return "No reminders. Try: remind me in 10 minutes to call mom";
        StringBuilder sb = new StringBuilder("Reminders:\n");
        long now = System.currentTimeMillis();
        for (Object[] it : r) {
            long mins = Math.max(0, (((Long) it[1]) - now) / 60000L);
            sb.append("• #").append(it[0]).append(" in ").append(mins).append(" min: ").append(it[2]).append("\n");
        }
        return sb.toString().trim();
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

    private String weather(String city) {
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
            return name + ", " + country + ": " + t + "°C, " + wdesc(code)
                    + ", wind " + wind + " km/h, humidity " + hum + "%";
        } catch (Exception e) {
            return "Weather lookup failed for \"" + city + "\" — check your connection.";
        }
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

    private String search(String q) {
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
        return w != null ? w : "No results for \"" + q + "\" — try: wiki " + q;
    }

    private String wiki(String title) {
        try {
            String u = "https://en.wikipedia.org/api/rest_v1/page/summary/"
                    + URLEncoder.encode(title, "UTF-8").replace("+", "%20");
            String resp = Net.get(u, 20000);
            String e = new JSONObject(resp).optString("extract");
            if (e == null || e.trim().isEmpty()) return null;
            if (e.length() > 700) e = e.substring(0, 700) + "…";
            return e.trim();
        } catch (Exception ex) {
            return null;
        }
    }

    private String translate(String text, String to) {
        String code = langCode(to);
        // primary: Google gtx (no key)
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
            if (!out.isEmpty()) return out + "  (→ " + code + ")";
        } catch (Exception e) { }
        // fallback: MyMemory (free, no key)
        try {
            String u = "https://api.mymemory.translated.net/get?q=" + URLEncoder.encode(text, "UTF-8")
                    + "&langpair=en|" + code;
            String resp = Net.get(u, 15000);
            String out = new JSONObject(resp).optJSONObject("responseData").optString("translatedText");
            if (out != null && !out.trim().isEmpty() && !out.equalsIgnoreCase(text)) {
                return out.trim() + "  (→ " + code + ")";
            }
        } catch (Exception e) { }
        return "Couldn't translate to \"" + code + "\".";
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

    private Out image(String prompt) {
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
                                String path = saveImage(bytes);
                                o.text = "Here's your image of \"" + prompt + "\" (Gemini)";
                                o.imageNote = path;
                                return o;
                            }
                        }
                    }
                }
            } catch (Exception ignored) { }
        }
        // free fallback: Pollinations
        try {
            String url = "https://image.pollinations.ai/prompt/" + URLEncoder.encode(prompt, "UTF-8")
                    + "?width=1024&height=1024&nologo=true";
            byte[] bytes = Net.getBytes(url, 90000);
            if (bytes != null && bytes.length > 1000) {
                Bitmap b = decode(bytes);
                if (b != null) {
                    o.image = b;
                    String path = saveImage(bytes);
                    o.text = "Here's your image of \"" + prompt + "\" (free)";
                    o.imageNote = path;
                    return o;
                }
            }
        } catch (Exception ignored) { }
        o.text = "Image generation failed — check your connection, or add a free Gemini key in ⚙ Settings for better quality.";
        return o;
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

    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*_-+=".toCharArray();

    private static String password(int len) {
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(CHARS[rnd.nextInt(CHARS.length)]);
        return sb.toString();
    }
}
