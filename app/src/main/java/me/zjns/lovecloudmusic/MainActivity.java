package me.zjns.lovecloudmusic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by YiTry on 2018/1/27
 */

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkState();
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    private boolean isModuleEnabled() {
        return false;
    }

    private void checkState() {
        if (!isModuleEnabled()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("模块未激活，请先激活模块并重启手机！")
                    .setPositiveButton("激活", (dialog, id) -> openXposed())
                    .setNegativeButton("取消", (dialog, id) -> finish())
                    .show();
        }
    }

    private void openXposed() {
        if (isXposedInstalled()) {
            Intent intent = new Intent("de.robv.android.xposed.installer.OPEN_SECTION");
            PackageManager packageManager = getPackageManager();
            if (packageManager == null) {
                return;
            }
            if (packageManager.queryIntentActivities(intent, 0).isEmpty()) {
                intent = packageManager.getLaunchIntentForPackage("de.robv.android.xposed.installer");
            }
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("section", "modules")
                        .putExtra("fragment", 1)
                        .putExtra("module", MainActivity.class.getPackage().getName());
                startActivity(intent);
            }
        } else {
            Toast.makeText(this, "未安装 XposedInstaller !", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isXposedInstalled() {
        try {
            getPackageManager().getApplicationInfo("de.robv.android.xposed.installer", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.xposed_prefs);
            fixFolderPermission();
            WorldReadableHelper.getInstance(getActivity());
            SwitchPreference hideAppIcon = (SwitchPreference) getPreferenceScreen().findPreference("hide_icon");
            hideAppIcon.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object object) {
            if (preference.getKey().equals("hide_icon")) {
                ChangeIconStatus(!(boolean) object);
            }
            return true;
        }

        private void ChangeIconStatus(boolean isShow) {
            final ComponentName aliasName = new ComponentName(getActivity(), MainActivity.class.getName() + "Alias");
            final PackageManager packageManager = getActivity().getPackageManager();
            int status = isShow ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            if (packageManager.getComponentEnabledSetting(aliasName) != status) {
                packageManager.setComponentEnabledSetting(aliasName, status, PackageManager.DONT_KILL_APP);
            }
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @SuppressLint("SetWorldReadable")
        private void fixFolderPermission() {
            File dataDir = new File(getActivity().getApplicationInfo().dataDir);
            File prefsDir = new File(dataDir, "shared_prefs");
            //File prefsFile = new File(prefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
            if (prefsDir.exists()) {
                for (File file : new File[]{dataDir, prefsDir}) {
                    file.setReadable(true, false);
                    file.setExecutable(true, false);
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            ArrayList<String> keys = new ArrayList<>();
            keys.add("remove_comment_ad");
            keys.add("remove_comment_video");
            keys.add("remove_comment_topic");
            keys.add("remove_comment_concert_info");
            keys.add("zero_point_video");
            keys.add("enable_vip_feature");
            keys.add("disable_sign_jump_to_small");
            if (keys.contains(key)) {
                Toast.makeText(getActivity(), R.string.work_right_now, Toast.LENGTH_SHORT).show();
            } else if (!key.equals("hide_icon")) {
                Toast.makeText(getActivity(), R.string.work_after_reload, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
