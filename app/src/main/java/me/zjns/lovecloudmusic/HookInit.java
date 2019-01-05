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

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(Constants.MODULE_PACKAGE_NAME)) {
            makeModuleActive(lpparam.classLoader);
        }
        if (lpparam.packageName.equals(Constants.HOOK_PACKAGE_NAME)) {
            findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    CloudMusic.getInstance().hookHandler(lpparam);
                }
            });
        }
    }

    private void makeModuleActive(ClassLoader loader) {
        findAndHookMethod(Constants.MODULE_PACKAGE_NAME + ".MainActivity", loader, "isModuleEnabled", XC_MethodReplacement.returnConstant(true));
    }
}
