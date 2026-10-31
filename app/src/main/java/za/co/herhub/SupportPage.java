package za.co.herhub;

import android.widget.CheckBox;
import android.widget.LinearLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SupportPage extends Page {
    private LinearLayout feed;

    @Override protected String title() { return "Support & Community"; }

    @Override protected void build() {
        p("A kind, moderated space. Posts are reviewed before they appear.");
        LinearLayout s = card();
        in(s, "Need to talk to someone?", 17, R.color.ink, true, 0);
        btn(s, "GBV Command Centre 0800 428 428", false, R.drawable.ic_domestic_violence_support, () -> U.dial(ctx, "0800428428"));
        btn(s, "SADAG 0800 21 22 23", false, R.drawable.ic_therapy, () -> U.dial(ctx, "0800212223"));
        btn(s, "More numbers", false, R.drawable.ic_emergency_contacts, () -> open(new DirectoryPage()));

        h("Share with the community");
        LinearLayout c = card();
        if (Store.user() == null) {
            in(c, "Create a free account to post and support others.", 14, R.color.ink_soft, false, 0);
            btn(c, "Create account / sign in", true, () -> {
                Store.putB(ctx, "guest", false);
                startActivity(new android.content.Intent(ctx, AuthActivity.class));
                act().finish();
            });
        } else {
            final TextInputLayout t = U.multi(ctx, "Share a thought, question or encouragement", 3);
            c.addView(t);
            final CheckBox anon = new CheckBox(ctx);
            anon.setText("Post anonymously");
            anon.setChecked(true);
            anon.setTextColor(U.c(ctx, R.color.ink));
            U.add(c, anon, 8);
            btn(c, "Submit for review", true, () -> post(U.val(t), anon.isChecked(), t));
        }

        h("Community guidelines");
        LinearLayout g = card();
        in(g, "• Be kind and respectful.\n• Never post names, addresses, phone numbers or photos of others.\n• No unverified accusations.\n• No advertising or scams.\n• This is not an emergency service. If you're in danger, call 112 or 10111.", 14, R.color.ink, false, 0);

        h("From the community");
        feed = U.col(ctx);
        U.add(box, feed, 6);
        feed.addView(U.tv(ctx, "Loading…", 14, R.color.ink_soft, false));
        load();
    }

    private void post(String text, boolean anon, TextInputLayout field) {
        if (text.length() < 5) { U.toast(ctx, "Please write a little more."); return; }
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("text", text);
            m.put("anonymous", anon);
            m.put("status", "pending_review");
            m.put("uid", Store.user() == null ? "" : Store.user().getUid());
            m.put("createdAt", FieldValue.serverTimestamp());
            final android.content.Context app = ctx.getApplicationContext();
            FirebaseFirestore.getInstance().collection("communityPosts").add(m)
                    .addOnSuccessListener(x -> U.toast(app, "Thank you. Your post will appear once reviewed."))
                    .addOnFailureListener(x -> U.toast(app, "Couldn't post right now. Check your connection."));
            if (field.getEditText() != null) field.getEditText().setText("");
        } catch (Throwable t) { U.toast(ctx, "Couldn't post right now."); }
    }

    private void load() {
        try {
            FirebaseFirestore.getInstance().collection("communityPosts").whereEqualTo("status", "approved").limit(30).get()
                    .addOnSuccessListener(snap -> {
                        if (!isAdded() || feed == null) return;
                        feed.removeAllViews();
                        if (snap.isEmpty()) { feed.addView(U.tv(ctx, "No posts yet. Be the first to share something kind.", 14, R.color.ink_soft, false)); return; }
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            LinearLayout c = U.card(ctx);
                            c.addView(U.tv(ctx, String.valueOf(d.getString("text")), 14, R.color.ink, false));
                            U.add(feed, c, 8);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || feed == null) return;
                        feed.removeAllViews();
                        feed.addView(U.tv(ctx, "Community posts couldn't load right now.", 14, R.color.ink_soft, false));
                    });
        } catch (Throwable t) {
            feed.removeAllViews();
            feed.addView(U.tv(ctx, "Community posts are unavailable right now.", 14, R.color.ink_soft, false));
        }
    }
}
