package com.jarvis.assistant;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Wikipedia page. */
public class WikiActivity extends BasePage {
    private EditText q;
    private TextView out;
    private Skills skills;

    @Override protected String titleText() { return "Wikipedia"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        q = input("Topic, e.g. black holes", false);
        c.addView(q);
        gap(10);
        c.addView(btnPrimary("SUMMARIZE", new Runnable() { public void run() { fetch(); } }));
        gap(12);
        out = text("Type a topic and tap SUMMARIZE.");
        c.addView(out);
    }

    private void fetch() {
        final String s = q.getText().toString().trim();
        if (s.isEmpty()) { toast("Type a topic first"); return; }
        out.setText("Looking up...");
        bg(new Runnable() {
            public void run() {
                final String r = skills.wiki(s);
                runOnUiThread(new Runnable() {
                    public void run() { out.setText(r == null ? "Not found - try another topic." : r); }
                });
            }
        });
    }
}
