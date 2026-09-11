package com.jarvis.assistant;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Build page - the AI writes a whole app. */
public class BuildActivity extends BasePage {
    private EditText idea;
    private TextView out;
    private Skills skills;
    private String last = "";

    @Override protected String titleText() { return "Build"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        idea = input("What should I build? e.g. coin flip game", false);
        c.addView(idea);
        gap(10);
        c.addView(btnPrimary("BUILD IT", new Runnable() { public void run() { build(); } }));
        gap(8);
        c.addView(btn("COPY CODE", new Runnable() { public void run() { if (!last.isEmpty()) copy(last); else toast("Build something first"); } }));
        gap(12);
        out = text("Describe an app or script - I'll write the full code and save it to your phone.");
        c.addView(out);
    }

    private void build() {
        final String s = idea.getText().toString().trim();
        if (s.isEmpty()) { toast("Describe what to build"); return; }
        out.setText("Writing code...");
        bg(new Runnable() {
            public void run() {
                final String r = skills.buildApp(s);
                last = r;
                runOnUiThread(new Runnable() {
                    public void run() {
                        out.setTypeface(android.graphics.Typeface.MONOSPACE);
                        out.setText(r);
                    }
                });
            }
        });
    }
}
