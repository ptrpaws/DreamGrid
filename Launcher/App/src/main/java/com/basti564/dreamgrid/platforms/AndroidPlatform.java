package com.basti564.dreamgrid.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import java.io.File;
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
    public void loadIcon(Activity activity, ImageView iconView, ApplicationInfo appInfo) {
        PackageManager packageManager = activity.getPackageManager();
        Resources appResources;
        try {
            appResources = packageManager.getResourcesForApplication(appInfo.packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        int iconId = appInfo.icon;
        if (iconId == 0) {
            iconId = android.R.drawable.sym_def_app_icon;
        }
        Drawable appIconDrawable = ResourcesCompat.getDrawableForDensity(appResources, iconId, DisplayMetrics.DENSITY_XXXHIGH, null);
        iconView.setImageDrawable(appIconDrawable);

        final File appFilePath = packageToPath(activity, appInfo.packageName);
        if (appFilePath.exists()) {
            AbstractPlatform.updateIcon(iconView, appFilePath, appInfo.packageName);
        }
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
