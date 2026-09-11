package com.jarvis.assistant;

import android.widget.LinearLayout;

/** CLI Hub page - explains the CLI-Anything registry on the core. */
public class ClihubActivity extends BasePage {
    @Override protected String titleText() { return "CLI Hub"; }

    @Override
    protected void build(LinearLayout c) {
        Skills skills = new Skills(this, new Brain(this));
        gap(8);
        c.addView(text("CLI-Anything's CLI-Hub is a registry of 40+ agent-native CLIs (Claude Code, OpenCode, OpenClaw, Codex, Qodercli, Goose, GitHub Copilot). It runs on the Kali core."));
        gap(12);
        c.addView(btnPrimary("OPEN FULL CORE", new Runnable() { public void run() { go(ServerActivity.class); } }));
        gap(10);
        c.addView(label("In the core, run:  clihub list    or    clihub install <name>"));
        gap(12);
        c.addView(text(skills.clihubGuide()));
    }
}
