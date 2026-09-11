package com.jarvis.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Fires a system notification when a reminder is due (via AlarmManager). */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String text = i.getStringExtra("text");
        if (text == null || text.isEmpty()) text = "Reminder";
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("jarvis", "JARVIS reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) b = new Notification.Builder(c, "jarvis");
        else b = new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("JARVIS reminder")
                .setContentText(text)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());
        try {
            nm.notify((int) (System.currentTimeMillis() / 1000), b.build());
        } catch (Exception ignored) { }
    }
}
