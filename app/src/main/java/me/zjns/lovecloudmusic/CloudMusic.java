package me.zjns.lovecloudmusic;

import android.content.Context;
import android.content.Intent;
import android.os.BaseBundle;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static me.zjns.lovecloudmusic.Utils.findAndHookConstructor;
import static me.zjns.lovecloudmusic.Utils.findAndHookMethod;
import static me.zjns.lovecloudmusic.Utils.findMethodByExactParameters;
import static me.zjns.lovecloudmusic.Utils.getPackageVersionName;
import static me.zjns.lovecloudmusic.Utils.hookMethod;

final class CloudMusic {
    private boolean dontJump;
    private boolean autoSign;
    private boolean convertToPlay;
    private boolean removeAd;
    private boolean removeVideo;
    private boolean removeTopic;
    private boolean removeLive;
    private boolean removeConcertInfo;
    private boolean zeroPointDLMV;
    private boolean enableVipFeature;
    private boolean removeVideoFlowAd;
    private boolean removeSearchBanner;
    private boolean removeMyInfoView;
    private boolean removeFuncDynamic;
    private boolean removeFuncVideo;
    private boolean removeFuncRadio;
    private boolean hideDot;
    private boolean hideItemVIP;
    private boolean hideItemLiveTicket;
    private boolean hideItemShop;
    private boolean hideItemGame;
    private boolean hideItemFreeData;
    private boolean hideItemNearby;
    private boolean hideItemTicket;
    private boolean hideItemBaby;
    private boolean hideItemSinger;
    private Boolean mIsVipPro = null;
    static String versionName;
    private static ClassLoader loader;
    private static WeakReference<XSharedPreferences> mSharedPrefs = new WeakReference<>(null);
    private static CloudMusic sInstance;
    private boolean hasExtraException = false;
    private WeakReference<View> viewRadio = null;
    private int concertType;
    private HookInfo hookInfo;

    private CloudMusic() {
    }

    static CloudMusic getInstance() {
        if (sInstance == null) {
            sInstance = new CloudMusic();
        }
        return sInstance;
    }

    void hookHandler(LoadPackageParam lpparam) throws Throwable {
        versionName = getPackageVersionName(Constants.HOOK_PACKAGE_NAME);
        hookInfo = new HookInfo(versionName, lpparam.classLoader);
        loader = lpparam.classLoader;
        setConcertType();

        loadPrefs();
        if (!mSharedPrefs.get().getBoolean("enable_all_functions", true)) return;

        convertToPlayVersion();
        disableSignJumpToSmall();
        removeCommentAd();
        removeVideoFlowAd();
        removeSearchBannerAd();
        hideSideBarItems();
        hideUIFuncItems();
        hookVideoPoint();
        hookVIPTheme();
        hookLyricTemplate();
        removeSplashAd();
        removeMyInfoView();
        enableVIPAudioEffect();

        hookInfo.saveHookInfo(hasExtraException);
    }

    private XSharedPreferences getSharedPrefs() {
        XSharedPreferences prefs = mSharedPrefs.get();
        if (prefs == null) {
            prefs = new XSharedPreferences(Constants.MODULE_PACKAGE_NAME);
            mSharedPrefs = new WeakReference<>(prefs);
        }
        return prefs;
    }

