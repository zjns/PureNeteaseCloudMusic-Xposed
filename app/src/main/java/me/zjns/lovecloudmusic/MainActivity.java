package me.zjns.lovecloudmusic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Toast;

import java.io.File;

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
                    .setNegativeButton("忽略", null)
                    .show();
        }
    }

    private void openXposed() {
        if (isXposedInstalled()) {
            Intent intent = new Intent("de.robv.android.xposed.installer.OPEN_SECTION");
            if (getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
                intent = getPackageManager().getLaunchIntentForPackage("de.robv.android.xposed.installer");
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

    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.xposed_prefs);
            setWorldReadable();
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
        private void setWorldReadable() {
            File prefsDir = new File(getActivity().getApplicationInfo().dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            setWorldReadable();
        }
    }
}
