package me.zjns.lovecloudmusic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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
    private static final String KEY_HIDE_ICON = "hide_icon";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isInXposedEnvironment()) {
            if (!isModuleEnabled()) {
                checkState();
            }
        } else {
            if (!isModuleInExposedEnabled(this)) {
                checkState();
            }
        }
    }

    private boolean isModuleEnabled() {
        return false;
    }

    private boolean isModuleInExposedEnabled(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null!!");
        }

        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver == null) {
            return false;
        }

        Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
        Bundle result = contentResolver.call(uri, "active", null, null);
        if (result == null) {
            return false;
        }

        return result.getBoolean("active", false);
    }

    private boolean isInXposedEnvironment() {
        boolean flag = false;
        try {
            ClassLoader.getSystemClassLoader().loadClass("de.robv.android.xposed.XposedBridge");
            flag = true;
        } catch (ClassNotFoundException ignored) {
        }
        return flag;
    }

    private void checkState() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(R.string.msg_module_not_active)
                .setPositiveButton(R.string.btn_active, (dialog, which) -> {
                    if (isInXposedEnvironment()) {
                        openXposedInstaller();
                    } else {
                        openExposed();
                    }
                })
                .setNegativeButton(R.string.btn_ignore, null)
                .show();
    }

    private void openXposedInstaller() {
        String xposed = "de.robv.android.xposed.installer";
        if (Utils.isPackageInstalled(this, xposed)) {
            Intent intent = new Intent("de.robv.android.xposed.installer.OPEN_SECTION");
            if (getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
                intent = getPackageManager().getLaunchIntentForPackage(xposed);
            }
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("section", "modules")
                        .putExtra("fragment", 1)
                        .putExtra("module", Constants.MODULE_PACKAGE_NAME);
                startActivity(intent);
            }
        } else {
            Toast.makeText(this, R.string.toast_xposed_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void openExposed() {
        String exposed = "me.weishu.exp";
        if (Utils.isPackageInstalled(this, exposed)) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(exposed);
            if (intent != null) {
                startActivity(intent);
            }
        } else {
            Toast.makeText(this, R.string.toast_exposed_not_installed, Toast.LENGTH_SHORT).show();
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
            if (KEY_HIDE_ICON.equals(preference.getKey())) {
                changeIconStatus(!(Boolean) object);
            }
            return true;
        }

        private void changeIconStatus(boolean isShow) {
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
