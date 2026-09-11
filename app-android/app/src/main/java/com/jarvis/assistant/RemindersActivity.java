package com.jarvis.assistant;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Reminders page - set "in N minutes to TEXT", list, delete. */
public class RemindersActivity extends BasePage {
    private EditText mins;
    private EditText text;
    private EditText del;
    private TextView list;
    private Skills skills;

    @Override protected String titleText() { return "Reminders"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        label("Remind me in (minutes):");
        gap(4);
        mins = input("10", false);
        c.addView(mins);
        gap(6);
        label("To do:");
        gap(4);
        text = input("e.g. call mom", false);
        c.addView(text);
        gap(10);
        c.addView(btnPrimary("SET REMINDER", new Runnable() { public void run() { add(); } }));
        gap(10);
        del = input("Delete reminder number", false);
        c.addView(del);
        gap(8);
        c.addView(btn("DELETE", new Runnable() { public void run() { delR(); } }));
        gap(12);
        list = text("");
        c.addView(list);
        refresh();
    }

    private void add() {
        try {
            int m = Integer.parseInt(mins.getText().toString().trim());
            String t = text.getText().toString().trim();
            if (t.isEmpty()) { toast("What should I remind you to do?"); return; }
            skills.addReminder(m, t);
            mins.setText(""); text.setText("");
            toast("Reminder set");
            refresh();
        } catch (Exception e) { toast("Minutes must be a number"); }
    }

    private void delR() {
        try {
            long id = Long.parseLong(del.getText().toString().trim());
            skills.delReminder(id);
            del.setText("");
            refresh();
        } catch (Exception e) { toast("Enter a reminder number"); }
    }

    private void refresh() {
        list.setText(skills.reminderList());
    }
}
