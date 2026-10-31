package za.co.herhub;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

public class LocationPage extends Page {
    private TextView out;
    private Location loc;
    private LinearLayout actions;

    @Override protected String title() { return "Location & Safety Tracking"; }

    @Override protected void build() {
        p("Share where you are only with people you trust. HerHub never shares your location on its own.");
        LinearLayout c = card();
        out = in(c, "Tap the button to find your location.", 15, R.color.ink, false, 0);
        btn(c, "Get my current location", true, R.drawable.ic_location_user, this::find);
        actions = U.col(ctx);
        c.addView(actions);
        if (loc != null) showActions();

        h("More tools");
        item(R.drawable.ic_safety_map, "Open safety map", "See your position and community notices", () -> act().goTab(R.id.nav_map));
        item(R.drawable.ic_safe_journey, "Safe Journey", "Share a trip and set a check-in timer", () -> open(new JourneyPage()));
        item(R.drawable.ic_trusted_circle, "Trusted Circle", "Choose who receives your location", () -> open(new CirclePage()));
        p("Tip: turn on Location in your phone settings for the best accuracy.");
    }

    private void find() {
        out.setText("Finding you…");
        locate(l -> {
            if (!isAdded()) return;
            if (l == null) { out.setText("Couldn't get a location. Make sure Location is on and permission is allowed, then try again."); return; }
            loc = l;
            out.setText(String.format(Locale.US, "Latitude %.5f\nLongitude %.5f\nAccuracy about %d m", l.getLatitude(), l.getLongitude(), Math.round(l.getAccuracy())));
            showActions();
            lookupAddress(l);
        });
    }

    private void lookupAddress(final Location l) {
        new Thread(() -> {
            String line = null;
            try {
                if (Geocoder.isPresent()) {
                    List<Address> a = new Geocoder(ctx.getApplicationContext(), Locale.getDefault()).getFromLocation(l.getLatitude(), l.getLongitude(), 1);
                    if (a != null && !a.isEmpty()) line = a.get(0).getAddressLine(0);
                }
            } catch (Throwable ignored) { }
            final String fl = line;
            if (fl != null && isAdded()) requireActivity().runOnUiThread(() -> {
                if (isAdded() && out != null && loc == l) out.setText(out.getText() + "\nNear: " + fl);
            });
        }).start();
    }

    private void showActions() {
        actions.removeAllViews();
        final String msg = "My location: " + Loc.link(loc);
        U.add(actions, U.btn(ctx, "Share location", false, R.drawable.ic_location_share, () -> U.share(ctx, msg)), 10);
        U.add(actions, U.btn(ctx, "Send to trusted circle", false, R.drawable.ic_sos_contact, () -> alertCircle("Here is where I am right now.")), 8);
        U.add(actions, U.btn(ctx, "Open in Maps", false, R.drawable.ic_destination_pin, () -> U.maps(ctx, loc.getLatitude() + "," + loc.getLongitude())), 8);
        U.add(actions, U.btn(ctx, "Copy link", false, R.drawable.ic_copy, () -> U.copy(ctx, msg)), 8);
    }
}
