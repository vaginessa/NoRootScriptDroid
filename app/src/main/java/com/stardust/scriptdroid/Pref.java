package com.stardust.scriptdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.stardust.autojs.runtime.accessibility.AutomatorConfig;

import java.util.concurrent.TimeUnit;

/**
 * Created by Stardust on 2017/1/31.
 */
public class Pref {

    private static final SharedPreferences DISPOSABLE_BOOLEAN = App.getApp().getSharedPreferences("DISPOSABLE_BOOLEAN", Context.MODE_PRIVATE);
    private static final String KEY_SERVER_ADDRESS = "Still love you...17.5.14";
    private static final String KEY_SHOULD_SHOW_ANNUNCIATION = "Sing about all the things you forgot, things you are not";
    private static final String KEY_FIRST_SHOW_AD = "En, Today is 17.7.7, but, I'm still love you so....";
    private static final String KEY_LAST_SHOW_AD_MILLIS = "But... it seems that...you will not come back any more...";
    private static SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.key_guard_mode))) {
                AutomatorConfig.setIsUnintendedGuardEnabled(sharedPreferences.getBoolean(getString(R.string.key_guard_mode), false));
            }
        }
    };

    static {
        AutomatorConfig.setIsUnintendedGuardEnabled(def().getBoolean(getString(R.string.key_guard_mode), false));
    }

    private static SharedPreferences def() {
        return PreferenceManager.getDefaultSharedPreferences(App.getApp());
    }

    private static boolean getDisposableBoolean(String key, boolean defaultValue) {
        boolean b = DISPOSABLE_BOOLEAN.getBoolean(key, defaultValue);
        if (b == defaultValue) {
            DISPOSABLE_BOOLEAN.edit().putBoolean(key, !defaultValue).apply();
        }
        return b;
    }

    public static boolean isFirstGoToAccessibilitySetting() {
        return getDisposableBoolean("I miss you so much ...", true);
    }

    public static int oldVersion() {
        return 0;
    }

    public static boolean isRecordVolumeControlEnable() {
        return def().getBoolean(getString(R.string.key_use_volume_control_record), false);
    }

    public static boolean isRunningVolumeControlEnabled() {
        return def().getBoolean(getString(R.string.key_use_volume_control_running), false);
    }

    public static boolean enableAccessibilityServiceByRoot() {
        return def().getBoolean(getString(R.string.key_enable_accessibility_service_by_root), false);
    }

    private static String getString(int id) {
        return App.getApp().getString(id);
    }

    public static int getMaxTextLengthForCodeCompletion() {
        try {
            return Integer.parseInt(def().getString(App.getApp().getString(R.string.key_max_length_for_code_completion), "2000"));
        } catch (NumberFormatException e) {
            return 2000;
        }
    }

    public static boolean isFirstUsing() {
        return getDisposableBoolean("isFirstUsing", true);
    }

    static {
        def().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public static boolean isEditActivityFirstUsing() {
        return getDisposableBoolean("Still Love Eating 17.4.6", true);
    }

    public static String getServerAddressOrDefault(String defaultAddress) {
        return def().getString(KEY_SERVER_ADDRESS, defaultAddress);
    }

    public static void saveServerAddress(String address) {
        def().edit().putString(KEY_SERVER_ADDRESS, address).apply();
    }

    public static boolean shouldShowAnnunciation() {
        return getDisposableBoolean(KEY_SHOULD_SHOW_ANNUNCIATION, true);
    }

    public static boolean shouldShowAd() {
        String adShowingMode = def().getString(getString(R.string.key_ad_showing_mode), "Default");
        switch (adShowingMode) {
            case "Default":
                return true;
            case "OncePerDay":
                long lastShowMillis = def().getLong(KEY_LAST_SHOW_AD_MILLIS, 0);
                if (System.currentTimeMillis() - lastShowMillis < TimeUnit.DAYS.toMillis(1)) {
                    return false;
                }
                def().edit().putLong(KEY_LAST_SHOW_AD_MILLIS, System.currentTimeMillis()).apply();
                return true;
            case "Off":
                return false;
        }
        return true;
    }

    public static boolean isFirstShowingAd() {
        return getDisposableBoolean(KEY_FIRST_SHOW_AD, true);
    }

    public static boolean isRecordWithRootEnabled() {
        //always return true after version 3.0.0
        //record without root has been deprecated
        return true;
    }

    public static boolean isRecordToastEnabled() {
        return def().getBoolean(getString(R.string.key_record_toast), true);
    }

    public static boolean rootRecordGeneratesBinary() {
        return def().getString(getString(R.string.key_root_record_out_file_type), "binary")
                .equals("binary");
    }

    public static boolean isObservingKeyEnabled() {
        return def().getBoolean(getString(R.string.key_enable_observe_key), false);
    }

    public static boolean isStableModeEnabled() {
        return def().getBoolean(getString(R.string.key_stable_mode), false);
    }

    public static String getDocumentationUrl() {
        return "file:///android_asset/docs/";
    }
}
