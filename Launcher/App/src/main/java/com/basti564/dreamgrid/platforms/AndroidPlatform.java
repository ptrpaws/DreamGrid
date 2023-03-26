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

import java.io.File;
import java.util.ArrayList;

public class AndroidPlatform extends AbstractPlatform {
    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        ArrayList<ApplicationInfo> output = new ArrayList<>();
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (!isVirtualRealityApp(app)) {
                output.add(app);
            }
        }
        return output;
    }

    @Override
    public boolean isSupported(Context context) {
        return true;
    }

    @Override
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name) {
        PackageManager pm = activity.getPackageManager();
        Resources resources;
        try {
            resources = pm.getResourcesForApplication(app.packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        int iconId = app.icon;
        if (iconId == 0) {
            iconId = android.R.drawable.sym_def_app_icon;
        }
        Drawable appIcon = resources.getDrawableForDensity(iconId, DisplayMetrics.DENSITY_XXXHIGH);
        icon.setImageDrawable(appIcon);

        final File file = pkg2path(activity, app.packageName);
        if (file.exists()) {
            AbstractPlatform.updateIcon(icon, file, app.packageName);
        }
    }

    @Override
    public void runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (multiwindow) {
            context.getApplicationContext().startActivity(launchIntent);
        } else {
            context.startActivity(launchIntent);
        }
    }
}
