package za.co.herhub;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Animated launch screen. The app icon already carries the HerHub wordmark, so the title here is the
 * tagline: "Your space. Your strength."  Pulse rings, a glowing logo pop-in, drifting leaves and a
 * loading heartbeat, all in the HerHub colours (and the dark-mode palette automatically).
 */
public class SplashActivity extends AppCompatActivity {
    private static final long HOLD_MS = 3000;

    private final Handler h = new Handler(Looper.getMainLooper());
    private ValueAnimator loop;
    private boolean left = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat ctl = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ctl.setAppearanceLightStatusBars(false);
        ctl.setAppearanceLightNavigationBars(false);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(U.gradient(this, R.color.splash_start, R.color.splash_end, 0));
        setContentView(root);

        // Drifting leaf flourishes in the corners (same botanical motif as the prototype).
        final ImageView leafBL = leaf(root, Gravity.BOTTOM | Gravity.START, 230, 0f, false);
        final ImageView leafTR = leaf(root, Gravity.TOP | Gravity.END, 200, 180f, true);

        // Centre stack
        LinearLayout centre = U.col(this);
        centre.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(centre, clp);

        // Logo stage: rings + glow + logo
        final int stage = U.dp(this, 300);
        FrameLayout logoStage = new FrameLayout(this);
        centre.addView(logoStage, new LinearLayout.LayoutParams(stage, stage));

        final View[] rings = new View[3];
        for (int i = 0; i < rings.length; i++) {
            View r = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(Color.TRANSPARENT);
            g.setStroke(U.dp(this, 2), 0xFFFFFFFF);
            r.setBackground(g);
            r.setAlpha(0f);
            int size = U.dp(this, 170);
            logoStage.addView(r, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
            rings[i] = r;
        }

        final View glow = new View(this);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0x66FFFFFF, 0x00FFFFFF});
        gd.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gd.setGradientRadius(U.dp(this, 150));
        gd.setShape(GradientDrawable.OVAL);
        glow.setBackground(gd);
        glow.setAlpha(0f);
        logoStage.addView(glow, new FrameLayout.LayoutParams(stage, stage, Gravity.CENTER));

        final ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.herhub_logo);
        logo.setContentDescription("HerHub logo");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(U.bg(this, R.color.white, 0, 40));
        logo.setClipToOutline(true);
        logo.setElevation(U.dp(this, 14));
        logo.setScaleX(.35f);
        logo.setScaleY(.35f);
        logo.setAlpha(0f);
        int ls = U.dp(this, 168);
        logoStage.addView(logo, new FrameLayout.LayoutParams(ls, ls, Gravity.CENTER));

        // Title = the tagline (the logo already says HerHub)
        final TextView title = U.tv(this, "Your space.\nYour strength.", 30, R.color.white, true);
        title.setGravity(Gravity.CENTER);
        title.setLineSpacing(0f, 1.05f);
        title.setLetterSpacing(0.01f);
        title.setShadowLayer(U.dp(this, 6), 0, U.dp(this, 2), 0x40000000);
        title.setAlpha(0f);
        title.setTranslationY(U.dp(this, 28));
        centre.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final View underline = new View(this);
        underline.setBackground(U.bg(this, R.color.brand_coral, 0, 3));
        underline.setPivotX(0f);
        underline.setScaleX(0f);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(U.dp(this, 64), U.dp(this, 4));
        ulp.topMargin = U.dp(this, 16);
        underline.setPivotX(U.dp(this, 32));
        centre.addView(underline, ulp);

        // Loading heartbeat dots at the bottom
        final LinearLayout dots = U.row(this);
        dots.setGravity(Gravity.CENTER);
        dots.setAlpha(0f);
        final View[] dot = new View[3];
        for (int i = 0; i < 3; i++) {
            View d = new View(this);
            d.setBackground(U.bg(this, R.color.white, 0, 5));
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(U.dp(this, 10), U.dp(this, 10));
            dl.setMargins(U.dp(this, 5), 0, U.dp(this, 5), 0);
            dots.addView(d, dl);
            dot[i] = d;
        }
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dlp.bottomMargin = U.dp(this, 64);
        root.addView(dots, dlp);

        // --- Entrance sequence ---
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(950)
                .setInterpolator(new OvershootInterpolator(1.5f)).start();
        glow.animate().alpha(1f).setStartDelay(150).setDuration(900).start();
        title.animate().alpha(1f).translationY(0f).setStartDelay(650).setDuration(800)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
        underline.animate().scaleX(1f).setStartDelay(1100).setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).start();
        dots.animate().alpha(1f).setStartDelay(1200).setDuration(500).start();

        // --- Continuous loop: expanding rings, breathing glow, leaf drift, dot wave ---
        loop = ValueAnimator.ofFloat(0f, 1f);
        loop.setDuration(2400);
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setInterpolator(new LinearInterpolator());
        loop.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            for (int i = 0; i < rings.length; i++) {
                float p = (t + i / 3f) % 1f;
                float s = 1f + p * 0.75f;
                rings[i].setScaleX(s);
                rings[i].setScaleY(s);
                rings[i].setAlpha((1f - p) * 0.55f * Math.min(1f, glow.getAlpha() * 1.2f));
            }
            double wave = Math.sin(t * 2 * Math.PI);
            glow.setScaleX((float) (1f + 0.06f * wave));
            glow.setScaleY((float) (1f + 0.06f * wave));
            leafBL.setTranslationY((float) (U.dp(this, 10) * wave));
            leafTR.setTranslationY((float) (-U.dp(this, 10) * wave));
            for (int i = 0; i < dot.length; i++) {
                float p = (t * 2f + i * 0.2f) % 1f;
                float k = (float) (0.5f + 0.5f * Math.sin(p * 2 * Math.PI));
                dot[i].setAlpha(0.35f + 0.65f * k);
                dot[i].setScaleX(0.8f + 0.5f * k);
                dot[i].setScaleY(0.8f + 0.5f * k);
            }
        });
        loop.start();

        // Gentle breathing once the logo has landed
        h.postDelayed(() -> {
            if (left) return;
            logo.animate().scaleX(1.05f).scaleY(1.05f).setDuration(900)
                    .setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(new Runnable() {
                        boolean up = false;
                        @Override public void run() {
                            if (left) return;
                            float s = up ? 1.05f : 1f;
                            up = !up;
                            logo.animate().scaleX(s).scaleY(s).setDuration(900).withEndAction(this).start();
                        }
                    }).start();
        }, 1000);

        h.postDelayed(this::next, HOLD_MS);
    }

    private ImageView leaf(FrameLayout root, int gravity, int sizeDp, float rotation, boolean mirror) {
        ImageView v = new ImageView(this);
        v.setImageResource(R.drawable.ic_leaf_flourish);
        v.setColorFilter(0xFFFFFFFF);
        v.setAlpha(0.16f);
        v.setRotation(rotation);
        if (mirror) v.setScaleX(-1f);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(U.dp(this, sizeDp), U.dp(this, sizeDp), gravity);
        root.addView(v, lp);
        return v;
    }

    private void next() {
        if (left || isFinishing()) return;
        left = true;
        boolean signedIn = Store.user() != null || Store.getB(this, "guest", false);
        Intent i = new Intent(this, signedIn ? MainActivity.class : AuthActivity.class);
        startActivity(i);
        finish();
        try { overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); } catch (Throwable ignored) { }
    }

    @Override protected void onDestroy() {
        left = true;
        h.removeCallbacksAndMessages(null);
        if (loop != null) loop.cancel();
        super.onDestroy();
    }
}
