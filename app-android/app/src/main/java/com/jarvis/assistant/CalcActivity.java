package com.jarvis.assistant;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Calculator page - expression input + result, with a button pad. */
public class CalcActivity extends BasePage {
    private EditText expr;
    private TextView result;

    @Override protected String titleText() { return "Calculator"; }

    @Override
    protected void build(LinearLayout c) {
        expr = input("e.g. (5+3)^2", false);
        c.addView(expr);

        gap(8);
        result = big("0");
        result.setTextSize(30);
        c.addView(result);
        gap(8);

        String[][] rows = {
            {"7", "8", "9", "/"},
            {"4", "5", "6", "*"},
            {"1", "2", "3", "-"},
            {"0", ".", "(", ")"},
            {"+", "^", "%", "C"},
            {"=", "=", "=", "="}
        };
        for (String[] r : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (final String k : r) {
                android.widget.Button b = new android.widget.Button(this);
                b.setText(k);
                b.setTextColor(k.equals("=") ? android.graphics.Color.BLACK : GREEN);
                b.setBackgroundColor(k.equals("=") ? GREEN : CARD);
                b.setAllCaps(false);
                b.setOnClickListener(new android.view.View.OnClickListener() {
                    public void onClick(android.view.View v) { key(k); }
                });
                row.addView(b, new LinearLayout.LayoutParams(0, dp(54), 1f));
            }
            c.addView(row);
        }
    }

    private void key(String k) {
        if (k.equals("C")) { expr.setText(""); result.setText("0"); return; }
        if (k.equals("=")) { calc(); return; }
        expr.setText(expr.getText().toString() + k);
    }

    private void calc() {
        String v = Calc.eval(expr.getText().toString().trim());
        if (v == null) result.setText("?");
        else { result.setText(v); }
    }
}
