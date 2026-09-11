package com.jarvis.assistant;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String DEFAULT_URL = "http://127.0.0.1:8000";

    private WebView web;
    private EditText urlBar;
    private LinearLayout bar;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("jarvis", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5, 8, 12));

        // ---- top strip (always visible) ----
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setBackgroundColor(Color.rgb(7, 16, 25));
        strip.setPadding(16, 8, 16, 8);

        TextView title = new TextView(this);
        title.setText("JARVIS");
        title.setTextColor(Color.rgb(51, 209, 122));
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tlp);
        strip.addView(title);

        Button gear = new Button(this);
        gear.setText("\u2699");
        gear.setTextColor(Color.rgb(51, 209, 122));
        gear.setBackgroundColor(Color.rgb(7, 16, 25));
        gear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bar.setVisibility(bar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        strip.addView(gear);
        root.addView(strip);

        // ---- URL bar (hidden by default) ----
        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Color.rgb(7, 16, 25));
        bar.setPadding(8, 4, 8, 4);

        urlBar = new EditText(this);
        urlBar.setTextColor(Color.rgb(51, 209, 122));
        urlBar.setHintTextColor(Color.rgb(90, 120, 100));
        urlBar.setHint(DEFAULT_URL);
        urlBar.setText(prefs.getString("url", DEFAULT_URL));
        urlBar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(urlBar);

        Button go = new Button(this);
        go.setText("GO");
        go.setTextColor(Color.BLACK);
        go.setBackgroundColor(Color.rgb(51, 209, 122));
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String u = urlBar.getText().toString().trim();
                if (!u.startsWith("http")) u = "http://" + u;
                prefs.edit().putString("url", u).apply();
                web.loadUrl(u);
                bar.setVisibility(View.GONE);
            }
        });
        bar.addView(go);
        bar.setVisibility(View.GONE);
        root.addView(bar);

        // ---- WebView ----
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                String html = "<html><body style='background:#05080c;color:#33d17a;font-family:sans-serif;padding:40px;'>"
                        + "<h2>JARVIS not reachable</h2>"
                        + "<p>Could not reach <b>" + failingUrl + "</b></p>"
                        + "<p>Tap the gear top-right, set the server URL, then GO.</p>"
                        + "<p>Tip: start JARVIS on the phone with<br><b>bash ~/.jarvis/start.sh</b></p>"
                        + "</body></html>";
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        setContentView(root);
        web.loadUrl(prefs.getString("url", DEFAULT_URL));
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
