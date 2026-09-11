package com.jarvis.assistant;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Settings: every connected AI provider + local Ollama + JARVIS core URL. */
public class SettingsActivity extends Activity {
    private static class Field { String key; String label; EditText e; }

    private final List<Field> fields = new ArrayList<Field>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("jarvis", MODE_PRIVATE);
        setContentView(build());
    }

    private View build() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.rgb(5, 8, 12));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(18));
        sv.addView(root);

        TextView title = new TextView(this);
        title.setText("JARVIS - Settings");
        title.setTextColor(Color.rgb(51, 209, 122));
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Free keys:\nGroq groq.com | Gemini aistudio.google.com | OpenRouter openrouter.ai | Cerebras cloud.cerebras.ai | Mistral console.mistral.ai | xAI x.ai | DeepSeek platform.deepseek.com | GitHub Models github.com/settings/tokens\n\nCustom provider: any OpenAI-compatible API (base URL + key + model).\n\nLeave everything empty and JARVIS still answers using the free anonymous brain.");
        sub.setTextColor(Color.rgb(140, 160, 150));
        sub.setTextSize(12);
        sub.setPadding(0, dp(6), 0, dp(16));
        root.addView(sub);

        addField(root, "groq_key", "Groq API key (gsk_)");
        addField(root, "gemini_key", "Google Gemini key (AIza)");
        addField(root, "openrouter_key", "OpenRouter key (sk-or-)");
        addField(root, "cerebras_key", "Cerebras key");
        addField(root, "mistral_key", "Mistral key");
        addField(root, "xai_key", "xAI / Grok key");
        addField(root, "deepseek_key", "DeepSeek key");
        addField(root, "github_key", "GitHub Models key (ghp_)");
        addField(root, "custom_base", "Custom base URL (e.g. https://api.example.com/v1)");
        addField(root, "custom_key", "Custom provider key");
        addField(root, "custom_model", "Custom provider model (e.g. model-name)");
        addField(root, "ollama_url", "Local Ollama URL (optional)");
        addField(root, "server_url", "JARVIS Core server URL");

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button save = new Button(this);
        save.setText("SAVE");
        save.setTextColor(Color.BLACK);
        save.setBackgroundColor(Color.rgb(51, 209, 122));
        save.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { save(); } });
        row.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button test = new Button(this);
        test.setText("TEST BRAIN");
        test.setTextColor(Color.rgb(51, 209, 122));
        test.setBackgroundColor(Color.rgb(12, 22, 32));
        test.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { testBrain(); } });
        row.addView(test, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button core = new Button(this);
        core.setText("OPEN CORE");
        core.setTextColor(Color.rgb(51, 209, 122));
        core.setBackgroundColor(Color.rgb(12, 22, 32));
        core.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                save();
                startActivity(new Intent(SettingsActivity.this, ServerActivity.class));
            }
        });
        row.addView(core, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(row);
        return sv;
    }

    private void addField(LinearLayout root, String key, String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.rgb(180, 200, 190));
        tv.setTextSize(13);
        tv.setPadding(0, dp(10), 0, dp(4));
        root.addView(tv);

        EditText e = new EditText(this);
        e.setText(prefs.getString(key, ""));
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(90, 120, 100));
        e.setBackgroundColor(Color.rgb(12, 22, 32));
        e.setSingleLine(true);
        e.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.addView(e);

        Field f = new Field();
        f.key = key;
        f.label = label;
        f.e = e;
        fields.add(f);
    }

    private void save() {
        SharedPreferences.Editor ed = prefs.edit();
        for (Field f : fields) ed.putString(f.key, f.e.getText().toString().trim());
        ed.apply();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private void testBrain() {
        save();
        Toast.makeText(this, "Testing brain...", Toast.LENGTH_SHORT).show();
        final Brain brain = new Brain(this);
        new Thread(new Runnable() {
            public void run() {
                List<ChatMessage> h = new ArrayList<ChatMessage>();
                h.add(new ChatMessage(ChatMessage.USER, "Reply with exactly the word OK."));
                final Brain.Result r = brain.ask(h, "assistant");
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (r == null) Toast.makeText(SettingsActivity.this,
                                "No provider answered - check your keys or connection.", Toast.LENGTH_LONG).show();
                        else Toast.makeText(SettingsActivity.this,
                                "OK via " + r.provider, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
