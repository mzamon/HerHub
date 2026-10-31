package za.co.herhub;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.InputType;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONObject;

public class CirclePage extends Page {
    private static final int MAX = 5;

    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != Activity.RESULT_OK || r.getData() == null || r.getData().getData() == null || !isAdded()) return;
                try (Cursor c = requireContext().getContentResolver().query(r.getData().getData(),
                        new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
                    if (c != null && c.moveToFirst()) save(c.getString(0), c.getString(1), "");
                } catch (Throwable t) { U.toast(requireContext(), "Couldn't read that contact."); }
            });

    @Override protected String title() { return "Trusted Circle"; }

    @Override protected void build() {
        p("Up to " + MAX + " people you trust. They're the ones HerHub prepares alerts for. Nothing is sent without you tapping Send.");
        JSONArray a = Store.arr(ctx, "d_circle");
        if (a.length() == 0) {
            LinearLayout e = card();
            in(e, "No one added yet", 16, R.color.ink, true, 0);
            in(e, "Add a friend, family member or neighbour who can respond quickly.", 13, R.color.ink_soft, false, 2);
        }
        for (int i = 0; i < a.length(); i++) {
            final JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            final int idx = i;
            LinearLayout c = card();
            head(c, R.drawable.ic_profile, o.optString("name"), 17, R.color.ink, true, 0);
            String rel = o.optString("rel");
            in(c, o.optString("phone") + (rel.isEmpty() ? "" : "  ·  " + rel), 13, R.color.ink_soft, false, 2);
            LinearLayout r = U.row(c);
            U.add(c, r, 10);
            U.addW(r, U.btn(ctx, "Call", true, () -> U.dial(ctx, o.optString("phone"))), 2);
            U.addW(r, U.btn(ctx, "Message", false, () -> U.sms(ctx, o.optString("phone"), "Hi, it's me. Are you free to talk?")), 2);
            U.addW(r, U.btn(ctx, "Remove", false, () -> U.confirm(ctx, "Remove contact?", o.optString("name") + " will be removed from your circle.", "Remove", () -> {
                Store.removeAt(ctx, "d_circle", idx);
                refresh();
            })), 2);
        }
        if (a.length() < MAX) {
            btn(box, "Add a contact", true, R.drawable.ic_add, this::addDialog);
            btn(box, "Choose from phone contacts", false, R.drawable.ic_sos_contact, () -> {
                try { picker.launch(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)); }
                catch (Throwable t) { U.toast(ctx, "No contacts app available."); }
            });
        } else p("Your circle is full. Remove someone to add another.");
        if (a.length() > 0) btn(box, "Send a check-in to everyone", false, R.drawable.ic_safety_check, () -> alertCircle("Hi, just letting you know where I am."));
    }

    private void addDialog() {
        LinearLayout c = U.col(ctx);
        final TextInputLayout n = U.field(ctx, "Name");
        final TextInputLayout ph = U.field(ctx, "Phone number", InputType.TYPE_CLASS_PHONE);
        final TextInputLayout rl = U.field(ctx, "Relationship (optional)");
        c.addView(n);
        U.add(c, ph, 10);
        U.add(c, rl, 10);
        U.dialog(ctx, "Add trusted contact", c, "Save", () -> save(U.val(n), U.val(ph), U.val(rl)));
    }

    private void save(String name, String phone, String rel) {
        if (name == null || name.trim().isEmpty() || phone == null || phone.replaceAll("[^0-9+]", "").length() < 5) {
            U.toast(ctx, "Please enter a name and a valid phone number.");
            return;
        }
        Store.prepend(ctx, "d_circle", Store.obj("name", name.trim(), "phone", phone.replaceAll("[^0-9+]", ""), "rel", rel == null ? "" : rel.trim()), MAX);
        refresh();
    }
}
