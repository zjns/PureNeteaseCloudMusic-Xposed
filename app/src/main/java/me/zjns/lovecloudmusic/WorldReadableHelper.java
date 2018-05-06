package me.zjns.lovecloudmusic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;

import java.io.File;

/**
 * Created by YiTry on 2018/1/28
 */
//@SuppressWarnings("all")
class WorldReadableHelper {
    private static Context mContext;
    private static WorldReadableHelper mInstance;
    private FileObserver mFileObserver;
    private static Handler mHandler;

    private WorldReadableHelper(Context context) {
        mContext = context;
        mHandler = new Handler();
        registerFileObserver();
    }

    static void getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new WorldReadableHelper(context);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetWorldReadable")
    private void registerFileObserver() {
        mFileObserver = new FileObserver(mContext.getApplicationInfo().dataDir + "/shared_prefs", FileObserver.CLOSE_WRITE) {
            @Override
            public void onEvent(int event, String path) {
                mHandler.postDelayed(() -> {
                    File dataDir = new File(mContext.getApplicationInfo().dataDir);
                    File prefsDir = new File(dataDir, "shared_prefs");
                    File prefsFile = new File(prefsDir, mContext.getPackageName() + "_preferences.xml");
                    if (prefsFile.exists()) {
                        prefsFile.setReadable(true, false);
                    }
                }, 100);
            }
        };
        mFileObserver.startWatching();
    }
}
