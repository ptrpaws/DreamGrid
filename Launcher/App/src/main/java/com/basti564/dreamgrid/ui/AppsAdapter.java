package com.basti564.dreamgrid.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.basti564.dreamgrid.ImageUtils;
import com.basti564.dreamgrid.MainActivity;
import com.basti564.dreamgrid.R;
import com.basti564.dreamgrid.SettingsProvider;
import com.basti564.dreamgrid.platforms.AbstractPlatform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppsAdapter extends BaseAdapter {
    private static Drawable iconDrawable;
    private static File iconFile;
    private static String packageName;
    private static long lastClickTime;
    private final MainActivity mainActivityContext;
    private final List<ApplicationInfo> appList;
    private final boolean isEditMode;
    private final boolean showTextLabels;
    private final int itemScale;
    private final SettingsProvider settingsProvider;

    public AppsAdapter(MainActivity context, boolean editMode, int scale, boolean names) {
        mainActivityContext = context;
        isEditMode = editMode;
        showTextLabels = names;
        itemScale = scale;
        settingsProvider = SettingsProvider.getInstance(mainActivityContext);

        ArrayList<String> sortedGroups = settingsProvider.getAppGroupsSorted(false);
        ArrayList<String> sortedSelectedGroups = settingsProvider.getAppGroupsSorted(true);
        boolean isFirstGroupSelected = !sortedSelectedGroups.isEmpty() && !sortedGroups.isEmpty() && sortedSelectedGroups.get(0).compareTo(sortedGroups.get(0)) == 0;
        appList = settingsProvider.getInstalledApps(context, sortedSelectedGroups, isFirstGroupSelected);
    }

    public int getCount() {
        return appList.size();
    }

    public Object getItem(int position) {
        return appList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final ApplicationInfo currentApp = appList.get(position);
        LayoutInflater layoutInflater = (LayoutInflater) mainActivityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View gridView = layoutInflater.inflate(R.layout.lv_app, parent, false);

        // Set size of items
        RelativeLayout layout = gridView.findViewById(R.id.layout);
        ViewGroup.LayoutParams params = layout.getLayoutParams();
        params.width = itemScale;
        params.height = itemScale;
        layout.setLayoutParams(params);

        // set value into textview
        PackageManager packageManager = mainActivityContext.getPackageManager();
        String name = SettingsProvider.getAppDisplayName(mainActivityContext, currentApp.packageName, currentApp.loadLabel(packageManager));
        ProgressBar progressBar = gridView.findViewById(R.id.progress_bar);
        TextView textView = gridView.findViewById(R.id.textLabel);
        textView.setText(name);
        textView.setVisibility(showTextLabels ? View.VISIBLE : View.GONE);

        if (isEditMode) {
            // short click for app details, long click to activate drag and drop
            layout.setOnTouchListener((view, motionEvent) -> {
                if ((motionEvent.getAction() == MotionEvent.ACTION_DOWN) ||
                        (motionEvent.getAction() == MotionEvent.ACTION_POINTER_DOWN)) {
                    packageName = currentApp.packageName;
                    lastClickTime = System.currentTimeMillis();
                    ClipData dragData = ClipData.newPlainText(name, name);
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    view.startDragAndDrop(dragData, shadowBuilder, view, 0);
                }
                return false;
            });

            // drag and drop
            layout.setOnDragListener((view, event) -> {
                if (currentApp.packageName.compareTo(packageName) == 0) {
                    if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                        view.setVisibility(View.INVISIBLE);
                    } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                        mainActivityContext.reloadUI();
                    } else if (event.getAction() == DragEvent.ACTION_DROP) {
                        if (System.currentTimeMillis() - lastClickTime < 250) {
                            showAppDetails(currentApp);
                        } else {
                            mainActivityContext.reloadUI();
                        }
                    }
                    return event.getAction() != DragEvent.ACTION_DROP;
                }
                return true;
            });
        } else {
            layout.setOnClickListener(view -> {
                progressBar.setVisibility(View.VISIBLE);
                mainActivityContext.openApp(currentApp);
            });
            layout.setOnLongClickListener(view -> {
                showAppDetails(currentApp);
                return false;
            });
        }

        // set application icon
        AbstractPlatform appPlatform = AbstractPlatform.getPlatform(currentApp);
        ImageView imageView = gridView.findViewById(R.id.imageLabel);
        appPlatform.loadIcon(mainActivityContext, imageView, currentApp, name);

        return gridView;
    }

    public void onImageSelected(String path, ImageView selectedImageView) {
        AbstractPlatform.clearIconCache();
        if (path != null) {
            Bitmap bitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(path), 450);
            ImageUtils.saveBitmap(bitmap, iconFile);
            selectedImageView.setImageBitmap(bitmap);
        } else {
            selectedImageView.setImageDrawable(iconDrawable);
            AbstractPlatform.updateIcon(selectedImageView, iconFile, packageName);
        }
        mainActivityContext.reloadUI();
        this.notifyDataSetChanged(); // for real time updates
    }

    private void showAppDetails(ApplicationInfo currentApp) {
        // set layout
        Context context = mainActivityContext;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setView(R.layout.dialog_app_details);
        AlertDialog appDetailsDialog = dialogBuilder.create();
        appDetailsDialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        appDetailsDialog.show();

        // info action
        appDetailsDialog.findViewById(R.id.info).setOnClickListener(view13 -> mainActivityContext.openAppDetails(currentApp.packageName));

        // set name
        PackageManager packageManager = mainActivityContext.getPackageManager();
        String name = SettingsProvider.getAppDisplayName(mainActivityContext, currentApp.packageName, currentApp.loadLabel(packageManager));
        final EditText appNameEditText = appDetailsDialog.findViewById(R.id.app_name);
        appNameEditText.setText(name);
        appDetailsDialog.findViewById(R.id.ok).setOnClickListener(view12 -> {
            settingsProvider.setAppDisplayName(context, currentApp, appNameEditText.getText().toString());
            mainActivityContext.reloadUI();
            appDetailsDialog.dismiss();
        });

        // load icon
        ImageView tempImage = appDetailsDialog.findViewById(R.id.app_icon);
        AbstractPlatform appPlatform = AbstractPlatform.getPlatform(currentApp);
        appPlatform.loadIcon(mainActivityContext, tempImage, currentApp, name);

        tempImage.setOnClickListener(iconPickerView -> {
            iconDrawable = currentApp.loadIcon(packageManager);
            packageName = currentApp.packageName;
            iconFile = AbstractPlatform.packageToPath(mainActivityContext, currentApp.packageName);
            if (iconFile.exists()) {
                iconFile.delete();
            }
            mainActivityContext.setSelectedImageView(tempImage);
            ImageUtils.showImagePicker(mainActivityContext, MainActivity.PICK_ICON_CODE);
        });
    }

    public String getSelectedPackage() {
        return packageName;
    }
}
