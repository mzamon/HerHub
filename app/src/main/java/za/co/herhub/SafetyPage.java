package za.co.herhub;

import android.widget.LinearLayout;

public class SafetyPage extends Page {
    @Override protected boolean top() { return true; }
    @Override protected String title() { return "Safety"; }

    @Override protected void build() {
        p("Tools for when you feel unsafe, plus ways to prepare ahead of time.");
        LinearLayout c = card();
        in(c, "Emergency", 17, R.color.ink, true, 0);
        in(c, "If you are in immediate danger, call emergency services first.", 13, R.color.ink_soft, false, 2);
        U.add(c, U.danger(ctx, "SOS  Get help", R.drawable.ic_sos_alert, () -> open(new SosPage())), 10);
        LinearLayout r = U.row(c);
        U.add(c, r, 10);
        U.addW(r, U.btn(ctx, "112", true, R.drawable.ic_emergency_phone, () -> U.dial(ctx, "112")), 3);
        U.addW(r, U.btn(ctx, "10111", false, R.drawable.ic_police_station, () -> U.dial(ctx, "10111")), 3);
        btn(c, Siren.isOn() ? "Stop panic siren" : "Sound panic siren", false,
                Siren.isOn() ? R.drawable.ic_volume_off : R.drawable.ic_siren_alarm, () -> { Siren.toggle(ctx); refresh(); });
        btn(c, "Alert my trusted circle", false, R.drawable.ic_sos_contact, () -> alertCircle("I need help. Please contact me now."));

        h("Stay safe");
        item(R.drawable.ic_location_live, "Location & Safety Tracking", "Share where you are", () -> open(new LocationPage()));
        item(R.drawable.ic_safe_journey, "Safe Journey", "Trip timer with arrival check-in", () -> open(new JourneyPage()));
        item(R.drawable.ic_trusted_circle, "Trusted Circle", "Manage your trusted contacts", () -> open(new CirclePage()));
        item(R.drawable.ic_abuse_support, "Abuse & Harassment Support", "Safety plan, private log, quick exit", () -> open(new AbusePage()));
        item(R.drawable.ic_privacy_lock, "Digital & Personal Security", "Protect your accounts and privacy", () -> open(new SecurityPage()));
        item(R.drawable.ic_emergency_contacts, "Emergency Resource Directory", "Hotlines, tap to call", () -> open(new DirectoryPage()));
    }
}
