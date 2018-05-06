package me.zjns.lovecloudmusic;

import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Created by YiTry on 2018/3/9
 */

final class ClassHelper {

    static List<String> getFilteredClasses(boolean useCache, Pattern pattern) {
        try {
            return getFilteredClasses(useCache, pattern, null);
        } catch (PackageManager.NameNotFoundException e) {
            return new ArrayList<>();
        }
    }

    private static List<String> getFilteredClasses(boolean useCache, Pattern pattern, Comparator<String> comparator) throws PackageManager.NameNotFoundException {
        List<String> list = filterList(MultiDexHelper.getAllClasses(useCache), pattern);
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
}
