package za.co.herhub;

import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FinancePage extends Page {
    // {keyword, weight, reason}
    private static final Object[][] RULES = {
            {"otp", 2, "Asks for or mentions a one-time PIN. Real banks never ask for it."},
            {"one-time pin", 2, "Asks for a one-time PIN."},
            {"pin ", 1, "Mentions a PIN."},
            {"password", 1, "Mentions your password."},
            {"cvv", 2, "Asks for card security details."},
            {"card number", 2, "Asks for your card number."},
            {"bank details", 2, "Asks for your banking details."},
            {"verify your account", 2, "Pressure to 'verify' your account."},
            {"suspended", 1, "Threatens account suspension."},
            {"blocked", 1, "Claims something is blocked."},
            {"urgent", 1, "Creates urgency."},
            {"immediately", 1, "Pushes you to act immediately."},
            {"within 24 hours", 1, "Sets a tight deadline."},
            {"click", 1, "Asks you to click a link."},
            {"http", 1, "Contains a link. Check it carefully."},
            {"bit.ly", 2, "Uses a shortened link that hides the destination."},
            {"tinyurl", 2, "Uses a shortened link that hides the destination."},
            {"won", 1, "Claims you've won something."},
            {"winner", 2, "Claims you're a winner."},
            {"prize", 2, "Mentions a prize."},
            {"lottery", 2, "Mentions a lottery you didn't enter."},
            {"congratulations", 1, "Unexpected congratulations."},
            {"gift card", 2, "Asks for gift cards. A classic scam sign."},
            {"bitcoin", 2, "Asks for crypto payment."},
            {"crypto", 1, "Mentions crypto investment."},
            {"guaranteed", 2, "Promises guaranteed returns."},
            {"double your", 2, "Promises to double your money."},
            {"processing fee", 2, "Asks for an upfront fee."},
            {"registration fee", 2, "Asks for an upfront fee."},
            {"advance fee", 2, "Asks for an upfront fee."},
            {"inheritance", 2, "Mentions an unexpected inheritance."},
            {"sars", 1, "Claims to be from SARS. Check on the official website."},
            {"refund", 1, "Offers an unexpected refund."},
            {"western union", 2, "Asks for money transfer."},
            {"work from home", 1, "Too-good-to-be-true job offer."},
            {"easy money", 2, "Promises easy money."},
            {"loan approved", 2, "Unexpected loan approval, often with a fee."},
            {"whatsapp me", 1, "Wants to move you to WhatsApp."}
    };

    @Override protected String title() { return "Financial Safety"; }

    @Override protected void build() {
        h("Scam checker");
        LinearLayout c = card();
        in(c, "Paste a suspicious SMS, email or WhatsApp message. It's checked on your phone and not sent anywhere.", 13, R.color.ink_soft, false, 0);
        final TextInputLayout msg = U.multi(ctx, "Paste the message here", 4);
        U.add(c, msg, 10);
        final LinearLayout result = U.col(ctx);
        btn(c, "Check message", true, R.drawable.ic_scam_alert, () -> check(U.val(msg), result));
        c.addView(result);

        h("Savings goal");
        LinearLayout g = card();
        JSONObject goal = goal();
        if (goal == null) {
            in(g, "Set a goal for something that matters (emergency fund, a safe place, a course).", 14, R.color.ink_soft, false, 0);
            btn(g, "Set a goal", true, R.drawable.ic_budget, this::setGoal);
        } else {
            double target = goal.optDouble("target", 1), saved = goal.optDouble("saved", 0);
            head(g, R.drawable.ic_budget, goal.optString("name"), 17, R.color.ink, true, 0);
            in(g, String.format(Locale.US, "R%,.0f of R%,.0f", saved, target), 20, R.color.brand, true, 4);
            ProgressBar pb = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(1000);
            pb.setProgress((int) Math.min(1000, saved / Math.max(1, target) * 1000));
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(U.c(ctx, R.color.brand)));
            U.add(g, pb, 10);
            btn(g, "Add savings", true, R.drawable.ic_add, this::addSavings);
            btn(g, "Reset goal", false, () -> { Store.remove(ctx, "d_goal"); refresh(); });
        }

        h("Stay safe with money");
        LinearLayout t = card();
        in(t, "• Banks never ask for your PIN, password or OTP.\n• Never pay a fee to receive a loan, prize or job.\n• Check the sender. Call your bank on the number on the back of your card.\n• Be careful with 'guaranteed returns'. If it sounds too good to be true, it is.\n• Don't give someone remote access to your phone or computer.\n• Use a different PIN for each card and app.\n• If you suspect fraud, call your bank straight away to block your card.", 14, R.color.ink, false, 0);

        h("Report fraud");
        LinearLayout r = card();
        btn(r, "FSCA consumer line 0800 20 3722", false, () -> U.dial(ctx, "0800203722"));
        btn(r, "Police 10111", false, () -> U.dial(ctx, "10111"));
        p("General awareness only, not financial advice.");
    }

    /** Shared with the assistant: scores a pasted message and fills in the reasons. */
    static int scan(String text, List<String> why) {
        String s = text.toLowerCase(Locale.ROOT) + " ";
        int score = 0;
        for (Object[] r : RULES) if (s.contains((String) r[0])) { score += (Integer) r[1]; why.add((String) r[2]); }
        return score;
    }

    private void check(String text, LinearLayout out) {
        out.removeAllViews();
        if (text.length() < 8) { U.toast(ctx, "Paste a message first."); return; }
        String s = text.toLowerCase(Locale.ROOT) + " ";
        int score = 0;
        List<String> why = new ArrayList<>();
        for (Object[] r : RULES) if (s.contains((String) r[0])) { score += (Integer) r[1]; why.add((String) r[2]); }
        String level = score >= 5 ? "High risk" : score >= 3 ? "Suspicious" : score >= 1 ? "Some warning signs" : "No obvious red flags";
        in(out, level, 18, score >= 5 ? R.color.danger : R.color.ink, true, 14);
        StringBuilder sb = new StringBuilder();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>(why);
        for (String w : seen) sb.append("• ").append(w).append("\n");
        if (sb.length() > 0) in(out, sb.toString().trim(), 14, R.color.ink, false, 6);
        in(out, score >= 3 ? "Don't click links, reply or share any details. Contact the company directly using a number you trust." : "No checker is perfect. If something feels off, verify directly with the company.", 13, R.color.ink_soft, false, 8);
    }

    private JSONObject goal() {
        try { String s = Store.get(ctx, "d_goal", null); return s == null ? null : new JSONObject(s); } catch (Throwable t) { return null; }
    }

    private void setGoal() {
        LinearLayout c = U.col(ctx);
        final TextInputLayout n = U.field(ctx, "Goal name");
        final TextInputLayout a = U.field(ctx, "Target amount (R)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        c.addView(n);
        U.add(c, a, 10);
        U.dialog(ctx, "New savings goal", c, "Save", () -> {
            double t;
            try { t = Double.parseDouble(U.val(a)); } catch (Exception e) { t = 0; }
            if (U.val(n).isEmpty() || t <= 0) { U.toast(ctx, "Enter a name and an amount."); return; }
            Store.put(ctx, "d_goal", Store.obj("name", U.val(n), "target", String.valueOf(t), "saved", "0").toString());
            refresh();
        });
    }

    private void addSavings() {
        final TextInputLayout a = U.field(ctx, "Amount saved (R)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        U.dialog(ctx, "Add savings", a, "Add", () -> {
            double v;
            try { v = Double.parseDouble(U.val(a)); } catch (Exception e) { v = 0; }
            JSONObject g = goal();
            if (g == null || v <= 0) return;
            try { g.put("saved", String.valueOf(g.optDouble("saved", 0) + v)); } catch (Throwable ignored) { }
            Store.put(ctx, "d_goal", g.toString());
            refresh();
        });
    }
}
