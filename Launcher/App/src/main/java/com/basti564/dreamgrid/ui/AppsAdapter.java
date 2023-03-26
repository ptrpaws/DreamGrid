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

public class AppsAdapter extends BaseAdapter
{
    private final MainActivity mContext;
    private final List<ApplicationInfo> mInstalledApps;
    private final boolean mEditMode;
    private final boolean mNames;
    private final int mScale;
    private final SettingsProvider mSettings;

    private static Drawable mTempIcon;
    private static File mTempFile;
    private static ImageView mTempImage;
    private static String mTempPackage;
    private static long mTempTimestamp;

    public AppsAdapter(MainActivity context, boolean editMode, int scale, boolean names)
    {
        mContext = context;
        mEditMode = editMode;
        mNames = names;
        mScale = scale;
        mSettings = SettingsProvider.getInstance(mContext);

        ArrayList<String> groups = mSettings.getAppGroupsSorted(false);
        ArrayList<String> selected = mSettings.getAppGroupsSorted(true);
        boolean first = !selected.isEmpty() && selected.get(0).compareTo(groups.get(0)) == 0;
        mInstalledApps = mSettings.getInstalledApps(context, selected, first);
    }

    public int getCount()
    {
        return mInstalledApps.size();
    }

    public Object getItem(int position)
    {
        return mInstalledApps.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        final ApplicationInfo actApp = mInstalledApps.get(position);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View gridView = inflater.inflate(R.layout.lv_app, parent, false);

        // Set size of items
        RelativeLayout layout = gridView.findViewById(R.id.layout);
        ViewGroup.LayoutParams params = layout.getLayoutParams();
        params.width = mScale;
        params.height = mScale;
        layout.setLayoutParams(params);

        // set value into textview
        PackageManager pm = mContext.getPackageManager();
        String name = SettingsProvider.getAppDisplayName(mContext, actApp.packageName, actApp.loadLabel(pm));
        ProgressBar progressBar = gridView.findViewById(R.id.progress_bar);
        TextView textView = gridView.findViewById(R.id.textLabel);
        textView.setText(name);
        textView.setVisibility(mNames ? View.VISIBLE : View.GONE);

        if (mEditMode) {
            // short click for app details, long click to activate drag and drop
            layout.setOnTouchListener((view, motionEvent) -> {
                if ((motionEvent.getAction() == MotionEvent.ACTION_DOWN) ||
                        (motionEvent.getAction() == MotionEvent.ACTION_POINTER_DOWN)) {
                    mTempPackage = actApp.packageName;
                    mTempTimestamp = System.currentTimeMillis();
                    ClipData data = ClipData.newPlainText(name, name);
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    view.startDragAndDrop(data, shadowBuilder, view, 0);
                }
                return false;
            });

            // drag and drop
            layout.setOnDragListener((view, event) -> {
                if (actApp.packageName.compareTo(mTempPackage) == 0) {
                    if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                        view.setVisibility(View.INVISIBLE);
                    } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                        mContext.reloadUI();
                    } else if (event.getAction() == DragEvent.ACTION_DROP) {
                        if (System.currentTimeMillis() - mTempTimestamp < 250) {
                            showAppDetails(actApp);
                        } else {
                            mContext.reloadUI();
                        }
                    }
                    return event.getAction() != DragEvent.ACTION_DROP;
                }
                return true;
            });
        } else {
            layout.setOnClickListener(view -> {
                progressBar.setVisibility(View.VISIBLE);
                mContext.openApp(actApp);
            });
            layout.setOnLongClickListener(view -> {
                showAppDetails(actApp);
                return false;
            });
        }

        // set application icon
        AbstractPlatform platform = AbstractPlatform.getPlatform(actApp);
        ImageView imageView = gridView.findViewById(R.id.imageLabel);
        platform.loadIcon(mContext, imageView, actApp, name);

        return gridView;
    }

    public void onImageSelected(String path) {
        AbstractPlatform.clearIconCache();
        if (path != null) {
            Bitmap bitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(path), 450);
            ImageUtils.saveBitmap(bitmap, mTempFile);
            mTempImage.setImageBitmap(bitmap);
        } else {
            mTempImage.setImageDrawable(mTempIcon);
            AbstractPlatform.updateIcon(mTempImage, mTempFile, mTempPackage);
        }
        mContext.reloadUI();
    }

    private void showAppDetails(ApplicationInfo actApp) {

        //set layout
        Context context = mContext;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(R.layout.dialog_app_details);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        dialog.show();

        //info action
        dialog.findViewById(R.id.info).setOnClickListener(view13 -> mContext.openAppDetails(actApp.packageName));

        //set name
        PackageManager pm = mContext.getPackageManager();
        String name = SettingsProvider.getAppDisplayName(mContext, actApp.packageName, actApp.loadLabel(pm));
        final EditText input = dialog.findViewById(R.id.app_name);
        input.setText(name);
        dialog.findViewById(R.id.ok).setOnClickListener(view12 -> {
            mSettings.setAppDisplayName(context, actApp, input.getText().toString());
            mContext.reloadUI();
            dialog.dismiss();
        });

        //set icon
        mTempImage = dialog.findViewById(R.id.app_icon);
        mTempImage.setOnClickListener(view1 -> {
            mTempIcon = actApp.loadIcon(pm);
            mTempPackage = actApp.packageName;
            mTempFile = AbstractPlatform.pkg2path(mContext, actApp.packageName);
            if (mTempFile.exists()) {
                mTempFile.delete();
            }
            ImageUtils.showImagePicker(mContext, MainActivity.PICK_ICON_CODE);
        });
        AbstractPlatform platform = AbstractPlatform.getPlatform(actApp);
        platform.loadIcon(mContext, mTempImage, actApp, name);
    }

    public String getSelectedPackage() {
        return mTempPackage;
    }
}
