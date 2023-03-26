package com.basti564.dreamgrid;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class ButtonManager extends AccessibilityService {
    public static void isAccesibilityInitialized(Context context) {
        try {
            android.provider.Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);

            String settingValue = android.provider.Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                context.getPackageName();
            }
        } catch (android.provider.Settings.SettingNotFoundException ignored) {
        }
    }

    public static void requestAccessibility(Context context) {
        Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        localIntent.setPackage("com.android.settings");
        context.startActivity(localIntent);
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventText = event.getText().toString();
            String[] exploreAccessibilityEventNames = getResources().getStringArray(R.array.explore_accessibility_event_names);

            for (String eventName : exploreAccessibilityEventNames) {
                if (eventName.compareTo(eventText) == 0) {
                    MainActivity.reset(getApplicationContext());
                    break;
                }
            }
        }
    }

    public void onInterrupt() {
    }

    protected void onServiceConnected() {
        super.onServiceConnected();
    }
}