package com.basti564.dreamgrid.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Environment;
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
    private static final String CONFIG_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/PSP/SYSTEM/ppssppvr.ini";
    private static final String PPSSPPVR_PACKAGE_NAME = "org.ppsspp.ppssppvr";
    private static final String GAME_FILENAME_PREFIX = "FileName";
    private static final String RECENT_GAMES_TAG = "[Recent]";

    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        ArrayList<ApplicationInfo> installedApps = new ArrayList<>();
        if (!isSupported(context)) {
            return installedApps;
        }

        for (String gamePath : locateGames()) {
            ApplicationInfo app = new ApplicationInfo();
            app.name = gamePath.substring(gamePath.lastIndexOf('/') + 1);
            app.packageName = PACKAGE_PREFIX + gamePath;
            installedApps.add(app);
        }
        return installedApps;
    }

    @Override
    public boolean isSupported(Context context) {
        for (ApplicationInfo app : new VRPlatform().getInstalledApps(context)) {
            if (app.packageName.startsWith(PPSSPPVR_PACKAGE_NAME)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app) {
        final File file = packageToPath(activity, app.packageName);
        if (file.exists()) {
            if (AbstractPlatform.updateIcon(icon, file, app.packageName)) {
                return;
            }
        }

        new Thread(() -> {
            try {
                file.getParentFile().mkdirs();
                String gameIsoFilePath = app.packageName.substring(PACKAGE_PREFIX.length());
                ISO9660FileSystem discFs = new ISO9660FileSystem(new File(gameIsoFilePath), true);

                Enumeration entries = discFs.getEntries();
                while (entries.hasMoreElements()) {
                    ISO9660FileEntry entry = (ISO9660FileEntry) entries.nextElement();
                    if (entry.getName().contains("ICON0.PNG")) {
                        if (saveStream(discFs.getInputStream(entry), file)) {
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
        String gamePath = app.packageName.substring(PACKAGE_PREFIX.length());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + gamePath), "*/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(PPSSPPVR_PACKAGE_NAME);
        context.getApplicationContext().startActivity(intent);
    }

    private ArrayList<String> locateGames() {
        ArrayList<String> gamePaths = new ArrayList<>();
        try {
            boolean isReadingGames = false;
            FileInputStream configFileStream = new FileInputStream(CONFIG_FILE_PATH);
            Scanner configScanner = new Scanner(configFileStream);
            while (configScanner.hasNext()) {
                String configLine = configScanner.nextLine();
                if (isReadingGames && configLine.startsWith(GAME_FILENAME_PREFIX)) {
                    gamePaths.add(configLine.substring(configLine.indexOf('/')));
                }

                if (configLine.startsWith(RECENT_GAMES_TAG)) {
                    isReadingGames = true;
                } else if (configLine.startsWith("[")) {
                    isReadingGames = false;
                }
            }
            configScanner.close();
            configFileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gamePaths;
    }
}