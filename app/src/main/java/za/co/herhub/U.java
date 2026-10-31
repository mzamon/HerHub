package za.co.herhub;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** UI helpers shared by every screen. All colours come from res/values(-night)/colors.xml so dark mode is automatic. */
public final class U {
    private U() { }

    public static int c(Context x, int res) { return ContextCompat.getColor(x, res); }
    public static int dp(Context x, float v) { return Math.round(v * x.getResources().getDisplayMetrics().density); }
    public static boolean night(Context x) {
        return (x.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
    public static void toast(Context x, String s) { try { Toast.makeText(x, s, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) { } }

    public static TextView tv(Context x, String s, float sp, int colorRes, boolean bold) {
        TextView t = new TextView(x);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(c(x, colorRes));
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    public static GradientDrawable bg(Context x, int fillRes, int strokeRes, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c(x, fillRes));
        g.setCornerRadius(dp(x, radiusDp));
        if (strokeRes != 0) g.setStroke(dp(x, 1), c(x, strokeRes));
        return g;
    }

    public static GradientDrawable gradient(Context x, int startRes, int endRes, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{c(x, startRes), c(x, endRes)});
        g.setCornerRadius(dp(x, radiusDp));
        return g;
    }

    public static LinearLayout col(Context x) {
        LinearLayout l = new LinearLayout(x);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    public static LinearLayout row(Context x) {
        LinearLayout l = new LinearLayout(x);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    public static LinearLayout row(View parent) { return row(parent.getContext()); }
    public static LinearLayout col(View parent) { return col(parent.getContext()); }
    public static LinearLayout card(Context x) {
        LinearLayout l = col(x);
        l.setBackgroundResource(R.drawable.bg_card);
        l.setElevation(dp(x, 3));
        int p = dp(x, 18);
        l.setPadding(p, p, p, p);
        return l;
    }

    /** Feature icon for a label. tintRes == 0 keeps the icon's own two-tone HerHub colours (best on light tiles);
     *  pass a colour (for example brand_on) to flatten it, for use on gradient backgrounds. Always labelled for accessibility. */
    public static ImageView icon(Context x, String label, int sizeDp, int tintRes) {
        return icon(x, iconFor(label), label, sizeDp, tintRes);
    }

    public static ImageView icon(Context x, int drawableRes, String label, int sizeDp, int tintRes) {
        ImageView v = new ImageView(x);
        v.setImageResource(drawableRes);
        if (tintRes != 0) v.setColorFilter(c(x, tintRes));
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.setContentDescription(label);
        int pad = dp(x, sizeDp * .22f);
        v.setPadding(pad, pad, pad, pad);
        return v;
    }

    /** Icon in a soft rounded tile, the standard HerHub feature badge. */
    public static ImageView tile(Context x, int drawableRes, String label, int sizeDp) {
        ImageView v = icon(x, drawableRes, label, sizeDp, 0);
        v.setBackgroundResource(R.drawable.bg_icon_bubble);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(x, sizeDp), dp(x, sizeDp)));
        return v;
    }

    private static final java.util.LinkedHashMap<String, Integer> ICONS = new java.util.LinkedHashMap<>();
    private static void ic(String key, int res) { ICONS.put(key, res); }
    static {
        // One unique icon per feature. Longest matching key wins.
        ic("personal safety", R.drawable.ic_sos_alert);
        ic("location & safety", R.drawable.ic_location_live);
        ic("community safety map", R.drawable.ic_safety_map);
        ic("safety map", R.drawable.ic_safety_map);
        ic("period", R.drawable.ic_period_calendar);
        ic("pregnancy", R.drawable.ic_pregnancy);
        ic("mental health", R.drawable.ic_meditation);
        ic("digital", R.drawable.ic_privacy_lock);
        ic("trusted circle", R.drawable.ic_trusted_circle);
        ic("safe journey", R.drawable.ic_safe_journey);
        ic("information hub", R.drawable.ic_womens_hub);
        ic("support & community", R.drawable.ic_forum);
        ic("abuse", R.drawable.ic_abuse_support);
        ic("financial", R.drawable.ic_financial_safety);
        ic("notification", R.drawable.ic_notification_bell);
        ic("resource directory", R.drawable.ic_emergency_contacts);
        ic("assistant", R.drawable.ic_ai_assistant);
        ic("share location", R.drawable.ic_location_share);
        ic("mood check-in", R.drawable.ic_mood_great);
        ic("talk it through", R.drawable.ic_chat_bubble);
        ic("open safety map", R.drawable.ic_safety_map);
        ic("my account", R.drawable.ic_profile);
        ic("appearance", R.drawable.ic_settings);
        ic("privacy", R.drawable.ic_privacy_lock);
        ic("help", R.drawable.ic_help_support);
        ic("know your rights", R.drawable.ic_legal_aid);
        ic("settings", R.drawable.ic_settings);
        ic("herhub", R.drawable.ic_herhub_mark);
        ic("safety", R.drawable.ic_nav_safety);
    }

    public static int iconFor(String label) {
        String s = label == null ? "" : label.toLowerCase(java.util.Locale.ROOT).trim();
        if (s.equals("health & wellness")) return R.drawable.ic_health_record;
        if (s.equals("health")) return R.drawable.ic_nav_health;
        if (s.equals("more")) return R.drawable.ic_nav_more;
        int best = 0, res = R.drawable.ic_help_support;
        for (java.util.Map.Entry<String, Integer> e : ICONS.entrySet()) {
            if (s.contains(e.getKey()) && e.getKey().length() > best) { best = e.getKey().length(); res = e.getValue(); }
        }
        return res;
    }

    /** Staggered fade-and-rise entrance so screens feel alive. */
    public static void rise(View v, int index) {
        v.setAlpha(0f);
        v.setTranslationY(dp(v.getContext(), 14));
        v.animate().alpha(1f).translationY(0f).setStartDelay(Math.min(index, 12) * 45L).setDuration(380)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }

    /** Gentle press feedback: the view shrinks a touch while held. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    public static void pressable(View v) {
        v.setOnTouchListener((view, ev) -> {
            int a = ev.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_DOWN) view.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start();
            else if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL) view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            return false;
        });
    }

    /** Soft heartbeat pulse, used on the SOS button. */
    public static void pulse(View v) {
        try { v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(v.getContext(), R.anim.anim_pulse_infinite)); }
        catch (Throwable ignored) { }
    }

    public static void add(ViewGroup p, View v, int topDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        l.topMargin = dp(p.getContext(), topDp);
        p.addView(v, l);
    }
    public static void addW(LinearLayout p, View v, int marginDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l.setMargins(dp(p.getContext(), marginDp), 0, dp(p.getContext(), marginDp), 0);
        p.addView(v, l);
    }

    public static MaterialButton btn(Context x, String s, boolean filled, final Runnable r) {
        return btn(x, s, filled, 0, r);
    }

    /** Button with a leading icon. Filled buttons flatten the icon to white; outlined buttons keep the two-tone HerHub icon. */
    public static MaterialButton btn(Context x, String s, boolean filled, int iconRes, final Runnable r) {
        MaterialButton b = filled ? new MaterialButton(x)
                : new MaterialButton(x, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(s);
        b.setAllCaps(false);
        b.setCornerRadius(dp(x, 18));
        b.setMinHeight(dp(x, 52));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (iconRes != 0) {
            b.setIconResource(iconRes);
            b.setIconSize(dp(x, 22));
            b.setIconPadding(dp(x, 10));
            b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            b.setIconTint(filled ? ColorStateList.valueOf(Color.WHITE) : null);
        }
        if (filled) {
            b.setBackground(gradient(x, R.color.brand_hot, R.color.brand_dark, 18));
            b.setBackgroundTintList(null);
            b.setTextColor(Color.WHITE);
            b.setRippleColor(ColorStateList.valueOf(0x33FFFFFF));
            b.setElevation(dp(x, 2));
        } else {
            b.setTextColor(c(x, R.color.brand_dark));
            b.setStrokeColor(ColorStateList.valueOf(c(x, R.color.outline)));
            b.setBackgroundTintList(ColorStateList.valueOf(c(x, R.color.surface)));
        }
        pressable(b);
        if (r != null) b.setOnClickListener(v -> {
            try { r.run(); } catch (Throwable t) { toast(x, "Something went wrong. Please try again."); }
        });
        return b;
    }

    /** Button variant for compact toolbars where the icon should sit above its label. */
    public static MaterialButton verticalBtn(Context x, String s, boolean filled, int iconRes, final Runnable r) {
        MaterialButton b = btn(x, s, filled, iconRes, r);
        b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_TOP);
        b.setIconPadding(dp(x, 4));
        b.setMinHeight(dp(x, 76));
        b.setPadding(dp(x, 8), dp(x, 8), dp(x, 8), dp(x, 8));
        return b;
    }

    public static MaterialButton danger(Context x, String s, Runnable r) { return danger(x, s, 0, r); }
    public static MaterialButton danger(Context x, String s, int iconRes, Runnable r) {
        MaterialButton b = btn(x, s, true, iconRes, r);
        b.setBackground(gradient(x, R.color.danger, R.color.danger_dark, 18));
        b.setBackgroundTintList(null);
        b.setTextColor(Color.WHITE);
        b.setRippleColor(ColorStateList.valueOf(0x33FFFFFF));
        return b;
    }

    /** Google sign-in button, shared by the login and registration screens. */
    public static MaterialButton google(Context x, String label, final Runnable r) {
        MaterialButton b = new MaterialButton(x, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(label);
        b.setAllCaps(false);
        b.setCornerRadius(dp(x, 26));
        b.setMinHeight(dp(x, 54));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setIconResource(R.drawable.ic_google);
        b.setIconSize(dp(x, 22));
        b.setIconPadding(dp(x, 12));
        b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        b.setIconTint(null);
        b.setTextColor(c(x, R.color.ink));
        b.setStrokeWidth(dp(x, 2));
        b.setStrokeColor(ColorStateList.valueOf(c(x, R.color.brand)));
        b.setBackgroundTintList(ColorStateList.valueOf(c(x, R.color.surface)));
        b.setRippleColor(ColorStateList.valueOf(0x22D92F78));
        b.setElevation(dp(x, 2));
        pressable(b);
        b.setOnClickListener(v -> {
            try { r.run(); } catch (Throwable t) { toast(x, "Google sign-in isn't available right now."); }
        });
        return b;
    }

    public static TextInputLayout field(Context x, String hint, int type) {
        TextInputLayout til = new TextInputLayout(x, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint(hint);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        til.setBoxBackgroundColor(c(x, R.color.surface_alt));
        til.setBoxCornerRadii(dp(x, 18), dp(x, 18), dp(x, 18), dp(x, 18));
        til.setBoxStrokeColor(c(x, R.color.brand));
        til.setBoxStrokeWidth(dp(x, 1));
        til.setBoxStrokeWidthFocused(dp(x, 2));
        til.setHintTextColor(ColorStateList.valueOf(c(x, R.color.ink_soft)));
        TextInputEditText e = new TextInputEditText(til.getContext());
        e.setInputType(type);
        e.setTextColor(c(x, R.color.ink));
        e.setHintTextColor(c(x, R.color.ink_soft));
        e.setTextSize(15);
        e.setPadding(dp(x, 15), dp(x, 15), dp(x, 15), dp(x, 15));
        til.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return til;
    }
    public static TextInputLayout field(Context x, String hint) {
        return field(x, hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    }
    public static TextInputLayout multi(Context x, String hint, int lines) {
        TextInputLayout t = field(x, hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (t.getEditText() != null) { t.getEditText().setMinLines(lines); t.getEditText().setGravity(Gravity.TOP | Gravity.START); }
        return t;
    }
    public static String val(TextInputLayout t) {
        return t.getEditText() == null || t.getEditText().getText() == null ? "" : t.getEditText().getText().toString().trim();
    }

    public static void dialog(Context x, String title, View content, String ok, final Runnable onOk) {
        FrameLayout f = new FrameLayout(x);
        int p = dp(x, 20);
        f.setPadding(p, dp(x, 8), p, 0);
        f.addView(content);
        ScrollView sv = new ScrollView(x);
        sv.addView(f);
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(x).setTitle(title).setView(sv);
        if (ok != null) b.setPositiveButton(ok, (d, w) -> { if (onOk != null) onOk.run(); });
        b.setNegativeButton(ok == null ? "Close" : "Cancel", null);
        b.show();
    }
    public static void info(Context x, String title, String body) {
        TextView t = tv(x, body, 15, R.color.ink, false);
        t.setLineSpacing(0f, 1.15f);
        dialog(x, title, t, null, null);
    }
    public static void confirm(Context x, String title, String msg, String ok, final Runnable r) {
        new MaterialAlertDialogBuilder(x).setTitle(title).setMessage(msg)
                .setPositiveButton(ok, (d, w) -> r.run()).setNegativeButton("Cancel", null).show();
    }

    public static void dial(Context x, String number) {
        try {
            x.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number.replace(" ", "")))));
        } catch (Throwable t) { toast(x, "No phone app available. Number: " + number); }
    }
    public static void sms(Context x, String numbers, String body) {
        try {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(numbers)));
            i.putExtra("sms_body", body);
            x.startActivity(i);
        } catch (Throwable t) { share(x, body); }
    }
    public static void share(Context x, String text) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            x.startActivity(Intent.createChooser(i, "Share via"));
        } catch (Throwable t) { toast(x, "Nothing available to share with."); }
    }
    public static void url(Context x, String u) {
        try { x.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }
        catch (Throwable t) { toast(x, "No browser available."); }
    }
    public static void maps(Context x, String query) {
        try { x.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))); }
        catch (ActivityNotFoundException e) { url(x, "https://www.openstreetmap.org/search?query=" + Uri.encode(query)); }
        catch (Throwable t) { toast(x, "Could not open maps."); }
    }
    public static void copy(Context x, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) x.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("HerHub", text));
            toast(x, "Copied");
        } catch (Throwable ignored) { }
    }

    public static Drawable ripple(Context x) {
        TypedArray a = x.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        Drawable d = a.getDrawable(0);
        a.recycle();
        return d;
    }

    /** Edge-to-edge handling (required on Android 15) + correct status-bar icon colour for light/dark. */
    public static void edge(Activity a, View root, final View hideOnIme) {
        WindowCompat.setDecorFitsSystemWindows(a.getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            if (hideOnIme != null) hideOnIme.setVisibility(insets.isVisible(WindowInsetsCompat.Type.ime()) ? View.GONE : View.VISIBLE);
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        boolean dark = night(a);
        androidx.core.view.WindowInsetsControllerCompat ctl = WindowCompat.getInsetsController(a.getWindow(), root);
        ctl.setAppearanceLightStatusBars(!dark);
        ctl.setAppearanceLightNavigationBars(!dark);
    }

    public static String today() { return java.time.LocalDate.now().toString(); }
}
