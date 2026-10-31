package za.co.herhub;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONObject;

public class MindPage extends Page {
    private static final int[] MOOD_ICONS = {R.drawable.ic_mood_sad, R.drawable.ic_mood_low, R.drawable.ic_mood_okay, R.drawable.ic_mood_good, R.drawable.ic_mood_great};
    private static final String[] MOOD_NAMES = {"Struggling", "Low", "Okay", "Good", "Great"};
    private static final String[] AFFIRM = {
            "I am allowed to take up space.", "My feelings are valid, and they will change.", "I can ask for help, and that is strength.",
            "I am doing the best I can today.", "I deserve safety, rest and kindness.", "One small step is still progress."};
    private static final String[] PHASE = {"Breathe in…", "Hold…", "Breathe out…", "Hold…"};
    private static final float[] SCALE = {1f, 1f, 0.55f, 0.55f};

    private final Handler h = new Handler(Looper.getMainLooper());
    private Runnable breath;
    private View circle;
    private TextView phase;
    private MaterialButton breathBtn;

    @Override protected String title() { return "Mental Health & Wellness"; }

    @Override protected void build() {
        stopBreathing(false);
        LinearLayout c = card();
        in(c, "How are you feeling right now?", 17, R.color.ink, true, 0);
        LinearLayout r = U.row(c);
        U.add(c, r, 12);
        for (int i = 0; i < MOOD_ICONS.length; i++) {
            final int ix = i;
            LinearLayout b = U.col(ctx);
            b.setGravity(Gravity.CENTER);
            b.setClickable(true);
            b.setForeground(U.ripple(ctx));
            b.setPadding(0, U.dp(ctx, 6), 0, U.dp(ctx, 6));
            b.addView(U.icon(ctx, MOOD_ICONS[i], MOOD_NAMES[i], 44, 0), new LinearLayout.LayoutParams(U.dp(ctx, 44), U.dp(ctx, 44)));
            TextView l = U.tv(ctx, MOOD_NAMES[i], 10, R.color.ink_soft, false);
            l.setGravity(Gravity.CENTER);
            b.addView(l);
            b.setContentDescription(MOOD_NAMES[i]);
            b.setOnClickListener(v -> moodDialog(ix));
            U.addW(r, b, 1);
        }
        JSONArray m = Store.arr(ctx, "d_moods");
        if (m.length() > 0) {
            in(c, "Recent", 12, R.color.ink_soft, true, 10);
            LinearLayout recent = U.row(c);
            U.add(c, recent, 4);
            for (int i = 0; i < Math.min(m.length(), 10); i++) {
                JSONObject o = m.optJSONObject(i);
                if (o == null) continue;
                int lv = Math.max(0, Math.min(4, o.optInt("mood", 2)));
                recent.addView(U.icon(ctx, MOOD_ICONS[lv], MOOD_NAMES[lv], 30, 0), new LinearLayout.LayoutParams(U.dp(ctx, 30), U.dp(ctx, 30)));
            }
            JSONObject last = m.optJSONObject(0);
            if (last != null && last.optInt("mood", 2) <= 1)
                in(c, "I'm sorry it's been hard. You're not alone — see the support lines below if you'd like to talk to someone.", 13, R.color.ink_soft, false, 6);
        }

        h("Guided breathing");
        LinearLayout b = card();
        b.setGravity(Gravity.CENTER_HORIZONTAL);
        in(b, "Box breathing: 4 seconds in, hold, out, hold.", 13, R.color.ink_soft, false, 0);
        FrameLayoutHolder holder = new FrameLayoutHolder();
        b.addView(holder.frame, new LinearLayout.LayoutParams(U.dp(ctx, 200), U.dp(ctx, 200)));
        phase = U.tv(ctx, "Ready when you are", 16, R.color.ink, true);
        phase.setGravity(Gravity.CENTER);
        U.add(b, phase, 4);
        breathBtn = btn(b, "Start", true, R.drawable.ic_breathing, this::toggleBreathing);

        h("Grounding: 5-4-3-2-1");
        LinearLayout g = card();
        in(g, "Name 5 things you can see, 4 you can feel, 3 you can hear, 2 you can smell and 1 you can taste. Move slowly and breathe.", 14, R.color.ink, false, 0);

        h("Today's affirmation");
        LinearLayout a = card();
        head(a, R.drawable.ic_journal, AFFIRM[java.time.LocalDate.now().getDayOfYear() % AFFIRM.length], 16, R.color.ink, true, 0);

        h("Talk to someone");
        LinearLayout s = card();
        in(s, "If you're thinking about harming yourself or feel you can't cope, please reach out now.", 14, R.color.ink, false, 0);
        btn(s, "SADAG 0800 21 22 23", false, R.drawable.ic_therapy, () -> U.dial(ctx, "0800212223"));
        btn(s, "Suicide Crisis Line 0800 567 567", false, R.drawable.ic_crisis_support, () -> U.dial(ctx, "0800567567"));
        btn(s, "Lifeline 0861 322 322", false, R.drawable.ic_phone_call, () -> U.dial(ctx, "0861322322"));
        btn(s, "Emergency 112", true, R.drawable.ic_emergency_phone, () -> U.dial(ctx, "112"));
        item(R.drawable.ic_chat_bubble, "Talk it through with the assistant", "Gentle guidance and next steps", () -> open(new AssistantPage()));
    }

    private class FrameLayoutHolder {
        final android.widget.FrameLayout frame = new android.widget.FrameLayout(ctx);
        FrameLayoutHolder() {
            circle = new View(ctx);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(U.c(ctx, R.color.brand_soft));
            g.setStroke(U.dp(ctx, 3), U.c(ctx, R.color.brand));
            circle.setBackground(g);
            circle.setScaleX(0.55f);
            circle.setScaleY(0.55f);
            frame.addView(circle, new android.widget.FrameLayout.LayoutParams(-1, -1));
        }
    }

    private void toggleBreathing() { if (breath != null) stopBreathing(true); else startBreathing(); }

    private void startBreathing() {
        breathBtn.setText("Stop");
        final int[] step = {0};
        breath = new Runnable() {
            @Override public void run() {
                if (!isAdded() || circle == null) return;
                int i = step[0]++ % 4;
                phase.setText(PHASE[i]);
                circle.animate().scaleX(SCALE[i]).scaleY(SCALE[i]).setDuration(i == 0 || i == 2 ? 4000 : 200).start();
                h.postDelayed(this, 4000);
            }
        };
        h.post(breath);
    }

    private void stopBreathing(boolean ui) {
        if (breath != null) { h.removeCallbacks(breath); breath = null; }
        if (ui && circle != null && phase != null) {
            circle.animate().cancel();
            circle.animate().scaleX(0.55f).scaleY(0.55f).setDuration(300).start();
            phase.setText("Well done. Notice how you feel.");
            breathBtn.setText("Start");
        }
    }

    private void moodDialog(final int mood) {
        final TextInputLayout n = U.multi(ctx, "Want to note what's behind this feeling? (optional)", 2);
        U.dialog(ctx, MOOD_NAMES[mood], n, "Save", () -> {
            Store.prepend(ctx, "d_moods", Store.obj("date", U.today(), "mood", String.valueOf(mood), "note", U.val(n)), 90);
            U.toast(ctx, "Check-in saved");
            refresh();
        });
    }

    @Override public void onDestroyView() { stopBreathing(false); circle = null; phase = null; super.onDestroyView(); }
}
