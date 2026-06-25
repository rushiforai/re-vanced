package com.ultrasandbox;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContentSandbox {

    private static final Set<String> BLOCKED_AUTHORITIES = new HashSet<>(List.of(
        "com.android.contacts", "contacts", "call_log", "icc",
        "sms", "mms", "mms-sms", "com.android.calendar", "calendar",
        "telephony", "com.android.browser", "browser"
    ));

    private static boolean isBlocked(Uri uri) {
        var authority = uri.getAuthority();
        if (authority == null) return false;
        for (String blocked : BLOCKED_AUTHORITIES) {
            if (authority.equals(blocked) || authority.startsWith(blocked + ".")) return true;
        }
        return false;
    }

    public static Cursor query(ContentResolver cr, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder) {
        if (isBlocked(uri)) return null;
        return cr.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public static Cursor queryWithSignal(ContentResolver cr, Uri uri, String[] projection,
                                         String selection, String[] selectionArgs,
                                         String sortOrder, CancellationSignal signal) {
        if (isBlocked(uri)) return null;
        return cr.query(uri, projection, selection, selectionArgs, sortOrder, signal);
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.O)
    public static Cursor queryWithBundle(ContentResolver cr, Uri uri, String[] projection,
                                         Bundle queryArgs, CancellationSignal signal) {
        if (isBlocked(uri)) return null;
        return cr.query(uri, projection, queryArgs, signal);
    }
}
