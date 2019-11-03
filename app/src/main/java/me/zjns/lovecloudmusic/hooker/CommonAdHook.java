package me.zjns.lovecloudmusic.hooker;

import android.graphics.Canvas;
import android.os.BaseBundle;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import me.zjns.lovecloudmusic.hooker.base.BaseHook;

import static me.zjns.lovecloudmusic.Utils.findAndHookMethod;

public class CommonAdHook extends BaseHook {

    private boolean removeSplashAd;
    private boolean removeSearchAd;

    @Override
    protected void hookMain() {
        removeSplashAd();
        removeSearchBannerAd();
    }

    @Override
    protected void initPrefs() {
        removeSplashAd = prefs.getBoolean("convert_to_play", false);
        removeSearchAd = prefs.getBoolean("remove_search_banner_ad", false);
    }

    private void removeSplashAd() {
        Class<?> BundleClass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? BaseBundle.class : Bundle.class;
        findAndHookMethod(BundleClass,
                "getSerializable", String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if ("adInfo".equals(param.args[0])) {
                            loadPrefs();
                            if (!removeSplashAd) return;
                            param.setResult(null);
                        }
                    }
                });
    }

    private void removeSearchBannerAd() {
        findAndHookMethod("com.netease.cloudmusic.ui.AdBannerView", loader,
                "onDraw", Canvas.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        loadPrefs();
                        if (!removeSearchAd) return;

                        View view = (View) param.thisObject;
                        View parent = (View) view.getParent().getParent();
                        boolean isTarget = parent instanceof LinearLayout;
                        if (isTarget) {
                            LinearLayout real = (LinearLayout) parent;
                            if (real.getChildCount() != 0) {
                                real.setVisibility(View.GONE);
                                real.removeAllViews();
                                param.setResult(null);
                            }
                        }
                    }
                });
    }
}
