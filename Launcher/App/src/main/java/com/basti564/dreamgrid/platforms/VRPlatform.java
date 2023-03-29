package com.basti564.dreamgrid.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class VRPlatform extends AbstractPlatform {

    private static final String ICONS1_URL = "https://github.com/vKolerts/quest_icons/raw/master/450/";
    private static final String ICONS2_URL = "https://raw.githubusercontent.com/lvonasek/binary/master/QuestPiLauncher/icons/";

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
    public void loadIcon(Activity activity, ImageView iconView, ApplicationInfo appInfo) {
        PackageManager packageManager = activity.getPackageManager();
        Resources resources;
        try {
            resources = packageManager.getResourcesForApplication(appInfo.packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        int iconId = appInfo.icon;
        if (iconId == 0) {
            iconId = android.R.drawable.sym_def_app_icon;
        }
        Drawable appIcon = resources.getDrawableForDensity(iconId, DisplayMetrics.DENSITY_XXXHIGH);
        iconView.setImageDrawable(appIcon);

        String pkgName = appInfo.packageName;
        if (cachedIcons.containsKey(pkgName)) {
            iconView.setImageDrawable(cachedIcons.get(pkgName));
            return;
        }

        final File iconFile = packageToPath(activity, pkgName);
        if (iconFile.exists()) {
            if (updateIcon(iconView, iconFile, pkgName)) {
                return;
            }
        }

        downloadIcon(activity, pkgName, () -> updateIcon(iconView, iconFile, pkgName));
    }

    @Override
    public void runApp(Context context, ApplicationInfo appInfo, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
        context.getApplicationContext().startActivity(launchIntent);
    }

    private void downloadIcon(Activity activity, String pkgName, Runnable callback) {
        final File iconFile = packageToPath(activity, pkgName);
        new Thread(() -> {
            try {
                String url = ICONS1_URL + pkgName + ".jpg";
                if (downloadIconFromUrl(url, iconFile)) {
                    activity.runOnUiThread(callback);
                } else {
                    url = ICONS2_URL + pkgName.toLowerCase(Locale.US) + ".jpg";
                    if (downloadIconFromUrl(url, iconFile)) {
                        activity.runOnUiThread(callback);
                    } else {
                        Log.d("Missing icon", iconFile.getName());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}