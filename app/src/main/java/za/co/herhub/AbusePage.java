package za.co.herhub;

import android.widget.CheckBox;
import android.widget.LinearLayout;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONObject;

public class AbusePage extends Page {
    private static final String[] PLAN = {
            "I know a safe place I can go (friend, family, shelter)",
            "I have a bag ready: ID, cash, medication, keys, charger, children's documents",
            "A trusted person knows my situation",
            "I have a code word to ask friends for help",
            "I know the exits and safest rooms in my home",
            "I have copies of important documents stored safely elsewhere",
            "I've memorised key emergency numbers",
            "I know how to apply for a protection order"};

    @Override protected String title() { return "Abuse & Harassment Support"; }

    @Override protected void build() {
        LinearLayout q = card();
        q.setBackground(U.bg(ctx, R.color.brand_soft, R.color.danger, 18));
        in(q, "Need to leave this screen quickly?", 16, R.color.danger, true, 0);
        btn(q, "Quick exit", true, R.drawable.ic_emergency_exit, () -> act().finishAndRemoveTask());
        in(q, "Closes HerHub and removes it from recent apps. Note: a phone may still show other activity, so use a safe device if you think you're being monitored.", 12, R.color.ink_soft, false, 8);

        h("Get help now");
        LinearLayout h1 = card();
        btn(h1, "GBV Command Centre 0800 428 428", true, () -> U.dial(ctx, "0800428428"));
        btn(h1, "Police 10111", false, () -> U.dial(ctx, "10111"));
        btn(h1, "Emergency 112", false, () -> U.dial(ctx, "112"));
        btn(h1, "Alert my trusted circle", false, R.drawable.ic_sos_contact, () -> alertCircle("I need help. Please contact me when you can."));

        h("Recognising abuse");
        LinearLayout t = card();
        in(t, "Abuse isn't only physical. It can be controlling your money, phone or movements, constant put-downs, threats, jealousy and isolation, or forcing sex. It's never your fault, and you deserve to be safe.", 14, R.color.ink, false, 0);

        h("My safety plan");
        LinearLayout p = card();
        int done = 0;
        for (int i = 0; i < PLAN.length; i++) if (Store.getB(ctx, "private_plan_" + i, false)) done++;
        in(p, done + " of " + PLAN.length + " steps done. Saved privately on this phone only.", 13, R.color.ink_soft, false, 0);
        for (int i = 0; i < PLAN.length; i++) {
            final int ix = i;
            CheckBox cb = new CheckBox(ctx);
            cb.setText(PLAN[i]);
            cb.setTextColor(U.c(ctx, R.color.ink));
            cb.setChecked(Store.getB(ctx, "private_plan_" + i, false));
            cb.setOnCheckedChangeListener((v, on) -> Store.putB(ctx, "private_plan_" + ix, on));
            U.add(p, cb, 4);
        }

        h("Private incident log");
        p("Notes with date and time can help if you decide to report. Stored only on this phone, never uploaded.");
        btn(box, "Add an entry", true, R.drawable.ic_add, this::addEntry);
        JSONArray log = Store.arr(ctx, "private_log");
        for (int i = 0; i < log.length(); i++) {
            JSONObject o = log.optJSONObject(i);
            if (o == null) continue;
            final int idx = i;
            LinearLayout c = card();
            in(c, o.optString("when"), 12, R.color.ink_soft, true, 0);
            in(c, o.optString("note"), 14, R.color.ink, false, 2);
            btn(c, "Delete", false, () -> U.confirm(ctx, "Delete entry?", "This can't be undone.", "Delete", () -> { Store.removeAt(ctx, "private_log", idx); refresh(); }));
        }
        if (log.length() > 0) btn(box, "Copy all entries", false, R.drawable.ic_copy, () -> {
            StringBuilder sb = new StringBuilder();
            JSONArray l = Store.arr(ctx, "private_log");
            for (int i = 0; i < l.length(); i++) { JSONObject o = l.optJSONObject(i); if (o != null) sb.append(o.optString("when")).append(": ").append(o.optString("note")).append("\n\n"); }
            U.copy(ctx, sb.toString());
        });

        h("Online & workplace harassment");
        LinearLayout o = card();
        in(o, "• Screenshot messages with dates; don't delete them.\n• Block and report the account on the platform.\n• You can apply for a protection order under the Protection from Harassment Act.\n• For work harassment, report in writing to your employer.\n• Talk to someone you trust.", 14, R.color.ink, false, 0);
        item(R.drawable.ic_legal_aid, "Know your rights", "Protection orders and reporting", () -> open(new InfoPage()));
    }

    private void addEntry() {
        final TextInputLayout n = U.multi(ctx, "What happened? Where, when, who saw it?", 4);
        U.dialog(ctx, "New private entry", n, "Save", () -> {
            if (U.val(n).isEmpty()) return;
            String when = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"));
            Store.prepend(ctx, "private_log", Store.obj("when", when, "note", U.val(n)), 200);
            refresh();
        });
    }
}
