package za.co.herhub;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.LinearLayout;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.Locale;

public class NotifyPage extends Page {
    // {id, icon key, title, body, defaultHour, defaultMinute}
    static final String[][] R_ = {
            {"9101", "water", "Drink water", "Time for a glass of water.", "10", "0"},
            {"9102", "pill", "Medication / pill", "Time to take your medication.", "8", "0"},
            {"9103", "mood", "Mood check-in", "How are you feeling today? Take a moment for yourself.", "20", "0"},
            {"9104", "safety", "Safety check-in", "Quick safety check: are your trusted contacts up to date?", "18", "0"},
            {"9105", "period", "Log period & symptoms", "Log today's flow and symptoms in HerHub.", "21", "0"}
    };

    public static void rescheduleAll(Context c) {
        try {
            for (String[] r : R_) {
                int id = Integer.parseInt(r[0]);
                if (Store.getB(c, "rem_" + id + "_on", false))
                    ReminderReceiver.scheduleDaily(c, id, Store.getI(c, "rem_" + id + "_h", Integer.parseInt(r[4])), Store.getI(c, "rem_" + id + "_m", Integer.parseInt(r[5])), r[2], r[3]);
            }
        } catch (Throwable ignored) { }
    }

    static int remIcon(String k) {
        switch (k) {
            case "water": return R.drawable.ic_water_tracker;
            case "pill": return R.drawable.ic_medication;
            case "mood": return R.drawable.ic_mood_checkin;
            case "safety": return R.drawable.ic_safety_check;
            default: return R.drawable.ic_period_calendar;
        }
    }

    @Override protected String title() { return "Smart Notifications"; }

    @Override protected void build() {
        p("Choose the reminders you want. They repeat daily at the time you pick, and everything is controlled on your phone.");
        for (final String[] r : R_) {
            final int id = Integer.parseInt(r[0]);
            final int hh = Store.getI(ctx, "rem_" + id + "_h", Integer.parseInt(r[4]));
            final int mm = Store.getI(ctx, "rem_" + id + "_m", Integer.parseInt(r[5]));
            boolean on = Store.getB(ctx, "rem_" + id + "_on", false);
            LinearLayout c = card();
            LinearLayout top = U.row(c);
            top.addView(U.tile(ctx, remIcon(r[1]), r[2], 40), new LinearLayout.LayoutParams(U.dp(ctx, 40), U.dp(ctx, 40)));
            android.widget.TextView rt = U.tv(ctx, r[2], 16, R.color.ink, true);
            rt.setPadding(U.dp(ctx, 12), 0, 0, 0);
            top.addView(rt, new LinearLayout.LayoutParams(0, -2, 1f));
            final MaterialSwitch sw = new MaterialSwitch(ctx);
            sw.setChecked(on);
            sw.setContentDescription(r[2]);
            top.addView(sw);
            in(c, "Daily at " + String.format(Locale.US, "%02d:%02d", hh, mm), 13, R.color.ink_soft, false, 4);
            btn(c, "Change time", false, () -> {
                TimePickerDialog d = new TimePickerDialog(ctx, (v, h2, m2) -> {
                    Store.putI(ctx, "rem_" + id + "_h", h2);
                    Store.putI(ctx, "rem_" + id + "_m", m2);
                    if (Store.getB(ctx, "rem_" + id + "_on", false)) ReminderReceiver.scheduleDaily(ctx, id, h2, m2, r[2], r[3]);
                    refresh();
                }, hh, mm, DateFormat.is24HourFormat(ctx));
                d.show();
            });
            sw.setOnCheckedChangeListener((v, checked) -> {
                if (checked) {
                    withNotifPermission(() -> {
                        Store.putB(ctx, "rem_" + id + "_on", true);
                        ReminderReceiver.scheduleDaily(ctx, id, hh, mm, r[2], r[3]);
                        U.toast(ctx, "Reminder on");
                    });
                } else {
                    Store.putB(ctx, "rem_" + id + "_on", false);
                    ReminderReceiver.cancel(ctx, id);
                }
            });
        }
        p("Battery-saver modes can delay reminders. If they don't appear, allow HerHub notifications in system settings.");
    }
}
