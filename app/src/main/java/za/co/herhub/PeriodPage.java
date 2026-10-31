package za.co.herhub;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.LinearLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class PeriodPage extends Page {
    static final String[] FLOW = {"Spotting", "Light", "Medium", "Heavy"};
    static final String[] SYMPTOMS = {"Cramps", "Headache", "Bloating", "Fatigue", "Mood swings", "Back pain", "Nausea", "Acne", "Cravings", "Tender breasts"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault());

    /** {nextPeriod, ovulation, fertileStart, fertileEnd, avgCycle(as LocalDate unused)}; null if no data. */
    static LocalDate[] predict(Context c) {
        JSONArray a = Store.arr(c, "d_periods");
        if (a.length() == 0) return null;
        try {
            LocalDate last = LocalDate.parse(a.getJSONObject(0).getString("start"));
            return forecast(last, avgCycle(a));
        } catch (Throwable t) { return null; }
    }
    static int avgCycle(JSONArray a) {
        try {
            int sum = 0, n = 0;
            for (int i = 0; i + 1 < a.length() && n < 6; i++) {
                LocalDate x = LocalDate.parse(a.getJSONObject(i).getString("start"));
                LocalDate y = LocalDate.parse(a.getJSONObject(i + 1).getString("start"));
                long d = ChronoUnit.DAYS.between(y, x);
                if (d >= 15 && d <= 60) { sum += d; n++; }
            }
            int avg = n == 0 ? 28 : Math.round((float) sum / n);
            return Math.max(21, Math.min(45, avg));
        } catch (Throwable t) { return 28; }
    }
    private static LocalDate[] forecast(LocalDate last, int cycle) {
        LocalDate next = last.plusDays(cycle);
        while (next.isBefore(LocalDate.now())) next = next.plusDays(cycle);
        LocalDate ov = next.minusDays(14);
        return new LocalDate[]{next, ov, ov.minusDays(5), ov.plusDays(1)};
    }
    static String summary(Context c) {
        LocalDate[] p = predict(c);
        if (p == null) return "Log your first period to see predictions";
        long d = ChronoUnit.DAYS.between(LocalDate.now(), p[0]);
        return d == 0 ? "Period expected today" : "Next period in " + d + (d == 1 ? " day" : " days");
    }

    @Override protected String title() { return "Period Tracking"; }

    @Override protected void build() {
        final JSONArray periods = Store.arr(ctx, "d_periods");
        LinearLayout c = card();
        LocalDate[] p = predict(ctx);
        if (p == null) {
            head(c, R.drawable.ic_cycle_log, "Welcome", 18, R.color.ink, true, 0);
            in(c, "Log when your period starts. After a couple of cycles your predictions get more accurate.", 14, R.color.ink_soft, false, 4);
        } else {
            in(c, summary(ctx), 20, R.color.brand, true, 0);
            in(c, "Expected " + p[0].format(FMT), 14, R.color.ink, false, 2);
            head(c, R.drawable.ic_ovulation, "Ovulation about " + p[1].format(FMT), 14, R.color.ink, false, 8);
            head(c, R.drawable.ic_fertility, "Fertile window " + p[2].format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())) + " – " + p[3].format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())), 14, R.color.ink, false, 4);
            in(c, "Average cycle: " + avgCycle(periods) + " days. Predictions are estimates, not medical advice or contraception.", 12, R.color.ink_soft, false, 8);
        }
        btn(c, "My period started today", true, R.drawable.ic_cycle_log, () -> addStart(LocalDate.now()));
        btn(c, "Log a different start date", false, R.drawable.ic_cycle_prediction, this::pickDate);

        h("Today's log");
        LinearLayout l = card();
        in(l, "How are you feeling today?", 15, R.color.ink, true, 0);
        btn(l, "Add flow, symptoms & notes", true, R.drawable.ic_symptom_journal, this::logDialog);
        JSONArray log = Store.arr(ctx, "d_plog");
        for (int i = 0; i < Math.min(log.length(), 7); i++) {
            JSONObject o = log.optJSONObject(i);
            if (o == null) continue;
            final int idx = i;
            LinearLayout r = U.row(l);
            U.add(l, r, 10);
            LinearLayout t = U.col(ctx);
            t.addView(U.tv(ctx, o.optString("date") + "  ·  " + o.optString("flow"), 14, R.color.ink, true));
            String s = o.optString("symptoms"), n = o.optString("note");
            if (!s.isEmpty()) t.addView(U.tv(ctx, s, 13, R.color.ink_soft, false));
            if (!n.isEmpty()) t.addView(U.tv(ctx, n, 13, R.color.ink_soft, false));
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
            android.widget.ImageView del = U.icon(ctx, R.drawable.ic_close, "Delete", 36, 0);
            del.setOnClickListener(v -> { Store.removeAt(ctx, "d_plog", idx); refresh(); });
            r.addView(del, new LinearLayout.LayoutParams(U.dp(ctx, 36), U.dp(ctx, 36)));
        }

        h("Cycle history");
        if (periods.length() == 0) p("No periods logged yet.");
        for (int i = 0; i < periods.length(); i++) {
            JSONObject o = periods.optJSONObject(i);
            if (o == null) continue;
            final int idx = i;
            LinearLayout r = U.row(ctx);
            r.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 14));
            r.setPadding(U.dp(ctx, 14), U.dp(ctx, 10), U.dp(ctx, 8), U.dp(ctx, 10));
            String shown;
            try { shown = LocalDate.parse(o.optString("start")).format(FMT); } catch (Throwable t) { shown = o.optString("start"); }
            r.addView(U.icon(ctx, R.drawable.ic_period_calendar, "Period start", 30, 0), new LinearLayout.LayoutParams(U.dp(ctx, 30), U.dp(ctx, 30)));
            r.addView(U.tv(ctx, "  " + shown, 14, R.color.ink, false), new LinearLayout.LayoutParams(0, -2, 1f));
            android.widget.ImageView del = U.icon(ctx, R.drawable.ic_close, "Delete", 36, 0);
            del.setOnClickListener(v -> U.confirm(ctx, "Delete this entry?", "This removes it from your history and predictions.", "Delete", () -> { Store.removeAt(ctx, "d_periods", idx); refresh(); }));
            r.addView(del, new LinearLayout.LayoutParams(U.dp(ctx, 36), U.dp(ctx, 36)));
            U.add(box, r, 8);
        }
        item(R.drawable.ic_pregnancy, "Pregnancy & Fertility", "Due date and fertile window", () -> open(new PregnancyPage()));
        p("If your periods are very irregular, very painful, or suddenly change, talk to a healthcare professional.");
    }

    private void pickDate() {
        LocalDate n = LocalDate.now();
        DatePickerDialog d = new DatePickerDialog(ctx, (v, y, m, day) -> addStart(LocalDate.of(y, m + 1, day)), n.getYear(), n.getMonthValue() - 1, n.getDayOfMonth());
        d.getDatePicker().setMaxDate(System.currentTimeMillis());
        d.show();
    }

    private void addStart(LocalDate date) {
        JSONArray old = Store.arr(ctx, "d_periods");
        java.util.List<String> dates = new java.util.ArrayList<>();
        dates.add(date.toString());
        for (int i = 0; i < old.length(); i++) {
            String s = old.optJSONObject(i) == null ? null : old.optJSONObject(i).optString("start");
            if (s != null && !dates.contains(s)) dates.add(s);
        }
        java.util.Collections.sort(dates, java.util.Collections.reverseOrder());
        JSONArray n = new JSONArray();
        for (int i = 0; i < dates.size() && i < 36; i++) n.put(Store.obj("start", dates.get(i)));
        Store.putArr(ctx, "d_periods", n);
        refresh();
    }

    private void logDialog() {
        LinearLayout c = U.col(ctx);
        final int[] flow = {-1};
        final boolean[] sel = new boolean[SYMPTOMS.length];
        c.addView(U.tv(ctx, "Flow", 14, R.color.ink, true));
        LinearLayout r = U.row(ctx);
        U.add(c, r, 6);
        final com.google.android.material.button.MaterialButton[] bs = new com.google.android.material.button.MaterialButton[FLOW.length];
        for (int i = 0; i < FLOW.length; i++) {
            final int ix = i;
            bs[i] = U.btn(ctx, FLOW[i], false, () -> {
                flow[0] = ix;
                for (int k = 0; k < bs.length; k++) bs[k].setBackgroundTintList(android.content.res.ColorStateList.valueOf(k == ix ? U.c(ctx, R.color.brand_soft) : android.graphics.Color.TRANSPARENT));
            });
            bs[i].setTextSize(11);
            bs[i].setInsetTop(0); bs[i].setInsetBottom(0);
            bs[i].setPadding(U.dp(ctx, 2), 0, U.dp(ctx, 2), 0);
            U.addW(r, bs[i], 1);
        }
        c.addView(U.tv(ctx, "Symptoms", 14, R.color.ink, true));
        final android.widget.TextView symTxt = U.tv(ctx, "None selected — tap to choose", 14, R.color.brand, false);
        symTxt.setPadding(0, U.dp(ctx, 8), 0, U.dp(ctx, 8));
        symTxt.setOnClickListener(v -> new MaterialAlertDialogBuilder(ctx).setTitle("Symptoms")
                .setMultiChoiceItems(SYMPTOMS, sel, (d, w, ch) -> sel[w] = ch)
                .setPositiveButton("Done", (d, w) -> symTxt.setText(join(sel))).show());
        c.addView(symTxt);
        final TextInputLayout note = U.multi(ctx, "Notes (optional)", 2);
        c.addView(note);
        U.dialog(ctx, "Log for today", c, "Save", () -> {
            String f = flow[0] < 0 ? "No flow noted" : FLOW[flow[0]];
            Store.prepend(ctx, "d_plog", Store.obj("date", U.today(), "flow", f, "symptoms", join(sel).startsWith("None") ? "" : join(sel), "note", U.val(note)), 60);
            refresh();
        });
    }

    private String join(boolean[] sel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sel.length; i++) if (sel[i]) { if (sb.length() > 0) sb.append(", "); sb.append(SYMPTOMS[i]); }
        return sb.length() == 0 ? "None selected — tap to choose" : sb.toString();
    }
}
