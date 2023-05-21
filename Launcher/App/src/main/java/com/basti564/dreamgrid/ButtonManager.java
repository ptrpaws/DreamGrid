package com.basti564.dreamgrid;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;

public class ButtonManager extends AccessibilityService {
    public static void checkAccessibilitySettings(Context context) {
        try {
            android.provider.Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);

            String enabledServices = android.provider.Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabledServices != null) {
                context.getPackageName();
            }
        } catch (android.provider.Settings.SettingNotFoundException ignored) {
        }
    }

    public static void requestAccessibilitySettings(Context context) {
        Intent intent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.settings");
        context.startActivity(intent);
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventText = event.getText().toString();
            String[] eventNames = getResources().getStringArray(R.array.explore_accessibility_event_names);

            for (String eventName : eventNames) {
                if (eventName.compareTo(eventText) == 0) {
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> MainActivity.reset(getApplicationContext()), 1000);
                    break;
                }
            }
        }
    }

    public void onInterrupt() {
    }
}