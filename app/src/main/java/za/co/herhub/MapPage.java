package za.co.herhub;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;
import java.util.HashMap;
import java.util.Map;

/** Community Safety Map on OpenStreetMap (no API key required, so it can't fail on a missing/expired key). */
public class MapPage extends Page {
    private static final String[] CATS = {"Poor lighting", "Harassment", "Unsafe area", "Suspicious activity", "Road hazard", "Safe space / help point", "Other"};
    private MapView map;
    private Marker me;
    private ListenerRegistration live;
    private final java.util.List<Marker> cloudMarkers = new java.util.ArrayList<>();

    @Override protected boolean top() { return true; }
    @Override protected boolean scroll() { return false; }
    @Override protected String title() { return "Safety Map"; }

    @Override protected void build() {
        FrameLayout fl = new FrameLayout(ctx);
        box.addView(fl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        map = new MapView(ctx);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setMinZoomLevel(3.0);
        map.setMaxZoomLevel(19.0);
        if (U.night(ctx)) map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        double lat = Double.parseDouble(Store.get(ctx, "map_lat", "-26.2041"));
        double lon = Double.parseDouble(Store.get(ctx, "map_lon", "28.0473"));
        map.getController().setZoom((double) Store.getI(ctx, "map_zoom", 13));
        map.getController().setCenter(new GeoPoint(lat, lon));
        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override public boolean longPressHelper(GeoPoint p) { reportDialog(p); return true; }
        }));
        fl.addView(map, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView hint = U.tv(ctx, "Long-press the map to report a concern", 12, R.color.ink, true);
        hint.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 16));
        hint.setPadding(U.dp(ctx, 12), U.dp(ctx, 6), U.dp(ctx, 12), U.dp(ctx, 6));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        hp.topMargin = U.dp(ctx, 10);
        fl.addView(hint, hp);

        TextView attr = U.tv(ctx, "© OpenStreetMap contributors", 10, R.color.ink, false);
        attr.setBackgroundColor(0x99FFFFFF);
        attr.setTextColor(0xFF333333);
        attr.setPadding(U.dp(ctx, 4), 0, U.dp(ctx, 4), 0);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        ap.bottomMargin = U.dp(ctx, 78);
        fl.addView(attr, ap);

        LinearLayout bar = U.row(ctx);
        bar.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 20));
        bar.setPadding(U.dp(ctx, 8), U.dp(ctx, 6), U.dp(ctx, 8), U.dp(ctx, 6));
        U.addW(bar, U.btn(ctx, "Me", true, R.drawable.ic_location_user, this::locateMe), 3);
        U.addW(bar, U.btn(ctx, "Report", false, R.drawable.ic_incident_report, () -> {
            if (map != null) reportDialog((GeoPoint) map.getMapCenter());
        }), 3);
        U.addW(bar, U.btn(ctx, "Help", false, R.drawable.ic_hospital, this::nearbyHelp), 3);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        bp.setMargins(U.dp(ctx, 10), 0, U.dp(ctx, 10), U.dp(ctx, 10));
        fl.addView(bar, bp);

        showLocalReports();
        loadCloudReports();
        if (Loc.has(ctx)) locateMe();
    }

    private void nearbyHelp() {
        final String[] opts = {"Police station", "Hospital", "Clinic", "Pharmacy", "Shelter"};
        new MaterialAlertDialogBuilder(ctx).setTitle("Find nearby help")
                .setItems(opts, (d, w) -> U.maps(ctx, opts[w])).show();
    }

    private void locateMe() {
        locate(l -> {
            if (!isAdded() || map == null) return;
            if (l == null) { U.toast(ctx, "Couldn't get your location. Check that Location is turned on."); return; }
            GeoPoint gp = new GeoPoint(l.getLatitude(), l.getLongitude());
            if (me == null) {
                me = new Marker(map);
                me.setTitle("You are here");
                me.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.ic_marker_me);
                if (d != null) me.setIcon(d.mutate());
                map.getOverlays().add(me);
            }
            me.setPosition(gp);
            map.getController().setZoom(16.0);
            map.getController().animateTo(gp);
            map.invalidate();
        });
    }

    private Marker addMarker(double lat, double lon, String title, String snippet) {
        if (map == null) return null;
        Marker m = new Marker(map);
        m.setPosition(new GeoPoint(lat, lon));
        m.setTitle(title);
        m.setSnippet(snippet);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(m);
        map.invalidate();
        return m;
    }

    private void showLocalReports() {
        JSONArray a = Store.arr(ctx, "d_reports");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            addMarker(o.optDouble("lat"), o.optDouble("lon"), o.optString("cat") + " (your report)", o.optString("desc"));
        }
    }

    /** Live reports from all users. Updates by itself while the map is open. */
    private void loadCloudReports() {
        try {
            final String myUid = Store.user() == null ? "" : Store.user().getUid();
            live = FirebaseFirestore.getInstance().collection("safetyReports")
                    .whereIn("status", java.util.Arrays.asList("approved", "pending_review")).limit(300)
                    .addSnapshotListener((snap, err) -> {
                        if (!isAdded() || map == null || snap == null) return;
                        for (Marker m : cloudMarkers) map.getOverlays().remove(m);
                        cloudMarkers.clear();
                        for (QueryDocumentSnapshot d : snap) {
                            // Your own reports are already shown from this phone.
                            if (!myUid.isEmpty() && myUid.equals(d.getString("uid"))) continue;
                            Double la = d.getDouble("lat"), lo = d.getDouble("lng");
                            if (la == null || lo == null) continue;
                            String c = d.getString("category");
                            Marker m = addMarker(la, lo, c == null ? "Community notice" : c, d.getString("details"));
                            if (m != null) cloudMarkers.add(m);
                        }
                        map.invalidate();
                    });
        } catch (Throwable ignored) { }
    }

    private void reportDialog(final GeoPoint gp) {
        if (!isAdded()) return;
        LinearLayout c = U.col(ctx);
        final Spinner sp = new Spinner(ctx);
        sp.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, CATS));
        c.addView(sp);
        final TextInputLayout d = U.multi(ctx, "What happened? (no names or private details)", 3);
        U.add(c, d, 12);
        TextView note = U.tv(ctx, "Your report appears on the map for other HerHub users straight away. Please keep it factual. For emergencies, call 112 or 10111.", 12, R.color.ink_soft, false);
        U.add(c, note, 10);
        U.dialog(ctx, "Report a concern here", c, "Submit", () -> submit(gp, String.valueOf(sp.getSelectedItem()), U.val(d)));
    }

    private void submit(GeoPoint gp, String cat, String desc) {
        Store.prepend(ctx, "d_reports", Store.obj("cat", cat, "desc", desc, "lat", String.valueOf(gp.getLatitude()), "lon", String.valueOf(gp.getLongitude()), "date", U.today()), 50);
        addMarker(gp.getLatitude(), gp.getLongitude(), cat + " (your report)", desc);
        try {
            Map<String, Object> r = new HashMap<>();
            r.put("category", cat);
            r.put("details", desc);
            r.put("lat", gp.getLatitude());
            r.put("lng", gp.getLongitude());
            r.put("status", "approved");
            r.put("createdAt", FieldValue.serverTimestamp());
            if (Store.user() != null) r.put("uid", Store.user().getUid());
            final android.content.Context app = ctx.getApplicationContext();
            FirebaseFirestore.getInstance().collection("safetyReports").add(r)
                    .addOnSuccessListener(x -> U.toast(app, "Report shared with the community. Thank you."))
                    .addOnFailureListener(x -> U.toast(app, "Saved on this phone. Couldn't reach the server right now."));
        } catch (Throwable t) { U.toast(ctx, "Saved on this phone."); }
    }

    @Override public void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override public void onPause() {
        if (map != null) {
            try {
                GeoPoint c = (GeoPoint) map.getMapCenter();
                Store.put(ctx, "map_lat", String.valueOf(c.getLatitude()));
                Store.put(ctx, "map_lon", String.valueOf(c.getLongitude()));
                Store.putI(ctx, "map_zoom", (int) map.getZoomLevelDouble());
            } catch (Throwable ignored) { }
            map.onPause();
        }
        super.onPause();
    }
    @Override public void onDestroyView() {
        if (live != null) { live.remove(); live = null; }
        cloudMarkers.clear();
        if (map != null) { try { map.onDetach(); } catch (Throwable ignored) { } map = null; me = null; }
        super.onDestroyView();
    }
}
