package za.co.herhub;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CH = "herhub_reminders";

    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "HerHub reminders", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Check-ins, journey alerts and wellness reminders");
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override public void onReceive(Context c, Intent i) {
        try {
            int id = i.getIntExtra("id", 1);
            post(c, id, i.getStringExtra("title"), i.getStringExtra("body"));
            if (i.getBooleanExtra("daily", false))
                scheduleDaily(c, id, i.getIntExtra("h", 8), i.getIntExtra("m", 0), i.getStringExtra("title"), i.getStringExtra("body"));
        } catch (Throwable ignored) { }
    }

    static void post(Context c, int id, String title, String body) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        createChannel(c);
        PendingIntent open = PendingIntent.getActivity(c, id, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_stat_sos)
                .setContentTitle(title == null ? "HerHub" : title)
                .setContentText(body == null ? "" : body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body == null ? "" : body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(open);
        NotificationManagerCompat.from(c).notify(id, b.build());
    }

    private static PendingIntent pi(Context c, int id, String title, String body, boolean daily, int h, int m) {
        Intent in = new Intent(c, ReminderReceiver.class);
        in.putExtra("id", id).putExtra("title", title).putExtra("body", body).putExtra("daily", daily).putExtra("h", h).putExtra("m", m);
        return PendingIntent.getBroadcast(c, id, in, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void scheduleAt(Context c, int id, long when, String title, String body) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi(c, id, title, body, false, 0, 0));
        } catch (Throwable ignored) { }
    }

    public static void scheduleDaily(Context c, int id, int h, int m, String title, String body) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, m); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1);
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi(c, id, title, body, true, h, m));
        } catch (Throwable ignored) { }
    }

    public static void cancel(Context c, int id) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            am.cancel(pi(c, id, "", "", false, 0, 0));
        } catch (Throwable ignored) { }
    }
}
