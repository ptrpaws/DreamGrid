package com.basti564.dreamgrid.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class AbstractPlatform {

    protected static final HashMap<String, Drawable> iconCache = new HashMap<>();
    protected static final HashSet<String> ignoredIcons = new HashSet<>();

    public abstract ArrayList<ApplicationInfo> getInstalledApps(Context context);
    public abstract boolean isSupported(Context context);
    public abstract void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name);
    public abstract void runApp(Context context, ApplicationInfo app, boolean multiwindow);

    public static void clearIconCache() {
        ignoredIcons.clear();
        iconCache.clear();
    }

    public static AbstractPlatform getPlatform(ApplicationInfo app) {
        if (app.packageName.startsWith(PSPPlatform.PACKAGE_PREFIX)) {
            return new PSPPlatform();
        } else if (isVirtualRealityApp(app)) {
            return new VRPlatform();
        } else {
            return new AndroidPlatform();
        }
    }

    public static File pkg2path(Context context, String pkg) {
        return new File(context.getApplicationInfo().dataDir, pkg + ".jpg");
    }

    public static boolean updateIcon(ImageView icon, File file, String pkg) {
        try {
            Drawable drawable = Drawable.createFromPath(file.getAbsolutePath());
            if (drawable != null) {
                icon.setImageDrawable(drawable);
                iconCache.put(pkg, drawable);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    boolean downloadIconFromUrl(String url, File outputFile) {
        try {
            try (InputStream is = new URL(url).openStream()) {
                if (saveStream(is, outputFile)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected static boolean saveStream(InputStream is, File outputFile) {
        try {
            DataInputStream dis = new DataInputStream(is);

            int length;
            byte[] buffer = new byte[65536];
            FileOutputStream fos = new FileOutputStream(outputFile);
            while ((length = dis.read(buffer))>0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            fos.close();

            if (outputFile.length() >= 64 * 1024) {
                Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                if (bitmap != null) {
                    try {
                        fos = new FileOutputStream(outputFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isMagicLeapHeadset() {
        String vendor = Build.MANUFACTURER.toUpperCase();
        return vendor.startsWith("MAGIC LEAP");
    }

    public static boolean isOculusHeadset() {
        String vendor = Build.MANUFACTURER.toUpperCase();
        return vendor.startsWith("META") || vendor.startsWith("OCULUS");
    }

    public static boolean isPicoHeadset() {
        String vendor = Build.MANUFACTURER.toUpperCase();
        return vendor.startsWith("PICO");
    }

    protected static boolean isVirtualRealityApp(ApplicationInfo app) {
        if (app.metaData != null) {
            for (String key : app.metaData.keySet()) {
                if (key.startsWith("notch.config")) {
                    return true;
                }
                if (key.startsWith("com.oculus")) {
                    return true;
                }
                if (key.startsWith("pvr.")) {
                    return true;
                }
                if (key.contains("vr.application.mode")) {
                    return true;
                }
            }
        }
        return false;
    }
}
