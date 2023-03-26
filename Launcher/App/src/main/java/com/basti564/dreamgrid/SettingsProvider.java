package com.basti564.dreamgrid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.basti564.dreamgrid.platforms.AndroidPlatform;
import com.basti564.dreamgrid.platforms.PSPPlatform;
import com.basti564.dreamgrid.platforms.VRPlatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsProvider {
    public static final String KEY_CUSTOM_NAMES = "KEY_CUSTOM_NAMES";
    public static final String KEY_CUSTOM_OPACITY = "KEY_CUSTOM_OPACITY";
    public static final String KEY_CUSTOM_SCALE = "KEY_CUSTOM_SCALE";
    public static final String KEY_CUSTOM_THEME = "KEY_CUSTOM_THEME";
    public static final String KEY_EDITMODE = "KEY_EDITMODE";
    public static final String KEY_PLATFORM_ANDROID = "KEY_PLATFORM_ANDROID";
    public static final String KEY_PLATFORM_PSP = "KEY_PLATFORM_PSP";
    public static final String KEY_PLATFORM_VR = "KEY_PLATFORM_VR";
    private static SettingsProvider instance;
    private static Context context;
    private final String KEY_APP_GROUPS = "prefAppGroups";
    private final String KEY_APP_LIST = "prefAppList";
    private final String KEY_SELECTED_GROUPS = "prefSelectedGroups";
    private final String SEPARATOR = "#@%";
    //storage
    private final SharedPreferences sharedPreferences;
    private Map<String, String> appListMap = new HashMap<>();
    private Set<String> appGroupsSet = new HashSet<>();
    private Set<String> selectedGroupsSet = new HashSet<>();

    private SettingsProvider(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsProvider.context = context;
    }

    public static synchronized SettingsProvider getInstance(Context context) {
        if (SettingsProvider.instance == null) {
            SettingsProvider.instance = new SettingsProvider(context);
        }
        return SettingsProvider.instance;
    }

    public static String getAppDisplayName(Context context, String pkg, CharSequence label) {
        String name = PreferenceManager.getDefaultSharedPreferences(context).getString(pkg, "");
        if (!name.isEmpty()) {
            return name;
        }

        String retVal = label.toString();
        if (retVal == null || retVal.equals("")) {
            retVal = pkg;
        }
        return retVal;
    }

    public Map<String, String> getAppList() {
        readValues();
        return appListMap;
    }

    public void setAppList(Map<String, String> appList) {
        appListMap = appList;
        storeValues();
    }

    public ArrayList<ApplicationInfo> getInstalledApps(Context context, List<String> selected, boolean first) {

        // Get list of installed apps
        Map<String, String> apps = getAppList();
        ArrayList<ApplicationInfo> installedApplications = new ArrayList<>();
        if (isPlatformEnabled(KEY_PLATFORM_ANDROID)) {
            List<ApplicationInfo> androidApps = new AndroidPlatform().getInstalledApps(context);
            for (ApplicationInfo app : androidApps) {
                if (!appListMap.containsKey(app.packageName) && appGroupsSet.contains("Tools")) {
                    appListMap.put(app.packageName, "Tools");
                }
            }
            installedApplications.addAll(androidApps);
        }
        if (isPlatformEnabled(KEY_PLATFORM_PSP) && new PSPPlatform().isSupported(context)) {
            // only add PSP apps if the platform is supported
            List<ApplicationInfo> pspApps = new PSPPlatform().getInstalledApps(context);
            for (ApplicationInfo app : pspApps) {
                if (!appListMap.containsKey(app.packageName) && appGroupsSet.contains("PSP")) {
                    appListMap.put(app.packageName, "PSP");
                }
            }
            installedApplications.addAll(pspApps);
        }
        if (isPlatformEnabled(KEY_PLATFORM_VR)) {
            List<ApplicationInfo> vrApps = new VRPlatform().getInstalledApps(context);
            for (ApplicationInfo app : vrApps) {
                if (!appListMap.containsKey(app.packageName) && appGroupsSet.contains(context.getString(R.string.default_apps_group))) {
                    appListMap.put(app.packageName, context.getString(R.string.default_apps_group));
                }
            }
            installedApplications.addAll(vrApps);
        }

        // Save changes to app list
        setAppList(appListMap);

        // Put them into a map with package name as keyword for faster handling
        String packageName = context.getApplicationContext().getPackageName();
        Map<String, ApplicationInfo> appMap = new LinkedHashMap<>();
        for (ApplicationInfo installedApplication : installedApplications) {
            String pkg = installedApplication.packageName;
            boolean showAll = selected.isEmpty();
            boolean isNotAssigned = !apps.containsKey(pkg) && first;
            boolean isInGroup = apps.containsKey(pkg) && selected.contains(apps.get(pkg));
            boolean isVr = hasMetadata(installedApplication, "com.samsung.android.vr.application.mode");
            boolean isEnvironment = !isVr && hasMetadata(installedApplication, "com.oculus.environmentVersion");
            if (showAll || isNotAssigned || isInGroup) {
                boolean isSystemApp = (installedApplication.flags & ApplicationInfo.FLAG_SYSTEM) == 1;
                String[] systemAppPrefixes = context.getResources().getStringArray(R.array.system_app_prefixes);
                String[] nonSystemAppPrefixes = context.getResources().getStringArray(R.array.non_system_app_prefixes);
                for (String prefix : systemAppPrefixes) {
                    if (pkg.startsWith(prefix)) {
                        isSystemApp = true;
                        break;
                    }
                }
                for (String prefix : nonSystemAppPrefixes) {
                    if (pkg.startsWith(prefix)) {
                        isSystemApp = false;
                        break;
                    }
                }
                if (!isSystemApp && !isEnvironment && !pkg.equals(packageName)) {
                    appMap.put(pkg, installedApplication);
                }
            }
        }

        // Create new list of apps
        PackageManager packageManager = context.getPackageManager();
        ArrayList<ApplicationInfo> sortedApps = new ArrayList<>(appMap.values());
        sortedApps.sort((a, b) -> {
            String displayNameA = getAppDisplayName(context, a.packageName, a.loadLabel(packageManager)).toUpperCase();
            String displayNameB = getAppDisplayName(context, b.packageName, b.loadLabel(packageManager)).toUpperCase();
            return displayNameA.compareTo(displayNameB);
        });
        return sortedApps;
    }

    public boolean hasMetadata(ApplicationInfo app, String metadata) {
        if (app.metaData != null) {
            for (String key : app.metaData.keySet()) {
                if (metadata.compareTo(key) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<String> getAppGroups() {
        readValues();
        return appGroupsSet;
    }

    public void setAppGroups(Set<String> appGroups) {
        appGroupsSet = appGroups;
        storeValues();
    }

    public Set<String> getSelectedGroups() {
        readValues();
        return selectedGroupsSet;
    }

    public void setSelectedGroups(Set<String> appGroups) {
        selectedGroupsSet = appGroups;
        storeValues();
    }

    public ArrayList<String> getAppGroupsSorted(boolean selected) {
        readValues();
        ArrayList<String> sortedApplicationList = new ArrayList<>(selected ? selectedGroupsSet : appGroupsSet);
        sortedApplicationList.sort((a, b) -> {
            String simplifiedNameA = simplifyName(a.toUpperCase());
            String simplifiedNameB = simplifyName(b.toUpperCase());
            return simplifiedNameA.compareTo(simplifiedNameB);
        });
        return sortedApplicationList;
    }

    private synchronized void readValues() {
        try {
            Set<String> defaultGroupsSet = new HashSet<>();
            defaultGroupsSet.add(context.getString(R.string.default_apps_group));
            defaultGroupsSet.add("Tools");
            if (new PSPPlatform().isSupported(context)) {
                defaultGroupsSet.add("PSP");
            }
            appGroupsSet = sharedPreferences.getStringSet(KEY_APP_GROUPS, defaultGroupsSet);
            selectedGroupsSet = sharedPreferences.getStringSet(KEY_SELECTED_GROUPS, defaultGroupsSet);

            appListMap.clear();
            Set<String> appListSet = new HashSet<>();
            appListSet = sharedPreferences.getStringSet(KEY_APP_LIST, appListSet);
            for (String s : appListSet) {
                String[] data = s.split(SEPARATOR);
                appListMap.put(data[0], data[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void storeValues() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(KEY_APP_GROUPS, appGroupsSet);
            editor.putStringSet(KEY_SELECTED_GROUPS, selectedGroupsSet);

            Set<String> appListSet = new HashSet<>();
            for (String pkg : appListMap.keySet()) {
                appListSet.add(pkg + SEPARATOR + appListMap.get(pkg));
            }
            editor.putStringSet(KEY_APP_LIST, appListSet);

            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String addGroup() {
        String newGroupName = "New";
        List<String> existingGroups = getAppGroupsSorted(false);
        if (existingGroups.contains(newGroupName)) {
            int index = 1;
            while (existingGroups.contains(newGroupName + " " + index)) {
                index++;
            }
            newGroupName = newGroupName + " " + index;
        }
        existingGroups.add(newGroupName);
        setAppGroups(new HashSet<>(existingGroups));
        return newGroupName;
    }

    public void selectGroup(String name) {
        Set<String> selectFirst = new HashSet<>();
        selectFirst.add(name);
        setSelectedGroups(selectFirst);
    }

    public void setAppDisplayName(Context context, ApplicationInfo appInfo, String newName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(appInfo.packageName, newName);
        editor.apply();
    }

    public String simplifyName(String name) {
        StringBuilder simplifiedName = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'A') && (c <= 'Z')) simplifiedName.append(c);
            if ((c >= '0') && (c <= '9')) simplifiedName.append(c);
        }
        return simplifiedName.toString();
    }

    public boolean isPlatformEnabled(String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, true);
    }
}