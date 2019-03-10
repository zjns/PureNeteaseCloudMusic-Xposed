package me.zjns.lovecloudmusic;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import me.zjns.lovecloudmusic.hooker.CommentListHook;
import me.zjns.lovecloudmusic.hooker.CommonAdHook;
import me.zjns.lovecloudmusic.hooker.OtherHook;
import me.zjns.lovecloudmusic.hooker.SideBarHook;
import me.zjns.lovecloudmusic.hooker.VipFeatureHook;
import me.zjns.lovecloudmusic.hooker.base.BaseHook;

import static me.zjns.lovecloudmusic.Utils.getPackageVersionName;

public final class CloudMusic {
    private static String versionName;
    private static ClassLoader loader;
    private static WeakReference<XSharedPreferences> mSharedPrefs = new WeakReference<>(null);
    private static HookInfo hookInfo;
    private List<BaseHook> hooks = new ArrayList<>();

    private CloudMusic() {
    }

    static CloudMusic getInstance() {
        return new CloudMusic();
    }

    public static XSharedPreferences getSharedPrefs() {
        XSharedPreferences prefs = mSharedPrefs.get();
        if (prefs == null) {
            prefs = new XSharedPreferences(Constants.MODULE_PACKAGE_NAME);
            mSharedPrefs = new WeakReference<>(prefs);
        }
        return prefs;
    }

    public static String getVersionName() {
        return versionName;
    }

    public static ClassLoader getLoader() {
        return loader;
    }

    public static HookInfo getHookInfo() {
        return hookInfo;
    }

    void hookHandler(LoadPackageParam lpparam) throws Throwable {
        if (!isHookSwitchOpened()) return;

        versionName = getPackageVersionName(Constants.HOOK_PACKAGE_NAME);
        hookInfo = new HookInfo(lpparam.classLoader);
        loader = lpparam.classLoader;

        startAllHook();

        hookInfo.saveHookInfo(BaseHook.hasExtraException);
    }

    private void startAllHook() {
        hooks.clear();
        hooks.add(new CommentListHook());
        hooks.add(new CommonAdHook());
        hooks.add(new SideBarHook());
        hooks.add(new VipFeatureHook());
        hooks.add(new OtherHook());
        for (BaseHook hook : hooks) {
            hook.startHook();
        }
    }

    private boolean isHookSwitchOpened() {
        XSharedPreferences prefs = getSharedPrefs();
        return prefs.getBoolean("enable_all_functions", false);
    }
}
