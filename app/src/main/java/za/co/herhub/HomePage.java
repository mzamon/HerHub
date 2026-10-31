package za.co.herhub;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;

/** Dashboard. The welcome line is the only place in the app that carries an emoji. */
public class HomePage extends Page {
    private static final String[] TIPS = {
            "Share your journey with someone you trust before you set off.",
            "Trust your instincts. If a situation feels wrong, it's okay to leave.",
            "Keep your phone charged and your emergency contacts up to date.",
            "Never share OTPs or PINs, even with someone claiming to be your bank.",
            "Tell a friend where you're going on a first date, and meet in public.",
            "Take a few slow breaths. Calm minds make clearer safety decisions.",
            "Lock your phone and review which apps can see your location.",
            "Walk in well-lit areas and keep one ear free of headphones at night."
    };

    @Override protected boolean top() { return true; }
    @Override protected String title() { return "HerHub"; }

    @Override protected void build() {
        FirebaseUser u = Store.user();
        String n = u != null && u.getDisplayName() != null && !u.getDisplayName().trim().isEmpty()
                ? u.getDisplayName().trim().split("\\s+")[0] : null;

        // ----- Welcome row (the one allowed emoji) + notification bell
        LinearLayout welcome = U.row(ctx);
        LinearLayout words = U.col(ctx);
        words.addView(U.tv(ctx, n != null ? "Welcome, " + n + " \uD83D\uDC4B" : "Welcome \uD83D\uDC4B", 25, R.color.ink, true));
        TextView sub = U.tv(ctx, "You're safe, strong and capable.", 14, R.color.ink_soft, false);
        sub.setPadding(0, U.dp(ctx, 3), 0, 0);
        words.addView(sub);
        welcome.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView bell = U.icon(ctx, R.drawable.ic_notification_bell, "Notifications", 46, 0);
        bell.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 23));
        bell.setElevation(U.dp(ctx, 1));
        bell.setOnClickListener(v -> open(new NotifyPage()));
        U.pressable(bell);
        welcome.addView(bell, new LinearLayout.LayoutParams(U.dp(ctx, 46), U.dp(ctx, 46)));
        U.add(box, welcome, 4);

        // ----- Siren warning
        if (Siren.isOn()) {
            LinearLayout a = card();
            a.setBackground(U.bg(ctx, R.color.danger_soft, R.color.danger, 22));
            LinearLayout ar = U.row(ctx);
            a.addView(ar);
            ar.addView(U.icon(ctx, R.drawable.ic_siren_alarm, "Siren sounding", 40, 0), new LinearLayout.LayoutParams(U.dp(ctx, 40), U.dp(ctx, 40)));
            TextView at = U.tv(ctx, "Siren is sounding", 16, R.color.danger, true);
            at.setPadding(U.dp(ctx, 10), 0, 0, 0);
            ar.addView(at);
            btn(a, "Stop siren", true, R.drawable.ic_volume_off, () -> { Siren.stop(); refresh(); });
        }

        // ----- Status card
        JSONObject j = JourneyPage.active(ctx);
        LinearLayout status = U.row(ctx);
        status.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 22));
        status.setElevation(U.dp(ctx, 2));
        status.setPadding(U.dp(ctx, 14), U.dp(ctx, 14), U.dp(ctx, 10), U.dp(ctx, 14));
        status.setForeground(U.ripple(ctx));
        status.setClickable(true);
        ImageView si = U.icon(ctx, R.drawable.ic_safety_check, "Safety status", 46, 0);
        si.setBackground(U.bg(ctx, R.color.safe_soft, 0, 16));
        status.addView(si, new LinearLayout.LayoutParams(U.dp(ctx, 46), U.dp(ctx, 46)));
        LinearLayout sw = U.col(ctx);
        sw.setPadding(U.dp(ctx, 12), 0, 0, 0);
        sw.addView(U.tv(ctx, j != null ? "Journey in progress" : "You're safe", 16, R.color.ink, true));
        sw.addView(U.tv(ctx, j != null ? "Heading to " + j.optString("dest") : "Your safety tools are ready", 12, R.color.ink_soft, false));
        status.addView(sw, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        status.addView(U.icon(ctx, R.drawable.ic_chevron_right, "Open", 28, 0), new LinearLayout.LayoutParams(U.dp(ctx, 28), U.dp(ctx, 28)));
        status.setOnClickListener(v -> { if (JourneyPage.active(ctx) != null) open(new JourneyPage()); else act().goTab(R.id.nav_safety); });
        U.pressable(status);
        U.add(box, status, 16);

        // ----- Your safety comes first / SOS
        LinearLayout sos = U.col(ctx);
        sos.setBackground(U.gradient(ctx, R.color.brand_hot, R.color.brand_dark, 26));
        sos.setElevation(U.dp(ctx, 4));
        sos.setPadding(U.dp(ctx, 20), U.dp(ctx, 20), U.dp(ctx, 20), U.dp(ctx, 18));
        in(sos, "YOUR SAFETY COMES FIRST", 11, R.color.brand_on, true, 0);
        in(sos, "Need help right now?", 22, R.color.brand_on, true, 8);
        in(sos, "HerHub can help you reach your trusted circle. In immediate danger in South Africa, call 112 from a mobile phone or 10111 for police.", 13, R.color.brand_on, false, 6);

        LinearLayout sosBtn = U.row(ctx);
        sosBtn.setGravity(Gravity.CENTER_VERTICAL);
        sosBtn.setBackground(U.bg(ctx, R.color.white, 0, 30));
        sosBtn.setElevation(U.dp(ctx, 4));
        sosBtn.setPadding(U.dp(ctx, 10), U.dp(ctx, 10), U.dp(ctx, 18), U.dp(ctx, 10));
        sosBtn.setForeground(U.ripple(ctx));
        sosBtn.setClickable(true);
        sosBtn.setContentDescription("SOS, get help");
        ImageView sosIc = U.icon(ctx, R.drawable.ic_sos_alert, "SOS", 56, 0);
        sosIc.setBackground(U.bg(ctx, R.color.danger_soft, 0, 28));
        sosBtn.addView(sosIc, new LinearLayout.LayoutParams(U.dp(ctx, 56), U.dp(ctx, 56)));
        U.pulse(sosIc);
        LinearLayout st = U.col(ctx);
        st.setPadding(U.dp(ctx, 14), 0, 0, 0);
        TextView s1 = U.tv(ctx, "SOS  Get help", 20, R.color.danger, true);
        st.addView(s1);
        st.addView(U.tv(ctx, "Tap for emergency help", 12, R.color.ink_soft, false));
        sosBtn.addView(st, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sosBtn.addView(U.icon(ctx, R.drawable.ic_chevron_right, "Open", 30, 0), new LinearLayout.LayoutParams(U.dp(ctx, 30), U.dp(ctx, 30)));
        sosBtn.setOnClickListener(v -> open(new SosPage()));
        U.pressable(sosBtn);
        U.add(sos, sosBtn, 16);

        MaterialButton call = U.btn(ctx, "Call 112", false, R.drawable.ic_emergency_phone, () -> U.dial(ctx, "112"));
        U.add(sos, call, 10);
        U.add(box, sos, 16);

        // ----- Shortcuts: 8 features, 8 different icons
        h("Your shortcuts");
        LinearLayout r1 = U.row(ctx), r2 = U.row(ctx);
        U.add(box, r1, 8);
        U.add(box, r2, 8);
        tile(r1, R.drawable.ic_location_share, "Share location", () -> open(new LocationPage()));
        tile(r1, R.drawable.ic_trusted_circle, "Trusted circle", () -> open(new CirclePage()));
        tile(r1, R.drawable.ic_safe_journey, "Safe journey", () -> open(new JourneyPage()));
        tile(r1, R.drawable.ic_safety_map, "Safety map", () -> act().goTab(R.id.nav_map));
        tile(r2, R.drawable.ic_mood_checkin, "Mood check-in", () -> open(new MindPage()));
        tile(r2, R.drawable.ic_siren_alarm, Siren.isOn() ? "Stop siren" : "Panic siren", () -> { Siren.toggle(ctx); refresh(); });
        tile(r2, R.drawable.ic_emergency_contacts, "Emergency numbers", () -> open(new DirectoryPage()));
        tile(r2, R.drawable.ic_ai_assistant, "Ask HerHub", () -> open(new AssistantPage()));

        // ----- Check-in with mood icons
        h("A little check-in");
        LinearLayout check = card();
        check.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 24));
        in(check, "How are you feeling today?", 17, R.color.ink, true, 0);
        in(check, "Take a moment for yourself. Every feeling belongs here.", 13, R.color.ink_soft, false, 5);
        LinearLayout moods = U.row(ctx);
        U.add(check, moods, 12);
        mood(moods, R.drawable.ic_mood_great, "Good", R.color.mood_great);
        mood(moods, R.drawable.ic_mood_okay, "Okay", R.color.mood_okay);
        mood(moods, R.drawable.ic_mood_sad, "Low", R.color.mood_sad);
        mood(moods, R.drawable.ic_mood_anxious, "Anxious", R.color.mood_anxious);

        // ----- Today overview
        h("Your today overview");
        LinearLayout today = card();
        today.setBackground(U.bg(ctx, R.color.surface_alt, 0, 22));
        overview(today, R.drawable.ic_period_calendar, "Period tracker", PeriodPage.summary(ctx), () -> open(new PeriodPage()), 0);
        overview(today, R.drawable.ic_water_tracker, "Water today", HealthPage.water(ctx) + " of 8 glasses", () -> open(new HealthPage()), 12);
        overview(today, R.drawable.ic_destination_pin, "Journey", j != null ? "To " + j.optString("dest") + " is active" : "No active journey", () -> open(new JourneyPage()), 12);

        // ----- Daily reminder
        h("A reminder for you");
        LinearLayout tip = card();
        tip.setBackground(U.gradient(ctx, R.color.brand_soft, R.color.surface, 22));
        LinearLayout tr = U.row(ctx);
        tip.addView(tr);
        tr.addView(U.icon(ctx, R.drawable.ic_lightbulb, "Daily reminder", 40, 0), new LinearLayout.LayoutParams(U.dp(ctx, 40), U.dp(ctx, 40)));
        TextView tt = U.tv(ctx, "A little reminder", 16, R.color.brand_dark, true);
        tt.setPadding(U.dp(ctx, 10), 0, 0, 0);
        tr.addView(tt);
        int day = java.time.LocalDate.now().getDayOfYear();
        in(tip, TIPS[day % TIPS.length], 14, R.color.ink, false, 8);

        // ----- Explore
        h("Explore HerHub");
        item(R.drawable.ic_womens_hub, "Women\u2019s Information Hub", "Health, rights and personal-safety guides", () -> open(new InfoPage()));
        item(R.drawable.ic_forum, "Support & Community", "Support lines and a moderated community", () -> open(new SupportPage()));
        item(R.drawable.ic_financial_safety, "Financial Safety", "Scam checker, savings goal and fraud tips", () -> open(new FinancePage()));
    }

    private void tile(LinearLayout row, int icon, String label, Runnable r) {
        LinearLayout c = U.col(ctx);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 20));
        c.setForeground(U.ripple(ctx));
        c.setClickable(true);
        c.setContentDescription(label);
        c.setPadding(U.dp(ctx, 4), U.dp(ctx, 12), U.dp(ctx, 4), U.dp(ctx, 10));
        ImageView iv = U.tile(ctx, icon, label, 44);
        c.addView(iv, new LinearLayout.LayoutParams(U.dp(ctx, 44), U.dp(ctx, 44)));
        TextView l = U.tv(ctx, label, 11, R.color.ink, true);
        l.setGravity(Gravity.CENTER);
        l.setMaxLines(2);
        l.setPadding(0, U.dp(ctx, 6), 0, 0);
        c.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        U.pressable(c);
        c.setOnClickListener(v -> r.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(U.dp(ctx, 3), 0, U.dp(ctx, 3), 0);
        row.addView(c, lp);
    }

    private void mood(LinearLayout row, int icon, String label, int colorRes) {
        LinearLayout item = U.col(ctx);
        item.setGravity(Gravity.CENTER);
        item.setBackground(U.bg(ctx, R.color.surface_alt, 0, 16));
        item.setForeground(U.ripple(ctx));
        item.setClickable(true);
        item.setContentDescription("Mood: " + label);
        item.setPadding(U.dp(ctx, 6), U.dp(ctx, 10), U.dp(ctx, 6), U.dp(ctx, 8));
        ImageView iv = U.icon(ctx, icon, label, 44, 0);
        item.addView(iv, new LinearLayout.LayoutParams(U.dp(ctx, 44), U.dp(ctx, 44)));
        TextView t = U.tv(ctx, label, 11, colorRes, true);
        t.setGravity(Gravity.CENTER);
        U.add(item, t, 3);
        U.pressable(item);
        item.setOnClickListener(v -> open(new MindPage()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(U.dp(ctx, 3), 0, U.dp(ctx, 3), 0);
        row.addView(item, lp);
    }

    private void overview(LinearLayout parent, int icon, String label, String value, Runnable r, int topDp) {
        LinearLayout row = U.row(ctx);
        row.setForeground(U.ripple(ctx));
        row.setClickable(true);
        row.setContentDescription(label + ": " + value);
        row.addView(U.tile(ctx, icon, label, 40), new LinearLayout.LayoutParams(U.dp(ctx, 40), U.dp(ctx, 40)));
        LinearLayout w = U.col(ctx);
        w.setPadding(U.dp(ctx, 12), 0, 0, 0);
        w.addView(U.tv(ctx, label, 12, R.color.ink_soft, false));
        TextView v = U.tv(ctx, value, 14, R.color.ink, true);
        w.addView(v);
        row.addView(w, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnClickListener(x -> r.run());
        U.add(parent, row, topDp);
    }
}
