package com.jarvis.assistant;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

/** Opens the full JARVIS core (the Python service on your Kali phone) inside the app.
 *  Useful for the heavy server-side skills: app-building, video, voice web UI. */
public class ServerActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences prefs = getSharedPreferences("jarvis", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5, 8, 12));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Color.rgb(7, 16, 25));
        bar.setPadding(8, 6, 8, 6);
        bar.addView(btn("←", new View.OnClickListener() { public void onClick(View v) { finish(); } }));
        bar.addView(btn("⟳", new View.OnClickListener() { public void onClick(View v) { web.reload(); } }));
        root.addView(bar);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.setBackgroundColor(Color.rgb(5, 8, 12));
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(android.webkit.WebView view, int errorCode, String description, String failingUrl) {
                String html = "<html><body style='background:#05080c;color:#33d17a;font-family:sans-serif;padding:40px;'>"
                        + "<h2>JARVIS core not reachable</h2>"
                        + "<p>Could not reach <b>" + failingUrl + "</b></p>"
                        + "<p>Start the core on your Kali phone with:<br><b>bash ~/.jarvis/start.sh</b></p>"
                        + "<p>Or set its address in SET (top-right) - e.g. http://127.0.0.1:8000 when it runs on this phone.</p>"
                        + "</body></html>";
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        setContentView(root);
        web.loadUrl(prefs.getString("server_url", "http://127.0.0.1:8000"));
    }

    private Button btn(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.rgb(51, 209, 122));
        b.setBackgroundColor(Color.rgb(12, 22, 32));
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
