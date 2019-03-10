package me.zjns.lovecloudmusic.hooker;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import me.zjns.lovecloudmusic.Constants;
import me.zjns.lovecloudmusic.hooker.base.BaseHook;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static me.zjns.lovecloudmusic.Utils.findAndHookConstructor;
import static me.zjns.lovecloudmusic.Utils.findAndHookMethod;

public class SideBarHook extends BaseHook {

    private DrawerItem drawerItem;
    private Object mainActivity;

    private boolean disableSignJump;
    private boolean autoSign;
    private boolean isFromAutoSign = false;
    private boolean hideDot;
    private boolean hideTicket;
    private boolean hideVip;
    private boolean hideStore;
    private boolean hideGame;
    private boolean hideFree;
    private boolean hideNearBy;
    private boolean hideChildMode;
    private boolean hideCoupon;
    private boolean hideViewer;

    @Override
    protected void hookMain() {
        drawerItem = new DrawerItem();
        drawerItem.initItems();
        hookDrawerItem();
        hideRedDot();
        disableSignJump();
        noToggleAfterAutoSign();
    }

    @Override
    protected void initPrefs() {
        disableSignJump = prefs.getBoolean("disable_sign_jump_to_small", false);
        autoSign = prefs.getBoolean("auto_sign", false);
        hideDot = prefs.getBoolean("hide_dot", false);
        hideTicket = prefs.getBoolean("hide_item_live_ticket", false);
        hideVip = prefs.getBoolean("hide_item_vip", false);
        hideStore = prefs.getBoolean("hide_item_shop", false);
        hideGame = prefs.getBoolean("hide_item_game", false);
        hideFree = prefs.getBoolean("hide_item_free_data", false);
        hideNearBy = prefs.getBoolean("hide_item_nearby", false);
        hideChildMode = prefs.getBoolean("hide_item_baby", false);
        hideCoupon = prefs.getBoolean("hide_item_ticket", false);
        hideViewer = prefs.getBoolean("hide_item_singer", false);
    }

    private void hookDrawerItem() {
        findAndHookMethod("com.netease.cloudmusic.ui.MainDrawer", loader,
                "refreshDrawer",
                new DrawerItemHook());
    }

