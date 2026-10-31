package za.co.herhub;

import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SecurityPage extends Page {
    private static final String[] CHECKS = {
            "I use a strong, unique password for my email",
            "2-step verification is on for my email and social media",
            "I never share OTPs, PINs or passwords with anyone",
            "My phone has a screen lock (PIN, pattern or fingerprint)",
            "I've reviewed which apps can see my location",
            "I only share my location with people I trust",
            "I avoid banking on public Wi-Fi",
            "I log out of accounts on shared devices",
            "Someone I trust knows how to reach me in an emergency"};

    @Override protected String title() { return "Digital & Personal Security"; }

    @Override protected void build() {
        LinearLayout c = card();
        int done = 0;
        for (int i = 0; i < CHECKS.length; i++) if (Store.getB(ctx, "sec_" + i, false)) done++;
        in(c, "Your security checklist: " + done + " / " + CHECKS.length, 17, R.color.brand, true, 0);
        for (int i = 0; i < CHECKS.length; i++) {
            final int ix = i;
            CheckBox cb = new CheckBox(ctx);
            cb.setText(CHECKS[i]);
            cb.setTextColor(U.c(ctx, R.color.ink));
            cb.setChecked(Store.getB(ctx, "sec_" + i, false));
            cb.setOnCheckedChangeListener((v, on) -> { Store.putB(ctx, "sec_" + ix, on); });
            U.add(c, cb, 4);
        }

        h("Password strength checker");
        LinearLayout p = card();
        in(p, "Typed here stays on your phone. It's never saved or sent.", 12, R.color.ink_soft, false, 0);
        final TextInputLayout f = U.field(ctx, "Try a password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        f.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        U.add(p, f, 8);
        final TextView res = in(p, "", 15, R.color.ink, true, 8);
        f.getEditText().addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c2) { }
            public void onTextChanged(CharSequence s, int a, int b, int c2) { }
            public void afterTextChanged(Editable e) {
                String s = e.toString();
                if (s.isEmpty()) { res.setText(""); return; }
                int sc = 0;
                if (s.length() >= 8) sc++;
                if (s.length() >= 12) sc++;
                if (s.matches(".*[a-z].*") && s.matches(".*[A-Z].*")) sc++;
                if (s.matches(".*\\d.*")) sc++;
                if (s.matches(".*[^A-Za-z0-9].*")) sc++;
                String l = s.toLowerCase();
                if (l.contains("password") || l.contains("123456") || l.contains("qwerty") || l.contains("herhub")) sc = Math.min(sc, 1);
                String[] n = {"Very weak", "Very weak", "Weak", "Okay", "Strong", "Very strong"};
                res.setText("Strength: " + n[sc] + (sc < 4 ? "  ·  Try a longer phrase with numbers and symbols." : ""));
                res.setTextColor(U.c(ctx, sc >= 4 ? R.color.ok : sc >= 3 ? R.color.brand : R.color.danger));
            }
        });

        h("Your account");
        final FirebaseUser u = Store.user();
        LinearLayout a = card();
        if (u != null && u.getEmail() != null) {
            in(a, u.getEmail(), 15, R.color.ink, true, 0);
            btn(a, "Email me a password reset link", false, R.drawable.ic_email, () -> {
                try {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(u.getEmail()).addOnCompleteListener(t ->
                            U.toast(ctx.getApplicationContext(), t.isSuccessful() ? "Reset link sent. Check your email." : "Couldn't send the link. Check your connection."));
                } catch (Throwable t) { U.toast(ctx, "Couldn't send the link right now."); }
            });
            btn(a, "Delete my account", false, R.drawable.ic_delete, () -> U.confirm(ctx, "Delete your account?", "This permanently deletes your HerHub login. Data on this phone stays until you clear it.", "Delete", () -> {
                try {
                    u.delete().addOnCompleteListener(t -> {
                        Context2.run(ctx, t.isSuccessful(), () -> {
                            Store.putB(ctx, "guest", false);
                            Intent i = new Intent(ctx, AuthActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                        });
                    });
                } catch (Throwable t) { U.toast(ctx, "Couldn't delete right now."); }
            }));
        } else in(a, "You're using HerHub without an account. Create one from More > Account to back up your data.", 14, R.color.ink_soft, false, 0);
        btn(a, "Clear all data on this phone", false, R.drawable.ic_clear_data, () -> U.confirm(ctx, "Clear all local data?", "This erases your logs, contacts and settings on this phone. It cannot be undone.", "Erase", () -> {
            Siren.stop();
            Store.clearAll(ctx);
            Intent i = new Intent(ctx, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }));

        h("Stay private");
        LinearLayout t = card();
        in(t, "• Lock your phone and hide notification previews on the lock screen.\n• If someone may be monitoring your phone, use a safer device for sensitive searches.\n• Check Settings > Apps for ones you don't recognise.\n• Use a different password for every important account.\n• Be cautious of links in messages, even from people you know.", 14, R.color.ink, false, 0);
    }

    /** Tiny helper so we only touch the UI if the fragment is still attached. */
    private static final class Context2 {
        static void run(android.content.Context c, boolean ok, Runnable onOk) {
            if (ok) onOk.run(); else U.toast(c, "For security, please sign out, sign in again, then retry.");
        }
    }
}
