package com.jarvis.assistant;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Password generator page. */
public class PasswordActivity extends BasePage {
    private TextView out;
    private Skills skills;
    private int len = 16;
    private LinearLayout lenRow;
    private final int[] lens = {8, 12, 16, 24, 32};

    @Override protected String titleText() { return "Password"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        label("Length:");
        gap(4);
        lenRow = new LinearLayout(this);
        lenRow.setOrientation(LinearLayout.HORIZONTAL);
        c.addView(lenRow);
        gap(12);
        out = big(skills.password(len));
        out.setTextSize(20);
        out.setTypeface(android.graphics.Typeface.MONOSPACE);
        c.addView(out);
        gap(12);
        c.addView(btnPrimary("GENERATE", new Runnable() { public void run() { out.setText(skills.password(len)); } }));
        gap(8);
        c.addView(btn("COPY", new Runnable() { public void run() { copy(out.getText().toString()); } }));
        drawLens();
    }

    private void drawLens() {
        lenRow.removeAllViews();
        for (int L : lens) {
            final int LL = L;
            Button b = new Button(this);
            b.setText(String.valueOf(L));
            b.setTextColor(L == len ? Color.BLACK : GREEN);
            b.setBackgroundColor(L == len ? GREEN : CARD);
            b.setAllCaps(false);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { len = LL; out.setText(skills.password(len)); drawLens(); }
            });
            lenRow.addView(b, new LinearLayout.LayoutParams(0, dp(46), 1f));
        }
    }
}
