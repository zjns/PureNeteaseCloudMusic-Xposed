package me.zjns.lovecloudmusic;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

@SuppressWarnings("unchecked")
public final class HookInfo {
    private final Pattern PATTERN_CLASSES_CHANNEL = Pattern.compile("^com\\.netease\\.cloudmusic\\.utils\\.[a-z]$");
    private ClassLoader loader;
    private boolean needUpdate = true;
    private Set<String> keys = new HashSet<>();
    private HashMap<String, String> hookInfoCache = new HashMap<>();

    HookInfo(ClassLoader classLoader) {
        setKeys();
        setClassLoader(classLoader);
        long before = System.currentTimeMillis();
        readHookInfo();
        long after = System.currentTimeMillis();
        Log.i("kofua", "readHookInfo: cost" + (after - before) + "ms");
    }

    private void setClassLoader(ClassLoader classLoader) {
        loader = classLoader;
    }

    private void setKeys() {
        keys.add(Constants.KEY_METHOD_CHANNEL);
    }

    private void readHookInfo() {
        try {
            Context context = Utils.getPackageContext(Constants.HOOK_PACKAGE_NAME);
            long lastUpdateTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
            File hookInfoFile = new File(context.getCacheDir(), Constants.FILE_HOOK_INFO);
            if (hookInfoFile.isFile() && hookInfoFile.canRead()) {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(hookInfoFile));
                if (in.readLong() == lastUpdateTime) {
                    HashMap<String, String> info = (HashMap<String, String>) in.readObject();
                    if (info.keySet().containsAll(keys)) {
                        hookInfoCache = info;
                        needUpdate = false;
                    }
                }
            }
        } catch (Exception e) {
            log(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void saveHookInfo(boolean hasExtraException) {
        Log.i("kofua", "saveHookInfo: needUpdate=" + needUpdate + "; hasExtraException=" + hasExtraException);

        if (hookInfoCache.keySet().containsAll(keys) && needUpdate && !hasExtraException) {
            try {
                Context context = Utils.getPackageContext(Constants.HOOK_PACKAGE_NAME);
                long lastUpdateTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
                File hookInfoFile = new File(context.getCacheDir(), Constants.FILE_HOOK_INFO);
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(hookInfoFile));
                out.writeLong(lastUpdateTime);
                out.writeObject(hookInfoCache);
                out.flush();
                out.close();
            } catch (Exception e) {
                log(e);
            }
        }
        if (!hookInfoCache.keySet().containsAll(keys) || hasExtraException) {
            try {
                Context context = Utils.getPackageContext(Constants.HOOK_PACKAGE_NAME);
                File hookInfoFile = new File(context.getCacheDir(), Constants.FILE_HOOK_INFO);
                if (hookInfoFile.exists()) {
                    hookInfoFile.delete();
                }
            } catch (Exception e) {
                log(e);
            }
        }
    }

    public Method getMethodChannel() {
        if (hookInfoCache.containsKey(Constants.KEY_METHOD_CHANNEL)) {
            String tempMethodName = hookInfoCache.get(Constants.KEY_METHOD_CHANNEL);
            String tempClassName = hookInfoCache.get(Constants.KEY_CLASS_CHANNEL);
            return findMethodExact(tempClassName, loader, tempMethodName, Context.class, String.class);
        }
        List<String> list = ClassHelper.getFilteredClasses(true, PATTERN_CLASSES_CHANNEL);
        for (String clazzName : list) {
            Class<?> clazz = findClass(clazzName, loader);
            Field[] fields = clazz.getDeclaredFields();
            if (fields.length == 1
                    && fields[0].getType() == String.class
                    && Modifier.isPrivate(fields[0].getModifiers())
                    && Modifier.isStatic(fields[0].getModifiers())) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getReturnType() == String.class
                            && method.getParameterTypes().length == 2
                            && method.getParameterTypes()[0] == Context.class
                            && method.getParameterTypes()[1] == String.class
                            && Modifier.isPublic(method.getModifiers())
                            && Modifier.isStatic(method.getModifiers())) {
                        if (!hookInfoCache.containsKey(Constants.KEY_METHOD_CHANNEL)) {
                            hookInfoCache.put(Constants.KEY_METHOD_CHANNEL, method.getName());
                            hookInfoCache.put(Constants.KEY_CLASS_CHANNEL, method.getDeclaringClass().getName());
                        }
                        return method;
                    }
                }
            }
        }
        return null;
    }

    public Object getProfile() {
        Class<?> NeteaseMusicUtils = findClass("com.netease.cloudmusic.utils.NeteaseMusicUtils", loader);
        Method mProfile = null;
        for (Method m : NeteaseMusicUtils.getDeclaredMethods()) {
            if (m.getReturnType() == Object.class
                    && m.getParameterTypes().length == 3
                    && m.getParameterTypes()[0] == Context.class
                    && m.getParameterTypes()[1] == String.class
                    && m.getParameterTypes()[2] == Boolean.TYPE) {
                mProfile = m;
                mProfile.setAccessible(true);
                break;
            }
        }
        if (mProfile == null) {
            return null;
        }
        try {
            Context context = Utils.getPackageContext(Constants.HOOK_PACKAGE_NAME);
            return mProfile.invoke(null, context, "Session.Profile", true);
        } catch (Throwable t) {
            return null;
        }
    }
}