    private void hideRedDot() {
        Class<?> MessageBubbleView = findClass("com.netease.cloudmusic.ui.MessageBubbleView", loader);
        findAndHookMethod(View.class, "setVisibility", Integer.TYPE, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object obj = param.thisObject;
                if (obj.getClass() == MessageBubbleView) {
                    boolean hasText = getBooleanField(obj, "mShowBubbleWithText");
                    if (!hasText) {
                        loadPrefs();
                        if (hideDot) {
                            param.args[0] = View.GONE;
                        }
                    }
                }
            }
        });
    }

    private void disableSignJump() {
        findAndHookConstructor("com.netease.cloudmusic.ui.MainDrawer", loader,
                findClass("com.netease.cloudmusic.activity.MainActivity", loader),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mainActivity = param.args[0];
                    }
                });
        findAndHookMethod("com.netease.cloudmusic.activity.ReactNativeActivity", loader,
                "a", Context.class, boolean.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object context = param.args[0];
                        boolean bool = (Boolean) param.args[1];
                        if (context == mainActivity && bool) {
                            loadPrefs();
                            if (!disableSignJump) return;
                            param.setResult(null);
                        }
                    }
                });
        findAndHookMethod("com.netease.cloudmusic.ui.MainDrawer", loader,
                "updateSignIn", Boolean.TYPE, Boolean.TYPE,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        loadPrefs();
                        if (!disableSignJump) return;
                        Object obj = param.thisObject;
                        boolean bool1 = (Boolean) param.args[0];
                        boolean bool2 = (Boolean) param.args[1];
                        boolean running = (Boolean) callMethod(obj, "isDoingSinginTask");
                        if ((!bool2 || !running) && bool1) {
                            TextView drawerUserSignIn = (TextView) getObjectField(obj, "drawerUserSignIn");
                            drawerUserSignIn.setOnClickListener(null);
                            drawerUserSignIn.setEnabled(false);
                            drawerUserSignIn.setText("已签到");
                            drawerUserSignIn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        }
                    }
                });
    }

    private void noToggleAfterAutoSign() {
        findAndHookMethod("com.netease.cloudmusic.ui.MainDrawer", loader,
                "toggleDrawerMenu",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (isFromAutoSign) {
                            param.setResult(null);
                            isFromAutoSign = false;
                        }
                    }
                });
    }

    private final class DrawerItemHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            loadPrefs();
            removeUselessItem(param);
            autoSign(param);
        }

        private void autoSign(MethodHookParam param) {
            Object obj = param.thisObject;
            TextView drawerUserSignIn = (TextView) getObjectField(obj, "drawerUserSignIn");
            if (disableSignJump && autoSign) {
                String text = drawerUserSignIn.getText().toString();
                if ("签到".equals(text)) {
                    isFromAutoSign = true;
                    drawerUserSignIn.performClick();
                }
            }
        }

        private void removeUselessItem(MethodHookParam param) {
            LinearLayout drawerContainer;
            LinearLayout dynamicContainer = null;
            drawerContainer = (LinearLayout) getObjectField(param.thisObject, "mDrawerContainer");
            if (versionName.compareTo(Constants.CM_VERSION_600) >= 0) {
                dynamicContainer = (LinearLayout) getObjectField(param.thisObject, "mDynamicContainer");
            }
            removeItemInner(drawerContainer);
            removeItemInner(dynamicContainer);
        }

        private void removeItemInner(LinearLayout container) {
            if (container == null) return;
            for (int i = 0; i < container.getChildCount(); i++) {
                View v = container.getChildAt(i);
                Object tag = v.getTag();
                if (tag != null && shouldRemove(tag)) {
                    v.setVisibility(View.GONE);
                }
            }
        }

        @SuppressWarnings("ConstantConditions")
        private boolean shouldRemove(Object tag) {
            return tag == drawerItem.TICKET && hideTicket
                    || tag == drawerItem.VIP && hideVip
                    || tag == drawerItem.STORE && hideStore
                    || tag == drawerItem.GAME && hideGame
                    || tag == drawerItem.FREE && hideFree
                    || tag == drawerItem.NEARBY && hideNearBy
                    || tag == drawerItem.CHILD_MODE && hideChildMode
                    || tag == drawerItem.DISCOUNT_COUPON && hideCoupon
                    || tag == drawerItem.MUSICIAN_VIEWER && hideViewer;
        }
    }

    @SuppressWarnings("unused")
    private final class DrawerItem {
        private Object PROFILE;
        private Object AVATAR;
        private Object MESSAGE;
        private Object MUSICIAN;
        private Object PROFIT;
        private Object VIP;
        private Object TICKET;
        private Object STORE;
        private Object GAME;
        private Object FREE;
        private Object COLOR_RING;
        private Object MY_FRIEND;
        private Object NEARBY;
        private Object THEME;
        private Object IDENTIFY;
        private Object CLOCK_PLAY;
        private Object SCAN;
        private Object ALARM_CLOCK;
        private Object VEHICLE_PLAYER;
        private Object CLASSICAL;
        private Object CHILD_MODE;
        private Object PRIVATE_CLOUD;
        private Object DISCOUNT_COUPON;
        private Object MUSICIAN_VIEWER;
        private Object SMALL_ICE;
        private Object VOIBOX;
        private Object SETTING;

        private Class classDrawerItemEnum;

        private void initItems() {
            String className = "com.netease.cloudmusic.ui.MainDrawer$DrawerItemEnum";
            classDrawerItemEnum = findClass(className, loader);
            PROFILE = getItem("PROFILE");
            AVATAR = getItem("AVATAR");
            MESSAGE = getItem("MESSAGE");
            MUSICIAN = getItem("MUSICIAN");
            PROFIT = getItem("PROFIT");
            VIP = getItem("VIP");
            TICKET = getItem("TICKET");
            STORE = getItem("STORE");
            GAME = getItem("GAME");
            FREE = getItem("FREE");
            COLOR_RING = getItem("COLOR_RING");
            MY_FRIEND = getItem("MY_FRIEND");
            NEARBY = getItem("NEARBY");
            THEME = getItem("THEME");
            IDENTIFY = getItem("IDENTIFY");
            CLOCK_PLAY = getItem("CLOCK_PLAY");
            SCAN = getItem("SCAN");
            ALARM_CLOCK = getItem("ALARM_CLOCK");
            VEHICLE_PLAYER = getItem("VEHICLE_PLAYER");
            CLASSICAL = getItem("CLASSICAL");
            CHILD_MODE = getItem("CHILD_MODE");
            PRIVATE_CLOUD = getItem("PRIVATE_CLOUD");
            DISCOUNT_COUPON = getItem("DISCOUNT_COUPON");
            MUSICIAN_VIEWER = getItem("MUSICIAN_VIEWER");
            SMALL_ICE = getItem("SMALL_ICE");
            VOIBOX = getItem("VOIBOX");
            SETTING = getItem("SETTING");
        }

        @SuppressWarnings("unchecked")
        private Object getItem(String name) {
            try {
                return Enum.valueOf(classDrawerItemEnum, name);
            } catch (Throwable t) {
                return null;
            }
        }
    }
}
