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

    protected static final HashMap<String, Drawable> cachedIcons = new HashMap<>();
    protected static final HashSet<String> excludedIconPackages = new HashSet<>();

    public static void clearIconCache() {
        excludedIconPackages.clear();
        cachedIcons.clear();
    }

    public static AbstractPlatform getPlatform(ApplicationInfo applicationInfo) {
        if (applicationInfo.packageName.startsWith(PSPPlatform.PACKAGE_PREFIX)) {
            return new PSPPlatform();
        } else if (isVirtualRealityApp(applicationInfo)) {
            return new VRPlatform();
        } else {
            return new AndroidPlatform();
        }
    }

    public static File packageToPath(Context context, String packageName) {
        return new File(context.getApplicationInfo().dataDir, packageName + ".jpg");
    }

    public static boolean updateIcon(ImageView iconView, File file, String packageName) {
        try {
            Drawable newIconDrawable = Drawable.createFromPath(file.getAbsolutePath());
            if (newIconDrawable != null) {
                iconView.setImageDrawable(newIconDrawable);
                cachedIcons.put(packageName, newIconDrawable);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected static boolean saveStream(InputStream inputStream, File outputFile) {
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            int length;
            byte[] buffer = new byte[65536];
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            while ((length = dataInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }
            fileOutputStream.flush();
            fileOutputStream.close();

            if (outputFile.length() >= 64 * 1024) {
                Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                if (bitmap != null) {
                    try {
                        fileOutputStream = new FileOutputStream(outputFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                        fileOutputStream.close();
                    } catch (Exception e) {
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
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        return manufacturer.startsWith("MAGIC LEAP");
    }

    public static boolean isOculusHeadset() {
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        return manufacturer.startsWith("META") || manufacturer.startsWith("OCULUS");
    }

    public static boolean isPicoHeadset() {
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        return manufacturer.startsWith("PICO") || manufacturer.startsWith("PİCO"); // PİCO on turkish systems
    }

    protected static boolean isVirtualRealityApp(ApplicationInfo applicationInfo) {
        if (applicationInfo.metaData != null) {
            for (String key : applicationInfo.metaData.keySet()) {
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

    public abstract ArrayList<ApplicationInfo> getInstalledApps(Context context);

    public abstract boolean isSupported(Context context);

    public abstract void loadIcon(Activity activity, ImageView iconView, ApplicationInfo applicationInfo, String name);

    public abstract void runApp(Context context, ApplicationInfo applicationInfo, boolean multiwindow);

    boolean downloadIconFromUrl(String url, File iconFile) {
        try {
            try (InputStream inputStream = new URL(url).openStream()) {
                if (saveStream(inputStream, iconFile)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
