package com.jarvis.assistant;

import android.graphics.Bitmap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Image generation page. */
public class ImageActivity extends BasePage {
    private EditText prompt;
    private ImageView img;
    private TextView status;
    private Skills skills;

    @Override protected String titleText() { return "Image"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        prompt = input("Describe the image, e.g. a red fox in snow", false);
        c.addView(prompt);
        gap(10);
        c.addView(btnPrimary("GENERATE", new Runnable() { public void run() { gen(); } }));
        gap(12);
        img = new ImageView(this);
        img.setAdjustViewBounds(true);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setVisibility(android.view.View.GONE);
        c.addView(img, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        gap(8);
        status = text("Free image generation (Gemini if a key is set, else Pollinations).");
        c.addView(status);
    }

    private void gen() {
        final String p = prompt.getText().toString().trim();
        if (p.isEmpty()) { toast("Describe an image first"); return; }
        status.setText("Generating...");
        bg(new Runnable() {
            public void run() {
                final Skills.Out o = skills.image(p);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (o.image != null) {
                            img.setImageBitmap(o.image);
                            img.setVisibility(android.view.View.VISIBLE);
                        } else {
                            img.setVisibility(android.view.View.GONE);
                        }
                        String note = o.imageNote == null ? "" : "\nSaved: " + o.imageNote;
                        status.setText(o.text + note);
                    }
                });
            }
        });
    }
}
