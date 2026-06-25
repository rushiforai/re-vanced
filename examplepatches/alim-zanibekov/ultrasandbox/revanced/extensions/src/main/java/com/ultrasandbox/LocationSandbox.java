package com.ultrasandbox;

import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;

import java.util.Collections;
import java.util.List;

public class LocationSandbox {

    public static CellLocation getCellLocation(TelephonyManager tm) {
        return null;
    }

    public static List<CellInfo> getAllCellInfo(TelephonyManager tm) {
        return Collections.emptyList();
    }

    public static List<NeighboringCellInfo> getNeighboringCellInfo(TelephonyManager tm) {
        return Collections.emptyList();
    }
}
