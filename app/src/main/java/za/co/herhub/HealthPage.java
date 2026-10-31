package za.co.herhub;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HealthPage extends Page {
    static int water(Context c) { return Store.getI(c, "water_" + U.today(), 0); }

    @Override protected String title() { return "Health & Wellness"; }

    @Override protected void build() {
        LinearLayout w = card();
        int g = water(ctx);
        head(w, R.drawable.ic_water_tracker, "Water today", 17, R.color.ink, true, 0);
        in(w, g + " of 8 glasses", 24, R.color.brand, true, 4);
        ProgressBar pb = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(8);
        pb.setProgress(Math.min(8, g));
        pb.setProgressTintList(android.content.res.ColorStateList.valueOf(U.c(ctx, R.color.brand)));
        U.add(w, pb, 10);
        LinearLayout r = U.row(w);
        U.add(w, r, 10);
        U.addW(r, U.btn(ctx, "− 1", false, () -> { Store.putI(ctx, "water_" + U.today(), Math.max(0, water(ctx) - 1)); refresh(); }), 3);
        U.addW(r, U.btn(ctx, "+ 1 glass", true, () -> { Store.putI(ctx, "water_" + U.today(), water(ctx) + 1); refresh(); }), 3);

        h("Medical ID");
        LinearLayout m = card();
        JSONObject id = medId();
        String[] keys = {"blood", "allergies", "meds", "conditions", "contact"};
        String[] labels = {"Blood type", "Allergies", "Medication", "Conditions", "Emergency note"};
        boolean any = false;
        for (int i = 0; i < keys.length; i++) {
            String v = id.optString(keys[i]);
            if (!v.isEmpty()) { any = true; in(m, labels[i] + ": " + v, 14, R.color.ink, false, i == 0 ? 0 : 4); }
        }
        if (!any) in(m, "Add allergies, medication and blood type so you have them ready in an emergency. Stored only on this phone.", 14, R.color.ink_soft, false, 0);
        btn(m, any ? "Edit Medical ID" : "Set up Medical ID", true, this::editId);
        if (any) btn(m, "Copy to share with a medic", false, () -> U.copy(ctx, idText()));

        h("Appointments");
        btn(box, "Add appointment", true, R.drawable.ic_doctor_appointment, this::addAppt);
        JSONArray ap = Store.arr(ctx, "d_appts");
        if (ap.length() == 0) p("No upcoming appointments. You'll get a reminder at 9:00 on the day.");
        for (int i = 0; i < ap.length(); i++) {
            final JSONObject o = ap.optJSONObject(i);
            if (o == null) continue;
            final int idx = i;
            LinearLayout c = card();
            head(c, R.drawable.ic_calendar_event, o.optString("title"), 16, R.color.ink, true, 0);
            in(c, o.optString("date"), 13, R.color.ink_soft, false, 2);
            btn(c, "Delete", false, () -> {
                ReminderReceiver.cancel(ctx, o.optInt("id", 0));
                Store.removeAt(ctx, "d_appts", idx);
                refresh();
            });
        }

        h("Health diary");
        btn(box, "Add diary entry", true, R.drawable.ic_journal, this::addDiary);
        JSONArray d = Store.arr(ctx, "d_diary");
        for (int i = 0; i < Math.min(d.length(), 15); i++) {
            JSONObject o = d.optJSONObject(i);
            if (o == null) continue;
            final int idx = i;
            LinearLayout c = card();
            in(c, o.optString("date"), 12, R.color.ink_soft, true, 0);
            in(c, o.optString("note"), 14, R.color.ink, false, 2);
            btn(c, "Delete", false, () -> { Store.removeAt(ctx, "d_diary", idx); refresh(); });
        }
        p("General information only. For medical concerns, see a healthcare professional.");
    }

    private JSONObject medId() {
        try { return new JSONObject(Store.get(ctx, "private_medid", "{}")); } catch (Throwable t) { return new JSONObject(); }
    }
    private String idText() {
        JSONObject o = medId();
        return "Medical ID\nBlood type: " + o.optString("blood") + "\nAllergies: " + o.optString("allergies") + "\nMedication: " + o.optString("meds")
                + "\nConditions: " + o.optString("conditions") + "\nNote: " + o.optString("contact");
    }
    private void editId() {
        JSONObject o = medId();
        LinearLayout c = U.col(ctx);
        final TextInputLayout b = U.field(ctx, "Blood type"), a = U.field(ctx, "Allergies"), m = U.field(ctx, "Medication"),
                cd = U.field(ctx, "Conditions"), n = U.field(ctx, "Emergency note");
        TextInputLayout[] all = {b, a, m, cd, n};
        String[] keys = {"blood", "allergies", "meds", "conditions", "contact"};
        for (int i = 0; i < all.length; i++) {
            if (all[i].getEditText() != null) all[i].getEditText().setText(o.optString(keys[i]));
            U.add(c, all[i], i == 0 ? 0 : 10);
        }
        U.dialog(ctx, "Medical ID", c, "Save", () -> {
            Store.put(ctx, "private_medid", Store.obj("blood", U.val(b), "allergies", U.val(a), "meds", U.val(m), "conditions", U.val(cd), "contact", U.val(n)).toString());
            refresh();
        });
    }

    private void addDiary() {
        final TextInputLayout t = U.multi(ctx, "How are you feeling? Symptoms, sleep, energy…", 4);
        U.dialog(ctx, "Diary entry", t, "Save", () -> {
            if (U.val(t).isEmpty()) return;
            Store.prepend(ctx, "d_diary", Store.obj("date", U.today(), "note", U.val(t)), 100);
            refresh();
        });
    }

    private void addAppt() {
        final TextInputLayout t = U.field(ctx, "Appointment (e.g. Clinic check-up)");
        U.dialog(ctx, "New appointment", t, "Pick date", () -> {
            final String title = U.val(t).isEmpty() ? "Appointment" : U.val(t);
            LocalDate n = LocalDate.now();
            DatePickerDialog dp = new DatePickerDialog(ctx, (v, y, m, day) -> {
                final LocalDate date = LocalDate.of(y, m + 1, day);
                withNotifPermission(() -> saveAppt(title, date));
            }, n.getYear(), n.getMonthValue() - 1, n.getDayOfMonth());
            dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dp.show();
        });
    }

    private void saveAppt(String title, LocalDate date) {
        int id = 20000 + Math.abs((title + date).hashCode() % 50000);
        long when = date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (when > System.currentTimeMillis()) ReminderReceiver.scheduleAt(ctx, id, when, "Appointment today", title);
        JSONArray old = Store.arr(ctx, "d_appts");
        List<JSONObject> l = new ArrayList<>();
        l.add(Store.obj("title", title, "date", date.toString()));
        try { l.get(0).put("id", id); } catch (Throwable ignored) { }
        for (int i = 0; i < old.length(); i++) if (old.optJSONObject(i) != null) l.add(old.optJSONObject(i));
        Collections.sort(l, (x, y) -> x.optString("date").compareTo(y.optString("date")));
        JSONArray n = new JSONArray();
        for (JSONObject o : l) n.put(o);
        Store.putArr(ctx, "d_appts", n);
        refresh();
    }
}
