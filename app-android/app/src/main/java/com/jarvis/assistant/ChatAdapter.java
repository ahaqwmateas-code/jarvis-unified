package com.jarvis.assistant;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Renders chat bubbles (user right / assistant left), with optional inline images. */
public class ChatAdapter extends BaseAdapter {
    private final Context ctx;
    private final List<ChatMessage> messages;

    public ChatAdapter(Context c, List<ChatMessage> m) {
        ctx = c;
        messages = m;
    }

    @Override public int getCount() { return messages.size(); }
    @Override public Object getItem(int i) { return messages.get(i); }
    @Override public long getItemId(int i) { return i; }

    @Override
    public View getView(int pos, View cv, ViewGroup parent) {
        ChatMessage m = messages.get(pos);
        boolean user = m.role == ChatMessage.USER;

        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(10), dp(6), dp(10), dp(6));

        LinearLayout bubble = new LinearLayout(ctx);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(user ? Color.rgb(9, 56, 34) : Color.rgb(13, 22, 32));
        gd.setCornerRadius(dp(14));
        bubble.setBackground(gd);

        if (m.image != null) {
            ImageView iv = new ImageView(ctx);
            iv.setImageBitmap(m.image);
            iv.setAdjustViewBounds(true);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            bubble.addView(iv, new LinearLayout.LayoutParams(dp(240), LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        if (m.text != null && !m.text.isEmpty()) {
            TextView tv = new TextView(ctx);
            tv.setText(m.text);
            tv.setTextColor(user ? Color.rgb(190, 255, 215) : Color.rgb(220, 230, 226));
            tv.setTextSize(15);
            tv.setLineSpacing(dp(2), 1f);
            bubble.addView(tv);
        }

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(user ? Gravity.RIGHT : Gravity.LEFT);
        row.addView(bubble, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrap.addView(row);
        return wrap;
    }

    private int dp(int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
