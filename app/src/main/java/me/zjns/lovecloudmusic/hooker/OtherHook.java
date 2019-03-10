package me.zjns.lovecloudmusic.hooker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import me.zjns.lovecloudmusic.Constants;
import me.zjns.lovecloudmusic.hooker.base.BaseHook;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static me.zjns.lovecloudmusic.Utils.findAndHookConstructor;
import static me.zjns.lovecloudmusic.Utils.findAndHookMethod;
import static me.zjns.lovecloudmusic.Utils.hookMethod;

public class OtherHook extends BaseHook {

    private boolean zeroPointDLMV;
    private boolean removeMyInfoView;
    private boolean changeChannel;

    @Override
    protected void hookMain() {
        hookVideoPoint();
        setChannelToGoogle();
        if (versionName.compareTo(Constants.CM_VERSION_590) <= 0) {
            removeMyInfoView();
        }
    }

    @Override
    protected void initPrefs() {
        zeroPointDLMV = prefs.getBoolean("zero_point_video", false);
        removeMyInfoView = prefs.getBoolean("remove_my_info_view", false);
        changeChannel = prefs.getBoolean("convert_to_play", false);
    }

    private void hookVideoPoint() {
        Class<?> metaMV = findClass("com.netease.cloudmusic.meta.MV", loader);
        findAndHookMethod(metaMV, "isDownloadNeedPoint", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                loadPrefs();
                if (zeroPointDLMV) {
                    param.setResult(false);
                }
            }
        });
        findAndHookMethod(metaMV, "isFreePointCurBitMvDownload", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                loadPrefs();
                if (zeroPointDLMV) {
                    param.setResult(true);
                }
            }
        });
    }

    private void removeMyInfoView() {
        Class<?> CardView = findClass("com.netease.cloudmusic.adapter.MyMusicAdapter$CardView", loader);
        findAndHookConstructor(View.class,
                Context.class, AttributeSet.class, Integer.TYPE, Integer.TYPE,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.thisObject.getClass() != CardView) return;
                        loadPrefs();
                        if (!removeMyInfoView) return;
                        View view = (View) param.thisObject;
                        view.setVisibility(View.GONE);
                    }
                });
    }

    private void setChannelToGoogle() {
        Method method = hookInfo.getMethodChannel();
        if (method == null) return;
        XC_MethodHook.Unhook unhook = hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                loadPrefs();
                if (changeChannel) {
                    param.setResult("google");
                }
            }
        });
        if (unhook == null) hasExtraException = true;
    }
}
