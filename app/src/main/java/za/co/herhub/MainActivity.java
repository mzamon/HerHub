package za.co.herhub;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView nav;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Store.user() == null && !Store.getB(this, "guest", false)) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_page_glow);
        FrameLayout content = new FrameLayout(this);
        content.setId(R.id.content);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        nav = new BottomNavigationView(this);
        nav.setBackgroundResource(R.drawable.bg_nav_surface);
        nav.setElevation(U.dp(this, 12));
        nav.setItemIconSize(U.dp(this, 24));
        nav.setItemPaddingTop(U.dp(this, 8));
        nav.setItemPaddingBottom(U.dp(this, 6));
        nav.setItemIconTintList(new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{U.c(this, R.color.brand), U.c(this, R.color.ink_soft)}));
        nav.setItemTextColor(new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{U.c(this, R.color.brand), U.c(this, R.color.ink_soft)}));
        nav.setItemActiveIndicatorColor(android.content.res.ColorStateList.valueOf(U.c(this, R.color.brand_soft)));
        nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        nav.setItemRippleColor(android.content.res.ColorStateList.valueOf(0x22D92F78));
        nav.inflateMenu(R.menu.bottom_nav);
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        navLp.setMargins(U.dp(this, 12), U.dp(this, 5), U.dp(this, 12), U.dp(this, 6));
        root.addView(nav, navLp);
        setContentView(root);
        U.edge(this, root, nav);

        nav.setOnItemSelectedListener(item -> { showTab(item.getItemId()); return true; });
        if (b == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content, new HomePage()).commit();
        }
        if (b == null && getIntent() != null && getIntent().getBooleanExtra("sos", false)) {
            if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
            open(new SosPage());
        }
        if (Store.getB(this, "sos_power", false) && Loc.has(this)) PowerSosService.start(this);
        NotifyPage.rescheduleAll(this);
        Store.pull(this);
        showLastCrash();
    }

    @Override protected void onDestroy() {
        if (isFinishing()) Siren.stop();
        super.onDestroy();
    }

    private void showTab(int id) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment f;
        if (id == R.id.nav_safety) f = new SafetyPage();
        else if (id == R.id.nav_map) f = new MapPage();
        else if (id == R.id.nav_wellness) f = new WellnessPage();
        else if (id == R.id.nav_more) f = new MorePage();
        else f = new HomePage();
        fm.beginTransaction().replace(R.id.content, f).commit();
    }

    public void goTab(int id) { if (nav != null) nav.setSelectedItemId(id); }

    public void open(@NonNull Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.content, f).addToBackStack(null).commit();
    }

    /** If the previous run crashed, show the stack trace once so it can be copied and reported. */
    private void showLastCrash() {
        try {
            String last = getSharedPreferences("crash", Context.MODE_PRIVATE).getString("last", null);
            if (last == null) return;
            getSharedPreferences("crash", Context.MODE_PRIVATE).edit().remove("last").apply();
            final String trace = last.length() > 3500 ? last.substring(0, 3500) : last;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("HerHub closed unexpectedly")
                    .setMessage("Sorry about that. Tap “Copy details” and send them to the developer so it can be fixed.\n\n" + trace)
                    .setPositiveButton("Copy details", (d, w) -> U.copy(this, trace))
                    .setNegativeButton("Dismiss", null).show();
        } catch (Throwable ignored) { }
    }
}
