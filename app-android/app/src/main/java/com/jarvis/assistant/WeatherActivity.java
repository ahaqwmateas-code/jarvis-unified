package com.jarvis.assistant;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Weather page. */
public class WeatherActivity extends BasePage {
    private EditText city;
    private TextView out;
    private Skills skills;

    @Override protected String titleText() { return "Weather"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        city = input("City, e.g. Sydney", false);
        c.addView(city);
        gap(10);
        c.addView(btnPrimary("GET FORECAST", new Runnable() { public void run() { fetch(); } }));
        gap(12);
        out = text("Free, no key - Open-Meteo. Enter a city and tap GET FORECAST.");
        c.addView(out);
    }

    private void fetch() {
        final String q = city.getText().toString().trim();
        if (q.isEmpty()) { toast("Enter a city first"); return; }
        out.setText("Looking up " + q + "...");
        bg(new Runnable() {
            public void run() {
                final String r = skills.weather(q);
                runOnUiThread(new Runnable() { public void run() { out.setText(r); } });
            }
        });
    }
}
