package com.basti564.dreamgrid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.basti564.dreamgrid.platforms.AbstractPlatform;
import com.basti564.dreamgrid.platforms.AndroidPlatform;
import com.basti564.dreamgrid.platforms.PSPPlatform;
import com.basti564.dreamgrid.platforms.VRPlatform;
import com.basti564.dreamgrid.ui.AppsAdapter;
import com.basti564.dreamgrid.ui.GroupsAdapter;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class MainActivity extends Activity {
    public static final int PICK_ICON_CODE = 450;
    public static final int PICK_THEME_CODE = 95;
    private static final String CUSTOM_THEME = "theme.png";
    private static final boolean DEFAULT_NAMES = true;
    private static final int DEFAULT_OPACITY = 10;
    private static final int DEFAULT_SCALE = 2;
    private static final int DEFAULT_THEME = 0;
    private static final int[] SCALE_VALUES = {55, 70, 95, 130, 210};
    private static final int[] THEME_DRAWABLES = {
            R.drawable.bkg_blossoms,
            R.drawable.bkg_drips,
            R.drawable.bkg_orange,
            R.drawable.bkg_dawn,
            R.drawable.bkg_bland
    };
    private static ImageView[] selectedThemeImageViews;
    private static MainActivity instance = null;
    private GridView appGridView;
    private ImageView backgroundImageView;
    private GridView groupPanelGridView;
    private boolean activityHasFocus;
    private SharedPreferences sharedPreferences;
    private SettingsProvider settingsProvider;
    private ImageView selectedImageView;
    private boolean isSettingsLookOpen = false;

    public static void reset(Context context) {
        try {
            if (instance != null) {
                instance.finishAffinity();
                instance = null;
            }
            context.startActivity(context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (AbstractPlatform.isMagicLeapHeadset()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        settingsProvider = SettingsProvider.getInstance(this);
        instance = this;

        BlurView blurView = findViewById(R.id.blurView);

        float blurRadiusDp = 20f;

        View windowDecorView = getWindow().getDecorView();
        ViewGroup rootViewGroup = windowDecorView.findViewById(android.R.id.content);

        Drawable windowBackground = windowDecorView.getBackground();

        blurView.setupWith(rootViewGroup, new RenderScriptBlur(this)) // or RenderEffectBlur
                .setFrameClearDrawable(windowBackground) // Optional
                .setBlurRadius(blurRadiusDp);

        blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView.setClipToOutline(true);

        // Get UI instances
        appGridView = findViewById(R.id.appsView);
        backgroundImageView = findViewById(R.id.background);
        groupPanelGridView = findViewById(R.id.groupsView);

        // Handle group click listener
        groupPanelGridView.setOnItemClickListener((parent, view, position, id) -> {
            List<String> groups = settingsProvider.getAppGroupsSorted(false);
            if (position == groups.size()) {
                settingsProvider.selectGroup(GroupsAdapter.HIDDEN_GROUP);
            } else if (position == groups.size() + 1) {
                settingsProvider.selectGroup(settingsProvider.addGroup());
            } else {
                settingsProvider.selectGroup(groups.get(position));
            }
            reloadUI();
        });

        // Multiple group selection
        groupPanelGridView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false)) {
                List<String> groups = settingsProvider.getAppGroupsSorted(false);
                Set<String> selectedGroups = settingsProvider.getSelectedGroups();

                String item = groups.get(position);
                if (selectedGroups.contains(item)) {
                    selectedGroups.remove(item);
                } else {
                    selectedGroups.add(item);
                }
                if (selectedGroups.isEmpty()) {
                    selectedGroups.add(groups.get(0));
                }
                settingsProvider.setSelectedGroups(selectedGroups);
                reloadUI();
            }
            return true;
        });

        // Set logo button
        ImageView settingsLogoImageView = findViewById(R.id.logo);
        settingsLogoImageView.setOnClickListener(view -> showSettingsMain());
    }

    @Override
    public void onBackPressed() {
        if (AbstractPlatform.isMagicLeapHeadset()) {
            super.onBackPressed();
        } else {
            showSettingsMain();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityHasFocus = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityHasFocus = true;

        String[] requiredPermissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        boolean hasReadPermission = checkSelfPermission(requiredPermissions[0]) == PackageManager.PERMISSION_GRANTED;
        boolean hasWritePermission = checkSelfPermission(requiredPermissions[1]) == PackageManager.PERMISSION_GRANTED;
        if (hasReadPermission && hasWritePermission) {
            reloadUI();
        } else {
            requestPermissions(requiredPermissions, 0);
        }
    }

    public void setSelectedImageView(ImageView imageView) {
        selectedImageView = imageView;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ICON_CODE) {
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    ((AppsAdapter) appGridView.getAdapter()).onImageSelected(image.getPath(), selectedImageView);
                    break;
                }
            } else {
                ((AppsAdapter) appGridView.getAdapter()).onImageSelected(null, selectedImageView);
            }
        } else if (requestCode == PICK_THEME_CODE) {
            if (resultCode == RESULT_OK) {

                for (Image image : ImagePicker.getImages(data)) {
                    Bitmap themeBitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(image.getPath()), 1280);
                    ImageUtils.saveBitmap(themeBitmap, new File(getApplicationInfo().dataDir, CUSTOM_THEME));
                    setTheme(selectedThemeImageViews, THEME_DRAWABLES.length);
                    reloadUI();
                    break;
                }
            }
        }
    }

    public String getSelectedPackage() {
        return ((AppsAdapter) appGridView.getAdapter()).getSelectedPackage();
    }

    public void reloadUI() {

        // set customization
        boolean names = sharedPreferences.getBoolean(SettingsProvider.KEY_CUSTOM_NAMES, DEFAULT_NAMES);
        int opacity = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY);
        int backgroundThemeIndex = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, DEFAULT_THEME);
        int newScaleValueIndex = getPixelFromDip(SCALE_VALUES[sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE)]);
        appGridView.setColumnWidth(newScaleValueIndex);
        if (backgroundThemeIndex < THEME_DRAWABLES.length) {
            Drawable backgroundThemeDrawable = getDrawable(THEME_DRAWABLES[backgroundThemeIndex]);
            backgroundThemeDrawable.setAlpha(255 * opacity / 10);
            backgroundImageView.setImageDrawable(backgroundThemeDrawable);
        } else {
            File file = new File(getApplicationInfo().dataDir, CUSTOM_THEME);
            Bitmap themeBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Drawable backgroundThemeDrawable = new BitmapDrawable(getResources(), themeBitmap);
            backgroundThemeDrawable.setAlpha(255 * opacity / 10);
            backgroundImageView.setImageDrawable(backgroundThemeDrawable);
        }

        // set context
        newScaleValueIndex += getPixelFromDip(8);
        boolean editMode = sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);
        appGridView.setAdapter(new AppsAdapter(this, editMode, newScaleValueIndex, names));
        groupPanelGridView.setAdapter(new GroupsAdapter(this, editMode));
        groupPanelGridView.setNumColumns(Math.min(groupPanelGridView.getAdapter().getCount(), GroupsAdapter.MAX_GROUPS - 1));
    }

    public void setTheme(ImageView[] views, int index) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SettingsProvider.KEY_CUSTOM_THEME, index);
        editor.apply();
        reloadUI();

        for (ImageView image : views) {
            image.setBackgroundColor(Color.TRANSPARENT);
            image.setAlpha(255);
        }
        views[index].setBackgroundColor(Color.WHITE);
        views[index].setAlpha(255 * sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY) / 10);
    }

    public Dialog showPopup(int layout) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(layout);
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();

        WindowManager.LayoutParams windowLayoutParams = new WindowManager.LayoutParams();
        windowLayoutParams.copyFrom(dialog.getWindow().getAttributes());
        windowLayoutParams.width = 660;
        windowLayoutParams.height = getWindowHeight() - 200;
        windowLayoutParams.gravity = Gravity.END;  // or Gravity.RIGHT
        windowLayoutParams.x = 50;  // add some distance from the border
        windowLayoutParams.y = 50;
        dialog.getWindow().setAttributes(windowLayoutParams);
        dialog.findViewById(R.id.layout).requestLayout();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        return dialog;
    }

    private int getWindowHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    private void showSettingsMain() {
        Dialog dialog = showPopup(R.layout.dialog_settings);

        dialog.findViewById(R.id.settings_look).setOnClickListener(view -> {
            if (!isSettingsLookOpen) {
                isSettingsLookOpen = true;
                showSettingsLook();
            }
        });
        ImageView apps = dialog.findViewById(R.id.settings_apps);
        boolean editMode = !sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);
        apps.setImageResource(editMode ? R.drawable.ic_editing_on : R.drawable.ic_editing_off);
        apps.setOnClickListener(view1 -> {
            ArrayList<String> selectedGroups = settingsProvider.getAppGroupsSorted(true);
            if (editMode && (selectedGroups.size() > 1)) {
                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(selectedGroups.get(0));
                settingsProvider.setSelectedGroups(selectFirst);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_EDITMODE, editMode);
            editor.apply();
            reloadUI();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.settings_look).setOnClickListener(view -> showSettingsLook());
        dialog.findViewById(R.id.settings_platforms).setOnClickListener(view -> showSettingsPlatforms());

        View appShortcutView = dialog.findViewById(R.id.service_app_shortcut);
        if (!(AbstractPlatform.isOculusHeadset() || AbstractPlatform.isPicoHeadset())) {
            appShortcutView.setVisibility(View.GONE);
        } else {
            appShortcutView.setOnClickListener(view -> {
                ButtonManager.checkAccessibilitySettings(this);
                ButtonManager.requestAccessibilitySettings(this);
            });
        }

        View appExploreView = dialog.findViewById(R.id.service_explore_app);
        if (!AbstractPlatform.isOculusHeadset()) {
            appExploreView.setVisibility(View.GONE);
        } else {
            appExploreView.setOnClickListener(view -> openAppDetails("com.oculus.explore"));
        }
    }

    private void showSettingsLook() {
        Dialog dialog = showPopup(R.layout.dialog_look);

        // set onDismissListener to reset the flag when dialog is dismissed
        dialog.setOnDismissListener(dialogInterface -> isSettingsLookOpen = false);

        CheckBox names = dialog.findViewById(R.id.checkbox_names);
        names.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_CUSTOM_NAMES, DEFAULT_NAMES));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_CUSTOM_NAMES, value);
            editor.apply();
            reloadUI();
        });

        SeekBar opacity = dialog.findViewById(R.id.bar_opacity);
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsProvider.KEY_CUSTOM_OPACITY, value);
                editor.apply();
                reloadUI();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        opacity.setProgress(sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY));
        opacity.setMax(10);
        opacity.setMin(0);

        SeekBar scale = dialog.findViewById(R.id.bar_scale);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsProvider.KEY_CUSTOM_SCALE, value);
                editor.apply();
                reloadUI();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        scale.setProgress(sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE));
        scale.setMax(SCALE_VALUES.length - 1);
        scale.setMin(0);

        int theme = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, DEFAULT_THEME);
        ImageView[] views = {
                dialog.findViewById(R.id.theme0),
                dialog.findViewById(R.id.theme1),
                dialog.findViewById(R.id.theme2),
                dialog.findViewById(R.id.theme3),
                dialog.findViewById(R.id.theme4),
                dialog.findViewById(R.id.theme_custom)
        };
        for (ImageView image : views) {
            image.setBackgroundColor(Color.TRANSPARENT);
            image.setAlpha(255);
        }
        views[theme].setBackgroundColor(Color.WHITE);
        views[theme].setAlpha(255 * sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY) / 10);
        for (int i = 0; i < views.length; i++) {
            int index = i;
            views[i].setOnClickListener(view12 -> {
                if (index >= THEME_DRAWABLES.length) {
                    selectedThemeImageViews = views;
                    ImageUtils.showImagePicker(this, PICK_THEME_CODE);
                } else {
                    setTheme(views, index);
                }
            });
        }
    }

    private void showSettingsPlatforms() {
        Dialog dialog = showPopup(R.layout.dialog_platforms);

        ImageView androidPlatformImageView = dialog.findViewById(R.id.settings_android);
        androidPlatformImageView.setOnClickListener(view -> {
            boolean isPlatformEnabled = sharedPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_ANDROID, true);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_ANDROID, !isPlatformEnabled);
            editor.apply();
            reloadUI();
        });
        androidPlatformImageView.setVisibility(new AndroidPlatform().isSupported(this) ? View.VISIBLE : View.GONE);

        ImageView pspPlatformImageView = dialog.findViewById(R.id.settings_psp);
        pspPlatformImageView.setOnClickListener(view -> {
            boolean isPlatformEnabled = sharedPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_PSP, true);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_PSP, !isPlatformEnabled);
            editor.apply();
            reloadUI();
        });
        pspPlatformImageView.setVisibility(new PSPPlatform().isSupported(this) ? View.VISIBLE : View.GONE);

        ImageView vrPlatformImageView = dialog.findViewById(R.id.settings_vr);
        vrPlatformImageView.setOnClickListener(view -> {
            boolean isPlatformEnabled = sharedPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_VR, true);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_VR, !isPlatformEnabled);
            editor.apply();
            reloadUI();
        });
        vrPlatformImageView.setVisibility(new VRPlatform().isSupported(this) ? View.VISIBLE : View.GONE);
    }

    private int getPixelFromDip(int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    public void openApp(ApplicationInfo app) {
        //fallback action
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (activityHasFocus) {
                openAppDetails(app.packageName);
            }
        }).start();

        //open the app
        AbstractPlatform platform = AbstractPlatform.getPlatform(app);
        platform.runApp(this, app, false);
    }

    public void openAppDetails(String pkg) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + pkg));
        startActivity(intent);
    }
}