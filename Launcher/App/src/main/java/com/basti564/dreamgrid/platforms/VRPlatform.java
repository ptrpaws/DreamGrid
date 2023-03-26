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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

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
    public void loadIcon(Activity activity, ImageView iconView, ApplicationInfo appInfo, String name) {
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

        downloadIcon(activity, pkgName, name, () -> updateIcon(iconView, iconFile, pkgName));
    }

    @Override
    public void runApp(Context context, ApplicationInfo appInfo, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
        context.getApplicationContext().startActivity(launchIntent);
    }

    private void downloadIcon(Activity activity, String pkgName, String name, Runnable callback) {
        final File iconFile = packageToPath(activity, pkgName);
        new Thread(() -> {
            try {
                String autogen = null;
                if (excludedIconPackages.contains(iconFile.getName())) {
                    // ignored icon
                } else if (downloadIconFromUrl(ICONS1_URL + pkgName + ".jpg", iconFile)) {
                    activity.runOnUiThread(callback);
                } else if (downloadIconFromUrl(ICONS2_URL + pkgName.toLowerCase(Locale.US) + ".jpg", iconFile)) {
                    activity.runOnUiThread(callback);
                } else {
                    int count = 0;
                    File infoFile = new File(activity.getApplicationInfo().dataDir, "applab.info");
                    if (downloadIconFromUrl(ICONS2_URL + "applab.info", infoFile)) {
                        try {
                            FileInputStream fis = new FileInputStream(infoFile);
                            Scanner sc = new Scanner(fis);
                            while (sc.hasNext()) {
                                String line = sc.nextLine();
                                if (line.contains(name)) {
                                    Scanner lineScanner = new Scanner(line);
                                    autogen = lineScanner.next();
                                    lineScanner.close();
                                    count++;
                                }
                            }
                            sc.close();
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (count == 0) {
                        Log.d("Missing icon", iconFile.getName());
                        excludedIconPackages.add(iconFile.getName());
                    } else if (count == 1) {
                        if (downloadIconFromUrl(ICONS2_URL + autogen + ".jpg", iconFile)) {
                            activity.runOnUiThread(callback);
                        } else {
                            Log.d("Missing icon", iconFile.getName());
                            excludedIconPackages.add(iconFile.getName());
                        }
                    } else {
                        Log.d("Too many icons", iconFile.getName());
                        excludedIconPackages.add(iconFile.getName());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}