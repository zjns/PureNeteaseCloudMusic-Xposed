package me.zjns.lovecloudmusic;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by YiTry on 2018/3/14
 */

final class Utils {

    static Context getPackageContext(String packageName) throws PackageManager.NameNotFoundException {
        Object currentThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        Context systemContext = (Context) callMethod(currentThread, "getSystemContext");
        return systemContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
    }

    static String getPackageVersionName(String packageName) {
        Object thread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        Context context = (Context) callMethod(thread, "getSystemContext");
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings("all")
    static Method findMethodByExactParameters(Class<?> clazz, String methodName, Class<?> returnType, Class<?>... parameterTypes) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodName) || method.getReturnType() != returnType)
                continue;
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            if (methodParameterTypes.length != parameterTypes.length)
                continue;
            boolean match = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (methodParameterTypes[i] != parameterTypes[i]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new RuntimeException("can't find method " + methodName + " in class " + clazz.getName());
    }

    static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }

    static XC_MethodHook.Unhook findAndHookMethod(String clazzName, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(clazzName, classLoader, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }

    static XC_MethodHook.Unhook findAndHookConstructor(String clazzName, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookConstructor(clazzName, classLoader, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }

    static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        try {
            return XposedBridge.hookMethod(hookMethod, callback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }
}
