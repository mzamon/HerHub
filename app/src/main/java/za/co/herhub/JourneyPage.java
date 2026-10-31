package za.co.herhub;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.util.Locale;

public class JourneyPage extends Page {
    static final int ALARM_ID = 9001;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Runnable tick;
    private boolean warned;

    public static JSONObject active(android.content.Context c) {
        try {
            String s = Store.get(c, "journey", null);
            return s == null ? null : new JSONObject(s);
        } catch (Throwable t) { return null; }
    }

    @Override protected String title() { return "Safe Journey"; }

    @Override protected void build() {
        stopTick();
        final JSONObject j = active(ctx);
        if (j == null) buildSetup(); else buildActive(j);
    }

    private void buildSetup() {
        p("Tell HerHub where you're going and when you expect to arrive. You'll get a check-in reminder, and can alert your circle if something's wrong.");
        LinearLayout c = card();
        final TextInputLayout dest = U.field(ctx, "Where are you going?");
        final TextInputLayout mins = U.field(ctx, "Minutes until you arrive", InputType.TYPE_CLASS_NUMBER);
        if (mins.getEditText() != null) mins.getEditText().setText("30");
        c.addView(dest);
        U.add(c, mins, 10);
        LinearLayout q = U.row(c);
        U.add(c, q, 8);
        for (final int m : new int[]{15, 30, 60, 120})
            U.addW(q, U.btn(ctx, m < 60 ? m + "m" : (m / 60) + "h", false, () -> { if (mins.getEditText() != null) mins.getEditText().setText(String.valueOf(m)); }), 2);
        final CheckBox cb = new CheckBox(ctx);
        cb.setText("Message my trusted circle when I start");
        cb.setTextColor(U.c(ctx, R.color.ink));
        U.add(c, cb, 10);
        btn(c, "▶ Start journey", true, () -> {
            String d = U.val(dest);
            int m;
            try { m = Integer.parseInt(U.val(mins)); } catch (Exception e) { m = 0; }
            if (d.isEmpty()) { U.toast(ctx, "Enter your destination."); return; }
            if (m < 1 || m > 720) { U.toast(ctx, "Enter minutes between 1 and 720."); return; }
            final int mm = m;
            withNotifPermission(() -> start(d, mm, cb.isChecked()));
        });
        p("Reminders are best-effort. Don't rely on HerHub alone: tell someone your plans too.");
    }

    private void start(String dest, int mins, boolean tell) {
        long end = System.currentTimeMillis() + mins * 60_000L;
        Store.put(ctx, "journey", Store.obj("dest", dest, "end", String.valueOf(end)).toString());
        ReminderReceiver.scheduleAt(ctx, ALARM_ID, end, "Safe journey check-in", "Have you arrived safely at " + dest + "? Open HerHub to confirm.");
        refresh();
        if (tell) alertCircle("I'm heading to " + dest + ". I expect to arrive in about " + mins + " minutes.");
    }

    private void buildActive(final JSONObject j) {
        final String dest = j.optString("dest");
        final long end = Long.parseLong(j.optString("end", "0"));
        LinearLayout c = card();
        head(c, R.drawable.ic_safe_journey, "Journey to " + dest, 18, R.color.ink, true, 0);
        final TextView cd = in(c, "", 34, R.color.brand, true, 8);
        final TextView sub = in(c, "", 13, R.color.ink_soft, false, 2);
        btn(c, "I've arrived safely", true, R.drawable.ic_arrival_check, () -> finish(true));
        btn(c, "Add 15 minutes", false, R.drawable.ic_checkin_clock, () -> extend(j, end));
        btn(c, "Alert my trusted circle", false, R.drawable.ic_sos_contact, () -> alertCircle("I'm on a journey to " + dest + " and need you to check on me."));
        btn(c, "Cancel journey", false, () -> finish(false));
        warned = false;
        tick = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                long left = end - System.currentTimeMillis();
                if (left > 0) {
                    long s = left / 1000;
                    cd.setText(s >= 3600 ? String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60) : String.format(Locale.US, "%02d:%02d", s / 60, s % 60));
                    sub.setText("until your expected arrival");
                } else {
                    cd.setText("00:00");
                    cd.setTextColor(U.c(ctx, R.color.danger));
                    sub.setText("Time's up. Are you safe? Confirm arrival or alert your circle.");
                    if (!warned) { warned = true; U.info(ctx, "Are you safe?", "Your journey timer has finished. If you've arrived, tap “I've arrived safely”. If not, alert your trusted circle."); }
                }
                h.postDelayed(this, 1000);
            }
        };
        h.post(tick);
    }

    private void extend(JSONObject j, long end) {
        long ne = Math.max(end, System.currentTimeMillis()) + 15 * 60_000L;
        String dest = j.optString("dest");
        Store.put(ctx, "journey", Store.obj("dest", dest, "end", String.valueOf(ne)).toString());
        ReminderReceiver.scheduleAt(ctx, ALARM_ID, ne, "Safe journey check-in", "Have you arrived safely at " + dest + "? Open HerHub to confirm.");
        refresh();
    }

    private void finish(boolean arrived) {
        ReminderReceiver.cancel(ctx, ALARM_ID);
        Store.remove(ctx, "journey");
        refresh();
        if (arrived) {
            U.toast(ctx, "Glad you're safe");
            if (!circleNumbers().isEmpty()) U.confirm(ctx, "Let your circle know?", "Send a quick “arrived safely” message.", "Send",
                    () -> U.sms(ctx, circleNumbers(), "I've arrived safely. Thanks for looking out for me."));
        }
    }

    private void stopTick() { if (tick != null) { h.removeCallbacks(tick); tick = null; } }
    @Override public void onDestroyView() { stopTick(); super.onDestroyView(); }
}
