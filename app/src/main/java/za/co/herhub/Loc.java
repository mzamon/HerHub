package za.co.herhub;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;

/** Small, crash-proof location helper (no Google Play Services dependency). */
public final class Loc {
    private Loc() { }
    public interface Cb { void got(Location l); }

    public static boolean has(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void fetch(Context ctx, final Cb cb) {
        final Context c = ctx.getApplicationContext();
        if (!has(c)) { cb.got(null); return; }
        final LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        try {
            for (String p : lm.getProviders(true)) {
                if (ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) break;
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getAccuracy() < best.getAccuracy())) best = l;
            }
        } catch (Throwable ignored) { }
        final Location last = best;
        String prov = null;
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) prov = LocationManager.GPS_PROVIDER;
            else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) prov = LocationManager.NETWORK_PROVIDER;
        } catch (Throwable ignored) { }
        if (prov == null) { cb.got(last); return; }
        final boolean[] done = {false};
        final Handler h = new Handler(Looper.getMainLooper());
        final LocationListener ll = new LocationListener() {
            public void onLocationChanged(Location l) {
                if (done[0]) return;
                done[0] = true;
                try { lm.removeUpdates(this); } catch (Throwable ignored) { }
                cb.got(l);
            }
            public void onStatusChanged(String p, int s, Bundle e) { }
            public void onProviderEnabled(String p) { }
            public void onProviderDisabled(String p) { }
        };
        try {
            if (!has(c)) { cb.got(last); return; }
            lm.requestLocationUpdates(prov, 0L, 0f, ll, Looper.getMainLooper());
        } catch (Throwable t) { cb.got(last); return; }
        h.postDelayed(() -> {
            if (done[0]) return;
            done[0] = true;
            try { lm.removeUpdates(ll); } catch (Throwable ignored) { }
            cb.got(last);
        }, 12000);
    }

    public static String link(Location l) {
        return "https://maps.google.com/?q=" + l.getLatitude() + "," + l.getLongitude();
    }
}
