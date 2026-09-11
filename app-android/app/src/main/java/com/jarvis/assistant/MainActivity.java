package com.jarvis.assistant;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** JARVIS - home screen: skill dashboard + chat + voice. Everything one tap away. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(5, 8, 12);
    private static final int PANEL = Color.rgb(7, 16, 25);
    private static final int GREEN = Color.rgb(51, 209, 122);
    private static final int GREY = Color.rgb(140, 160, 150);

    private ListView list;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<ChatMessage>();
    private EditText input;
    private TextView personaLabel;
    private Brain brain;
    private Skills skills;
    private SharedPreferences prefs;
    private SpeechRecognizer sr;
    private boolean listening;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("jarvis", MODE_PRIVATE);
        brain = new Brain(this);
        skills = new Skills(this, brain);
        setContentView(buildUI());

        messages.add(new ChatMessage(ChatMessage.ASSISTANT,
                "JARVIS online - everything runs on this phone.\n"
                        + "Tap any skill button below\n"
                        + "Press MIC and speak\n"
                        + "Or just type anything\n"
                        + "Type \"help\" for the full command list."));
        adapter.notifyDataSetChanged();

        askPermissions();
        setupVoice();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("jarvis", "JARVIS reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        String persona = prefs.getString("persona", "assistant");
        boolean keyed = !prefs.getString("groq_key", "").isEmpty()
                || !prefs.getString("gemini_key", "").isEmpty()
                || !prefs.getString("openrouter_key", "").isEmpty()
                || !prefs.getString("cerebras_key", "").isEmpty()
                || !prefs.getString("mistral_key", "").isEmpty()
                || !prefs.getString("ollama_url", "").isEmpty();
        personaLabel.setText("persona: " + persona + "   |   brain: " + (keyed ? "your keys" : "free (no key)"));
    }

    private View buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // ---- top bar ----
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setBackgroundColor(PANEL);
        top.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("JARVIS");
        title.setTextColor(GREEN);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        tt.addView(title);
        personaLabel = new TextView(this);
        personaLabel.setTextColor(GREY);
        personaLabel.setTextSize(11);
        tt.addView(personaLabel);
        row.addView(tt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(btn("CORE", new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, ServerActivity.class)); }
        }));
        row.addView(btn("SET", new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        }));
        top.addView(row);
        root.addView(top);

        // ---- full-core banner ----
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setBackgroundColor(Color.rgb(10, 30, 20));
        banner.setPadding(dp(12), dp(6), dp(8), dp(6));
        TextView btxt = new TextView(this);
        btxt.setText("FULL CORE - 18 skills, 2165 personas, image & video, voice");
        btxt.setTextColor(Color.rgb(190, 255, 215));
        btxt.setTextSize(12);
        banner.addView(btxt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        banner.addView(btn("OPEN", new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, ServerActivity.class)); }
        }));
        banner.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, ServerActivity.class)); }
        });
        root.addView(banner);

        // ---- skill dashboard (tap to run) ----
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setBackgroundColor(PANEL);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(dp(8), dp(6), dp(8), dp(6));
        chips.addView(chip("Time", "time", false));
        chips.addView(chip("Calc", "calc ", true));
        chips.addView(chip("Password", "password 16", false));
        chips.addView(chip("Weather", "weather ", true));
        chips.addView(chip("Search", "search ", true));
        chips.addView(chip("Wiki", "wiki ", true));
        chips.addView(chip("Translate", "translate  to ", true));
        chips.addView(chip("Image", "image ", true));
        chips.addView(chip("Notes", "note list", false));
        chips.addView(chip("Remind", "remind me in 10 minutes to ", true));
        chips.addView(chip("System", "system", false));
        chips.addView(chip("Network", "network", false));
        chips.addView(chip("Lang", "lang list", false));
        chips.addView(chip("Build", "build ", true));
        chips.addView(chip("Video", "video", false));
        chips.addView(chip("Brain", "brain status", false));
        chips.addView(chip("Personas", "persona list", false));
        chips.addView(chip("Help", "help", false));
        hsv.addView(chips);
        root.addView(hsv);

        // ---- chat list ----
        list = new ListView(this);
        list.setBackgroundColor(BG);
        list.setDivider(null);
        list.setDividerHeight(0);
        adapter = new ChatAdapter(this, messages);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ---- input row ----
        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.HORIZONTAL);
        in.setBackgroundColor(PANEL);
        in.setPadding(dp(8), dp(8), dp(8), dp(8));
        in.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(this);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(GREY);
        input.setHint("Ask JARVIS anything...");
        input.setBackgroundColor(Color.rgb(12, 22, 32));
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent e) {
                if (actionId == EditorInfo.IME_ACTION_SEND) { send(); return true; }
                return false;
            }
        });
        in.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        in.addView(btn("MIC", new View.OnClickListener() { public void onClick(View v) { toggleMic(); } }));
        in.addView(btn("SEND", new View.OnClickListener() { public void onClick(View v) { send(); } }));
        root.addView(in);
        return root;
    }

    private Button btn(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(GREEN);
        b.setBackgroundColor(Color.rgb(12, 22, 32));
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private Button chip(String label, final String action, final boolean prefill) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(GREEN);
        b.setAllCaps(false);
        b.setTextSize(13);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(12, 22, 32));
        bg.setStroke(dp(1), Color.rgb(24, 84, 54));
        bg.setCornerRadius(dp(18));
        b.setBackground(bg);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (prefill) prefill(action);
                else process(action);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void prefill(String s) {
        input.setText(s);
        input.requestFocus();
        input.setSelection(s.length());
        try {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception ignored) { }
    }

    private void send() {
        final String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        process(text);
    }

    private void process(final String text) {
        messages.add(new ChatMessage(ChatMessage.USER, text));
        adapter.notifyDataSetChanged();
        scroll();

        final List<ChatMessage> history = new ArrayList<ChatMessage>(messages);
        final ChatMessage pending = new ChatMessage(ChatMessage.ASSISTANT, "...");
        messages.add(pending);
        adapter.notifyDataSetChanged();
        scroll();

        new Thread(new Runnable() {
            public void run() {
                Skills.Out so = skills.handle(text);
                if (so != null) {
                    final Skills.Out fo = so;
                    runOnUiThread(new Runnable() {
                        public void run() { replacePending(pending, fo.text, fo.image, fo.imageNote); }
                    });
                    return;
                }
                String persona = prefs.getString("persona", "assistant");
                final Brain.Result r = brain.ask(history, persona);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (r == null) {
                            replacePending(pending,
                                    "No brain responded. Add a free API key in SET (Groq / Gemini / OpenRouter), or check your connection.\nBuilt-in skills still work - tap Help.",
                                    null, null);
                        } else {
                            replacePending(pending, r.text + "\n\n- via " + r.provider, null, null);
                        }
                    }
                });
            }
        }).start();
    }

    private void replacePending(ChatMessage pending, String text, android.graphics.Bitmap image, String note) {
        int idx = messages.indexOf(pending);
        if (idx >= 0) messages.remove(idx);
        String t = (text == null ? "" : text) + (note == null ? "" : "\nSaved: " + note);
        ChatMessage m = new ChatMessage(ChatMessage.ASSISTANT, t);
        m.image = image;
        if (idx >= 0) messages.add(idx, m); else messages.add(m);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        list.post(new Runnable() {
            public void run() { list.setSelection(adapter.getCount() - 1); }
        });
    }

    private void askPermissions() {
        List<String> need = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                need.add(Manifest.permission.RECORD_AUDIO);
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                need.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), 1);
    }

    private void setupVoice() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                sr = SpeechRecognizer.createSpeechRecognizer(this);
                sr.setRecognitionListener(new RecognitionListener() {
                    public void onReadyForSpeech(Bundle p) { listening = true; }
                    public void onBeginningOfSpeech() { }
                    public void onRmsChanged(float v) { }
                    public void onBufferReceived(byte[] b) { }
                    public void onEndOfSpeech() { listening = false; }
                    public void onError(int e) { listening = false; }
                    public void onResults(Bundle res) {
                        listening = false;
                        ArrayList<String> r = res.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (r != null && !r.isEmpty()) input.setText(r.get(0));
                    }
                    public void onPartialResults(Bundle p) { }
                    public void onEvent(int t, Bundle p) { }
                });
            }
        } catch (Exception e) {
            sr = null;
        }
    }

    private void toggleMic() {
        if (sr == null) {
            Toast.makeText(this, "Voice input isn't available on this device.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (listening) { sr.stopListening(); listening = false; return; }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        try { sr.startListening(i); } catch (Exception e) { listening = false; }
    }

    @Override
    protected void onDestroy() {
        if (sr != null) { try { sr.destroy(); } catch (Exception e) { } }
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
