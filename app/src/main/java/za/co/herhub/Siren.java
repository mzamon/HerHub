package za.co.herhub;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Panic siren. Plays a real, loud, looping emergency siren (res/raw/herhub_siren.wav) on the alarm stream at
 * maximum volume, vibrates, and blinks the camera torch exactly 10 times while the sound starts.
 */
public final class Siren {
    private Siren() { }

    public static final int BLINKS = 10;
    private static final long BLINK_MS = 260;

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static MediaPlayer player;
    private static String torchId;
    private static CameraManager camera;
    private static int step;
    private static boolean blinking;
    private static Context appCtx;

    public static boolean isOn() {
        try { return player != null && player.isPlaying(); } catch (Throwable t) { return false; }
    }

    public static void toggle(Context c) {
        if (isOn()) stop(); else start(c);
    }

    public static void start(Context c) {
        if (isOn()) return;
        final Context app = c.getApplicationContext();
        appCtx = app;
        stop();
        maxVolume(app);
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            AssetFileDescriptor afd = app.getResources().openRawResourceFd(R.raw.herhub_siren);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setLooping(true);
            mp.setVolume(1f, 1f);
            mp.setOnErrorListener((m, w, e) -> { stop(); return true; });
            mp.prepare();
            mp.start();
            player = mp;
        } catch (Throwable t) {
            player = null;
            U.toast(app, "Couldn't start the siren on this device.");
            return;
        }
        vibrate(app, true);
        blinkTorch(app);
    }

    public static void stop() {
        try {
            if (player != null) { try { player.stop(); } catch (Throwable ignored) { } player.release(); }
        } catch (Throwable ignored) { }
        player = null;
        blinking = false;
        H.removeCallbacksAndMessages(null);
        setTorch(false);
        if (appCtx != null) vibrate(appCtx, false);
    }

    private static void maxVolume(Context c) {
        try {
            AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        } catch (Throwable ignored) { }
    }

    private static void vibrate(Context c, boolean on) {
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (!on) { v.cancel(); return; }
            v.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 250}, 0));
        } catch (Throwable ignored) { }
    }

    /** Flashes the torch on/off exactly BLINKS times (one on+off pair per blink), then leaves it off. */
    private static void blinkTorch(Context c) {
        try {
            camera = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
            torchId = null;
            if (camera != null) {
                for (String id : camera.getCameraIdList()) {
                    CameraCharacteristics cc = camera.getCameraCharacteristics(id);
                    Boolean flash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                    if (flash != null && flash && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        torchId = id;
                        break;
                    }
                }
            }
        } catch (Throwable t) { torchId = null; }
        if (torchId == null) return;
        step = 0;
        blinking = true;
        final int total = BLINKS * 2;
        H.post(new Runnable() {
            @Override public void run() {
                if (!blinking) return;
                if (step >= total) { setTorch(false); blinking = false; return; }
                setTorch(step % 2 == 0);
                step++;
                H.postDelayed(this, BLINK_MS);
            }
        });
    }

    private static void setTorch(boolean on) {
        try {
            if (camera != null && torchId != null) camera.setTorchMode(torchId, on);
        } catch (Throwable ignored) { }
    }
}
