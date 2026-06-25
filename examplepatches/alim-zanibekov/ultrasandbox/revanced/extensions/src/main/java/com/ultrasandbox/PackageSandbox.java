package com.ultrasandbox;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.VpnService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PackageSandbox {

    private PackageSandbox() {}

    private static boolean isSystem(ApplicationInfo applicationInfo) {
        return applicationInfo != null && (
            applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
        ) != 0;
    }

    private static String callerPkg;

    public static PackageInfo getPackageInfo(
        PackageManager pm, String pkg, int flags
    ) throws NameNotFoundException {
        var info = pm.getPackageInfo(pkg, flags);
        if (callerPkg == null && info.applicationInfo != null) {
            try {
                var myUid = android.os.Process.myUid();
                if (info.applicationInfo.uid == myUid) callerPkg = pkg;
            } catch (Exception ignored) {}
        }
        if (pkg.equals(callerPkg) || isSystem(info.applicationInfo)) {
            return info;
        }
        throw new NameNotFoundException(pkg);
    }

    private static boolean isSelfOrSystem(ApplicationInfo ai) {
        return isSystem(ai) || (ai != null && ai.uid == android.os.Process.myUid());
    }

    public static List<PackageInfo> getInstalledPackages(PackageManager pm, int flags) {
        return pm.getInstalledPackages(flags).stream()
            .filter(it -> isSelfOrSystem(it.applicationInfo))
            .collect(Collectors.toList());
    }

    public static List<ApplicationInfo> getInstalledApplications(PackageManager pm, int flags) {
        return pm.getInstalledApplications(flags).stream()
            .filter(PackageSandbox::isSelfOrSystem)
            .collect(Collectors.toList());
    }

    public static List<ResolveInfo> queryIntentActivities(PackageManager pm, Intent intent, int flags) {
        return pm.queryIntentActivities(intent, flags).stream()
            .filter(it -> it.activityInfo != null && isSystem(it.activityInfo.applicationInfo))
            .collect(Collectors.toList());
    }

    public static List<ResolveInfo> queryIntentServices(PackageManager pm, Intent intent, int flags) {
        if (VpnService.SERVICE_INTERFACE.equals(intent.getAction())) return Collections.emptyList();
        return pm.queryIntentServices(intent, flags).stream()
            .filter(it -> it.serviceInfo != null && isSystem(it.serviceInfo.applicationInfo))
            .collect(Collectors.toList());
    }

    // API 33+ overloads (PackageInfoFlags / ApplicationInfoFlags / ResolveInfoFlags)
    @android.annotation.TargetApi(33)
    public static PackageInfo getPackageInfo(
        PackageManager pm, String pkg, PackageManager.PackageInfoFlags flags
    ) throws NameNotFoundException {
        var info = pm.getPackageInfo(pkg, flags);
        if (callerPkg == null && info.applicationInfo != null) {
            try {
                var myUid = android.os.Process.myUid();
                if (info.applicationInfo.uid == myUid) callerPkg = pkg;
            } catch (Exception ignored) {}
        }
        if (pkg.equals(callerPkg) || isSystem(info.applicationInfo)) {
            return info;
        }
        throw new NameNotFoundException(pkg);
    }

    @android.annotation.TargetApi(33)
    public static List<PackageInfo> getInstalledPackages(
        PackageManager pm, PackageManager.PackageInfoFlags flags
    ) {
        return pm.getInstalledPackages(flags).stream()
            .filter(it -> isSelfOrSystem(it.applicationInfo))
            .collect(Collectors.toList());
    }

    @android.annotation.TargetApi(33)
    public static List<ApplicationInfo> getInstalledApplications(
        PackageManager pm, PackageManager.ApplicationInfoFlags flags
    ) {
        return pm.getInstalledApplications(flags).stream()
            .filter(PackageSandbox::isSelfOrSystem)
            .collect(Collectors.toList());
    }

    @android.annotation.TargetApi(33)
    public static List<ResolveInfo> queryIntentActivities(
        PackageManager pm, Intent intent, PackageManager.ResolveInfoFlags flags
    ) {
        return pm.queryIntentActivities(intent, flags).stream()
            .filter(it -> it.activityInfo != null && isSystem(it.activityInfo.applicationInfo))
            .collect(Collectors.toList());
    }

    @android.annotation.TargetApi(33)
    public static List<ResolveInfo> queryIntentServices(
        PackageManager pm, Intent intent, PackageManager.ResolveInfoFlags flags
    ) {
        if (VpnService.SERVICE_INTERFACE.equals(intent.getAction())) return Collections.emptyList();
        return pm.queryIntentServices(intent, flags).stream()
            .filter(it -> it.serviceInfo != null && isSystem(it.serviceInfo.applicationInfo))
            .collect(Collectors.toList());
    }

    public static List<ProviderInfo> queryContentProviders(PackageManager pm, String process, int uid, int flags) {
        return pm.queryContentProviders(process, uid, flags).stream()
            .filter(it -> isSystem(it.applicationInfo))
            .collect(Collectors.toList());
    }
}
