package me.zjns.lovecloudmusic;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

@SuppressWarnings("unchecked")
final class HookInfo {
    private ClassLoader loader;
    private boolean needUpdate = true;
    private List<String> keys = new ArrayList<>();
    private HashMap<String, String> hookInfoCache = new HashMap<>();
    private final Pattern PATTERN_CLASSES_CHANNEL = Pattern.compile("^com\\.netease\\.cloudmusic\\.utils\\.[a-z]$");
    private final Pattern PATTERN_CLASSES_INNER_FRAGMENT = Pattern.compile("^com\\.netease\\.cloudmusic\\.fragment\\.[a-z]+\\$[a-z1-9]+$");
    private final Pattern PATTERN_CLASSES_FRAGMENT = Pattern.compile("^com\\.netease\\.cloudmusic\\.fragment\\.[a-z]+$");
    private final Pattern PATTERN_CLASSES_DATE_UTIL = Pattern.compile("^com\\.netease\\.cloudmusic\\.utils\\.[a-z]{2}$");
    private final Pattern PATTERN_CLASSES_STATE_UTIL = Pattern.compile("^com\\.netease\\.cloudmusic\\.module\\.[a-z]\\.[a-z]$");


    HookInfo(String version, ClassLoader classLoader) {
        setKeys(version);
        setClassLoader(classLoader);
        readHookInfo();
    }

    private void setClassLoader(ClassLoader classLoader) {
        loader = classLoader;
    }

    private void setKeys(String version) {
        keys.clear();
        keys.add(Constants.KEY_METHOD_CHANNEL);
        if (version.compareTo(Constants.CM_VERSION_421) > 0) {
            keys.add(Constants.KEY_CLASS_COMMENT_LIST_ENTRY);
            if (version.compareTo(Constants.CM_VERSION_435) >= 0) {
                keys.add(Constants.KEY_METHOD_IS_WEEKEND);
                keys.add(Constants.KEY_METHOD_USER_GROUP);
                if (version.compareTo(Constants.CM_VERSION_530) >= 0) {
                    keys.add(Constants.KEY_CLASS_VIDEO_TIMELINE_DATA);
                }
            }
        }
    }

    private void readHookInfo() {
        try {
            Context context = Utils.getPackageContext(Constants.HOOK_PACKAGE_NAME);
            long lastUpdateTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
            File hookInfoFile = new File(context.getCacheDir(), Constants.FILE_HOOK_INFO);
            if (hookInfoFile.isFile() && hookInfoFile.canRead()) {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(hookInfoFile));
                if (in.readLong() == lastUpdateTime) {
                    HashMap<String, String> map = (HashMap<String, String>) in.readObject();
                    if (map.keySet().containsAll(keys)) {
                        hookInfoCache = map;
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
        if (needUpdate && hookInfoCache.keySet().containsAll(keys) && !hasExtraException) {
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

    Method getMethodIsWeekend() {
        if (hookInfoCache.containsKey(Constants.KEY_METHOD_IS_WEEKEND)) {
            String tempMethodName = hookInfoCache.get(Constants.KEY_METHOD_IS_WEEKEND);
            String tempClassName = hookInfoCache.get(Constants.KEY_CLASS_IS_WEEKEND);
            return findMethodExact(tempClassName, loader, tempMethodName);
        }
        String clazzName = getDateUtilClassName();
        if (clazzName == null) return null;
        for (Method method : findClass(clazzName, loader).getDeclaredMethods()) {
            if (method.getReturnType() == Boolean.TYPE
                    && method.getParameterTypes().length == 0
                    && Modifier.isPublic(method.getModifiers())
                    && Modifier.isStatic(method.getModifiers())) {
                if (!hookInfoCache.containsKey(Constants.KEY_METHOD_IS_WEEKEND)) {
                    hookInfoCache.put(Constants.KEY_METHOD_IS_WEEKEND, method.getName());
                    hookInfoCache.put(Constants.KEY_CLASS_IS_WEEKEND, method.getDeclaringClass().getName());
                }
                return method;
            }
        }
        return null;
    }

    Method getMethodUserGroup() {
        if (hookInfoCache.containsKey(Constants.KEY_METHOD_USER_GROUP)) {
            String tempMethodName = hookInfoCache.get(Constants.KEY_METHOD_USER_GROUP);
            String tempClassName = hookInfoCache.get(Constants.KEY_CLASS_USER_GROUP);
            return findMethodExact(tempClassName, loader, tempMethodName, String.class);
        }
        String clazzName = getStateUtilClassName();
        if (clazzName == null) return null;
        for (Method method : findClass(clazzName, loader).getDeclaredMethods()) {
            if (method.getReturnType() == String.class
                    && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0] == String.class) {
                if (!hookInfoCache.containsKey(Constants.KEY_METHOD_USER_GROUP)) {
                    hookInfoCache.put(Constants.KEY_METHOD_USER_GROUP, method.getName());
                    hookInfoCache.put(Constants.KEY_CLASS_USER_GROUP, method.getDeclaringClass().getName());
                }
                return method;
            }
        }
        return null;
    }

    Method getMethodChannel() {
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

    Method getInnerFragmentMethod(String targetType) {
        return getFragmentMethodBase(targetType, PATTERN_CLASSES_INNER_FRAGMENT);
    }

    Method getFragmentMethod(String targetType) {
        return getFragmentMethodBase(targetType, PATTERN_CLASSES_FRAGMENT);
    }

    Object getProfile() {
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

    private Method getFragmentMethodBase(String targetType, Pattern pattern) {
        String methodName;
        if (CloudMusic.versionName.compareTo(Constants.CM_VERSION_540) > 0
                && Constants.META_TYPE_COMMENT_LIST_ENTRY.equals(targetType)) {
            methodName = "loadListData";
        } else {
            methodName = "a";
        }
        if (hookInfoCache.containsKey("class_" + targetType)) {
            String tempClassName = hookInfoCache.get("class_" + targetType);
            Class<?> clazz = findClass(tempClassName, loader);
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getReturnType() == List.class) {
                    return method;
                }
            }
        }
        List<String> list = ClassHelper.getFilteredClasses(true, pattern);
        for (String clazzName : list) {
            Class<?> clazz = findClass(clazzName, loader);
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getReturnType() == List.class) {
                    Type type = method.getGenericReturnType();
                    if (type instanceof ParameterizedType) {
                        Type[] types = ((ParameterizedType) type).getActualTypeArguments();
                        if (types.length == 1
                                && types[0] instanceof Class
                                && types[0].toString().endsWith(targetType)) {
                            if (!hookInfoCache.containsKey("class_" + targetType)) {
                                hookInfoCache.put("class_" + targetType, method.getDeclaringClass().getName());
                            }
                            return method;
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getDateUtilClassName() {
        List<String> list = ClassHelper.getFilteredClasses(true, PATTERN_CLASSES_DATE_UTIL);
        for (String clazzName : list) {
            Class<?> clazz = findClass(clazzName, loader);
            Field[] fields = clazz.getDeclaredFields();
            if (fields.length != 0) {
                int count = 0;
                for (Field field : fields) {
                    if (field.getType() == ThreadLocal.class) {
                        ++count;
                    }
                }
                if (count == 6) {
                    return clazzName;
                }
            }
        }
        return null;
    }

    private String getStateUtilClassName() {
        List<String> list = ClassHelper.getFilteredClasses(true, PATTERN_CLASSES_STATE_UTIL);
        for (String clazzName : list) {
            Class<?> clazz = findClass(clazzName, loader);
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == ConcurrentMap.class) {
                    return clazzName;
                }
            }
        }
        return null;
    }
}
