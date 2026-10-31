package za.co.herhub;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

/** Base class for every screen. build() is wrapped so a bug in one screen can never take the whole app down. */
public abstract class Page extends Fragment {
    protected Context ctx;
    protected LinearLayout box;
    private Loc.Cb pendingLoc;
    private Runnable pendingNotif;

    private final ActivityResultLauncher<String[]> locPerm = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> {
                Loc.Cb cb = pendingLoc;
                pendingLoc = null;
                if (cb == null || !isAdded()) return;
                if (Loc.has(requireContext())) Loc.fetch(requireContext(), cb);
                else { U.toast(requireContext(), "Location permission is needed for this feature."); cb.got(null); }
            });
    private final ActivityResultLauncher<String> notifPerm = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), ok -> {
                Runnable r = pendingNotif;
                pendingNotif = null;
                if (r != null && isAdded()) {
                    if (!ok) U.toast(requireContext(), "Notifications are off, so reminders won't appear. Enable them in system settings.");
                    r.run();
                }
            });

    private Runnable pendingSms;
    private final ActivityResultLauncher<String> smsPerm = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), ok -> {
                Runnable r = pendingSms;
                pendingSms = null;
                if (r == null || !isAdded()) return;
                if (!ok) { Store.putB(requireContext(), "sos_sms", false); U.toast(requireContext(), "SMS permission denied, so SOS won't text automatically. It will still open HerHub."); }
                r.run();
            });

    protected void withSms(Runnable r) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) r.run();
        else { pendingSms = r; smsPerm.launch(Manifest.permission.SEND_SMS); }
    }

    protected String title() { return ""; }
    protected boolean top() { return false; }
    protected boolean scroll() { return true; }
    protected boolean showHero() { return !(this instanceof HomePage) && !(this instanceof SosPage); }
    protected String subtitle() {
        String n = getClass().getSimpleName();
        if (n.contains("Safety") || n.contains("Sos") || n.contains("Abuse")) return "Practical tools to help you feel safer and more prepared.";
        if (n.contains("Period") || n.contains("Pregnancy") || n.contains("Health") || n.contains("Mind") || n.contains("Wellness")) return "Your body, your wellbeing, your pace.";
        if (n.contains("Map") || n.contains("Location") || n.contains("Journey")) return "Stay aware, stay connected and move with confidence.";
        if (n.contains("Circle") || n.contains("Directory") || n.contains("Support")) return "The people and resources you can turn to.";
        if (n.contains("Security") || n.contains("Notify") || n.contains("More")) return "Make HerHub work in a way that feels right for you.";
        return "A little space to feel safe, informed and supported.";
    }
    protected abstract void build();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle s) {
        ctx = requireContext();
        LinearLayout root = U.col(ctx);
        root.setBackgroundResource(R.drawable.bg_page_glow);

        LinearLayout bar = U.row(ctx);
        bar.setPadding(U.dp(ctx, 16), U.dp(ctx, 10), U.dp(ctx, 16), U.dp(ctx, 8));
        if (!top()) {
            ImageView back = U.icon(ctx, R.drawable.ic_arrow_back, "Back", 42, 0);
            back.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 16));
            back.setElevation(U.dp(ctx, 1));
            back.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
            U.pressable(back);
            bar.addView(back, new LinearLayout.LayoutParams(U.dp(ctx, 42), U.dp(ctx, 42)));
        }
        ImageView mark = new ImageView(ctx);
        mark.setImageResource(R.drawable.ic_herhub_mark);
        mark.setContentDescription("HerHub");
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setPadding(U.dp(ctx, 3), U.dp(ctx, 3), U.dp(ctx, 3), U.dp(ctx, 3));
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(U.dp(ctx, 40), U.dp(ctx, 40));
        markLp.setMargins(U.dp(ctx, top() ? 0 : 10), 0, U.dp(ctx, 8), 0);
        bar.addView(mark, markLp);
        LinearLayout brand = U.col(ctx);
        TextView brandName = U.tv(ctx, "HerHub", 17, R.color.brand, true);
        brand.addView(brandName);
        TextView brandTag = U.tv(ctx, "YOUR SPACE. YOUR STRENGTH.", 9, R.color.ink_soft, true);
        brandTag.setLetterSpacing(.08f);
        brand.addView(brandTag);
        bar.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(bar);

        box = U.col(ctx);
        if (scroll()) {
            box.setPadding(U.dp(ctx, 18), U.dp(ctx, 6), U.dp(ctx, 18), U.dp(ctx, 32));
            ScrollView sv = new ScrollView(ctx);
            sv.setClipToPadding(false);
            sv.setFillViewport(false);
            sv.addView(box);
            root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            box.setPadding(U.dp(ctx, 12), U.dp(ctx, 4), U.dp(ctx, 12), U.dp(ctx, 8));
            root.addView(box, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }
        if (showHero()) addHero();
        safeBuild();
        if (scroll()) for (int i = 0; i < box.getChildCount(); i++) U.rise(box.getChildAt(i), i);
        return root;
    }

    private void addHero() {
        LinearLayout hero = U.row(ctx);
        hero.setBackgroundResource(R.drawable.bg_hero_gradient);
        hero.setPadding(U.dp(ctx, 18), U.dp(ctx, 18), U.dp(ctx, 16), U.dp(ctx, 18));
        ImageView icon = U.icon(ctx, title(), 52, R.color.brand_on);
        icon.setBackground(U.bg(ctx, R.color.brand_coral, 0, 18));
        icon.setPadding(U.dp(ctx, 12), U.dp(ctx, 12), U.dp(ctx, 12), U.dp(ctx, 12));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(U.dp(ctx, 52), U.dp(ctx, 52));
        iconLp.setMargins(0, 0, U.dp(ctx, 14), 0);
        hero.addView(icon, iconLp);
        LinearLayout words = U.col(ctx);
        TextView eyebrow = U.tv(ctx, "YOUR HERHUB SPACE", 9, R.color.brand_on, true);
        eyebrow.setLetterSpacing(.12f);
        words.addView(eyebrow);
        TextView heading = U.tv(ctx, title(), 22, R.color.brand_on, true);
        heading.setPadding(0, U.dp(ctx, 3), 0, 0);
        words.addView(heading);
        TextView sub = U.tv(ctx, subtitle(), 12, R.color.brand_on, false);
        sub.setLineSpacing(0f, 1.12f);
        sub.setPadding(0, U.dp(ctx, 4), 0, 0);
        words.addView(sub);
        hero.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        U.add(box, hero, 2);
    }

    private void safeBuild() {
        try { build(); }
        catch (Throwable e) {
            Log.e("HerHub", "Screen failed: " + getClass().getSimpleName(), e);
            box.removeAllViews();
            LinearLayout c = card();
            c.addView(U.tv(ctx, "This screen hit a problem", 17, R.color.ink, true));
            c.addView(U.tv(ctx, "Go back and try again. If it keeps happening, restart the app.", 14, R.color.ink_soft, false));
        }
    }

    protected void refresh() {
        if (box == null) return;
        box.removeAllViews();
        if (showHero()) addHero();
        safeBuild();
    }
    protected MainActivity act() { return (MainActivity) requireActivity(); }
    protected void open(Page p) { act().open(p); }

    protected TextView h(String s) { TextView t = U.tv(ctx, s, 19, R.color.ink, true); t.setLetterSpacing(-.015f); U.add(box, t, 22); return t; }
    protected TextView p(String s) { TextView t = U.tv(ctx, s, 14, R.color.ink_soft, false); t.setLineSpacing(0f, 1.18f); U.add(box, t, 7); return t; }
    protected LinearLayout card() { LinearLayout c = U.card(ctx); U.add(box, c, 12); return c; }
    /** Small icon + text line. */
    protected TextView head(ViewGroup parent, int icon, String text, float sp, int color, boolean bold, int topDp) {
        LinearLayout r = U.row(ctx);
        r.addView(U.icon(ctx, icon, text, 30, 0), new LinearLayout.LayoutParams(U.dp(ctx, 30), U.dp(ctx, 30)));
        TextView t = U.tv(ctx, text, sp, color, bold);
        t.setPadding(U.dp(ctx, 8), 0, 0, 0);
        r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        U.add(parent, r, topDp);
        return t;
    }
    protected TextView in(ViewGroup parent, String s, float sp, int color, boolean bold, int topDp) {
        TextView t = U.tv(ctx, s, sp, color, bold);
        t.setLineSpacing(0f, 1.1f);
        U.add(parent, t, topDp);
        return t;
    }
    protected MaterialButton btn(ViewGroup parent, String s, boolean filled, Runnable r) {
        MaterialButton b = U.btn(ctx, s, filled, r);
        U.add(parent, b, 10);
        return b;
    }
    protected MaterialButton btn(ViewGroup parent, String s, boolean filled, int iconRes, Runnable r) {
        MaterialButton b = U.btn(ctx, s, filled, iconRes, r);
        U.add(parent, b, 10);
        return b;
    }

    /** A tappable list card with a unique HerHub icon (chosen from the title), title and description. */
    protected void item(String title, String desc, final Runnable r) { item(U.iconFor(title), title, desc, r); }

    protected void item(int iconRes, String title, String desc, final Runnable r) {
        LinearLayout c = U.row(ctx);
        c.setBackgroundResource(R.drawable.bg_card);
        c.setElevation(U.dp(ctx, 2));
        c.setForeground(U.ripple(ctx));
        c.setPadding(U.dp(ctx, 14), U.dp(ctx, 14), U.dp(ctx, 12), U.dp(ctx, 14));
        c.setClickable(true);
        ImageView e = U.tile(ctx, iconRes, title, 46);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(U.dp(ctx, 46), U.dp(ctx, 46));
        iconLp.setMargins(0, 0, U.dp(ctx, 13), 0);
        c.addView(e, iconLp);
        LinearLayout t = U.col(ctx);
        t.setPadding(0, 0, U.dp(ctx, 6), 0);
        t.addView(U.tv(ctx, title, 15, R.color.ink, true));
        if (desc != null && !desc.isEmpty()) {
            TextView description = U.tv(ctx, desc, 12, R.color.ink_soft, false);
            description.setPadding(0, U.dp(ctx, 3), 0, 0);
            description.setLineSpacing(0f, 1.12f);
            t.addView(description);
        }
        c.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView chev = U.icon(ctx, R.drawable.ic_chevron_right, "Open", 28, 0);
        c.addView(chev, new LinearLayout.LayoutParams(U.dp(ctx, 28), U.dp(ctx, 28)));
        U.pressable(c);
        c.setOnClickListener(v -> {
            try { r.run(); } catch (Throwable ex) { Log.e("HerHub", "click failed", ex); U.toast(ctx, "Something went wrong. Please try again."); }
        });
        U.add(box, c, 10);
    }

    protected void locate(Loc.Cb cb) {
        if (Loc.has(ctx)) Loc.fetch(ctx, cb);
        else { pendingLoc = cb; locPerm.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}); }
    }

    protected void withNotifPermission(Runnable r) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingNotif = r;
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else r.run();
    }

    protected String circleNumbers() {
        org.json.JSONArray a = Store.arr(ctx, "d_circle");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length(); i++) {
            org.json.JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(o.optString("phone"));
        }
        return sb.toString();
    }

    /** SMS the trusted circle with an optional live-location link. */
    protected void alertCircle(final String prefix) {
        final String nums = circleNumbers();
        if (nums.isEmpty()) {
            U.confirm(ctx, "No trusted contacts yet", "Add at least one trusted contact so HerHub can prepare an alert message for them.", "Add contacts",
                    () -> open(new CirclePage()));
            return;
        }
        U.toast(ctx, "Getting your location…");
        locate(l -> {
            if (!isAdded()) return;
            String msg = prefix + (l != null ? "\nMy location: " + Loc.link(l) : "\n(Location unavailable)") + "\nSent from HerHub";
            U.sms(ctx, nums, msg);
        });
    }
}
