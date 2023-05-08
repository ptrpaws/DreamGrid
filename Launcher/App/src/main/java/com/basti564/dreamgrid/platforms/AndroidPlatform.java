package com.basti564.dreamgrid.platforms;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;

public class AndroidPlatform extends AbstractPlatform {
    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ArrayList<ApplicationInfo> installedAppsList = new ArrayList<>();
        for (ApplicationInfo appInfo : packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (!isVirtualRealityApp(appInfo)) {
                installedAppsList.add(appInfo);
            }
        }
        return installedAppsList;
    }

    @Override
    public boolean isSupported(Context context) {
        return true;
    }

    @Override
    public void runApp(Context context, ApplicationInfo appInfo, boolean isMultiWindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
        if (isMultiWindow) {
            context.getApplicationContext().startActivity(launchIntent);
        } else {
            context.startActivity(launchIntent);
        }
    }
}
