package com.basti564.dreamgrid.platforms;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;

public class VRPlatform extends AbstractPlatform {

    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        ArrayList<ApplicationInfo> installedApps = new ArrayList<>();
        if (!isSupported(context)) {
            return installedApps;
        }

        PackageManager packageManager = context.getPackageManager();
        for (ApplicationInfo appInfo : packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (isVirtualRealityApp(appInfo)) {
                installedApps.add(appInfo);
            }
        }
        return installedApps;
    }

    @Override
    public boolean isSupported(Context context) {
        return isMagicLeapHeadset() || isOculusHeadset() || isPicoHeadset();
    }

    @Override
    public void runApp(Context context, ApplicationInfo appInfo, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
        context.getApplicationContext().startActivity(launchIntent);
    }
}