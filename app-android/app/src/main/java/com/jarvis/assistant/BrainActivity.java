package com.jarvis.assistant;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Brain status + live test + size selector page. */
public class BrainActivity extends BasePage {
    private TextView out;
    private Skills skills;
    private Brain brain;

    @Override protected String titleText() { return "Brain"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        brain = new Brain(this);

        label("Brain size:");
        gap(4);
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        final String[] sizes = {"slim", "balanced", "max"};
        final String[] labels = {"SLIM fast", "BALANCED", "MAX smart"};
        for (int i = 0; i < sizes.length; i++) {
            final String sz = sizes[i];
            Button b = new Button(this);
            b.setText(labels[i]);
            boolean cur = sz.equals(getSharedPreferences("jarvis", MODE_PRIVATE).getString("brain_size", "balanced"));
            b.setTextColor(cur ? Color.BLACK : GREEN);
            b.setBackgroundColor(cur ? GREEN : CARD);
            b.setAllCaps(false);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    getSharedPreferences("jarvis", MODE_PRIVATE).edit().putString("brain_size", sz).apply();
                    toast("Brain size: " + sz);
                    recreate();
                }
            });
            sizeRow.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1f));
        }
        c.addView(sizeRow);
        gap(12);

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
