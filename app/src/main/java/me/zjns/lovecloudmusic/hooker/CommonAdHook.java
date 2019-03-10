package me.zjns.lovecloudmusic.hooker;

import android.content.Context;
import android.os.BaseBundle;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import me.zjns.lovecloudmusic.Constants;
import me.zjns.lovecloudmusic.hooker.base.BaseHook;

import static de.robv.android.xposed.XposedHelpers.getIntField;
import static me.zjns.lovecloudmusic.Utils.findAndHookConstructor;
import static me.zjns.lovecloudmusic.Utils.findAndHookMethod;

public class CommonAdHook extends BaseHook {

    private boolean removeSplashAd;
    private boolean removeSearchAd;
    private boolean removeVideoAd;

    @Override
    protected void hookMain() {
        removeSplashAd();
        removeSearchBannerAd();
        removeVideoFlowAd();
    }

    @Override
    protected void initPrefs() {
        removeSplashAd = prefs.getBoolean("convert_to_play", false);
        removeSearchAd = prefs.getBoolean("remove_search_banner_ad", false);
        removeVideoAd = prefs.getBoolean("remove_video_flow_ad", false);
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
        findAndHookConstructor("com.netease.cloudmusic.ui.AdBannerView", loader,
                Context.class, AttributeSet.class, Integer.TYPE,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        loadPrefs();
                        if (!removeSearchAd) return;
                        View view = (View) param.thisObject;
                        view.setVisibility(View.GONE);
                    }
                });
    }

    private void removeVideoFlowAd() {
        findAndHookMethod("com.netease.cloudmusic.ui.PagerListView$LoadTaskAware", loader,
                "realOnPostExecute", List.class,
                new VideoFlowHook());
    }

    private final class VideoFlowHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            List list = (List) param.args[0];
            if (list == null || list.isEmpty()) return;

            Object obj = list.get(0);
            if (obj == null) return;

            String name = obj.getClass().getSimpleName();
            if (!Constants.META_TYPE_VIDEO_TIMELINE_DATA.equals(name)) return;

            loadPrefs();
            if (!removeVideoAd) return;

            Iterator it = list.iterator();
            while (it.hasNext()) {
                int type = getIntField(it.next(), "type");
                if (type == 19
                        || type == 23
                        || type == 24
                        || type == 25) {
                    it.remove();
                }
            }
        }
    }
}
