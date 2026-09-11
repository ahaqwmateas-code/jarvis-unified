package com.jarvis.assistant;

import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Composio page - 1,500+ app tools (Gmail, GitHub, Calendar, Notion...) via hosted MCP. */
public class ComposioActivity extends BasePage {
    private EditText tool;
    private EditText args;
    private TextView out;
    private SharedPreferences prefs;

    @Override protected String titleText() { return "Composio"; }

    @Override
    protected void build(LinearLayout c) {
        prefs = getSharedPreferences("jarvis", MODE_PRIVATE);
        gap(4);
        TextView intro = text("Composio gives JARVIS 1,500+ app tools (Gmail, GitHub, Google Calendar, Notion, Slack, and more) through one hosted MCP endpoint.");
        c.addView(intro);
        gap(10);
        c.addView(btnPrimary("CHECK STATUS", new Runnable() { public void run() { status(); } }));
        gap(8);
        c.addView(btn("ADD API KEY (SETTINGS)", new Runnable() { public void run() { go(SettingsActivity.class); } }));
        gap(12);

        label("Run a tool:");
        gap(4);
        tool = input("Tool name, e.g. GITHUB_SEARCH_REPOSITORIES", false);
        c.addView(tool);
        gap(6);
        args = input("Arguments (JSON), e.g. {\"query\":\"jarvis\"}", true);
        c.addView(args);
        gap(10);
        c.addView(btnPrimary("RUN TOOL", new Runnable() { public void run() { runTool(); } }));
        gap(12);
        out = text("Set your Composio API key in Settings, connect apps at dashboard.composio.dev, then CHECK STATUS.");
        c.addView(out);
    }

    private String key() {
        return prefs.getString("composio_key", "").trim();
    }

    private void status() {
        if (key().isEmpty()) {
            out.setText("No Composio API key yet.\nGet one at dashboard.composio.dev, add it in Settings (Composio field), connect your apps there, then CHECK STATUS.");
            return;
        }
        out.setText("Connecting to Composio...");
        bg(new Runnable() {
            public void run() {
                List<Mcp.Tool> tools = new Mcp(key()).listTools();
                final String s;
                if (tools == null) {
                    s = "Connection failed.\nCheck the API key in Settings and your internet connection.";
                } else if (tools.isEmpty()) {
                    s = "Connected, but no tools yet.\nConnect an app (e.g. Gmail, GitHub) at dashboard.composio.dev first.";
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Composio ONLINE - ").append(tools.size()).append(" tools:\n\n");
                    for (int i = 0; i < Math.min(tools.size(), 60); i++) {
                        sb.append(tools.get(i).name).append("\n");
                    }
                    if (tools.size() > 60) sb.append("... and ").append(tools.size() - 60).append(" more");
                    s = sb.toString().trim();
                }
                runOnUiThread(new Runnable() { public void run() { out.setText(s); } });
            }
        });
    }

    private void runTool() {
        if (key().isEmpty()) {
            out.setText("No Composio API key - add it in Settings first.");
            return;
        }
        final String name = tool.getText().toString().trim();
        final String a = args.getText().toString().trim();
        if (name.isEmpty()) { toast("Enter a tool name (see CHECK STATUS)"); return; }
        out.setText("Running " + name + "...");
        bg(new Runnable() {
            public void run() {
                final String r = new Mcp(key()).callTool(name, a);
                runOnUiThread(new Runnable() { public void run() { out.setText(r); } });
            }
        });
    }
}
