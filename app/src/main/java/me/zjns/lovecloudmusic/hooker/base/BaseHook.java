package me.zjns.lovecloudmusic.hooker.base;

import de.robv.android.xposed.XSharedPreferences;
import me.zjns.lovecloudmusic.CloudMusic;
import me.zjns.lovecloudmusic.Constants;
import me.zjns.lovecloudmusic.HookInfo;

import static de.robv.android.xposed.XposedBridge.log;

public abstract class BaseHook {
    public static boolean hasExtraException = false;
    protected String versionName;
    protected ClassLoader loader;
    protected HookInfo hookInfo;
    protected XSharedPreferences prefs;

    public BaseHook() {
        this.versionName = CloudMusic.getVersionName();
        this.loader = CloudMusic.getLoader();
        this.hookInfo = CloudMusic.getHookInfo();
        this.prefs = CloudMusic.getSharedPrefs();
    }

    protected abstract void hookMain();

    public final void startHook() {
        if (disableHook()) return;
        try {
            hookMain();
        } catch (Throwable t) {
            log(t);
        }
    }

    protected boolean disableHook() {
        // no longer support version that lower 5.5.2.
        return versionName.compareTo(Constants.CM_VERSION_552) < 0;
    }

    protected final void loadPrefs() {
        prefs = CloudMusic.getSharedPrefs();
        prefs.reload();
        initPrefs();
    }

    protected void initPrefs() {
    }
}
