package za.co.herhub;

import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * On-device guidance assistant: private, instant and offline. Answers come from assets/knowledge.json
 * (116 topics on safety, GBV, legal rights, health, pregnancy, mental health, money, digital safety and the app itself).
 * Questions are matched with weighted keywords and phrases, so many different wordings reach the right answer.
 * It gives general guidance only. It is not a medical, legal or emergency service.
 */
public class AssistantPage extends Page {
    private ScrollView sv;
    private LinearLayout msgs;
    private TextInputLayout input;
    private final Handler h = new Handler(Looper.getMainLooper());

    // ------------------------------------------------------------------------------------------ knowledge base

    private static final class Topic {
        String id, title, answer, action;
        List<String> kw = new ArrayList<>();
        List<String> rel = new ArrayList<>();
        boolean priority;
    }

    private static List<Topic> TOPICS;

    private static synchronized List<Topic> topics(android.content.Context c) {
        if (TOPICS != null) return TOPICS;
        List<Topic> list = new ArrayList<>();
        try {
            InputStream in = c.getAssets().open("knowledge.json");
            byte[] b = new byte[in.available()];
            int read = 0;
            while (read < b.length) { int n = in.read(b, read, b.length - read); if (n < 0) break; read += n; }
            in.close();
            JSONArray arr = new JSONArray(new String(b, 0, read, "UTF-8"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Topic t = new Topic();
                t.id = o.getString("id");
                t.title = o.optString("title", t.id);
                t.answer = o.optString("a", "");
                t.action = o.optString("act", "");
                t.priority = o.optInt("pri", 0) == 1;
                JSONArray k = o.optJSONArray("kw");
                if (k != null) for (int j = 0; j < k.length(); j++) t.kw.add(norm(k.getString(j)));
                JSONArray r = o.optJSONArray("rel");
                if (r != null) for (int j = 0; j < r.length(); j++) t.rel.add(r.getString(j));
                list.add(t);
            }
        } catch (Throwable ignored) { }
        TOPICS = list;
        return list;
    }

    private static Topic byId(List<Topic> all, String id) {
        for (Topic t : all) if (t.id.equals(id)) return t;
        return null;
    }

    /** Lower-case, drop apostrophes, turn punctuation into spaces. */
    private static String norm(String s) {
        if (s == null) return "";
        String x = s.toLowerCase(Locale.ROOT).replace("\u2019", "").replace("'", "");
        x = x.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (x.isEmpty()) return x;
        StringBuilder sb = new StringBuilder();
        for (String w : x.split(" ")) { if (sb.length() > 0) sb.append(' '); sb.append(stem(w)); }
        return sb.toString();
    }

    /** Very light stemmer so "following", "touched" and "periods" meet "follow", "touch" and "period". */
    private static String stem(String w) {
        if (w.length() <= 4 || w.endsWith("ss")) return w;
        if (w.endsWith("ing") && w.length() > 5) return w.substring(0, w.length() - 3);
        if (w.endsWith("ed") && w.length() > 5) return w.substring(0, w.length() - 2);
        if (w.endsWith("es") && w.length() > 5) return w.substring(0, w.length() - 2);
        if (w.endsWith("s")) return w.substring(0, w.length() - 1);
        return w;
    }

    private static final java.util.Set<String> SMALLTALK = new java.util.HashSet<>(
            java.util.Arrays.asList("hello", "thanks", "bye", "howareyou", "joke"));

    private static final class Hit {
        Topic t; int score;
        Hit(Topic t, int score) { this.t = t; this.score = score; }
    }

    /** Scores every topic against the question. Phrase hits count most, single words need a whole-word (or long-stem) match. */
    private static List<Hit> rank(List<Topic> all, String question) {
        String n = " " + norm(question) + " ";
        String[] toks = n.trim().isEmpty() ? new String[0] : n.trim().split(" ");
        List<String> tokList = java.util.Arrays.asList(toks);
        List<Hit> hits = new ArrayList<>();
        for (Topic t : all) {
            int score = 0;
            for (String k : t.kw) {
                if (k.isEmpty()) continue;
                if (k.indexOf(' ') >= 0) {
                    String[] words = k.split(" ");
                    if (n.contains(" " + k + " ")) score += 4 + words.length;
                    else if (words.length >= 3) {
                        int found = 0;
                        for (String w : words) if (tokList.contains(w)) found++;
                        if (found == words.length) score += 3;
                    }
                } else {
                    for (String tok : toks) {
                        if (tok.equals(k)) { score += 3; break; }
                        if (k.length() >= 5 && tok.length() > k.length() && tok.startsWith(k)) { score += 2; break; }
                    }
                }
            }
            if (SMALLTALK.contains(t.id) && toks.length > 7) score = 0;
            if (score > 0) hits.add(new Hit(t, score));
        }
        Collections.sort(hits, (a, b) -> {
            int sa = a.score + (a.t.priority ? 2 : 0), sb = b.score + (b.t.priority ? 2 : 0);
            return sb - sa;
        });
        return hits;
    }

    private static final String[] CHIPS = {"I feel unsafe", "My period is late", "I feel anxious", "Is this message a scam?",
            "Planning a safe trip", "How do I get a protection order?", "What can you do?"};

    // ------------------------------------------------------------------------------------------ name memory

    private static final String NAME_KEY = "d_assist_name";
    private static final Pattern NAME_PAT = Pattern.compile(
            "(?i)\\b(?:my name is|my name's|call me|i am called|they call me)\\s+([\\p{L}][\\p{L}'\\-]{1,24})");
    private static final String[] NOT_NAMES = {"not", "scared", "afraid", "unsafe", "anonymous", "nobody", "none", "private", "safe", "sad", "anxious"};

    /** Saved name, else first name from the account, else null. "-" means the user asked me to forget it. */
    private String userName() {
        String n = Store.get(ctx, NAME_KEY, null);
        if ("-".equals(n)) return null;
        if (n != null && !n.isEmpty()) return n;
        try {
            com.google.firebase.auth.FirebaseUser u = Store.user();
            if (u != null && u.getDisplayName() != null && !u.getDisplayName().trim().isEmpty())
                return u.getDisplayName().trim().split("\\s+")[0];
        } catch (Throwable ignored) { }
        return null;
    }

    private String nameIntent(String q) {
        String l = q.toLowerCase(Locale.ROOT);
        if (l.contains("forget my name") || l.contains("forget me") || l.contains("delete my name")) {
            Store.put(ctx, NAME_KEY, "-");
            return "Done. I've forgotten your name. You can tell me again any time with \u201cmy name is \u2026\u201d.";
        }
        if (l.contains("what's my name") || l.contains("what is my name") || l.contains("whats my name")
                || l.contains("do you know my name") || l.contains("who am i")) {
            String n = userName();
            return n != null ? "Your name is " + n + "." : "I don't know your name yet. Tell me with \u201cmy name is \u2026\u201d.";
        }
        Matcher m = NAME_PAT.matcher(q);
        if (m.find()) {
            String w = m.group(1);
            for (String bad : NOT_NAMES) if (w.equalsIgnoreCase(bad)) return null;
            String name = w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1).toLowerCase(Locale.ROOT);
            Store.put(ctx, NAME_KEY, name);
            return "Nice to meet you, " + name + ". I'll remember your name on this phone" + (Store.user() != null ? " and in your account" : "") + ". Say \u201cforget my name\u201d any time to remove it.";
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------ screen

    @Override protected String title() { return "Safety & Wellness Assistant"; }
    @Override protected boolean scroll() { return false; }

    @Override protected void build() {
        topics(ctx);
        sv = new ScrollView(ctx);
        msgs = U.col(ctx);
        msgs.setPadding(U.dp(ctx, 14), U.dp(ctx, 4), U.dp(ctx, 14), U.dp(ctx, 8));
        sv.addView(msgs);
        box.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        String nm = userName();
        bubble("Hi" + (nm != null ? " " + nm : "") + ", I'm your HerHub guide. I give general safety and wellbeing guidance on your device, privately and offline. "
                + "I'm not a doctor, lawyer or emergency service. In danger, call 112 or 10111."
                + (nm == null ? " Tell me your name by saying \u201cmy name is \u2026\u201d and I'll remember it." : "")
                + "\n\nAsk me anything about safety, abuse and your rights, periods, pregnancy, mental health, money scams, or how to use HerHub.", false, "");
        chipRow(java.util.Arrays.asList(CHIPS), null);

        LinearLayout bar = U.row(ctx);
        bar.setPadding(U.dp(ctx, 12), U.dp(ctx, 6), U.dp(ctx, 12), U.dp(ctx, 8));
        input = U.field(ctx, "Type a message", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        bar.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView send = U.icon(ctx, R.drawable.ic_send, "Send", 52, 0);
        send.setBackground(U.gradient(ctx, R.color.brand_hot, R.color.brand_dark, 26));
        send.setColorFilter(0xFFFFFFFF);
        send.setOnClickListener(v -> { String t = U.val(input); if (!t.isEmpty()) { input.getEditText().setText(""); ask(t); } });
        U.pressable(send);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(U.dp(ctx, 52), U.dp(ctx, 52));
        slp.setMargins(U.dp(ctx, 8), 0, 0, 0);
        bar.addView(send, slp);
        box.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override public void onDestroyView() { h.removeCallbacksAndMessages(null); super.onDestroyView(); }

    // Conversation context: "" = none, "TRIP" = waiting for a destination, "SCAM" = waiting for a pasted message.
    private String state = "";
    private String lastTopic = "";

    private static final String[][] NUMBERS = {
            {"0800 428 428", "GBV Command Centre"}, {"0800 150 150", "Stop Gender Violence"}, {"0800 567 567", "SADAG crisis line"},
            {"0800 21 22 23", "SADAG"}, {"0800 12 13 14", "SADAG 24-hour line"}, {"0861 322 322", "Lifeline"},
            {"0800 055 555", "Childline"}, {"0800 222 777", "Trafficking helpline"}, {"0800 110 110", "Legal Aid"},
            {"0800 012 322", "AIDS Helpline"}, {"0861 555 777", "Poisons helpline"}, {"0800 22 66 22", "CANSA"},
            {"0800 20 3722", "FSCA"}, {"10177", "Ambulance 10177"}, {"10111", "Police 10111"}, {"112", "Emergency 112"}};

    /** True if the text looks like a pasted message (link, or long and loaded with scam signals). */
    private static boolean looksPasted(String q) {
        String l = q.toLowerCase(Locale.ROOT);
        return l.contains("http") || l.contains("www.") || l.contains("bit.ly") || q.length() >= 60;
    }

    private String scamVerdict(String text) {
        List<String> why = new ArrayList<>();
        int score = FinancePage.scan(text, why);
        String level = score >= 5 ? "High risk: this is very likely a scam."
                : score >= 3 ? "Suspicious: this has several scam warning signs."
                : score >= 1 ? "A few warning signs. Be careful."
                : "I found no obvious red flags, but no checker is perfect.";
        StringBuilder sb = new StringBuilder(level);
        for (String w : new LinkedHashSet<>(why)) sb.append("\n\u2022 ").append(w);
        sb.append(score >= 3 ? "\n\nDon't click the link, reply, or share any PIN or OTP. Call the company on a number you trust."
                : "\n\nIf something feels off, check directly with the company.");
        return sb.toString();
    }

    private void ask(String q) {
        bubble(q, true, "");
        String nr = nameIntent(q);
        if (nr != null) { say(nr, "", null); return; }
        List<Topic> all = topics(ctx);
        String nm = userName();
        String l = norm(q);

        // 1) A pasted message gets scanned, with no button needed.
        boolean pasted = "SCAM".equals(state) ? q.length() >= 12 : (looksPasted(q) && FinancePage.scan(q, new ArrayList<>()) >= 3);
        List<Hit> hits = rank(all, q);
        Hit urgent = null;
        for (Hit x : hits) if (x.t.priority && x.score + 2 >= 5 && (x.score + 2) * 10 >= (hits.get(0).score + (hits.get(0).t.priority ? 2 : 0)) * 6) { urgent = x; break; }
        boolean emergency = urgent != null;
        if (pasted && !emergency) {
            state = "";
            say(scamVerdict(q), "finance", chips("scam_after", "otp", "simswap"));
            return;
        }

        // 2) We asked where they are going.
        if ("TRIP".equals(state) && q.length() <= 60 && !q.contains("?") && (hits.isEmpty() || hits.get(0).score < 5)) {
            state = "";
            say("Got it" + (nm != null ? ", " + nm : "") + ". For your trip to " + q.trim() + ": share your live location with someone you trust, keep your phone charged, and check in when you arrive. Want to start a Safe Journey with a check-in timer?",
                    "journey", chips("walkhome", "taxi", "selfdefence"));
            return;
        }

        // 3) "more" / "what else"
        if (l.equals("more") || l.equals("tell me more") || l.equals("what else") || l.equals("anything else") || l.equals("ok") || l.equals("okay")) {
            Topic last = byId(all, lastTopic);
            if (last != null && !last.rel.isEmpty()) {
                say("Here are related topics you may find helpful.", "", chips(last.rel.toArray(new String[0])));
                return;
            }
        }

        // 4) Best matching topic from the knowledge base
        if (!hits.isEmpty() && hits.get(0).score + (hits.get(0).t.priority ? 2 : 0) >= 3) {
            Hit best = urgent != null ? urgent : hits.get(0);
            Topic t = best.t;
            state = ("walkhome".equals(t.id) || "taxi".equals(t.id) || "travel".equals(t.id) || "journey_how".equals(t.id)) ? "TRIP"
                    : "scam".equals(t.id) ? "SCAM" : "";
            lastTopic = t.id;
            String reply = t.answer;
            if ("hello".equals(t.id) && nm != null) reply = reply.replace("Hi, I'm", "Hi " + nm + ", I'm");
            if ("TRIP".equals(state) && !"journey_how".equals(t.id)) reply += "\n\nWhere are you headed? Tell me and I'll tailor a tip.";
            List<String> chipIds = new ArrayList<>(t.rel);
            // If another topic scored nearly as well, offer it too.
            if (hits.size() > 1 && hits.get(1).score * 10 >= best.score * 6 && !chipIds.contains(hits.get(1).t.id) && !hits.get(1).t.id.equals(t.id))
                chipIds.add(0, hits.get(1).t.id);
            say(reply, t.action, chips(chipIds.toArray(new String[0])));
            return;
        }

        // 5) Didn't understand: guide the person instead of dead-ending
        state = "";
        List<String> ids = new ArrayList<>();
        if (!hits.isEmpty()) for (int i = 0; i < Math.min(3, hits.size()); i++) ids.add(hits.get(i).t.id);
        if (ids.isEmpty()) { ids.add("about_ai"); ids.add("danger"); ids.add("gbvhelp"); ids.add("anxiety"); }
        say((nm != null ? nm + ", I" : "I") + " didn't quite catch that. Try different words, or pick a topic below. I can help with safety, abuse and your rights, periods, pregnancy, mental health, scams and HerHub itself. For urgent help call 112, or the GBV line 0800 428 428.",
                "", chips(ids.toArray(new String[0])));
    }

    // ------------------------------------------------------------------------------------------ output

    private List<Chip> chips(String... ids) {
        List<Topic> all = topics(ctx);
        List<Chip> out = new ArrayList<>();
        int n = 0;
        for (String id : ids) {
            Topic t = byId(all, id);
            if (t == null || n >= 4) continue;
            final String label = t.title;
            Chip c = makeChip(label);
            c.setOnClickListener(v -> ask(label));
            out.add(c);
            n++;
        }
        return out;
    }

    private Chip makeChip(String label) {
        Chip c = new Chip(ctx);
        c.setText(label);
        c.setTextColor(U.c(ctx, R.color.brand_dark));
        c.setTextSize(13);
        c.setChipBackgroundColor(ColorStateList.valueOf(U.c(ctx, R.color.surface)));
        c.setChipStrokeColor(ColorStateList.valueOf(U.c(ctx, R.color.brand)));
        c.setChipStrokeWidth(U.dp(ctx, 1));
        c.setRippleColor(ColorStateList.valueOf(0x22D92F78));
        c.setCheckable(false);
        return c;
    }

    private void chipRow(List<String> labels, List<Chip> ready) {
        ChipGroup g = new ChipGroup(ctx);
        g.setChipSpacingVertical(U.dp(ctx, 2));
        if (ready != null) for (Chip c : ready) g.addView(c);
        else for (final String s : labels) {
            Chip c = makeChip(s);
            c.setOnClickListener(v -> ask(s));
            g.addView(c);
        }
        U.add(msgs, g, 8);
        scrollDown();
    }

    /** Shows a short typing indicator, then the reply (and suggestion chips). */
    private void say(final String text, final String action, final List<Chip> chipList) {
        final View typing = typingBubble();
        h.postDelayed(() -> {
            if (!isAdded() || msgs == null) return;
            msgs.removeView(typing);
            bubble(text, false, action);
            if (chipList != null && !chipList.isEmpty()) chipRow(null, chipList);
        }, 480);
    }

    private View typingBubble() {
        LinearLayout row = U.row(ctx);
        row.addView(avatar(), new LinearLayout.LayoutParams(U.dp(ctx, 34), U.dp(ctx, 34)));
        TextView t = U.tv(ctx, "Typing\u2026", 13, R.color.ink_soft, false);
        t.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 18));
        t.setPadding(U.dp(ctx, 14), U.dp(ctx, 9), U.dp(ctx, 14), U.dp(ctx, 9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = U.dp(ctx, 8);
        row.addView(t, lp);
        U.add(msgs, row, 10);
        scrollDown();
        return row;
    }

    private ImageView avatar() { return U.tile(ctx, R.drawable.ic_ai_assistant, "HerHub guide", 34); }

    private void scrollDown() { if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN)); }

    private void bubble(String text, boolean mine, final String action) {
        LinearLayout row = U.row(ctx);
        row.setGravity(mine ? Gravity.END : Gravity.START);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout wrap = U.col(ctx);
        wrap.setGravity(mine ? Gravity.END : Gravity.START);
        TextView t = U.tv(ctx, text, 15, mine ? R.color.white : R.color.ink, false);
        t.setLineSpacing(0f, 1.12f);
        t.setTextIsSelectable(false);
        if (mine) t.setBackground(U.gradient(ctx, R.color.brand_hot, R.color.brand_dark, 18));
        else t.setBackground(U.bg(ctx, R.color.surface, R.color.outline, 18));
        t.setPadding(U.dp(ctx, 14), U.dp(ctx, 10), U.dp(ctx, 14), U.dp(ctx, 10));
        t.setMaxWidth(U.dp(ctx, 290));
        wrap.addView(t);
        if (!mine) {
            // Tap-to-call buttons for any helpline mentioned in the reply.
            int shown = 0;
            String rest = text;
            for (final String[] n : NUMBERS) {
                if (shown >= 3 || !rest.contains(n[0])) continue;
                rest = rest.replace(n[0], "");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = U.dp(ctx, 6);
                wrap.addView(U.btn(ctx, "Call " + n[1], false, R.drawable.ic_phone_call, () -> U.dial(ctx, n[0].replace(" ", ""))), lp);
                shown++;
            }
            if (!action.isEmpty()) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = U.dp(ctx, 6);
                wrap.addView(U.btn(ctx, "Open " + Features.title(action), true, Features.icon(action), () -> Features.go(act(), action)), lp);
            }
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(U.dp(ctx, 34), U.dp(ctx, 34));
            alp.rightMargin = U.dp(ctx, 8);
            alp.gravity = Gravity.TOP;
            row.addView(avatar(), alp);
        }
        row.addView(wrap, new LinearLayout.LayoutParams(mine ? ViewGroup.LayoutParams.MATCH_PARENT : 0, ViewGroup.LayoutParams.WRAP_CONTENT, mine ? 0f : 1f));
        U.add(msgs, row, 10);
        scrollDown();
    }
}
