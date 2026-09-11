package com.jarvis.assistant;

import android.widget.LinearLayout;
import android.widget.TextView;

/** Brain status + live test page. */
public class BrainActivity extends BasePage {
    private TextView out;
    private Skills skills;
    private Brain brain;

    @Override protected String titleText() { return "Brain"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        brain = new Brain(this);
        out = text("");
        out.setTypeface(android.graphics.Typeface.MONOSPACE);
        c.addView(out);
        gap(10);
        c.addView(btnPrimary("TEST BRAIN", new Runnable() { public void run() { test(); } }));
        gap(8);
        c.addView(btn("ADD KEYS (SETTINGS)", new Runnable() { public void run() { go(SettingsActivity.class); } }));
        refresh();
    }

    private void refresh() {
        out.setText(skills.brainStatus());
    }

    private void test() {
        out.setText("Testing brain...");
        bg(new Runnable() {
            public void run() {
                java.util.List<ChatMessage> h = new java.util.ArrayList<ChatMessage>();
                h.add(new ChatMessage(ChatMessage.USER, "Reply with exactly the word OK."));
                final Brain.Result r = brain.ask(h, "assistant");
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (r == null) out.setText("No provider answered.\nAdd a free key in Settings, or check your connection.\n\n" + skills.brainStatus());
                        else out.setText("BRAIN OK via " + r.provider + "\n\n" + r.text + "\n\n" + skills.brainStatus());
                    }
                });
            }
        });
    }
}
