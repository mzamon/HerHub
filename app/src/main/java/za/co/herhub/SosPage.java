package za.co.herhub;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

/** Personal Safety & Emergency: emergency lines, hands-free SOS, siren, GBV help and the "If you feel unsafe" guide. */
public class SosPage extends Page {

    private final androidx.activity.result.ActivityResultLauncher<String> callPerm = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), ok -> {
                if (!isAdded()) return;
                if (!ok) { Store.putB(ctx, "sos_call", false); U.toast(ctx, "Phone permission denied, so HerHub can't call by itself. It will open the dialer on 112 instead."); }
                refresh();
            });

    // The siren now lives in Siren.java. These wrappers keep older call sites working.
    public static boolean alarmOn() { return Siren.isOn(); }
    public static void stopAlarm() { Siren.stop(); }
    public static void toggleAlarm(Context c) { Siren.toggle(c); }

    private void enablePower() {
        locate(l -> {
            if (!isAdded()) return;
            if (!Loc.has(ctx)) { U.toast(ctx, "Location permission is needed so your SOS can include where you are."); refresh(); return; }
            withNotifPermission(() -> {
                if (Store.getB(ctx, "sos_sms", true)) withSms(this::finishEnable); else finishEnable();
            });
        });
    }

    private void finishEnable() {
        Store.putB(ctx, "sos_power", true);
        PowerSosService.start(ctx);
        U.toast(ctx, "Hands-free SOS is on");
        refresh();
    }

    private void disablePower() {
        Store.putB(ctx, "sos_power", false);
        PowerSosService.stop(ctx);
        refresh();
    }

    @Override protected String title() { return "Personal Safety & Emergency"; }

    // ---------------------------------------------------------------------------------------------------------

    @Override protected void build() {
        buildEmergencyCard();

        h("Hands-free SOS");
        buildHandsFree();

        h("Alert people you trust");
        LinearLayout a = card();
        in(a, "Send your live location to your trusted circle by SMS.", 14, R.color.ink, false, 0);
        btn(a, "Alert my trusted circle", true, R.drawable.ic_sos_contact,
                () -> alertCircle("I need help. Please call me or come to me now."));
        btn(a, "Manage trusted circle", false, R.drawable.ic_trusted_circle, () -> open(new CirclePage()));

        h("Panic alarm");
        buildSiren();

        h("Gender-based violence help (SA)");
        LinearLayout g = card();
        lineRow(g, R.drawable.ic_domestic_violence_support, "GBV Command Centre", "0800 428 428", "Free, 24/7 support and counselling", false);
        lineRow(g, R.drawable.ic_women_org, "Stop Gender Violence", "0800 150 150", "Support and referrals", false);

        h("If you feel unsafe");
        buildUnsafe();
        p("Numbers are for South Africa. Outside SA, use your local emergency number.");
    }

    /** Red-to-plum emergency card. Three different lines, each with its own icon. */
    private void buildEmergencyCard() {
        LinearLayout c = U.col(ctx);
        c.setBackground(U.gradient(ctx, R.color.danger, R.color.danger_dark, 26));
        c.setElevation(U.dp(ctx, 4));
        c.setPadding(U.dp(ctx, 18), U.dp(ctx, 20), U.dp(ctx, 18), U.dp(ctx, 18));
        U.add(box, c, 4);

        LinearLayout top = U.row(ctx);
        c.addView(top);
        ImageView badge = U.icon(ctx, R.drawable.ic_sos_alert, "SOS emergency", 64, 0);
        badge.setBackground(U.bg(ctx, R.color.surface, 0, 32));
        top.addView(badge, new LinearLayout.LayoutParams(U.dp(ctx, 64), U.dp(ctx, 64)));
        U.pulse(badge);
        LinearLayout words = U.col(ctx);
        words.setPadding(U.dp(ctx, 14), 0, 0, 0);
        words.addView(U.tv(ctx, "IN IMMEDIATE DANGER?", 11, R.color.white, true));
        TextView t = U.tv(ctx, "Emergency help", 24, R.color.white, true);
        t.setPadding(0, U.dp(ctx, 2), 0, 0);
        words.addView(t);
        top.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        in(c, "HerHub does not dispatch emergency responders. Call the emergency services directly.", 13, R.color.white, false, 12);

        lineRow(c, R.drawable.ic_emergency_phone, "Emergency (any network)", "112", "Police, ambulance and fire", true);
        lineRow(c, R.drawable.ic_police_station, "Police", "10111", "SAPS Flying Squad", true);
        lineRow(c, R.drawable.ic_ambulance, "Ambulance", "10177", "Public medical emergency", true);
    }

    private void buildHandsFree() {
        LinearLayout hf = card();
        LinearLayout hr = U.row(hf);
        hf.addView(hr);
        hr.addView(U.tile(ctx, R.drawable.ic_shake_phone, "Hands-free SOS", 44), new LinearLayout.LayoutParams(U.dp(ctx, 44), U.dp(ctx, 44)));
        TextView ht = U.tv(ctx, "Double-press power button", 16, R.color.ink, true);
        ht.setPadding(U.dp(ctx, 12), 0, 0, 0);
        hr.addView(ht, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final MaterialSwitch sw = new MaterialSwitch(ctx);
        sw.setChecked(Store.getB(ctx, "sos_power", false));
        sw.setContentDescription("Hands-free SOS");
        sw.setOnCheckedChangeListener((v, on) -> { if (on) enablePower(); else disablePower(); });
        hr.addView(sw);
        in(hf, "Press the power button twice quickly, even with the screen off. You get a 5-second chance to cancel, then HerHub texts your trusted circle your location and calls your first trusted contact. Android blocks apps from auto-dialling 112, so use the Call 112 button on the alert for that.", 13, R.color.ink_soft, false, 8);
        if (Store.getB(ctx, "sos_power", false)) {
            switchRow(hf, R.drawable.ic_sos_contact, "Text my circle automatically", "sos_sms", true, on -> { if (on) withSms(this::refresh); });
            switchRow(hf, R.drawable.ic_siren_alarm, "Also sound the siren (full volume, 10 flashlight blinks)", "sos_alarm", false, null);
            switchRow(hf, R.drawable.ic_phone_call, "Also call my first trusted contact", "sos_call", false, on -> {
                if (on && androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CALL_PHONE)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) callPerm.launch(android.Manifest.permission.CALL_PHONE);
            });
            btn(hf, "Test it (5-second countdown)", false, R.drawable.ic_checkin_clock, () -> PowerSosService.test(ctx));
        }
        in(hf, "Limits: Android doesn't allow apps to read the power button directly, so this is detected from the screen turning off and on. Some phones stop background apps to save battery, so set HerHub to \u201cUnrestricted\u201d in Settings > Apps > HerHub > Battery, and test it. Don't rely on it alone.", 12, R.color.ink_soft, false, 8);
    }

    private interface OnToggle { void on(boolean checked); }

    private void switchRow(LinearLayout parent, int icon, String label, final String key, boolean def, final OnToggle extra) {
        LinearLayout r = U.row(ctx);
        U.add(parent, r, 10);
        ImageView ic = U.icon(ctx, icon, label, 34, 0);
        r.addView(ic, new LinearLayout.LayoutParams(U.dp(ctx, 34), U.dp(ctx, 34)));
        TextView l = U.tv(ctx, label, 14, R.color.ink, false);
        l.setPadding(U.dp(ctx, 10), 0, U.dp(ctx, 6), 0);
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final MaterialSwitch s = new MaterialSwitch(ctx);
        s.setChecked(Store.getB(ctx, key, def));
        s.setContentDescription(label);
        s.setOnCheckedChangeListener((v, on) -> { Store.putB(ctx, key, on); if (extra != null) extra.on(on); });
        r.addView(s);
    }

    private void buildSiren() {
        LinearLayout p = card();
        boolean on = alarmOn();
        LinearLayout head = U.row(ctx);
        p.addView(head);
        ImageView ic = U.tile(ctx, R.drawable.ic_siren_alarm, "Panic alarm", 44);
        head.addView(ic, new LinearLayout.LayoutParams(U.dp(ctx, 44), U.dp(ctx, 44)));
        LinearLayout w = U.col(ctx);
        w.setPadding(U.dp(ctx, 12), 0, 0, 0);
        w.addView(U.tv(ctx, on ? "Siren is sounding" : "Loud emergency siren", 16, on ? R.color.danger : R.color.ink, true));
        w.addView(U.tv(ctx, "Full volume. The flashlight blinks 10 times when it starts.", 12, R.color.ink_soft, false));
        head.addView(w, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        in(p, "A loud siren can attract attention and deter someone threatening you.", 14, R.color.ink, false, 10);
        if (on) {
            btn(p, "Stop siren", true, R.drawable.ic_volume_off, () -> { Siren.stop(); refresh(); });
        } else {
            MaterialButton b = btn(p, "Sound siren", true, R.drawable.ic_siren_alarm, () -> { Siren.start(ctx); refresh(); });
            b.setBackground(U.gradient(ctx, R.color.danger, R.color.danger_dark, 18));
        }
    }

    /** Themed, numbered safety guide: every step has its own icon and, where useful, a one-tap action. */
    private void buildUnsafe() {
        LinearLayout c = U.col(ctx);
        c.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 26));
        c.setElevation(U.dp(ctx, 3));
        U.add(box, c, 12);

        LinearLayout head = U.row(ctx);
        head.setBackground(U.gradient(ctx, R.color.brand_hot, R.color.brand_dark, 26));
        head.setPadding(U.dp(ctx, 18), U.dp(ctx, 16), U.dp(ctx, 18), U.dp(ctx, 16));
        ImageView hi = U.icon(ctx, R.drawable.ic_safe_place, "Safe place", 48, R.color.white);
        hi.setBackground(U.bg(ctx, R.color.brand_coral, 0, 16));
        head.addView(hi, new LinearLayout.LayoutParams(U.dp(ctx, 48), U.dp(ctx, 48)));
        LinearLayout hw = U.col(ctx);
        hw.setPadding(U.dp(ctx, 14), 0, 0, 0);
        hw.addView(U.tv(ctx, "Five steps to take right now", 17, R.color.white, true));
        TextView hs = U.tv(ctx, "Stay calm. You are not alone, and you can leave any situation that feels wrong.", 12, R.color.white, false);
        hs.setPadding(0, U.dp(ctx, 3), 0, 0);
        hw.addView(hs);
        head.addView(hw, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(head);

        LinearLayout body = U.col(ctx);
        body.setPadding(U.dp(ctx, 14), U.dp(ctx, 6), U.dp(ctx, 14), U.dp(ctx, 16));
        c.addView(body);

        step(body, 1, R.drawable.ic_safe_place, "Move toward people and light",
                "Head for open shops, petrol stations, security guards or a busy street.", "Find help nearby", R.drawable.ic_map_filter,
                () -> act().goTab(R.id.nav_map));
        step(body, 2, R.drawable.ic_phone_call, "Call someone and stay on the line",
                "Tell them where you are. Keep talking until you reach safety.", "Open trusted circle", R.drawable.ic_trusted_circle,
                () -> open(new CirclePage()));
        step(body, 3, R.drawable.ic_emergency_shield, "Call 112 or 10111 if you are in danger",
                "Do not wait. Say where you are and what is happening.", "Call 112", R.drawable.ic_emergency_phone,
                () -> U.dial(ctx, "112"));
        step(body, 4, R.drawable.ic_location_share, "Share your location with someone you trust",
                "Your trusted circle receives a map link by SMS.", "Share my location", R.drawable.ic_location_live,
                () -> alertCircle("Here is where I am right now. I feel unsafe."));
        step(body, 5, R.drawable.ic_incident_report, "Note the details once you are safe",
                "Time, place and descriptions help if you decide to report. Keep them in your private log.", "Open private log", R.drawable.ic_symptom_journal,
                () -> open(new AbusePage()));

        MaterialButton safe = U.btn(ctx, "I am safe now", true, R.drawable.ic_safety_check, () -> {
            Siren.stop();
            U.toast(ctx, "Glad you're safe. Look after yourself today.");
            refresh();
        });
        safe.setBackground(U.gradient(ctx, R.color.ok, R.color.brand_dark, 18));
        U.add(body, safe, 16);
    }

    private void step(LinearLayout parent, int n, int icon, String title, String detail, String actionLabel, int actionIcon, Runnable action) {
        LinearLayout r = U.row(ctx);
        r.setGravity(Gravity.TOP);
        r.setPadding(0, U.dp(ctx, 12), 0, U.dp(ctx, 4));
        U.add(parent, r, 4);

        TextView num = U.tv(ctx, String.valueOf(n), 13, R.color.white, true);
        num.setGravity(Gravity.CENTER);
        num.setBackground(U.gradient(ctx, R.color.brand_hot, R.color.brand_dark, 14));
        r.addView(num, new LinearLayout.LayoutParams(U.dp(ctx, 28), U.dp(ctx, 28)));

        LinearLayout text = U.col(ctx);
        text.setPadding(U.dp(ctx, 12), 0, 0, 0);
        LinearLayout tr = U.row(ctx);
        ImageView ic = U.tile(ctx, icon, title, 36);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(U.dp(ctx, 36), U.dp(ctx, 36));
        ilp.setMargins(0, 0, U.dp(ctx, 10), 0);
        tr.addView(ic, ilp);
        tr.addView(U.tv(ctx, title, 15, R.color.ink, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        text.addView(tr);
        TextView d = U.tv(ctx, detail, 13, R.color.ink_soft, false);
        d.setLineSpacing(0f, 1.12f);
        d.setPadding(0, U.dp(ctx, 6), 0, 0);
        text.addView(d);
        MaterialButton b = U.btn(ctx, actionLabel, false, actionIcon, action);
        b.setMinHeight(U.dp(ctx, 44));
        U.add(text, b, 8);
        r.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    /** A tappable emergency/help line with its own icon. onDark = sits on the red emergency card. */
    private void lineRow(LinearLayout parent, int icon, final String name, final String number, String note, boolean onDark) {
        LinearLayout r = U.row(ctx);
        r.setBackground(U.bg(ctx, R.color.surface, onDark ? 0 : R.color.outline, 20));
        r.setPadding(U.dp(ctx, 12), U.dp(ctx, 12), U.dp(ctx, 12), U.dp(ctx, 12));
        r.setForeground(U.ripple(ctx));
        r.setClickable(true);
        r.setContentDescription("Call " + name + " " + number);
        ImageView t = U.tile(ctx, icon, name, 46);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(U.dp(ctx, 46), U.dp(ctx, 46));
        tl.setMargins(0, 0, U.dp(ctx, 12), 0);
        r.addView(t, tl);
        LinearLayout w = U.col(ctx);
        w.addView(U.tv(ctx, name, 15, R.color.ink, true));
        w.addView(U.tv(ctx, note, 12, R.color.ink_soft, false));
        r.addView(w, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView pill = U.tv(ctx, number, 14, R.color.white, true);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(U.gradient(ctx, R.color.danger, R.color.danger_dark, 16));
        pill.setPadding(U.dp(ctx, 12), U.dp(ctx, 8), U.dp(ctx, 12), U.dp(ctx, 8));
        r.addView(pill);
        U.pressable(r);
        r.setOnClickListener((View v) -> U.dial(ctx, number));
        U.add(parent, r, 10);
    }
}
