package me.zjns.lovecloudmusic.hooker;

import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import me.zjns.lovecloudmusic.Constants;
import me.zjns.lovecloudmusic.hooker.base.BaseHook;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;
import static me.zjns.lovecloudmusic.Utils.findAndHookMethod;

/**
 * @author kofua
 * @date 2019/11/3 下午 3:01
 */
public class VideoFlowHook extends BaseHook {

    private DATA_TYPES mTypes;
    private boolean removeAd;
    private boolean removeLive;

    @Override
    protected void hookMain() {
        mTypes = new DATA_TYPES(loader);
        removeVideoFlowAd();
    }

    @Override
    protected void initPrefs() {
        removeAd = prefs.getBoolean("remove_video_flow_ad", false);
        removeLive = prefs.getBoolean("remove_video_flow_live", false);
    }

    private void removeVideoFlowAd() {
        findAndHookMethod("androidx.loader.content.AsyncTaskLoader$LoadTask", loader,
                "onPostExecute", Object.class, new VideoListHook());
    }

    private class VideoListHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Object arg = param.args[0];
            if (!(arg instanceof List)) return;

            List list = (List) arg;
            if (list.isEmpty()) return;

            Object o = list.get(0);
            if (o == null) return;

            String name = o.getClass().getSimpleName();
            if (!Constants.META_TYPE_VIDEO_TIMELINE_DATA.equals(name)) return;

            loadPrefs();

            Iterator it = list.iterator();
            while (it.hasNext()) {
                if (shouldRemove(it.next())) {
                    it.remove();
                }
            }
        }

        private boolean shouldRemove(Object entity) {
            int type = getIntField(entity, "type");
            return type == mTypes.TIMELINE_BANNER_AD && removeAd
                    || type == mTypes.TIMELINE_PIC_AD && removeAd
                    || type == mTypes.VIDEO_AD && removeAd
                    || isVideoGameAd(entity) && removeAd
                    || type == mTypes.LIVE && removeLive
                    || type == mTypes.LIVE_LIST && removeLive;
        }

        private boolean isVideoGameAd(Object entity) {
            boolean isVideoGameAd = false;
            try {
                isVideoGameAd = getBooleanField(entity, "isVideoGameAd");
            } catch (Throwable ignored) {
            }
            return isVideoGameAd;
        }
    }

    private static class DATA_TYPES {
        private static final String CLASS_NAME = "com.netease.cloudmusic.meta.VideoTimelineData$DATA_TYPES";

        private Class classType;

        private final int BANNER;
        private final int DISPLAYED;
        private final int LIVE;
        private final int LIVE_LIST;
        private final int MV;
        private final int MVBILLBOARD;
        private final int MVSELECTED;
        private final int MVTITLE_MORE;
        private final int MVTITLE_SELECTED;
        private final int MV_VIDEO;
        private final int PIC_ACTIVITY;
        private final int PREFERENCE;
        private final int PROFILE_RMD;
        private final int RELATED_SONG;
        private final int RELATED_SONG_EMPTY;
        private final int SHOWROOM;
        private final int TALENT_LIVE;
        private final int TIMELINE_BANNER_AD;
        private final int TIMELINE_PIC_AD;
        private final int UNKNOWN;
        private final int VIDEO;
        private final int VIDEO_AD;

        DATA_TYPES(ClassLoader loader) {
            classType = findClassIfExists(CLASS_NAME, loader);

            BANNER = getValue("BANNER");
            DISPLAYED = getValue("DISPLAYED");
            LIVE = getValue("LIVE");
            LIVE_LIST = getValue("LIVE_LIST");
            MV = getValue("MV");
            MVBILLBOARD = getValue("MVBILLBOARD");
            MVSELECTED = getValue("MVSELECTED");
            MVTITLE_MORE = getValue("MVTITLE_MORE");
            MVTITLE_SELECTED = getValue("MVTITLE_SELECTED");
            MV_VIDEO = getValue("MV_VIDEO");
            PIC_ACTIVITY = getValue("PIC_ACTIVITY");
            PREFERENCE = getValue("PREFERENCE");
            PROFILE_RMD = getValue("PROFILE_RMD");
            RELATED_SONG = getValue("RELATED_SONG");
            RELATED_SONG_EMPTY = getValue("RELATED_SONG_EMPTY");
            SHOWROOM = getValue("SHOWROOM");
            TALENT_LIVE = getValue("TALENT_LIVE");
            TIMELINE_BANNER_AD = getValue("TIMELINE_BANNER_AD");
            TIMELINE_PIC_AD = getValue("TIMELINE_PIC_AD");
            UNKNOWN = getValue("UNKNOWN");
            VIDEO = getValue("VIDEO");
            VIDEO_AD = getValue("VIDEO_AD");
        }

        private int getValue(String field) {
            try {
                return getStaticIntField(classType, field);
            } catch (Throwable ignored) {
            }
            return -1;
        }
    }
}
