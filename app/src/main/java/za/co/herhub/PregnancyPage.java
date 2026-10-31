package za.co.herhub;

import android.app.DatePickerDialog;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class PregnancyPage extends Page {
    private static final int[] WK = {4, 8, 12, 16, 20, 24, 28, 32, 36, 40};
    private static final String[] SIZE = {"a poppy seed", "a raspberry", "a lime", "an avocado", "a banana", "an ear of corn", "an eggplant", "a squash", "a honeydew melon", "a small pumpkin"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault());

    @Override protected String title() { return "Pregnancy & Fertility"; }

    @Override protected void build() {
        String lmpS = Store.get(ctx, "d_lmp", null);
        LinearLayout c = card();
        if (lmpS == null) {
            head(c, R.drawable.ic_pregnancy_mode, "Track your pregnancy", 18, R.color.ink, true, 0);
            in(c, "Enter the first day of your last period and HerHub will estimate your due date and weekly progress.", 14, R.color.ink_soft, false, 4);
            btn(c, "Set first day of last period", true, this::pick);
        } else {
            LocalDate lmp = LocalDate.parse(lmpS);
            long days = ChronoUnit.DAYS.between(lmp, LocalDate.now());
            LocalDate due = lmp.plusDays(280);
            if (days < 0 || days > 300) {
                in(c, "That date doesn't look right for a current pregnancy.", 14, R.color.danger, false, 0);
            } else {
                long wk = days / 7, d = days % 7;
                in(c, "Week " + wk + ", day " + d, 24, R.color.brand, true, 0);
                in(c, "Trimester " + (wk < 13 ? 1 : wk < 28 ? 2 : 3) + "  ·  Due " + due.format(FMT), 14, R.color.ink, false, 2);
                ProgressBar pb = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
                pb.setMax(280);
                pb.setProgress((int) Math.min(280, days));
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(U.c(ctx, R.color.brand)));
                U.add(c, pb, 12);
                long left = ChronoUnit.DAYS.between(LocalDate.now(), due);
                in(c, left >= 0 ? left + " days to go (estimate)" : "Past estimated due date. Follow your care provider's advice.", 13, R.color.ink_soft, false, 4);
                String size = SIZE[0];
                for (int i = 0; i < WK.length; i++) if (wk >= WK[i]) size = SIZE[i];
                if (wk >= 4) head(c, R.drawable.ic_baby_care, "Around now, your baby is about the size of " + size + ".", 14, R.color.ink, false, 10);
            }
            btn(c, "Change date", false, this::pick);
            btn(c, "Clear pregnancy data", false, () -> U.confirm(ctx, "Clear?", "This removes your pregnancy dates and notes.", "Clear", () -> {
                Store.remove(ctx, "d_lmp"); Store.putArr(ctx, "d_pregnotes", new JSONArray()); refresh(); }));
        }

        h("Appointments & notes");
        btn(box, "Add a note", true, R.drawable.ic_add, this::noteDialog);
        JSONArray notes = Store.arr(ctx, "d_pregnotes");
        for (int i = 0; i < notes.length(); i++) {
            final JSONObject o = notes.optJSONObject(i);
            if (o == null) continue;
            final int idx = i;
            LinearLayout n = card();
            in(n, o.optString("date"), 12, R.color.ink_soft, true, 0);
            in(n, o.optString("note"), 14, R.color.ink, false, 2);
            btn(n, "Delete", false, () -> { Store.removeAt(ctx, "d_pregnotes", idx); refresh(); });
        }

        h("Fertility");
        LinearLayout f = card();
        LocalDate[] p = PeriodPage.predict(ctx);
        DateTimeFormatter s = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault());
        if (p == null) in(f, "Log your periods to see your estimated fertile window.", 14, R.color.ink, false, 0);
        else {
            head(f, R.drawable.ic_fertility, "Estimated fertile window: " + p[2].format(s) + " – " + p[3].format(s), 15, R.color.ink, true, 0);
            in(f, "Estimated ovulation: " + p[1].format(s), 14, R.color.ink, false, 4);
            in(f, "These are estimates and not reliable for contraception.", 12, R.color.ink_soft, false, 6);
        }
        btn(f, "Open period tracker", false, R.drawable.ic_period_calendar, () -> open(new PeriodPage()));

        h("Seek urgent care if you have");
        LinearLayout w = card();
        in(w, "• Heavy bleeding or severe abdominal pain\n• Severe headache, blurred vision or sudden swelling\n• Fluid leaking or regular contractions before 37 weeks\n• Your baby moving much less than usual\n• Fever, or feeling very unwell", 14, R.color.ink, false, 0);
        btn(w, "Call emergency 112", true, R.drawable.ic_emergency_phone, () -> U.dial(ctx, "112"));
        p("HerHub gives general information only. Always follow your doctor or midwife.");
    }

    private void pick() {
        LocalDate n = LocalDate.now().minusWeeks(8);
        DatePickerDialog d = new DatePickerDialog(ctx, (v, y, m, day) -> {
            Store.put(ctx, "d_lmp", LocalDate.of(y, m + 1, day).toString());
            refresh();
        }, n.getYear(), n.getMonthValue() - 1, n.getDayOfMonth());
        d.getDatePicker().setMaxDate(System.currentTimeMillis());
        d.show();
    }

    private void noteDialog() {
        final TextInputLayout t = U.multi(ctx, "Appointment, symptom or question for your doctor", 3);
        U.dialog(ctx, "Add a note", t, "Save", () -> {
            if (U.val(t).isEmpty()) return;
            Store.prepend(ctx, "d_pregnotes", Store.obj("date", U.today(), "note", U.val(t)), 50);
            refresh();
        });
    }
}
