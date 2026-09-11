package com.jarvis.assistant;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/** Language page - choose the language JARVIS answers in. */
public class LangActivity extends BasePage {
    private Skills skills;
    private LinearLayout list;
    private final String[] langs = {"english", "spanish", "french", "german", "italian",
        "portuguese", "russian", "dutch", "polish", "turkish", "arabic", "hindi",
        "japanese", "korean", "chinese", "indonesian", "vietnamese", "thai", "swedish",
        "norwegian", "danish", "finnish", "greek", "czech", "romanian", "hungarian",
        "hebrew", "ukrainian", "malay", "filipino"};

    @Override protected String titleText() { return "Language"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        label("Pick the language JARVIS answers in:");
        gap(4);
        c.addView(btn("RESET (auto-detect)", new Runnable() {
            public void run() { skills.setLang(""); toast("Language auto-detect"); draw(); }
        }));
        gap(8);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        c.addView(list);
        draw();
    }

    private void draw() {
        list.removeAllViews();
        String cur = skills.currentLang();
        for (final String l : langs) {
            Button b = new Button(this);
            String labelTxt = l + (l.equalsIgnoreCase(cur) ? "  (active)" : "");
            b.setText(labelTxt);
            b.setTextColor(l.equalsIgnoreCase(cur) ? Color.BLACK : GREEN);
            b.setBackgroundColor(l.equalsIgnoreCase(cur) ? GREEN : CARD);
            b.setAllCaps(false);
            b.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { skills.setLang(l); toast("Language: " + l); draw(); }
            });
            list.addView(b, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        }
    }
}
