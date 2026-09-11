package com.jarvis.assistant;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Local SQLite storage for notes and reminders. */
public class NotesDb extends SQLiteOpenHelper {
    public NotesDb(Context c) { super(c, "jarvis.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT, ts INTEGER)");
        db.execSQL("CREATE TABLE reminders(id INTEGER PRIMARY KEY AUTOINCREMENT, when_ms INTEGER, text TEXT)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int a, int b) { }

    public void addNote(String t) {
        getWritableDatabase().execSQL("INSERT INTO notes(text,ts) VALUES(?,?)",
                new Object[]{t, System.currentTimeMillis()});
    }

    public List<String> notes() {
        List<String> r = new ArrayList<String>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,text FROM notes ORDER BY id", null);
        while (c.moveToNext()) r.add(c.getInt(0) + ". " + c.getString(1));
        c.close();
        return r;
    }

    public void delNote(int id) {
        getWritableDatabase().execSQL("DELETE FROM notes WHERE id=?", new Object[]{id});
    }

    public long addReminder(long whenMs, String t) {
        getWritableDatabase().execSQL("INSERT INTO reminders(when_ms,text) VALUES(?,?)", new Object[]{whenMs, t});
        Cursor c = getReadableDatabase().rawQuery("SELECT last_insert_rowid()", null);
        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    public List<Object[]> reminders() {
        List<Object[]> r = new ArrayList<Object[]>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,when_ms,text FROM reminders ORDER BY when_ms", null);
        while (c.moveToNext()) r.add(new Object[]{c.getLong(0), c.getLong(1), c.getString(2)});
        c.close();
        return r;
    }

    public void delReminder(long id) {
        getWritableDatabase().execSQL("DELETE FROM reminders WHERE id=?", new Object[]{id});
    }
}
