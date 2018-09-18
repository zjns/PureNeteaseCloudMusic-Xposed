package me.zjns.lovecloudmusic;

import android.content.pm.PackageManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Created by YiTry on 2018/3/9
 */

final class ClassHelper {
    private static WeakReference<List<String>> allClasses = new WeakReference<>(null);

    static List<String> getFilteredClasses(boolean useCache, Pattern pattern) {
        try {
            return getFilteredClasses(useCache, pattern, null);
        } catch (PackageManager.NameNotFoundException e) {
            return new ArrayList<>();
        }
    }

    private static List<String> getFilteredClasses(boolean useCache, Pattern pattern, Comparator<String> comparator) throws PackageManager.NameNotFoundException {
        List<String> list = filterList(getAllClasses(useCache), pattern);
        Collections.sort(list, comparator);
        return list;
    }

    private static List<String> filterList(List<String> list, Pattern pattern) {
        List<String> filteredList = new ArrayList<>();
        for (String curStr : list) {
            if (pattern.matcher(curStr).find()) {
                filteredList.add(curStr);
            }
        }
        return filteredList;
    }

    private static List<String> getAllClasses(boolean useCache) throws PackageManager.NameNotFoundException {
        List<String> list = allClasses.get();
        if (list == null) {
            list = MultiDexHelper.getAllClasses(useCache);
            allClasses = new WeakReference<>(list);
        }
        return list;
    }
}
