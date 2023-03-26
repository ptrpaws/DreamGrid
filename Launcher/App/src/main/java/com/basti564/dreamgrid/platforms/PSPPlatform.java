package com.basti564.dreamgrid.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.widget.ImageView;

import net.didion.loopy.iso9660.ISO9660FileEntry;
import net.didion.loopy.iso9660.ISO9660FileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

public class PSPPlatform extends AbstractPlatform {

    public static final String PACKAGE_PREFIX = "psp/";
    private static final String CONFIG_FILE = "/mnt/sdcard/PSP/SYSTEM/ppssppvr.ini";
    private static final String EMULATOR_PACKAGE = "org.ppsspp.ppssppvr";
    private static final String FILENAME_PREFIX = "FileName";
    private static final String RECENT_TAG = "[Recent]";

    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        ArrayList<ApplicationInfo> output = new ArrayList<>();
        if (!isSupported(context)) {
            return output;
        }

        for (String path : locateGames()) {
            ApplicationInfo app = new ApplicationInfo();
            app.name = path.substring(path.lastIndexOf('/') + 1);
            app.packageName = PACKAGE_PREFIX + path;
            output.add(app);
        }
        return output;
    }

    @Override
    public boolean isSupported(Context context) {
        for (ApplicationInfo app : new VRPlatform().getInstalledApps(context)) {
            if (app.packageName.startsWith(EMULATOR_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name) {
        final File file = pkg2path(activity, app.packageName);
        if (file.exists()) {
            if (AbstractPlatform.updateIcon(icon, file, app.packageName)) {
                return;
            }
        }

        new Thread(() -> {
            try {
                file.getParentFile().mkdirs();
                String isoToRead = app.packageName.substring(PACKAGE_PREFIX.length());
                ISO9660FileSystem discFs = new ISO9660FileSystem(new File(isoToRead), true);

                Enumeration es = discFs.getEntries();
                while (es.hasMoreElements()) {
                    ISO9660FileEntry fileEntry = (ISO9660FileEntry) es.nextElement();
                    if (fileEntry.getName().contains("ICON0.PNG")) {
                        if (saveStream(discFs.getInputStream(fileEntry), file)) {
                            activity.runOnUiThread(() -> AbstractPlatform.updateIcon(icon, file, app.packageName));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        String path = app.packageName.substring(PACKAGE_PREFIX.length());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + path), "*/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(EMULATOR_PACKAGE);
        context.getApplicationContext().startActivity(intent);
    }

    private ArrayList<String> locateGames() {
        ArrayList<String> output = new ArrayList<>();
        try {
            boolean enabled = false;
            FileInputStream fis = new FileInputStream(CONFIG_FILE);
            Scanner sc = new Scanner(fis);
            while (sc.hasNext()) {
                String line = sc.nextLine();
                if (enabled && line.startsWith(FILENAME_PREFIX)) {
                    output.add(line.substring(line.indexOf('/')));
                }

                if (line.startsWith(RECENT_TAG)) {
                    enabled = true;
                } else if (line.startsWith("[")) {
                    enabled = false;
                }
            }
            sc.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
}
