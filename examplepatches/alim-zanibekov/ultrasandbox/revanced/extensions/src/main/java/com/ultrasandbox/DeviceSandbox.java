package com.ultrasandbox;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.util.Arrays;

@SuppressLint("MissingPermission")
public final class DeviceSandbox {

    private DeviceSandbox() {}

    // Placeholder replaced at patch time.
    private static final String ANDROID_ID = "__ULTRASANDBOX_ANDROID_ID_PLACEHOLDER__";

    public static String settingsGetString(ContentResolver cr, String name) {
        if ("android_id".equals(name)) return ANDROID_ID;
        return Settings.Secure.getString(cr, name);
    }

    public static String getDeviceId(TelephonyManager tm) {
        tm.getDeviceId();
        return null;
    }

    public static String getDeviceIdSlot(TelephonyManager tm, int slot) {
        tm.getDeviceId(slot);
        return null;
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.O)
    public static String getImei(TelephonyManager tm) {
        tm.getImei();
        return null;
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.O)
    public static String getImeiSlot(TelephonyManager tm, int slot) {
        tm.getImei(slot);
        return null;
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.O)
    public static String getMeid(TelephonyManager tm) {
        tm.getMeid();
        return null;
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.O)
    public static String getMeidSlot(TelephonyManager tm, int slot) {
        tm.getMeid(slot);
        return null;
    }

    public static String getSubscriberId(TelephonyManager tm) {
        tm.getSubscriberId();
        return null;
    }

    public static String getSimSerialNumber(TelephonyManager tm) {
        tm.getSimSerialNumber();
        return null;
    }

    public static String getLine1Number(TelephonyManager tm) {
        tm.getLine1Number();
        return null;
    }

    public static Account[] getAccounts(AccountManager am) {
        return filterGoogle(am.getAccounts());
    }

    public static Account[] getAccountsByType(AccountManager am, String type) {
        if ("com.google".equals(type)) return am.getAccountsByType(type);
        return new Account[0];
    }

    private static Account[] filterGoogle(Account[] accounts) {
        if (accounts == null) return new Account[0];
        return Arrays.stream(accounts)
            .filter(a -> "com.google".equals(a.type))
            .toArray(Account[]::new);
    }

    public static ClipData getPrimaryClip(ClipboardManager cm) {
        return null;
    }

    public static boolean hasPrimaryClip(ClipboardManager cm) {
        return false;
    }
}
