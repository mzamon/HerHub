package za.co.herhub;

import android.content.Intent;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MorePage extends Page {
    @Override protected boolean top() { return true; }
    @Override protected String title() { return "More"; }

    @Override protected void build() {
        FirebaseUser u = Store.user();
        LinearLayout acc = card();
        titleRow(acc, R.drawable.ic_profile, "Account");
        if (u != null) {
            String n = u.getDisplayName() == null || u.getDisplayName().isEmpty() ? "HerHub member" : u.getDisplayName();
            in(acc, n, 15, R.color.ink, true, 6);
            in(acc, u.getEmail() == null ? "" : u.getEmail(), 13, R.color.ink_soft, false, 0);
            in(acc, "Your data backs up securely to your account.", 12, R.color.ink_soft, false, 4);
            btn(acc, "Sign out", false, R.drawable.ic_logout, () -> U.confirm(ctx, "Sign out?", "Your data stays safe in your account.", "Sign out", this::signOut));
        } else {
            in(acc, "You're using HerHub without an account. Data stays on this phone.", 13, R.color.ink_soft, false, 4);
            btn(acc, "Sign in or create account", true, R.drawable.ic_profile, this::signOut);
        }

        LinearLayout th = card();
        titleRow(th, R.drawable.ic_settings, "Appearance");
        int mode = Store.themeMode(ctx);
        in(th, "Current: " + (mode == AppCompatDelegate.MODE_NIGHT_YES ? "Dark" : mode == AppCompatDelegate.MODE_NIGHT_NO ? "Light" : "Follow system"), 13, R.color.ink_soft, false, 2);
        LinearLayout r = U.row(th);
        U.add(th, r, 10);
        U.addW(r, U.btn(ctx, "Light", mode == AppCompatDelegate.MODE_NIGHT_NO, R.drawable.ic_light_mode, () -> setTheme(AppCompatDelegate.MODE_NIGHT_NO)), 3);
        U.addW(r, U.btn(ctx, "Dark", mode == AppCompatDelegate.MODE_NIGHT_YES, R.drawable.ic_dark_mode, () -> setTheme(AppCompatDelegate.MODE_NIGHT_YES)), 3);
        U.addW(r, U.btn(ctx, "Auto", mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.drawable.ic_auto_mode, () -> setTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)), 3);

        h("All features");
        for (String[] f : Features.ALL) {
            final String key = f[0];
            item(Features.icon(key), f[1], f[2], () -> Features.go(act(), key));
        }
        h("About");
        p("HerHub 2.0 · Maps © OpenStreetMap contributors. HerHub is a support tool and does not dispatch emergency services.");
    }

    private void titleRow(LinearLayout parent, int icon, String text) {
        LinearLayout r = U.row(ctx);
        parent.addView(r);
        r.addView(U.tile(ctx, icon, text, 36), new LinearLayout.LayoutParams(U.dp(ctx, 36), U.dp(ctx, 36)));
        android.widget.TextView t = U.tv(ctx, text, 17, R.color.ink, true);
        t.setPadding(U.dp(ctx, 10), 0, 0, 0);
        r.addView(t);
    }

    private void setTheme(int mode) {
        Store.putI(ctx, "theme", mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void signOut() {
        try { FirebaseAuth.getInstance().signOut(); } catch (Throwable ignored) { }
        Store.putB(ctx, "guest", false);
        Intent i = new Intent(ctx, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        act().finish();
    }
}
