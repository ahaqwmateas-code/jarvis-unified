package com.jarvis.assistant;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Notes page - add, list, delete. */
public class NotesActivity extends BasePage {
    private EditText add;
    private EditText del;
    private TextView list;
    private Skills skills;

    @Override protected String titleText() { return "Notes"; }

    @Override
    protected void build(LinearLayout c) {
        skills = new Skills(this, new Brain(this));
        add = input("New note...", false);
        c.addView(add);
        gap(8);
        c.addView(btnPrimary("ADD NOTE", new Runnable() { public void run() { addNote(); } }));
        gap(10);
        del = input("Delete note number", false);
        c.addView(del);
        gap(8);
        c.addView(btn("DELETE", new Runnable() { public void run() { delNote(); } }));
        gap(12);
        list = text("");
        c.addView(list);
        refresh();
    }

    private void addNote() {
        String t = add.getText().toString().trim();
        if (t.isEmpty()) { toast("Type a note first"); return; }
        skills.addNote(t);
        add.setText("");
        refresh();
    }

    private void delNote() {
        try {
            int i = Integer.parseInt(del.getText().toString().trim());
            skills.delNote(i);
            del.setText("");
            refresh();
        } catch (Exception e) { toast("Enter a note number"); }
    }

    private void refresh() {
        List<String> n = skills.notes();
        if (n.isEmpty()) list.setText("No notes yet - add one above.");
        else {
            StringBuilder sb = new StringBuilder();
            for (String s : n) sb.append(s).append("\n");
            list.setText(sb.toString().trim());
        }
    }
}
