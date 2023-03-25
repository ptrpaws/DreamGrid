package com.basti564.dreamgrid;

import android.Manifest;
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
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.basti564.dreamgrid.platforms.AbstractPlatform;
import com.basti564.dreamgrid.platforms.AndroidPlatform;
import com.basti564.dreamgrid.platforms.PSPPlatform;
import com.basti564.dreamgrid.platforms.VRPlatform;
import com.basti564.dreamgrid.ui.AppsAdapter;
import com.basti564.dreamgrid.ui.GroupsAdapter;
import com.basti564.dreamgrid.ui.SettingsGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity
{
    private static final String CUSTOM_THEME = "theme.png";
    private static final boolean DEFAULT_NAMES = true;
    private static final int DEFAULT_OPACITY = 7;
    private static final int DEFAULT_SCALE = 2;
    private static final int DEFAULT_THEME = 0;
    public static final int PICK_ICON_CODE = 450;
    public static final int PICK_THEME_CODE = 95;

    private static final int[] SCALES = {55, 70, 95, 130, 210};
    private static final int[] THEMES = {
            R.drawable.bkg_default,
            R.drawable.bkg_blossoms,
            R.drawable.bkg_drips,
            R.drawable.bkg_orange,
            R.drawable.bkg_dawn
    };
    private static ImageView[] mTempViews;

    private GridView mAppGrid;
    private ImageView mBackground;
    private GridView mGroupPanel;

    private static MainActivity instance = null;
    private boolean mFocus;
    private SharedPreferences mPreferences;
    private SettingsProvider mSettings;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (AbstractPlatform.isMagicLeapHeadset()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mSettings = SettingsProvider.getInstance(this);
        instance = this;

        // Get UI instances
        mAppGrid = findViewById(R.id.appsView);
        mBackground = findViewById(R.id.background);
        mGroupPanel = findViewById(R.id.groupsView);

        // Handle group click listener
        mGroupPanel.setOnItemClickListener((parent, view, position, id) -> {
            List<String> groups = mSettings.getAppGroupsSorted(false);
            if (position == groups.size()) {
                mSettings.selectGroup(GroupsAdapter.HIDDEN_GROUP);
            } else if (position == groups.size() + 1) {
                mSettings.selectGroup(mSettings.addGroup());
            } else {
                mSettings.selectGroup(groups.get(position));
            }
            reloadUI();
        });

        // Multiple group selection
        mGroupPanel.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!mPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false)) {
                List<String> groups = mSettings.getAppGroupsSorted(false);
                Set<String> selected = mSettings.getSelectedGroups();

                String item = groups.get(position);
                if (selected.contains(item)) {
                    selected.remove(item);
                } else {
                    selected.add(item);
                }
                if (selected.isEmpty()) {
                    selected.add(groups.get(0));
                }
                mSettings.setSelectedGroups(selected);
                reloadUI();
            }
            return true;
        });

        // Set pi button
        View pi = findViewById(R.id.pi);
        pi.setOnClickListener(view -> showSettingsMain());
        pi.setOnLongClickListener(view -> {
            boolean editMode = mPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);
            if (!editMode) {
                mSettings.setSelectedGroups(mSettings.getAppGroups());
                reloadUI();
            }
            return true;
        });
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
        mFocus = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFocus = true;

        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        boolean read = checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED;
        boolean write = checkSelfPermission(permissions[1]) == PackageManager.PERMISSION_GRANTED;
        if (read && write) {
            reloadUI();
        } else {
            requestPermissions(permissions, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ICON_CODE) {
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    ((AppsAdapter)mAppGrid.getAdapter()).onImageSelected(image.getPath());
                    break;
                }
            } else {
                ((AppsAdapter)mAppGrid.getAdapter()).onImageSelected(null);
            }
        } else if (requestCode == PICK_THEME_CODE) {
            if (resultCode == RESULT_OK) {

                for (Image image : ImagePicker.getImages(data)) {
                    Bitmap bitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(image.getPath()), 1280);
                    ImageUtils.saveBitmap(bitmap, new File(getApplicationInfo().dataDir, CUSTOM_THEME));
                    setTheme(mTempViews, THEMES.length);
                    reloadUI();
                    break;
                }
            }
        }
    }

    public String getSelectedPackage() {
        return ((AppsAdapter)mAppGrid.getAdapter()).getSelectedPackage();
    }

    public void reloadUI() {

        // set customization
        boolean names = mPreferences.getBoolean(SettingsProvider.KEY_CUSTOM_NAMES, DEFAULT_NAMES);
        int opacity = mPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY);
        int theme = mPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, DEFAULT_THEME);
        int scale = getPixelFromDip(SCALES[mPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE)]);
        mAppGrid.setColumnWidth(scale);
        if (theme < THEMES.length) {
            Drawable d = getDrawable(THEMES[theme]);
            d.setAlpha(255 * opacity / 10);
            mBackground.setImageDrawable(d);
        } else {
            File file = new File(getApplicationInfo().dataDir, CUSTOM_THEME);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Drawable d = new BitmapDrawable(getResources(), bitmap);
            d.setAlpha(255 * opacity / 10);
            mBackground.setImageDrawable(d);
        }

        // set context
        scale += getPixelFromDip(8);
        boolean editMode = mPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);
        mAppGrid.setAdapter(new AppsAdapter(this, editMode, scale, names));
        mGroupPanel.setAdapter(new GroupsAdapter(this, editMode));
        mGroupPanel.setNumColumns(Math.min(mGroupPanel.getAdapter().getCount(), GroupsAdapter.MAX_GROUPS - 1));
    }

    public void setTheme(ImageView[] views, int index) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(SettingsProvider.KEY_CUSTOM_THEME, index);
        editor.apply();
        reloadUI();

        for (ImageView image : views) {
            image.setBackgroundColor(Color.TRANSPARENT);
            image.setAlpha(255);
        }
        views[index].setBackgroundColor(Color.WHITE);
        views[index].setAlpha(255 * mPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY) / 10);
    }

    public Dialog showPopup(int layout) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        AlertDialog dialog = builder.create();
        dialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = 660;
        lp.height = getWindowHeight() - 200;
        lp.gravity = Gravity.END;  // or Gravity.RIGHT
        lp.x = 50;  // add some distance from the border
        lp.y = 50;
        dialog.getWindow().setAttributes(lp);
        dialog.findViewById(R.id.layout).requestLayout();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        return dialog;
    }

    private int getWindowHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }



    private void showSettingsMain() {

        Dialog dialog = showPopup(R.layout.dialog_settings);
        SettingsGroup apps = dialog.findViewById(R.id.settings_apps);
        boolean editMode = !mPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);
        apps.setIcon(editMode ? R.drawable.ic_editing_on : R.drawable.ic_editing_off);
        apps.setText(getString(editMode ? R.string.settings_apps_enable : R.string.settings_apps_disable));
        apps.setTooltipText(getString(editMode ? R.string.settings_apps_enable : R.string.settings_apps_disable));
        apps.setOnClickListener(view1 -> {
            ArrayList<String> selected = mSettings.getAppGroupsSorted(true);
            if (editMode && (selected.size() > 1)) {
                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(selected.get(0));
                mSettings.setSelectedGroups(selectFirst);
            }
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_EDITMODE, editMode);
            editor.apply();
            reloadUI();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.settings_look).setOnClickListener(view -> showSettingsLook());
        dialog.findViewById(R.id.settings_platforms).setOnClickListener(view -> showSettingsPlatforms());
        dialog.findViewById(R.id.settings_tweaks).setOnClickListener(view -> showSettingsTweaks());
        dialog.findViewById(R.id.settings_device).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage("com.android.settings");
            startActivity(intent);
        });
        if (AbstractPlatform.isMagicLeapHeadset()) {
            dialog.findViewById(R.id.settings_device).setVisibility(View.GONE);
        }
        if (!AbstractPlatform.isOculusHeadset()) {
            dialog.findViewById(R.id.settings_tweaks).setVisibility(View.GONE);
        }
    }

    private void showSettingsLook() {
        Dialog d = showPopup(R.layout.dialog_look);

        CheckBox names = d.findViewById(R.id.checkbox_names);
        names.setChecked(mPreferences.getBoolean(SettingsProvider.KEY_CUSTOM_NAMES, DEFAULT_NAMES));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_CUSTOM_NAMES, value);
            editor.apply();
            reloadUI();
        });

        SeekBar opacity = d.findViewById(R.id.bar_opacity);
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt(SettingsProvider.KEY_CUSTOM_OPACITY, value);
                editor.apply();
                reloadUI();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        opacity.setProgress(mPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY));
        opacity.setMax(10);
        opacity.setMin(0);

        SeekBar scale = d.findViewById(R.id.bar_scale);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt(SettingsProvider.KEY_CUSTOM_SCALE, value);
                editor.apply();
                reloadUI();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        scale.setProgress(mPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE));
        scale.setMax(SCALES.length - 1);
        scale.setMin(0);

        int theme = mPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, DEFAULT_THEME);
        ImageView[] views = {
                d.findViewById(R.id.theme0),
                d.findViewById(R.id.theme1),
                d.findViewById(R.id.theme2),
                d.findViewById(R.id.theme3),
                d.findViewById(R.id.theme4),
                d.findViewById(R.id.theme_custom)
        };
        for (ImageView image : views) {
            image.setBackgroundColor(Color.TRANSPARENT);
            image.setAlpha(255);
        }
        views[theme].setBackgroundColor(Color.WHITE);
        views[theme].setAlpha(255 * mPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY) / 10);
        for (int i = 0; i < views.length; i++) {
            int index = i;
            views[i].setOnClickListener(view12 -> {
                if (index >= THEMES.length) {
                    mTempViews = views;
                    ImageUtils.showImagePicker(this, PICK_THEME_CODE);
                } else {
                    setTheme(views, index);
                }
            });
        }
    }


    private void showSettingsPlatforms() {
        Dialog d = showPopup(R.layout.dialog_platforms);

        CheckBox android = d.findViewById(R.id.checkbox_android);
        android.setChecked(mPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_ANDROID, true));
        android.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_ANDROID, value);
            editor.apply();
            reloadUI();
        });
        d.findViewById(R.id.layout_android).setVisibility(new AndroidPlatform().isSupported(this) ? View.VISIBLE : View.GONE);

        CheckBox psp = d.findViewById(R.id.checkbox_psp);
        psp.setChecked(mPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_PSP, true));
        psp.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_PSP, value);
            editor.apply();
            reloadUI();
        });
        d.findViewById(R.id.layout_psp).setVisibility(new PSPPlatform().isSupported(this) ? View.VISIBLE : View.GONE);

        CheckBox vr = d.findViewById(R.id.checkbox_vr);
        vr.setChecked(mPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_VR, true));
        vr.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_VR, value);
            editor.apply();
            reloadUI();
        });
        d.findViewById(R.id.layout_vr).setVisibility(new VRPlatform().isSupported(this) ? View.VISIBLE : View.GONE);
    }

    private void showSettingsTweaks() {
        Dialog d = showPopup(R.layout.dialog_tweaks);

        d.findViewById(R.id.service_app_shortcut).setOnClickListener(view -> {
            ButtonManager.isAccesibilityInitialized(this);
            ButtonManager.requestAccessibility(this);
        });
        d.findViewById(R.id.service_explore_app).setOnClickListener(view -> openAppDetails("com.oculus.explore"));
        d.findViewById(R.id.service_os_updater).setOnClickListener(view -> openAppDetails("com.oculus.updater"));
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
            if (mFocus) {
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
