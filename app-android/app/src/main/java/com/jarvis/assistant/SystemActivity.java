package com.jarvis.assistant;

import android.widget.LinearLayout;
import android.widget.TextView;

/** System info page. */
public class SystemActivity extends BasePage {
    private TextView out;
    private Skills skills;

    @Override protected String titleText() { return "System"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        out = text("");
        out.setTypeface(android.graphics.Typeface.MONOSPACE);
        c.addView(out);
        gap(10);
        c.addView(btnPrimary("REFRESH", new Runnable() { public void run() { refresh(); } }));
        refresh();
    }

    private void refresh() {
        out.setText(skills.systemInfo());
    }
}
