package me.zjns.lovecloudmusic.hooker;

import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import me.zjns.lovecloudmusic.Constants;
import me.zjns.lovecloudmusic.hooker.base.BaseHook;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;
import static me.zjns.lovecloudmusic.Utils.findAndHookMethod;

public class CommentListHook extends BaseHook {

    private TYPE mType;

    private boolean removeAd;
    private boolean removeConcert;
    private boolean removeVideo;
    private boolean removeLive;
    private boolean removeTopic;
    private boolean removeVipRcmd;

    @Override
    protected void hookMain() {
        mType = new TYPE();
        mType.initType();
        removeCommentAd();
    }

    @Override
    protected void initPrefs() {
        removeAd = prefs.getBoolean("remove_comment_ad", false);
        removeConcert = prefs.getBoolean("remove_comment_concert_info", false);
        removeVideo = prefs.getBoolean("remove_comment_video", false);
        removeLive = prefs.getBoolean("remove_comment_live", false);
        removeTopic = prefs.getBoolean("remove_comment_topic", false);
        removeVipRcmd = prefs.getBoolean("remove_comment_vip_rcmd", false);
    }

    private void removeCommentAd() {
        findAndHookMethod("com.netease.cloudmusic.ui.PagerListView$LoadTaskAware", loader,
                "realOnPostExecute", List.class,
                new ListHook());
    }

    private boolean shouldRemove(Object obj) {
        int type = (int) callMethod(obj, "getType");
        return type == mType.AD && removeAd
                || type == mType.BANNER_AD && removeAd
                || type == mType.VIDEO_AD && removeAd
                || type == mType.AD_POSITION_LIVING_TYPE && removeAd
                || type == mType.VIDEO && removeVideo
                || type == mType.CONCERT_INFO && removeConcert
                || type == mType.LIVE && removeLive
                || type == mType.LIVE_RCMD && removeLive
                || type == mType.RELATIVE_TOPIC && removeTopic
                || type == mType.VIP_RCMD && removeVipRcmd;
    }

    @SuppressWarnings("unused")
    private final class TYPE {
        private int AD;
        private int AD_POSITION_LIVING_TYPE;
        private int BANNER_AD;
        private int COMMENT;
        private int COMMENT_DELETE;
        private int COMMENT_IN_PICTURE;
        private int COMMENT_LIVING_TYPE;
        private int COMMENT_WITH_RES_CARD;
        private int COMMON_SECTION;
        private int COMMON_SECTION_IN_PICTURE;
        private int CONCERT_INFO;
        private int FESTIVAL;
        private int GENERAL;
        private int LIVE;
        private int LIVE_RCMD;
        private int MORE_HOT_COMMENT;
        private int RECENT_COMMENT_SECTION;
        private int RELATIVE_TOPIC;
        private int UNKONWN;
        private int VIDEO;
        private int VIDEO_AD;
        private int VIP_RCMD;

        private Class<?> classTYPE;

        private void initType() {
            String className = "com.netease.cloudmusic.meta.virtual.CommentListEntry$TYPE";
            classTYPE = findClass(className, loader);
            AD = getValue("AD");
            AD_POSITION_LIVING_TYPE = getValue("AD_POSITION_LIVING_TYPE");
            BANNER_AD = getValue("BANNER_AD");
            COMMENT = getValue("COMMENT");
            COMMENT_DELETE = getValue("COMMENT_DELETE");
            COMMENT_IN_PICTURE = getValue("COMMENT_IN_PICTURE");
            COMMENT_LIVING_TYPE = getValue("COMMENT_LIVING_TYPE");
            COMMENT_WITH_RES_CARD = getValue("COMMENT_WITH_RES_CARD");
            COMMON_SECTION = getValue("COMMON_SECTION");
            COMMON_SECTION_IN_PICTURE = getValue("COMMON_SECTION_IN_PICTURE");
            CONCERT_INFO = getValue("CONCERT_INFO");
            FESTIVAL = getValue("FESTIVAL");
            GENERAL = getValue("GENERAL");
            LIVE = getValue("LIVE");
            LIVE_RCMD = getValue("LIVE_RCMD");
            MORE_HOT_COMMENT = getValue("MORE_HOT_COMMENT");
            RECENT_COMMENT_SECTION = getValue("RECENT_COMMENT_SECTION");
            RELATIVE_TOPIC = getValue("RELATIVE_TOPIC");
            UNKONWN = getValue("UNKONWN");
            VIDEO = getValue("VIDEO");
            VIDEO_AD = getValue("VIDEO_AD");
            VIP_RCMD = getValue("VIP_RCMD");
        }

        private int getValue(String field) {
            try {
                return getStaticIntField(classTYPE, field);
            } catch (Throwable t) {
                return -2;
            }
        }
    }

    private class ListHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            List list = (List) param.args[0];
            if (list == null || list.isEmpty()) return;

            Object obj = list.get(0);
            if (obj == null) return;

            String name = obj.getClass().getSimpleName();
            if (!Constants.META_TYPE_COMMENT_LIST_ENTRY.equals(name)) return;

            loadPrefs();

            Iterator it = list.iterator();
            while (it.hasNext()) {
                Object entry = it.next();
                if (shouldRemove(entry)) {
                    it.remove();
                }
            }
        }
    }
}
