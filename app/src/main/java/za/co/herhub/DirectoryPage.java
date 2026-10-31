package za.co.herhub;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.LinearLayout;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Locale;

public class DirectoryPage extends Page {
    // {category, name, number, description}
    static final String[][] DIR = {
            {"Emergency", "Emergency (any mobile network)", "112", "Police, ambulance, fire"},
            {"Emergency", "Police (SAPS Flying Squad)", "10111", "Report a crime in progress"},
            {"Emergency", "Ambulance / medical emergency", "10177", "Public ambulance service"},
            {"Emergency", "Crime Stop", "08600 10111", "Anonymous crime tip-offs"},
            {"Abuse & violence", "GBV Command Centre (24/7, free)", "0800 428 428", "Gender-based violence help and counselling"},
            {"Abuse & violence", "Stop Gender Violence Helpline", "0800 150 150", "Support and referrals"},
            {"Abuse & violence", "TEARS Foundation (USSD)", "*134*7355#", "Free 'Please call me' for abuse and rape support"},
            {"Abuse & violence", "Childline South Africa", "0800 055 555", "Child abuse and safety"},
            {"Abuse & violence", "Human Trafficking Helpline", "0800 222 777", "Report or get help"},
            {"Mental health", "SADAG", "0800 21 22 23", "Counselling and mental-health support"},
            {"Mental health", "SADAG Suicide Crisis Line", "0800 567 567", "If you're thinking of ending your life"},
            {"Mental health", "SADAG 24-hour helpline", "0800 12 13 14", "Round-the-clock listening"},
            {"Mental health", "Lifeline Southern Africa", "0861 322 322", "Emotional support"},
            {"Health", "National AIDS Helpline", "0800 012 322", "HIV information and referrals"},
            {"Health", "Poisons Information Helpline", "0861 555 777", "Poisoning emergencies"},
            {"Legal & money", "Legal Aid South Africa", "0800 110 110", "Free legal help"},
            {"Legal & money", "FSCA consumer line", "0800 20 3722", "Report financial scams"},
            {"Legal & money", "Ombudsman for Banking Services", "0860 800 900", "Bank complaints"}
    };
    private LinearLayout list;

    @Override protected String title() { return "Emergency Resource Directory"; }

    @Override protected void build() {
        p("Tap a number to open your phone dialler. Numbers are for South Africa. Please verify, as numbers can change.");
        TextInputLayout f = U.field(ctx, "Search (e.g. abuse, police, bank)", InputType.TYPE_CLASS_TEXT);
        U.add(box, f, 10);
        list = U.col(ctx);
        U.add(box, list, 6);
        fill("");
        f.getEditText().addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) { fill(e.toString()); }
        });
    }

    private void fill(String q) {
        list.removeAllViews();
        String s = q.toLowerCase(Locale.ROOT).trim();
        String cat = "";
        int shown = 0;
        for (final String[] d : DIR) {
            if (!s.isEmpty() && !(d[0] + " " + d[1] + " " + d[2] + " " + d[3]).toLowerCase(Locale.ROOT).contains(s)) continue;
            shown++;
            if (!d[0].equals(cat)) { cat = d[0]; U.add(list, U.tv(ctx, cat, 15, R.color.brand, true), 14); }
            LinearLayout r = U.row(ctx);
            r.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 16));
            r.setPadding(U.dp(ctx, 14), U.dp(ctx, 12), U.dp(ctx, 8), U.dp(ctx, 12));
            LinearLayout t = U.col(ctx);
            t.addView(U.tv(ctx, d[1], 15, R.color.ink, true));
            t.addView(U.tv(ctx, d[3], 12, R.color.ink_soft, false));
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
            r.addView(U.btn(ctx, d[2], true, () -> U.dial(ctx, d[2])));
            U.add(list, r, 8);
        }
        if (shown == 0) U.add(list, U.tv(ctx, "No matches. Try another word.", 14, R.color.ink_soft, false), 12);
    }
}
