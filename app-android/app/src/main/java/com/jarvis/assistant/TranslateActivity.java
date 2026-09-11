package com.jarvis.assistant;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Translate page - text in, pick language, result out. */
public class TranslateActivity extends BasePage {
    private EditText src;
    private EditText lang;
    private TextView out;
    private Skills skills;
    private String target = "es";
    private LinearLayout langRow;
    private final String[] langs = {"es", "fr", "de", "it", "pt", "ru"};

    @Override protected String titleText() { return "Translate"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        src = input("Text to translate...", true);
        c.addView(src);
        gap(8);
        label("Target language:");
        gap(4);
        langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        c.addView(langRow);
        gap(6);
        lang = input("or type a language name (e.g. japanese)", false);
        c.addView(lang);
        gap(10);
        c.addView(btnPrimary("TRANSLATE", new Runnable() { public void run() { fetch(); } }));
        gap(12);
        out = text("Type text, pick a language, tap TRANSLATE.");
        c.addView(out);
        drawLangs();
    }

    private void drawLangs() {
        langRow.removeAllViews();
        for (final String L : langs) {
            Button b = new Button(this);
            b.setText(L.toUpperCase(java.util.Locale.US));
            b.setTextColor(L.equals(target) ? Color.BLACK : GREEN);
            b.setBackgroundColor(L.equals(target) ? GREEN : CARD);
            b.setAllCaps(false);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { target = L; drawLangs(); }
            });
            langRow.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1f));
        }
    }

    private void fetch() {
        final String t = src.getText().toString().trim();
        if (t.isEmpty()) { toast("Type something to translate"); return; }
        String custom = lang.getText().toString().trim();
        final String to = custom.isEmpty() ? target : custom;
        out.setText("Translating...");
        bg(new Runnable() {
            public void run() {
                final String r = skills.translate(t, to);
                runOnUiThread(new Runnable() { public void run() { out.setText(r); } });
            }
        });
    }
}
