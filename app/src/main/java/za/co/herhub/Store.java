package za.co.herhub;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/** Local-first storage. Keys starting with "d_" are also synced to Firestore (users/{uid}/data/{key}) when signed in.
 *  Keys starting with "private_" never leave the device. */
public final class Store {
    private Store() { }

    /** Each signed-in user gets their own private file, so accounts on one phone never see each other's data.
     *  Only the app theme is shared by everyone using the phone. Signed-out use has its own file ("herhub"). */
    private static SharedPreferences sp(Context c, String k) {
        FirebaseUser u = "theme".equals(k) ? null : user();
        String name = u == null ? "herhub" : "herhub_" + u.getUid();
        return c.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
    }
    private static SharedPreferences sp(Context c) { return sp(c, ""); }

    /** One-time clean-up: older versions kept every account's data in one shared file. Clear it so nothing leaks between accounts.
     *  Signed-in data comes back from each account's own cloud copy when that user signs in. */
    public static void migrate(Context c) {
        try {
            SharedPreferences g = c.getApplicationContext().getSharedPreferences("herhub", Context.MODE_PRIVATE);
            if (g.getBoolean("per_user_v1", false)) return;
            int theme = g.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            g.edit().clear().putInt("theme", theme).putBoolean("per_user_v1", true).apply();
        } catch (Throwable ignored) { }
    }
    public static String get(Context c, String k, String d) { return sp(c, k).getString(k, d); }
    public static void put(Context c, String k, String v) {
        SharedPreferences.Editor e = sp(c, k).edit().putString(k, v);
        if (k.startsWith("d_")) e.putLong("ts_" + k, System.currentTimeMillis());
        e.apply();
        sync(k, v);
    }
    public static boolean getB(Context c, String k, boolean d) { return sp(c, k).getBoolean(k, d); }
    public static void putB(Context c, String k, boolean v) { sp(c, k).edit().putBoolean(k, v).apply(); }
    public static int getI(Context c, String k, int d) { return sp(c, k).getInt(k, d); }
    public static void putI(Context c, String k, int v) { sp(c, k).edit().putInt(k, v).apply(); }
    public static void clearAll(Context c) { sp(c).edit().clear().apply(); }
    public static void remove(Context c, String k) {
        sp(c, k).edit().remove(k).remove("ts_" + k).apply();
        if (!k.startsWith("d_")) return;
        try {
            FirebaseUser u = user();
            if (u != null) FirebaseFirestore.getInstance().collection("users").document(u.getUid()).collection("data").document(k).delete();
        } catch (Throwable ignored) { }
    }

    public static int themeMode(Context c) { return getI(c, "theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); }

    public static JSONArray arr(Context c, String k) {
        try { return new JSONArray(get(c, k, "[]")); } catch (Exception e) { return new JSONArray(); }
    }
    public static void putArr(Context c, String k, JSONArray a) { put(c, k, a.toString()); }
    public static void prepend(Context c, String k, JSONObject o, int max) {
        JSONArray old = arr(c, k), n = new JSONArray();
        n.put(o);
        for (int i = 0; i < old.length() && n.length() < max; i++) n.put(old.opt(i));
        putArr(c, k, n);
    }
    public static void removeAt(Context c, String k, int idx) {
        JSONArray old = arr(c, k), n = new JSONArray();
        for (int i = 0; i < old.length(); i++) if (i != idx) n.put(old.opt(i));
        putArr(c, k, n);
    }
    public static JSONObject obj(String... kv) {
        JSONObject o = new JSONObject();
        try { for (int i = 0; i + 1 < kv.length; i += 2) o.put(kv[i], kv[i + 1]); } catch (Exception ignored) { }
        return o;
    }

    public static FirebaseUser user() {
        try { return FirebaseAuth.getInstance().getCurrentUser(); } catch (Throwable t) { return null; }
    }

    private static void sync(String k, String v) {
        if (!k.startsWith("d_")) return;
        try {
            FirebaseUser u = user();
            if (u == null) return;
            Map<String, Object> m = new HashMap<>();
            m.put("json", v);
            m.put("updatedAt", FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection("users").document(u.getUid())
                    .collection("data").document(k).set(m);
        } catch (Throwable ignored) { }
    }

    /** Brings in cloud data that is newer than this phone's copy (e.g. edits made on the web app). */
    public static void pull(final Context c) {
        try {
            FirebaseUser u = user();
            if (u == null) return;
            // Remember whose file this is now, so a late answer can never land in another account's data.
            final SharedPreferences mine = sp(c);
            FirebaseFirestore.getInstance().collection("users").document(u.getUid()).collection("data").get()
                    .addOnSuccessListener(snap -> {
                        try {
                            SharedPreferences.Editor e = mine.edit();
                            for (DocumentSnapshot d : snap.getDocuments()) {
                                String j = d.getString("json");
                                if (j == null) continue;
                                String key = d.getId();
                                java.util.Date t = d.getDate("updatedAt");
                                long cloud = t == null ? 0 : t.getTime();
                                boolean has = mine.getString(key, null) != null;
                                long local = mine.getLong("ts_" + key, 0);
                                // Local edits that haven't synced yet (newer than the cloud copy) always win.
                                if (!has || cloud > local + 2000) {
                                    e.putString(key, j).putLong("ts_" + key, cloud);
                                }
                            }
                            e.apply();
                        } catch (Throwable ignored) { }
                    });
        } catch (Throwable ignored) { }
    }
}
