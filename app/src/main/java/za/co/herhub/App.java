package za.co.herhub;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                getSharedPreferences("crash", MODE_PRIVATE).edit().putString("last", sw.toString()).commit();
            } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
        });
        Store.migrate(this);
        try { AppCompatDelegate.setDefaultNightMode(Store.themeMode(this)); } catch (Throwable ignored) { }
        try {
            IConfigurationProvider c = Configuration.getInstance();
            c.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
            c.setUserAgentValue(getPackageName());
            File base = new File(getCacheDir(), "osmdroid");
            c.setOsmdroidBasePath(base);
            c.setOsmdroidTileCache(new File(base, "tiles"));
        } catch (Throwable ignored) { }
        try { ReminderReceiver.createChannel(this); } catch (Throwable ignored) { }
    }
}
