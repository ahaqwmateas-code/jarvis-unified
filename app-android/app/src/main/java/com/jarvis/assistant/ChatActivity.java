package com.jarvis.assistant;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** The AI chat page - voice in, brain out. */
public class ChatActivity extends Activity {
    private ListView list;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<ChatMessage>();
    private EditText input;
    private TextView status;
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
                "JARVIS ready - ask me anything, or tap MIC and speak."));
        adapter.notifyDataSetChanged();

        setupVoice();
    }

    private View buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5, 8, 12));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setBackgroundColor(Color.rgb(7, 16, 25));
        top.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(btn("BACK", new Runnable() { public void run() { finish(); } }));
        TextView title = new TextView(this);
        title.setText("Chat");
        title.setTextColor(Color.rgb(51, 209, 122));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(10), 0, 0, 0);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        status = new TextView(this);
        status.setTextColor(Color.rgb(140, 160, 150));
        status.setTextSize(11);
        row.addView(status);
        top.addView(row);
        root.addView(top);

        list = new ListView(this);
        list.setBackgroundColor(Color.rgb(5, 8, 12));
        list.setDivider(null);
        list.setDividerHeight(0);
        adapter = new ChatAdapter(this, messages);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.HORIZONTAL);
        in.setBackgroundColor(Color.rgb(7, 16, 25));
        in.setPadding(dp(8), dp(8), dp(8), dp(8));
        in.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(140, 160, 150));
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
        in.addView(btn("MIC", new Runnable() { public void run() { toggleMic(); } }));
        in.addView(btn("SEND", new Runnable() { public void run() { send(); } }));
        root.addView(in);
        return root;
    }

    private Button btn(String label, final Runnable r) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.rgb(51, 209, 122));
        b.setBackgroundColor(Color.rgb(12, 22, 32));
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { r.run(); } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    @Override
    protected void onResume() {
        super.onResume();
        String persona = prefs.getString("persona", "assistant");
        boolean keyed = !prefs.getString("groq_key", "").isEmpty()
                || !prefs.getString("gemini_key", "").isEmpty()
                || !prefs.getString("openrouter_key", "").isEmpty()
                || !prefs.getString("cerebras_key", "").isEmpty()
                || !prefs.getString("mistral_key", "").isEmpty()
                || !prefs.getString("xai_key", "").isEmpty()
                || !prefs.getString("deepseek_key", "").isEmpty()
                || !prefs.getString("github_key", "").isEmpty()
                || !prefs.getString("custom_key", "").isEmpty()
                || !prefs.getString("ollama_url", "").isEmpty();
        status.setText("persona: " + persona + " | " + (keyed ? "your keys" : "free"));
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
                                    "No brain responded - add a free key in Settings, or check your connection. Built-in skills still work - type \"help\".",
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
