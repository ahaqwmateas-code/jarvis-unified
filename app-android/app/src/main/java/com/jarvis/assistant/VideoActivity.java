package com.jarvis.assistant;

import android.widget.LinearLayout;

/** Video page - explains the core-only MP4 pipeline. */
public class VideoActivity extends BasePage {
    @Override protected String titleText() { return "Video"; }

    @Override
    protected void build(LinearLayout c) {
        Skills skills = new Skills(this, new Brain(this));
        gap(8);
        c.addView(text("Real MP4 video generation runs on the full JARVIS core, because it needs ffmpeg, AI scene frames and Ken Burns motion on your Kali phone."));
        gap(12);
        c.addView(btnPrimary("OPEN FULL CORE", new Runnable() { public void run() { go(ServerActivity.class); } }));
        gap(10);
        c.addView(label("In the core, run:  video make <prompt>"));
        gap(12);
        c.addView(text(skills.videoGuide()));
    }
}
