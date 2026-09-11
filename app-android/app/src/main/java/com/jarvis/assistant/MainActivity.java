package com.jarvis.assistant;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** JARVIS home - a grid where EVERY feature is its own page. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(5, 8, 12);
    private static final int PANEL = Color.rgb(7, 16, 25);
    private static final int GREEN = Color.rgb(51, 209, 122);
    private static final int GREY = Color.rgb(140, 160, 150);

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("jarvis", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // header
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setBackgroundColor(PANEL);
        head.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout hr = new LinearLayout(this);
        hr.setOrientation(LinearLayout.HORIZONTAL);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("JARVIS");
        title.setTextColor(GREEN);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        ht.addView(title);
        TextView sub = new TextView(this);
        sub.setTextColor(GREY);
        sub.setTextSize(12);
        sub.setText("every feature is a page - tap one below");
        ht.addView(sub);
        hr.addView(ht, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hr.addView(btn("CORE", new Runnable() { public void run() { go(ServerActivity.class); } }));
        hr.addView(btn("SET", new Runnable() { public void run() { go(SettingsActivity.class); } }));
        head.addView(hr);
        root.addView(head);

        // grid of pages
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(10), dp(10), dp(10), dp(20));
        sv.addView(grid);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        addRow(grid,
            card("\uD83D\uDCAC", "Chat", "talk with the AI", ChatActivity.class),
            card("\uD83D\uDD50", "Time", "clock & date", TimeActivity.class));
        addRow(grid,
            card("\uD83E\uDDEE", "Calculator", "safe math", CalcActivity.class),
            card("\uD83D\uDD10", "Password", "strong passwords", PasswordActivity.class));
        addRow(grid,
            card("\uD83C\uDF24", "Weather", "free forecast", WeatherActivity.class),
            card("\uD83D\uDD0D", "Search", "web search", SearchActivity.class));
        addRow(grid,
            card("\uD83D\uDCDA", "Wikipedia", "instant summaries", WikiActivity.class),
            card("\uD83C\uDF10", "Translate", "to 30 languages", TranslateActivity.class));
        addRow(grid,
            card("\uD83D\uDCDD", "Notes", "save & list", NotesActivity.class),
            card("\u23F0", "Reminders", "notify me later", RemindersActivity.class));
        addRow(grid,
            card("\uD83C\uDFAD", "Personas", "16 personalities", PersonasActivity.class),
            card("\uD83D\uDDE3", "Language", "answer in 30 langs", LangActivity.class));
        addRow(grid,
            card("\uD83D\uDDBC", "Image", "generate pictures", ImageActivity.class),
            card("\uD83D\uDEE0", "Build", "AI writes apps", BuildActivity.class));
        addRow(grid,
            card("\uD83D\uDDA5", "System", "CPU RAM battery", SystemActivity.class),
            card("\uD83D\uDCE1", "Network", "IP & DNS", NetworkActivity.class));
        addRow(grid,
            card("\uD83E\uDDE0", "Brain", "providers & test", BrainActivity.class),
            card("\uD83C\uDFAC", "Video", "real MP4 (core)", VideoActivity.class));
        addRow(grid,
            card("\u2328", "CLI Hub", "40+ agent CLIs", ClihubActivity.class),
            card("\u2699", "Settings", "API keys & more", SettingsActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void addRow(LinearLayout grid, LinearLayout a, LinearLayout b) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        grid.addView(row);
    }

    private LinearLayout card(String emoji, String name, String desc, final Class<?> cls) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(12), dp(14), dp(12), dp(14));
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(12, 22, 32));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(24, 84, 54));
        c.setBackground(bg);

        TextView em = new TextView(this);
        em.setText(emoji);
        em.setTextSize(26);
        c.addView(em);

        TextView nm = new TextView(this);
        nm.setText(name);
        nm.setTextColor(GREEN);
        nm.setTextSize(15);
        nm.setTypeface(null, Typeface.BOLD);
        nm.setGravity(Gravity.CENTER);
        c.addView(nm);

        TextView ds = new TextView(this);
        ds.setText(desc);
        ds.setTextColor(GREY);
        ds.setTextSize(11);
        ds.setGravity(Gravity.CENTER);
        c.addView(ds);

        c.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { go(cls); }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        c.setLayoutParams(lp);
        return c;
    }

    private Button btn(String label, final Runnable r) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(GREEN);
        b.setBackgroundColor(Color.rgb(12, 22, 32));
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { r.run(); } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void go(Class<?> c) {
        startActivity(new Intent(this, c));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