    private void loadPrefs() {
        XSharedPreferences prefs = getSharedPrefs();
        prefs.reload();
        dontJump = prefs.getBoolean("disable_sign_jump_to_small", false);
        autoSign = prefs.getBoolean("auto_sign", false);
        convertToPlay = prefs.getBoolean("convert_to_play", false);
        removeAd = prefs.getBoolean("remove_comment_ad", false);
        removeVideo = prefs.getBoolean("remove_comment_video", false);
        removeTopic = prefs.getBoolean("remove_comment_topic", false);
        removeLive = prefs.getBoolean("remove_comment_live", false);
        removeConcertInfo = prefs.getBoolean("remove_comment_concert_info", false);
        zeroPointDLMV = prefs.getBoolean("zero_point_video", false);
        enableVipFeature = prefs.getBoolean("enable_vip_feature", false);
        removeVideoFlowAd = prefs.getBoolean("remove_video_flow_ad", false);
        removeSearchBanner = prefs.getBoolean("remove_search_banner_ad", false);
        removeMyInfoView = prefs.getBoolean("remove_my_info_view", false);
        removeFuncDynamic = prefs.getBoolean("remove_func_dynamic", false);
        removeFuncVideo = prefs.getBoolean("remove_func_video", false);
        removeFuncRadio = prefs.getBoolean("remove_func_radio", false);
        hideDot = prefs.getBoolean("hide_dot", false);
        hideItemVIP = prefs.getBoolean("hide_item_vip", false);
        hideItemLiveTicket = prefs.getBoolean("hide_item_live_ticket", false);
        hideItemShop = prefs.getBoolean("hide_item_shop", false);
        hideItemGame = prefs.getBoolean("hide_item_game", false);
        hideItemFreeData = prefs.getBoolean("hide_item_free_data", false);
        hideItemNearby = prefs.getBoolean("hide_item_nearby", false);
        hideItemTicket = prefs.getBoolean("hide_item_ticket", false);
        hideItemBaby = prefs.getBoolean("hide_item_baby", false);
        hideItemSinger = prefs.getBoolean("hide_item_singer", false);
    }

    @SuppressWarnings("all")
    private String decryptString(String string) {
        try {
            Method c = findClass("a.auu.a", loader).getDeclaredMethod("c", String.class);
            c.setAccessible(true);
            return (String) c.invoke(null, string);
        } catch (Throwable t) {
            return null;
        }
    }

    private void setConcertType() {
        if (versionName.compareTo(Constants.CM_VERSION_580) >= 0) {
            concertType = 10;
        } else {
            concertType = 5;
        }
    }

