package com.ultrasandbox;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressLint("MissingPermission")
public final class WifiBluetoothSandbox {

    private WifiBluetoothSandbox() {}

    public static List<ScanResult> getScanResults(WifiManager wm) {
        wm.getScanResults();
        return Collections.emptyList();
    }

    public static List<WifiConfiguration> getConfiguredNetworks(WifiManager wm) {
        wm.getConfiguredNetworks();
        return Collections.emptyList();
    }

    public static String getSSID(WifiInfo wifiInfo) {
        if (wifiInfo.getSSID() == null) return null;
        return "<unknown ssid>";
    }

    public static String getBSSID(WifiInfo wifiInfo) {
        if (wifiInfo.getBSSID() == null) return null;
        return "02:00:00:00:00:00";
    }

    public static String getMacAddress(WifiInfo wifiInfo) {
        if (wifiInfo.getMacAddress() == null) return null;
        return "02:00:00:00:00:00";
    }

    public static Set<BluetoothDevice> getBondedDevices(BluetoothAdapter adapter) {
        adapter.getBondedDevices();
        return Collections.emptySet();
    }
}
