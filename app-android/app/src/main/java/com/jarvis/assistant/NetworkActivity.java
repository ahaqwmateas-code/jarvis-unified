package com.jarvis.assistant;

import android.widget.LinearLayout;
import android.widget.TextView;

/** Network info page. */
public class NetworkActivity extends BasePage {
    private TextView out;
    private Skills skills;

    @Override protected String titleText() { return "Network"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        out = text("Checking network...");
        out.setTypeface(android.graphics.Typeface.MONOSPACE);
        c.addView(out);
        gap(10);
        c.addView(btnPrimary("REFRESH", new Runnable() { public void run() { refresh(); } }));
        refresh();
    }

    private void refresh() {
        out.setText("Checking network...");
        bg(new Runnable() {
            public void run() {
                final String r = skills.network();
                runOnUiThread(new Runnable() { public void run() { out.setText(r); } });
            }
        });
    }
}
