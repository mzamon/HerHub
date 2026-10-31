package za.co.herhub;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/** Login and registration. Both screens share one design, one Google button and the guest option. */
public class AuthActivity extends AppCompatActivity {
    private static final String PREFS = "herhub_auth_ui";

    private boolean register = false;
    private LinearLayout form;
    private TextView status;
    private ProgressBar progress;
    private MaterialButton primary, googleBtn;
    private TextInputLayout name, email, pass, pass2;
    private CheckBox remember;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundResource(R.drawable.bg_page_glow);

        // Botanical corner flourish, as in the prototype
        ImageView leaf = new ImageView(this);
        leaf.setImageResource(R.drawable.ic_leaf_flourish);
        leaf.setAlpha(0.55f);
        leaf.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams leafLp = new FrameLayout.LayoutParams(U.dp(this, 190), U.dp(this, 190), Gravity.BOTTOM | Gravity.END);
        root.addView(leaf, leafLp);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setClipToPadding(false);
        root.addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout col = U.col(this);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(U.dp(this, 22), U.dp(this, 22), U.dp(this, 22), U.dp(this, 28));
        sv.addView(col);

        // Brand block: the icon already carries the HerHub wordmark, so no second "HerHub" title here.
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.herhub_logo);
        logo.setContentDescription("HerHub logo");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(U.bg(this, R.color.white, 0, 30));
        logo.setClipToOutline(true);
        logo.setElevation(U.dp(this, 6));
        col.addView(logo, new LinearLayout.LayoutParams(U.dp(this, 112), U.dp(this, 112)));
        logo.setScaleX(.6f);
        logo.setScaleY(.6f);
        logo.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.3f)).start();

        TextView tag = U.tv(this, "YOUR SPACE. YOUR STRENGTH.", 12, R.color.brand_dark, true);
        tag.setLetterSpacing(0.14f);
        tag.setGravity(Gravity.CENTER);
        U.add(col, tag, 14);

        form = U.col(this);
        form.setBackground(U.bg(this, R.color.surface, R.color.outline, 28));
        form.setElevation(U.dp(this, 5));
        form.setPadding(U.dp(this, 20), U.dp(this, 22), U.dp(this, 20), U.dp(this, 18));
        U.add(col, form, 20);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        col.addView(progress);

        setContentView(root);
        U.edge(this, root, null);
        render();
        U.rise(form, 1);
    }

    private TextInputLayout iconField(String hint, int type, int iconRes) {
        TextInputLayout t = U.field(this, hint, type);
        t.setStartIconDrawable(iconRes);
        t.setStartIconTintList(null);
        t.setStartIconContentDescription(hint);
        return t;
    }

    private void render() {
        form.removeAllViews();

        TextView t = U.tv(this, register ? "Create your account" : "Welcome back", 26, R.color.ink, true);
        t.setGravity(Gravity.START);
        U.add(form, t, 0);
        TextView subtitle = U.tv(this, register
                ? "Your personal safe space starts here."
                : "Glad to see you again. Log in to continue your journey.", 14, R.color.ink_soft, false);
        subtitle.setLineSpacing(0f, 1.15f);
        U.add(form, subtitle, 4);

        status = U.tv(this, "", 13, R.color.danger, false);
        status.setPadding(U.dp(this, 2), 0, U.dp(this, 2), 0);
        U.add(form, status, 8);

        if (register) { name = iconField("Your name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, R.drawable.ic_profile); U.add(form, name, 10); }
        email = iconField("Email address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, R.drawable.ic_email);
        U.add(form, email, register ? 12 : 10);
        pass = iconField("Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, R.drawable.ic_password_lock);
        pass.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        pass.setEndIconTintList(ColorStateList.valueOf(U.c(this, R.color.icon_primary)));
        U.add(form, pass, 12);
        if (register) {
            pass2 = iconField("Confirm password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, R.drawable.ic_password_lock);
            pass2.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            pass2.setEndIconTintList(ColorStateList.valueOf(U.c(this, R.color.icon_primary)));
            U.add(form, pass2, 12);
        } else {
            LinearLayout opts = U.row(this);
            remember = new CheckBox(this);
            remember.setText("Remember me");
            remember.setTextColor(U.c(this, R.color.ink_soft));
            remember.setTextSize(13);
            remember.setButtonTintList(ColorStateList.valueOf(U.c(this, R.color.brand)));
            opts.addView(remember, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView forgot = U.tv(this, "Forgot password?", 13, R.color.brand, true);
            forgot.setPadding(U.dp(this, 8), U.dp(this, 12), U.dp(this, 4), U.dp(this, 12));
            forgot.setOnClickListener(v -> forgot());
            opts.addView(forgot);
            U.add(form, opts, 4);
            String saved = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("email", "");
            if (saved != null && !saved.isEmpty() && email.getEditText() != null) {
                email.getEditText().setText(saved);
                remember.setChecked(true);
            }
        }

        primary = U.btn(this, register ? "Create my account" : "Log In", true, this::submit);
        primary.setMinHeight(U.dp(this, 54));
        primary.setCornerRadius(U.dp(this, 27));
        U.add(form, primary, register ? 20 : 12);

        // "or" divider
        LinearLayout or = U.row(this);
        or.setGravity(Gravity.CENTER_VERTICAL);
        View l1 = new View(this), l2 = new View(this);
        l1.setBackgroundColor(U.c(this, R.color.outline));
        l2.setBackgroundColor(U.c(this, R.color.outline));
        or.addView(l1, new LinearLayout.LayoutParams(0, U.dp(this, 1), 1f));
        TextView ot = U.tv(this, "or", 13, R.color.ink_soft, false);
        ot.setPadding(U.dp(this, 14), 0, U.dp(this, 14), 0);
        or.addView(ot);
        or.addView(l2, new LinearLayout.LayoutParams(0, U.dp(this, 1), 1f));
        U.add(form, or, 16);

        // Same Google button on login and registration
        googleBtn = U.google(this, "Continue with Google", this::google);
        U.add(form, googleBtn, 16);

        LinearLayout switchRow = U.row(this);
        switchRow.setGravity(Gravity.CENTER);
        switchRow.addView(U.tv(this, register ? "Already part of HerHub?" : "Don't have an account?", 13, R.color.ink_soft, false));
        TextView switchAction = U.tv(this, register ? " Log in" : " Sign up", 13, R.color.brand, true);
        switchAction.setPadding(U.dp(this, 4), U.dp(this, 12), U.dp(this, 4), U.dp(this, 12));
        switchAction.setOnClickListener(v -> { register = !register; render(); U.rise(form, 0); });
        switchRow.addView(switchAction);
        U.add(form, switchRow, 12);

        MaterialButton guest = U.btn(this, "Continue as a guest", false, R.drawable.ic_anon_user, () -> {
            Store.putB(this, "guest", true);
            goMain();
        });
        U.add(form, guest, 4);

        TextView n = U.tv(this, "Your safety tools work without an account. Sign in to back up your data securely.", 12, R.color.ink_soft, false);
        n.setGravity(Gravity.CENTER);
        n.setLineSpacing(0f, 1.15f);
        U.add(form, n, 10);
    }

    private void busy(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        if (primary != null) primary.setEnabled(!b);
        if (googleBtn != null) googleBtn.setEnabled(!b);
    }
    private void err(String s) { busy(false); status.setTextColor(U.c(this, R.color.danger)); status.setText(s); }
    private void okMsg(String s) { busy(false); status.setTextColor(U.c(this, R.color.ok)); status.setText(s); }

    private FirebaseAuth auth() {
        try { return FirebaseAuth.getInstance(); } catch (Throwable t) { return null; }
    }

    // ---------------------------------------------------------------------------------------------- Email / password

    private void submit() {
        final String em = U.val(email), pw = pass.getEditText() == null ? "" : pass.getEditText().getText().toString();
        final String nm = register ? U.val(name) : "";
        if (register && nm.isEmpty()) { err("Please enter your name."); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(em).matches()) { err("Please enter a valid email address."); return; }
        if (pw.length() < 6) { err("Your password must be at least 6 characters."); return; }
        if (register) {
            String p2 = pass2.getEditText() == null ? "" : pass2.getEditText().getText().toString();
            if (!pw.equals(p2)) { err("The two passwords don't match."); return; }
        }
        FirebaseAuth a = auth();
        if (a == null) { err("Sign-in isn't available right now. You can continue without an account."); return; }
        busy(true);
        status.setText("");
        try {
            if (register) {
                a.createUserWithEmailAndPassword(em, pw).addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (task.isSuccessful()) {
                        FirebaseUser u = null;
                        try { u = task.getResult() != null ? task.getResult().getUser() : null; } catch (Throwable ignored) { }
                        if (u == null) u = Store.user();
                        finishRegister(u, nm, em);
                    } else err(explain(task.getException(), true));
                });
            } else {
                a.signInWithEmailAndPassword(em, pw).addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (task.isSuccessful()) { rememberEmail(em); goMain(); } else err(explain(task.getException(), false));
                });
            }
        } catch (Throwable t) { err(explain(t, register)); }
    }

    private void rememberEmail(String em) {
        try {
            android.content.SharedPreferences.Editor e = getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            if (remember != null && remember.isChecked()) e.putString("email", em); else e.remove("email");
            e.apply();
        } catch (Throwable ignored) { }
    }

    private void finishRegister(FirebaseUser u, String nm, String em) {
        if (u == null) { goMain(); return; }
        saveProfile(u, nm, em);
        try {
            u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(nm).build())
                    .addOnCompleteListener(t -> { if (!isFinishing() && !isDestroyed()) goMain(); });
        } catch (Throwable t) { goMain(); }
    }

    private void saveProfile(FirebaseUser u, String nm, String em) {
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("name", nm);
            m.put("email", em);
            m.put("createdAt", FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection("users").document(u.getUid()).set(m);
        } catch (Throwable ignored) { }
    }

    private void forgot() {
        String em = U.val(email);
        if (!Patterns.EMAIL_ADDRESS.matcher(em).matches()) { err("Enter your email above, then tap \u201cForgot password?\u201d."); return; }
        FirebaseAuth a = auth();
        if (a == null) { err("Password reset isn't available right now."); return; }
        busy(true);
        try {
            a.sendPasswordResetEmail(em).addOnCompleteListener(task -> {
                if (isFinishing() || isDestroyed()) return;
                if (task.isSuccessful()) okMsg("If an account exists for that email, a reset link is on its way.");
                else err(explain(task.getException(), false));
            });
        } catch (Throwable t) { err(explain(t, false)); }
    }

    // ---------------------------------------------------------------------------------------------- Google

    /** The web client id is generated into resources by the Google Services plugin from google-services.json. */
    private String webClientId() {
        try {
            int id = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
            if (id == 0) return null;
            String s = getString(id);
            return s == null || s.trim().isEmpty() ? null : s.trim();
        } catch (Throwable t) { return null; }
    }

    private void google() {
        final FirebaseAuth a = auth();
        final String clientId = webClientId();
        if (a == null || clientId == null) {
            err("Google sign-in isn't set up yet. In Firebase, enable the Google provider, add your SHA-1 key, download google-services.json again and rebuild.");
            return;
        }
        busy(true);
        status.setText("");
        try {
            GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build();
            GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();
            CredentialManager manager = CredentialManager.create(this);
            manager.getCredentialAsync(this, request, new CancellationSignal(), ContextCompat.getMainExecutor(this),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override public void onResult(GetCredentialResponse result) {
                            if (isFinishing() || isDestroyed()) return;
                            handleGoogle(a, result.getCredential());
                        }
                        @Override public void onError(GetCredentialException e) {
                            if (isFinishing() || isDestroyed()) return;
                            if (e instanceof GetCredentialCancellationException) { busy(false); return; }
                            if (e instanceof NoCredentialException) {
                                err("No Google account found on this phone. Add one in Settings > Accounts, then try again.");
                                return;
                            }
                            err("Google sign-in couldn't finish. Check your connection and try again.");
                        }
                    });
        } catch (Throwable t) { err("Google sign-in isn't available on this device right now."); }
    }

    private void handleGoogle(FirebaseAuth a, Credential credential) {
        try {
            if (credential instanceof CustomCredential
                    && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                GoogleIdTokenCredential g = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
                AuthCredential ac = GoogleAuthProvider.getCredential(g.getIdToken(), null);
                final String gName = g.getDisplayName() == null ? "" : g.getDisplayName();
                final String gEmail = g.getId();
                a.signInWithCredential(ac).addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (task.isSuccessful()) {
                        try {
                            FirebaseUser u = task.getResult() != null ? task.getResult().getUser() : null;
                            boolean isNew = task.getResult() != null && task.getResult().getAdditionalUserInfo() != null
                                    && task.getResult().getAdditionalUserInfo().isNewUser();
                            if (u != null && isNew) saveProfile(u, gName, gEmail);
                        } catch (Throwable ignored) { }
                        goMain();
                    } else err(explain(task.getException(), register));
                });
            } else err("That sign-in option isn't supported. Please try again.");
        } catch (Throwable t) { err("Google sign-in couldn't finish. Please try again."); }
    }

    // ---------------------------------------------------------------------------------------------- Helpers

    private String explain(Throwable e, boolean reg) {
        if (e == null) return "Something went wrong. Please try again.";
        if (e instanceof FirebaseAuthWeakPasswordException) return "That password is too weak. Use at least 6 characters, mixing letters and numbers.";
        if (e instanceof FirebaseAuthUserCollisionException) return "An account with this email already exists. Try logging in instead.";
        if (e instanceof FirebaseAuthInvalidUserException) return "No account found for that email. Check it or create an account.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return reg ? "That email address doesn't look right." : "Incorrect email or password.";
        if (e instanceof FirebaseNetworkException) return "No internet connection. Check your data or Wi-Fi and try again.";
        if (e instanceof FirebaseTooManyRequestsException) return "Too many attempts. Please wait a few minutes and try again.";
        String code = e instanceof FirebaseAuthException ? ((FirebaseAuthException) e).getErrorCode() : "";
        String msg = String.valueOf(e.getMessage());
        if ("ERROR_OPERATION_NOT_ALLOWED".equals(code) || msg.contains("CONFIGURATION_NOT_FOUND") || msg.contains("OPERATION_NOT_ALLOWED"))
            return "This sign-in method isn't enabled yet. In Firebase Console, open Authentication > Sign-in method and enable Email/Password and Google.";
        if (msg.contains("API key") || msg.contains("API_KEY")) return "The Firebase API key was rejected. Re-download google-services.json from Firebase and rebuild.";
        return "Couldn't complete that. " + msg;
    }

    private void goMain() {
        Store.pull(this);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
