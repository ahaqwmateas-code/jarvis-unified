package com.jarvis.assistant;

import android.graphics.Bitmap;

/** One chat bubble. role: USER / ASSISTANT / SYSTEM. image: optional generated picture. */
public class ChatMessage {
    public static final int USER = 0;
    public static final int ASSISTANT = 1;
    public static final int SYSTEM = 2;

    public int role;
    public String text;
    public Bitmap image;

    public ChatMessage(int role, String text) {
        this.role = role;
        this.text = text;
    }
}
