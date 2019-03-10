package me.zjns.lovecloudmusic.hooker;

import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import me.zjns.lovecloudmusic.Constants;
import me.zjns.lovecloudmusic.hooker.base.BaseHook;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static me.zjns.lovecloudmusic.Utils.findAndHookConstructor;
import static me.zjns.lovecloudmusic.Utils.findAndHookMethod;
import static me.zjns.lovecloudmusic.Utils.findMethodByExactParameters;
import static me.zjns.lovecloudmusic.Utils.hookMethod;

public class VipFeatureHook extends BaseHook {

    private Boolean mIsVipPro = null;

    private boolean enableVipFeature;

    @Override
    protected void hookMain() {
        hookVIPTheme();
        hookLyricTemplate();
        enableVIPAudioEffect();
    }

    @Override
    protected void initPrefs() {
        enableVipFeature = prefs.getBoolean("enable_vip_feature", false);
    }

    private void hookVIPTheme() {
        String ThemeDetailActivity = "com.netease.cloudmusic.activity.ThemeDetailActivity";
        String ThemeDetailActivity$1 = ThemeDetailActivity + "$1";
        if (versionName.compareTo(Constants.CM_VERSION_580) < 0) {
            findAndHookConstructor(ThemeDetailActivity$1, loader,
                    ThemeDetailActivity, Integer.TYPE, Boolean.TYPE, Boolean.TYPE, Integer.TYPE, Intent.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            loadPrefs();
                            if (!enableVipFeature || isVipPro()) return;
                            param.args[2] = false;
                            param.args[3] = false;
                        }
                    });
        } else {
            Class<?> ThemeInfo = findClass("com.netease.cloudmusic.theme.core.ThemeInfo", loader);
            findAndHookMethod(Bundle.class,
                    "getParcelable", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result != null && result.getClass() == ThemeInfo) {
                                loadPrefs();
                                if (enableVipFeature && !isVipPro()) {
                                    callMethod(result, "setPaid", true);
                                    callMethod(result, "setVip", false);
                                }
                            }
                        }
                    });
        }
    }

    private void hookLyricTemplate() {
        try {
            Method e = findMethodByExactParameters(
                    findClass("com.netease.cloudmusic.module.lyrictemplate.d", loader),
                    "e", Boolean.TYPE);
            hookMethod(e, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    loadPrefs();
                    if (enableVipFeature && !isVipPro()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }

    private void enableVIPAudioEffect() {
        if (versionName.compareTo(Constants.CM_VERSION_580) < 0) return;
        Class<?> AudioEffectButtonData = findClass("com.netease.cloudmusic.meta.AudioEffectButtonData", loader);
        Class<?> AudioActionView = findClass("com.netease.cloudmusic.ui.AudioActionView", loader);
        findAndHookMethod(AudioActionView,
                "setDownloadTypeViews", AudioEffectButtonData, Object.class, Integer.TYPE,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object data = param.args[0];
                        if (data != null && (Integer) callMethod(data, "getType") == 3) {
                            loadPrefs();
                            if (enableVipFeature && !isVipPro()) {
                                callMethod(data, "setType", 1);
                                callMethod(data, "setAnimVipType", 1);
                                callMethod(data, "setAeVipType", 1);
                            }
                        }
                    }
                });
    }

    private boolean isVipPro() {
        if (mIsVipPro != null) return mIsVipPro;
        if (versionName.compareTo(Constants.CM_VERSION_530) >= 0) {
            Class<?> UserPrivilege = findClass("com.netease.cloudmusic.meta.virtual.UserPrivilege", loader);
            String vipType = callStaticMethod(UserPrivilege, "getLogVipType").toString().trim();
            return mIsVipPro = "100".equals(vipType);
        }
        Object profile = hookInfo.getProfile();
        return mIsVipPro = profile != null && (boolean) callMethod(profile, "isVipPro");
    }
}
