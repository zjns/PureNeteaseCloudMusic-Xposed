package me.zjns.lovecloudmusic;

import android.app.Application;
import android.app.Instrumentation;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by YiTry on 2017/12/30
 */

public class HookInit implements IXposedHookLoadPackage {
    static final String MODULE_PACKAGE_NAME = HookInit.class.getPackage().getName();
    static final String HOOK_PACKAGE_NAME = "com.netease.cloudmusic";

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(MODULE_PACKAGE_NAME)) {
            makeModuleActive(lpparam.classLoader);
        }
        if (lpparam.packageName.equals(HOOK_PACKAGE_NAME)) {
            findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    CloudMusic.getInstance().hookHandler(lpparam);
                }
            });
        }
    }

    private void makeModuleActive(ClassLoader loader) {
        findAndHookMethod(MODULE_PACKAGE_NAME + ".MainActivity", loader, "isModuleEnabled", XC_MethodReplacement.returnConstant(true));
    }
}
