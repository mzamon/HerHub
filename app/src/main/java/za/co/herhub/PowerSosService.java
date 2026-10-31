package za.co.herhub;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

/**
 * Hands-free SOS. Android doesn't let apps read the power button directly, so this watches for two screen on/off
 * events in quick succession (what a double press of the power button causes). A 5-second cancel window follows
 * before anything is sent.
 */
public class PowerSosService extends Service {
    static final int ID_STATUS = 7001, ID_ALERT = 7002;
    static final String CH_STATUS = "herhub_sos_status", CH_ALERT = "herhub_sos_alert";
    static final String ACT_CANCEL = "za.co.herhub.SOS_CANCEL", ACT_TEST = "za.co.herhub.SOS_TEST";

    private final Handler h = new Handler(Looper.getMainLooper());
    private long first = 0, lastTrigger = 0;
    private int count = 0;
    private boolean pending = false;
    private Runnable fire;

    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (!Intent.ACTION_SCREEN_OFF.equals(a) && !Intent.ACTION_SCREEN_ON.equals(a)) return;
            long now = SystemClock.elapsedRealtime();
            if (now - first > 1800) { first = now; count = 1; } else count++;
            if (count >= 2 && !pending && now - lastTrigger > 15000) {
                count = 0;
                lastTrigger = now;
                arm();
            }
        }
    };

    public static void start(Context c) {
        try { ContextCompat.startForegroundService(c, new Intent(c, PowerSosService.class)); } catch (Throwable ignored) { }
    }
    public static void stop(Context c) {
        try { c.stopService(new Intent(c, PowerSosService.class)); } catch (Throwable ignored) { }
    }
    public static void test(Context c) {
        try { ContextCompat.startForegroundService(c, new Intent(c, PowerSosService.class).setAction(ACT_TEST)); } catch (Throwable ignored) { }
    }

    @Override public void onCreate() {
        super.onCreate();
        makeChannels(this);
        try {
            Notification n = new NotificationCompat.Builder(this, CH_STATUS)
                    .setSmallIcon(R.drawable.ic_stat_sos)
                    .setContentTitle("HerHub SOS shortcut is on")
                    .setContentText("Press the power button twice quickly to send an SOS.")
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                    .build();
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID_STATUS, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else startForeground(ID_STATUS, n);
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_SCREEN_OFF);
            f.addAction(Intent.ACTION_SCREEN_ON);
            ContextCompat.registerReceiver(this, rx, f, ContextCompat.RECEIVER_NOT_EXPORTED);
        } catch (Throwable t) {
            Store.putB(this, "sos_power", false);
            stopSelf();
        }
    }

    @Override public int onStartCommand(Intent i, int flags, int startId) {
        if (i != null) {
            if (ACT_CANCEL.equals(i.getAction())) cancel();
            else if (ACT_TEST.equals(i.getAction()) && !pending) arm();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(rx); } catch (Throwable ignored) { }
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    static void makeChannels(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            nm.createNotificationChannel(new NotificationChannel(CH_STATUS, "SOS shortcut status", NotificationManager.IMPORTANCE_LOW));
            nm.createNotificationChannel(new NotificationChannel(CH_ALERT, "SOS alerts", NotificationManager.IMPORTANCE_HIGH));
        }
    }

    private boolean canNotify() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void arm() {
        pending = true;
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(VibrationEffect.createWaveform(new long[]{0, 250, 120, 250}, -1));
        } catch (Throwable ignored) { }
        // Same as tapping SOS: bring up the Personal Safety & Emergency screen straight away.
        final Intent sosScreen = new Intent(this, MainActivity.class).putExtra("sos", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { startActivity(sosScreen); } catch (Throwable ignored) { }
        if (canNotify()) {
            PendingIntent sosPi = PendingIntent.getActivity(this, 3, sosScreen, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent cancel = PendingIntent.getService(this, 1, new Intent(this, PowerSosService.class).setAction(ACT_CANCEL), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationManagerCompat.from(this).notify(ID_ALERT, new NotificationCompat.Builder(this, CH_ALERT)
                    .setSmallIcon(R.drawable.ic_stat_sos)
                    .setContentTitle("SOS will be sent in 5 seconds")
                    .setContentText("Tap Cancel if this was a mistake.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(0, "Cancel", cancel)
                    .addAction(0, "Call 112", call112(5))
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(sosPi, true)
                    .setContentIntent(sosPi)
                    .setOngoing(true).build());
        }
        fire = this::execute;
        h.postDelayed(fire, 5000);
    }

    /** Opens the dialer on 112 (one tap to call). */
    private PendingIntent call112(int req) {
        Intent d = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, req, d, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void cancel() {
        if (fire != null) h.removeCallbacks(fire);
        pending = false;
        try { NotificationManagerCompat.from(this).cancel(ID_ALERT); } catch (Throwable ignored) { }
    }

    private void execute() {
        pending = false;
        final Context app = getApplicationContext();
        if (Store.getB(app, "sos_alarm", false) && !Siren.isOn()) Siren.start(app);
        final String nums = numbers(app);
        Loc.fetch(app, l -> {
            String msg = "I need help. Please call me or come to me now."
                    + (l != null ? "\nMy location: " + Loc.link(l) : "\n(Location unavailable)") + "\nSent from HerHub";
            int sent = 0;
            if (Store.getB(app, "sos_sms", true) && !nums.isEmpty()
                    && ContextCompat.checkSelfPermission(app, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
                sent = sms(app, nums, msg);
            result(sent, nums.isEmpty(), msg, nums);
            if (Store.getB(app, "sos_call", false)) callNow(app, nums);
        });
    }

    /**
     * Android does not let third-party apps place a call to an emergency number (112) by themselves,
     * so we automatically call the first trusted contact instead. If that is not possible, the dialer
     * opens with 112 typed in.
     */
    private void callNow(Context app, String nums) {
        String first = nums.isEmpty() ? "" : nums.split(";")[0].trim();
        if (!first.isEmpty() && ContextCompat.checkSelfPermission(app, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Uri tel = Uri.fromParts("tel", first, null);
            try {
                android.telecom.TelecomManager tm = (android.telecom.TelecomManager) app.getSystemService(Context.TELECOM_SERVICE);
                if (tm != null) {
                    Bundle extras = new Bundle();
                    extras.putBoolean(android.telecom.TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true);
                    tm.placeCall(tel, extras);
                    return;
                }
            } catch (Throwable ignored) { }
            try { app.startActivity(new Intent(Intent.ACTION_CALL, tel).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return; }
            catch (Throwable ignored) { }
        }
        try { app.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Throwable ignored) { }
    }

    private static String numbers(Context c) {
        JSONArray a = Store.arr(c, "d_circle");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(o.optString("phone"));
        }
        return sb.toString();
    }

    private static int sms(Context c, String nums, String msg) {
        int ok = 0;
        SmsManager sm = Build.VERSION.SDK_INT >= 31 ? c.getSystemService(SmsManager.class) : SmsManager.getDefault();
        if (sm == null) return 0;
        for (String n : nums.split(";")) {
            try {
                ArrayList<String> parts = sm.divideMessage(msg);
                sm.sendMultipartTextMessage(n, null, parts, null, null);
                ok++;
            } catch (Throwable ignored) { }
        }
        return ok;
    }

    private void result(int sent, boolean noContacts, String msg, String nums) {
        cancel();
        if (!canNotify()) return;
        Intent open = new Intent(this, MainActivity.class).putExtra("sos", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 2, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String text = sent > 0 ? "SOS text with your location sent to " + sent + (sent == 1 ? " contact." : " contacts.")
                : noContacts ? "No trusted contacts saved. Open HerHub to call for help."
                : "Couldn't text automatically. Open HerHub to alert your circle or call 112.";
        String first = nums.isEmpty() ? "" : nums.split(";")[0].trim();
        // Tapping the notification opens the SMS conversation that was just sent.
        PendingIntent smsPi = pi;
        if (!first.isEmpty()) {
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(first)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            smsPi = PendingIntent.getActivity(this, 4, smsIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CH_ALERT)
                .setSmallIcon(R.drawable.ic_stat_sos)
                .setContentTitle("SOS triggered")
                .setContentText(text + (first.isEmpty() ? "" : " Tap to open the chat."))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text + (first.isEmpty() ? "" : "\nTap to open the SMS chat, or send the same message on WhatsApp.")))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(smsPi)
                .setAutoCancel(true);
        // WhatsApp can't be sent silently: each button opens the chat with the SOS text ready, and you tap Send.
        String[] list = nums.isEmpty() ? new String[0] : nums.split(";");
        for (int k = 0; k < list.length && k < 2; k++) {
            String wa = waNumber(list[k]);
            if (wa.isEmpty()) continue;
            Intent w = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + wa + "?text=" + Uri.encode(msg)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            nb.addAction(0, list.length > 1 ? "WhatsApp " + (k + 1) : "WhatsApp",
                    PendingIntent.getActivity(this, 10 + k, w, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        }
        nb.addAction(0, "Call 112", call112(6));
        if (canNotify()) NotificationManagerCompat.from(this).notify(ID_ALERT + 1, nb.build());
    }

    /** Digits only, South African numbers starting with 0 become 27…, as wa.me needs. */
    private static String waNumber(String raw) {
        String d = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (d.startsWith("00")) d = d.substring(2);
        else if (d.startsWith("0")) d = "27" + d.substring(1);
        return d.length() >= 9 ? d : "";
    }
}