    private void removeSplashAd() {
        if (versionName.compareTo(Constants.CM_VERSION_520) < 0 || !convertToPlay) return;
        Class<?> BundleClass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? BaseBundle.class : Bundle.class;
        findAndHookMethod(BundleClass, "getSerializable", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ("adInfo".equals(param.args[0])) {
                    param.setResult(null);
                }
            }
        });
    }

    private void removeSearchBannerAd() {
        findAndHookConstructor("com.netease.cloudmusic.ui.AdBannerView", loader,
                Context.class, AttributeSet.class, Integer.TYPE, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        loadPrefs();
                        if (!removeSearchBanner) return;
                        View view = (View) param.thisObject;
                        view.setVisibility(View.GONE);
                    }
                });
    }

    private void convertToPlayVersion() {
        Method getChannel = hookInfo.getMethodChannel();
        if (getChannel != null) {
            Unhook unhook = hookMethod(getChannel, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    loadPrefs();
                    if (convertToPlay) {
                        param.setResult("google");
                    }
                }
            });
            if (unhook == null) hasExtraException = true;
        }
    }

    private void disableSignJumpToSmall() {
        if (versionName.compareTo(Constants.CM_VERSION_413) > 0
                && versionName.compareTo(Constants.CM_VERSION_433) < 0) {
            findAndHookMethod("android.app.SharedPreferencesImpl$EditorImpl", loader, "putString",
                    String.class, String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            loadPrefs();
                            if (!dontJump) return;
                            if ("siginWebUrl".equals(param.args[0])) {
                                param.args[1] = null;
                            }
                        }
                    });
        } else if (versionName.compareTo(Constants.CM_VERSION_433) >= 0
                && versionName.compareTo(Constants.CM_VERSION_435) < 0) {
            Method j = findMethodByExactParameters(findClass("com.netease.cloudmusic.module.a.b", loader), "j", Boolean.TYPE);
            hookMethod(j, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    loadPrefs();
                    if (!dontJump) return;
                    param.setResult(true);
                }
            });
        } else if (versionName.compareTo(Constants.CM_VERSION_435) >= 0) {
            Method userGroup = hookInfo.getMethodUserGroup();
            if (userGroup != null) {
                Unhook unhook = hookMethod(userGroup, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        loadPrefs();
                        if (dontJump && "yueqian".equals(param.args[0])) {
                            param.setResult("t2");
                        }
                    }
                });
                if (unhook == null) hasExtraException = true;
            }
            Method isWeekend = hookInfo.getMethodIsWeekend();
            if (isWeekend != null) {
                Unhook unhook = hookMethod(isWeekend, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        loadPrefs();
                        if (dontJump) {
                            param.setResult(false);
                        }
                    }
                });
                if (unhook == null) hasExtraException = true;
            }
        }
    }

    private boolean shouldRemove(Object obj) {
        int type = (int) callMethod(obj, "getType");
        return type == concertType && removeConcertInfo ||
                type == 6 && removeTopic ||
                type == 11 && removeVideo ||
                (type == 9 || type == 13 || type == 14) && removeAd ||
                type == 15 && removeLive;
    }

    private void removeCommentAd() {
        if (versionName.compareTo(Constants.CM_VERSION_421) <= 0) return;
        Method getComments = hookInfo.getInnerFragmentMethod(Constants.META_TYPE_COMMENT_LIST_ENTRY);
        if (getComments == null) return;
        Unhook unhook = hookMethod(getComments, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                loadPrefs();
                ArrayList<?> origList = (ArrayList) param.getResult();
                if (origList == null || origList.isEmpty()) return;
                Iterator<?> iterator = origList.iterator();
                while (iterator.hasNext()) {
                    Object entry = iterator.next();
                    if (shouldRemove(entry)) {
                        iterator.remove();
                    }
                }
            }
        });
        if (unhook == null) hasExtraException = true;
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
            findAndHookMethod(Bundle.class, "getParcelable", String.class, new XC_MethodHook() {
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
        if (versionName.compareTo(Constants.CM_VERSION_411) < 0) return;
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

    private void removeVideoFlowAd() {
        if (versionName.compareTo(Constants.CM_VERSION_530) < 0) return;
        Method getVideos = hookInfo.getFragmentMethod(Constants.META_TYPE_VIDEO_TIMELINE_DATA);
        Unhook unhook = hookMethod(getVideos, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                loadPrefs();
                if (!removeVideoFlowAd) return;
                List<?> list = (List) param.getResult();
                if (list == null || list.isEmpty()) return;
                Iterator<?> it = list.iterator();
                while (it.hasNext()) {
                    int type = getIntField(it.next(), "type");
                    if (type == 19 || type == 23 || type == 24) {
                        it.remove();
                    }
                }
            }
        });
        if (unhook == null) hasExtraException = true;
    }

    private void hideSideBarItems() {
        findAndHookMethod(TextView.class, "setText",
                CharSequence.class, TextView.BufferType.class, Boolean.TYPE, Integer.TYPE,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0] == null) return;
                        String content = param.args[0].toString();
                        if ("优惠券".equals(content)) {
                            loadPrefs();
                            if (!hideItemTicket) return;
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null) {
                                Class<?> clazz = parent.getClass().getSuperclass();
                                if (clazz == FrameLayout.class) {
                                    parent.setVisibility(View.GONE);
                                }
                            }
                        } else if ("电台".equals(content)) {
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null) {
                                Class<?> clazz = parent.getClass().getSuperclass();
                                if (clazz == LinearLayout.class) {
                                    viewRadio = new WeakReference<>(parent);
                                }
                            }
                        } else if ("VIP会员".equals(content) || "会员中心".equals(content) || "我的会员".equals(content)) {
                            loadPrefs();
                            if (!hideItemVIP) return;
                            TextView textView = (TextView) param.thisObject;
                            String name = textView.getClass().getName();
                            View parent = (View) textView.getParent();
                            if (name.endsWith("CustomThemeTextView") && parent != null) {
                                parent.setVisibility(View.GONE);
                            }
                        } else if ("云村有票".equals(content)) {
                            loadPrefs();
                            if (!hideItemLiveTicket) return;
                            TextView textView = (TextView) param.thisObject;
                            String name = textView.getClass().getName();
                            View parent = (View) textView.getParent();
                            if (name.endsWith("CustomThemeTextView") && parent != null) {
                                ViewGroup.LayoutParams params = parent.getLayoutParams();
                                params.height = 0;
                            }
                        } else if ("商城".equals(content)) {
                            loadPrefs();
                            if (!hideItemShop) return;
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null) {
                                Class<?> clazz = parent.getClass().getSuperclass();
                                if (clazz == FrameLayout.class) {
                                    parent.setVisibility(View.GONE);
                                }
                            }
                        } else if (content.contains("游戏推荐")) {
                            loadPrefs();
                            if (!hideItemGame) return;
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null) {
                                ViewGroup.LayoutParams params = parent.getLayoutParams();
                                params.height = 0;
                            }
                        } else if ("在线听歌免流量".equals(content)) {
                            loadPrefs();
                            if (!hideItemFreeData) return;
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null) {
                                Class<?> clazz = parent.getClass().getSuperclass();
                                if (clazz == FrameLayout.class) {
                                    ViewGroup.LayoutParams params = parent.getLayoutParams();
                                    params.height = 0;
                                }
                            }
                        } else if ("附近的人".equals(content)) {
                            loadPrefs();
                            if (!hideItemNearby) return;
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null) {
                                Class<?> clazz = parent.getClass().getSuperclass();
                                if (clazz == FrameLayout.class) {
                                    ViewGroup.LayoutParams params = parent.getLayoutParams();
                                    params.height = 0;
                                }
                            }
                        } else if ("签到".equals(content)) {
                            loadPrefs();
                            if (!dontJump || !autoSign) return;
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null && parent.getClass() == LinearLayout.class) {
                                textView.performClick();
                            }
                        } else if ("亲子频道".equals(content)) {
                            loadPrefs();
                            if (!hideItemBaby) return;
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null) {
                                Class<?> clazz = parent.getClass().getSuperclass();
                                if (clazz == FrameLayout.class) {
                                    ViewGroup.LayoutParams params = parent.getLayoutParams();
                                    params.height = 0;
                                }
                            }
                        } else if ("加入网易音乐人".equals(content)) {
                            loadPrefs();
                            if (!hideItemSinger) return;
                            TextView textView = (TextView) param.thisObject;
                            View parent = (View) textView.getParent();
                            if (parent != null) {
                                Class<?> clazz = parent.getClass().getSuperclass();
                                if (clazz == FrameLayout.class) {
                                    ViewGroup.LayoutParams params = parent.getLayoutParams();
                                    params.height = 0;
                                }
                            }
                        }
                    }
                });
    }

    private void hideUIFuncItems() {
        findAndHookMethod(View.class, "setContentDescription", CharSequence.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] == null) return;
                String content = param.args[0].toString();
                List<String> items = new ArrayList<>();
                if (removeFuncDynamic) {
                    items.add("我的动态");
                }
                if (removeFuncVideo) {
                    items.add("我的视频");
                }
                if (items.contains(content)) {
                    View view = (View) param.thisObject;
                    view.setVisibility(View.GONE);
                }
            }
        });
        Class<?> MessageBubbleView = findClass("com.netease.cloudmusic.ui.MessageBubbleView", loader);
        findAndHookMethod(View.class, "setVisibility", Integer.TYPE, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (viewRadio != null
                        && param.thisObject == viewRadio.get()) {
                    loadPrefs();
                    if (removeFuncRadio) {
                        param.args[0] = View.GONE;
                    }
                } else if (param.thisObject.getClass() == MessageBubbleView) {
                    loadPrefs();
                    if (hideDot) {
                        param.args[0] = View.GONE;
                    }
                }
            }
        });
    }

    private void removeMyInfoView() {
        if (versionName.compareTo(Constants.CM_VERSION_530) < 0) return;
        Class<?> CardView = findClass("com.netease.cloudmusic.adapter.MyMusicAdapter$CardView", loader);
        findAndHookConstructor(View.class, Context.class, AttributeSet.class, Integer.TYPE, Integer.TYPE,
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

    private void enableVIPAudioEffect() {
        if (versionName.compareTo(Constants.CM_VERSION_580) < 0) return;
        Class<?> AudioEffectButtonData = findClass("com.netease.cloudmusic.meta.AudioEffectButtonData", loader);
        Class<?> AudioActionView = findClass("com.netease.cloudmusic.ui.AudioActionView", loader);
        findAndHookMethod(AudioActionView, "setDownloadTypeViews", AudioEffectButtonData, Object.class, Integer.TYPE,
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
