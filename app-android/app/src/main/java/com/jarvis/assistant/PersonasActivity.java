package com.jarvis.assistant;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/** Personas page - pick a personality for the AI. */
public class PersonasActivity extends BasePage {
    private Skills skills;
    private LinearLayout list;

    @Override protected String titleText() { return "Personas"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        label("Tap a persona to use it in Chat:");
        gap(6);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        c.addView(list);
        draw();
    }

    private void draw() {
        list.removeAllViews();
        String cur = skills.currentPersona();
        for (final String[] p : Personas.ALL) {
            Button b = new Button(this);
            String labelTxt = p[0] + (p[0].equalsIgnoreCase(cur) ? "  (active)" : "");
            b.setText(labelTxt);
            b.setTextColor(p[0].equalsIgnoreCase(cur) ? Color.BLACK : GREEN);
            b.setBackgroundColor(p[0].equalsIgnoreCase(cur) ? GREEN : CARD);
            b.setAllCaps(false);
            b.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { skills.setPersona(p[0]); toast("Persona: " + p[0]); draw(); }
            });
            list.addView(b, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        }
    }
}
