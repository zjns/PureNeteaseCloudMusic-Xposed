package me.zjns.lovecloudmusic;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

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

public final class Utils {

    public static Context getPackageContext(String packageName) throws PackageManager.NameNotFoundException {
        Object currentThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        Context systemContext = (Context) callMethod(currentThread, "getSystemContext");
        return systemContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
    }

    public static String getPackageVersionName(String packageName) {
        Object thread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        Context context = (Context) callMethod(thread, "getSystemContext");
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static Application getCurrentApplication() {
        Object thread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        return (Application) callMethod(thread, "getApplication");
    }

    public static boolean isPackageInstalled(Context context, String pkgName) {
        boolean flag = false;
        try {
            context.getPackageManager().getApplicationInfo(pkgName, 0);
            flag = true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return flag;
    }

    public static String getCurrentProcessName(Context context) {
        String currentProcessName = "";
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = null;
        try {
            runningProcesses = manager.getRunningAppProcesses();
        } catch (SecurityException ignored) {
        }
        if (runningProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.pid == pid) {
                    currentProcessName = processInfo.processName;
                    break;
                }
            }
        }
        return currentProcessName;
    }

    public static boolean isInMainProcess(Context context) {
        String current = getCurrentProcessName(context);
        return current.equals(context.getPackageName());
    }

    public static void showToast(String message, boolean longTime, long delay) {
        int duration = longTime ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        new Handler(Looper.getMainLooper()).postDelayed(() -> Toast.makeText(getCurrentApplication(), message, duration).show(), delay);
    }

    @SuppressWarnings("all")
    public static Method findMethodByExactParameters(Class<?> clazz, String methodName, Class<?> returnType, Class<?>... parameterTypes) {
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

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String clazzName, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(clazzName, classLoader, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(String clazzName, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookConstructor(clazzName, classLoader, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookConstructor(clazz, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        try {
            return XposedBridge.hookMethod(hookMethod, callback);
        } catch (Throwable t) {
            log(t);
            return null;
        }
    }
}
