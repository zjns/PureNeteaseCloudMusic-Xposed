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
    private static ClassLoader loader;
    private static boolean needUpdate = true;
    private static List<String> keys = new ArrayList<>();
    private static HashMap<String, String> hookInfoCache = new HashMap<>();

    static void setClassLoader(ClassLoader classLoader) {
        loader = classLoader;
    }

    static void setKeys(String version) {
        keys.clear();
        keys.add("method_channel");
        if (version.compareTo("4.2.1") > 0) {
            keys.add("class_CommentListEntry");
            if (version.compareTo("4.3.5") >= 0) {
                keys.add("method_is_weekend");
                keys.add("method_user_group");
                if (version.compareTo("5.3.0") >= 0) {
                    keys.add("class_VideoTimelineData");
                }
            }
        }
    }

    static void readHookInfo() {
        try {
            Context context = Utils.getPackageContext(HookInit.HOOK_PACKAGE_NAME);
            long lastUpdateTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
            File hookInfoFile = new File(context.getCacheDir(), "HookInfo.dat");
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

    static void saveHookInfo(boolean hasExtraException) {
        if (needUpdate && hookInfoCache.keySet().containsAll(keys) && !hasExtraException) {
            try {
                Context context = Utils.getPackageContext(HookInit.HOOK_PACKAGE_NAME);
                long lastUpdateTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
                File hookInfoFile = new File(context.getCacheDir(), "HookInfo.dat");
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
                Context context = Utils.getPackageContext(HookInit.HOOK_PACKAGE_NAME);
                File hookInfoFile = new File(context.getCacheDir(), "HookInfo.dat");
                if (hookInfoFile.exists()) {
                    hookInfoFile.delete();
                }
            } catch (Exception e) {
                log(e);
            }
        }
    }

    static Method getMethodIsWeekend() {
        if (hookInfoCache.containsKey("method_is_weekend")) {
            String tempMethodName = hookInfoCache.get("method_is_weekend");
            String tempClassName = hookInfoCache.get("class_is_weekend");
            return findMethodExact(tempClassName, loader, tempMethodName);
        }
        String clazzName = getDateUtilClassName();
        if (clazzName == null) return null;
        for (Method method : findClass(clazzName, loader).getDeclaredMethods()) {
            if (method.getReturnType() == Boolean.TYPE
                    && method.getParameterTypes().length == 0
                    && Modifier.isPublic(method.getModifiers())
                    && Modifier.isStatic(method.getModifiers())) {
                if (!hookInfoCache.containsKey("method_is_weekend")) {
                    hookInfoCache.put("method_is_weekend", method.getName());
                    hookInfoCache.put("class_is_weekend", method.getDeclaringClass().getName());
                }
                return method;
            }
        }
        return null;
    }

    static Method getMethodUserGroup() {
        if (hookInfoCache.containsKey("method_user_group")) {
            String tempMethodName = hookInfoCache.get("method_user_group");
            String tempClassName = hookInfoCache.get("class_user_group");
            return findMethodExact(tempClassName, loader, tempMethodName, String.class);
        }
        String clazzName = getStateUtilClassName();
        if (clazzName == null) return null;
        for (Method method : findClass(clazzName, loader).getDeclaredMethods()) {
            if (method.getReturnType() == String.class
                    && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0] == String.class) {
                if (!hookInfoCache.containsKey("method_user_group")) {
                    hookInfoCache.put("method_user_group", method.getName());
                    hookInfoCache.put("class_user_group", method.getDeclaringClass().getName());
                }
                return method;
            }
        }
        return null;
    }

    static Method getMethodChannel() {
        if (hookInfoCache.containsKey("method_channel")) {
            String tempMethodName = hookInfoCache.get("method_channel");
            String tempClassName = hookInfoCache.get("class_channel");
            return findMethodExact(tempClassName, loader, tempMethodName, Context.class, String.class);
        }
        Pattern pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.utils\\.[a-z]$");
        List<String> list = ClassHelper.getFilteredClasses(true, pattern);
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
                        if (!hookInfoCache.containsKey("method_channel")) {
                            hookInfoCache.put("method_channel", method.getName());
                            hookInfoCache.put("class_channel", method.getDeclaringClass().getName());
                        }
                        return method;
                    }
                }
            }
        }
        return null;
    }

    static Method getInnerFragmentMethod(String targetType) {
        Pattern pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.fragment\\.[a-z]+\\$[a-z1-9]+$");
        return getFragmentMethodBase(targetType, pattern);
    }

    static Method getFragmentMethod(String targetType) {
        Pattern pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.fragment\\.[a-z]+$");
        return getFragmentMethodBase(targetType, pattern);
    }

    static Object getProfile() {
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
            Context context = Utils.getPackageContext(HookInit.HOOK_PACKAGE_NAME);
            return mProfile.invoke(null, context, "Session.Profile", true);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method getFragmentMethodBase(String targetType, Pattern pattern) {
        String methodName;
        if (CloudMusic.versionName.compareTo("5.4.0") > 0 && targetType.equals("CommentListEntry")) {
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

    private static String getDateUtilClassName() {
        Pattern pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.utils\\.[a-z]{2}$");
        List<String> list = ClassHelper.getFilteredClasses(true, pattern);
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

    private static String getStateUtilClassName() {
        Pattern pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.module\\.[a-z]\\.[a-z]$");
        List<String> list = ClassHelper.getFilteredClasses(true, pattern);
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

    static List<String> getInnerFragmentClassName(String type) {
        Pattern pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.fragment\\.[a-z]+\\$[1-9]+$");
        List<String> list = ClassHelper.getFilteredClasses(true, pattern);
        List<String> classNameList = new ArrayList<>();
        for (String clazzName : list) {
            Class<?> clazz = findClass(clazzName, loader);
            if (findInnerFragmentClass(clazz, type)) {
                classNameList.add(clazzName);
            }
        }
        return classNameList;
    }

    private static boolean findInnerFragmentClass(Class<?> clazz, String targetType) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals("a") && method.getReturnType() == List.class) {
                Type type = method.getGenericReturnType();
                if (type instanceof ParameterizedType) {
                    Type[] types = ((ParameterizedType) type).getActualTypeArguments();
                    if (types.length == 1
                            && types[0] instanceof Class
                            && types[0].toString().endsWith(targetType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
