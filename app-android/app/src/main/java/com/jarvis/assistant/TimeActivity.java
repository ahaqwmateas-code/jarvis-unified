package com.jarvis.assistant;

import android.os.Bundle;
import android.widget.LinearLayout;
import java.util.Timer;
import java.util.TimerTask;

/** Live clock page. */
public class TimeActivity extends BasePage {
    private android.widget.TextView clock, date;
    private Timer timer;

    @Override protected String titleText() { return "Time"; }

    @Override
    protected void build(LinearLayout c) {
        gap(30);
        clock = big("");
        clock.setTextSize(40);
        clock.setGravity(android.view.Gravity.CENTER);
        c.addView(clock, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        date = text("");
        date.setGravity(android.view.Gravity.CENTER);
        date.setTextColor(GREY);
        c.addView(date, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        gap(10);
        tick();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() { runOnUiThread(new Runnable() { public void run() { tick(); } }); }
        }, 1000, 1000);
    }

    private void tick() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        String hh = String.format(java.util.Locale.US, "%02d", now.get(java.util.Calendar.HOUR_OF_DAY));
        String mm = String.format(java.util.Locale.US, "%02d", now.get(java.util.Calendar.MINUTE));
        String ss = String.format(java.util.Locale.US, "%02d", now.get(java.util.Calendar.SECOND));
        clock.setText(hh + ":" + mm + ":" + ss);
        date.setText(new java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.US).format(now.getTime()));
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
