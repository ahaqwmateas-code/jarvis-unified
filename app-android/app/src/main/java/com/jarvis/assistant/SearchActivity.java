package com.jarvis.assistant;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Web search page. */
public class SearchActivity extends BasePage {
    private EditText q;
    private TextView out;
    private Skills skills;

    @Override protected String titleText() { return "Search"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        q = input("Search the web...", false);
        c.addView(q);
        gap(10);
        c.addView(btnPrimary("SEARCH", new Runnable() { public void run() { fetch(); } }));
        gap(12);
        out = text("Type a query and tap SEARCH (DuckDuckGo + Wikipedia fallback, no key).");
        c.addView(out);
    }

    private void fetch() {
        final String s = q.getText().toString().trim();
        if (s.isEmpty()) { toast("Type a query first"); return; }
        out.setText("Searching...");
        bg(new Runnable() {
            public void run() {
                final String r = skills.search(s);
                runOnUiThread(new Runnable() { public void run() { out.setText(r); } });
            }
        });
    }
}
