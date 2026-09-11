package com.jarvis.assistant;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Shared scaffold for every feature page: themed top bar + scrollable content area. */
public abstract class BasePage extends Activity {
    public static final int BG = Color.rgb(5, 8, 12);
    public static final int PANEL = Color.rgb(7, 16, 25);
    public static final int CARD = Color.rgb(12, 22, 32);
    public static final int GREEN = Color.rgb(51, 209, 122);
    public static final int GREY = Color.rgb(140, 160, 150);
    public static final int WHITE = Color.rgb(225, 234, 229);

    protected LinearLayout content;

    protected abstract String titleText();
    protected abstract void build(LinearLayout c);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // top bar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(PANEL);
        bar.setPadding(dp(10), dp(8), dp(12), dp(8));
        bar.addView(btn("BACK", new Runnable() { public void run() { finish(); } }));
        TextView title = new TextView(this);
        title.setText(titleText());
        title.setTextColor(GREEN);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(12), 0, 0, 0);
        bar.addView(title);
        root.addView(bar);

        // scrollable content
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        sv.addView(content);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        build(content);
    }

    // ---------- helpers ----------

    protected int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    protected TextView text(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(WHITE);
        t.setTextSize(15);
        t.setLineSpacing(dp(3), 1f);
        return t;
    }

    protected TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(GREY);
        t.setTextSize(13);
        return t;
    }

    protected TextView big(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(GREEN);
        t.setTextSize(22);
        t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    protected EditText input(String hint, boolean multiline) {
        EditText e = new EditText(this);
        e.setTextColor(WHITE);
        e.setHintTextColor(GREY);
        e.setHint(hint);
        e.setBackgroundColor(CARD);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (multiline) {
            e.setSingleLine(false);
            e.setMaxLines(6);
            e.setGravity(Gravity.TOP);
            e.setImeOptions(EditorInfo.IME_ACTION_NONE);
        } else {
            e.setSingleLine(true);
        }
        return e;
    }

    protected Button btn(String label, final Runnable onClick) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(GREEN);
        b.setAllCaps(false);
        b.setBackgroundColor(CARD);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { onClick.run(); }
        });
        return b;
    }

    protected Button btnPrimary(String label, final Runnable onClick) {
        Button b = btn(label, onClick);
        b.setTextColor(Color.BLACK);
        b.setBackgroundColor(GREEN);
        return b;
    }

    protected void gap(int h) {
        LinearLayout g = new LinearLayout(this);
        content.addView(g, new LinearLayout.LayoutParams(1, dp(h)));
    }

    protected void card(View v) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(CARD);
        gd.setCornerRadius(dp(12));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(4), dp(4), dp(4), dp(4));
        wrap.setBackground(gd);
        wrap.addView(v);
        content.addView(wrap, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    protected void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    protected void go(Class<?> c) {
        startActivity(new Intent(this, c));
    }

    protected void copy(String s) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("jarvis", s));
            toast("Copied");
        } catch (Exception e) {
            toast("Copy failed");
        }
    }

    protected void bg(Runnable r) {
        new Thread(r).start();
    }
}
