package com.basti564.dreamgrid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class PopupUtils {
    public static Dialog showPopup(Activity activity, int layout) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setView(layout);
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();

        WindowManager.LayoutParams windowLayoutParams = dialog.getWindow().getAttributes();
        windowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowLayoutParams.gravity = Gravity.RIGHT & Gravity.BOTTOM;
        windowLayoutParams.x = 50;
        windowLayoutParams.y = 50;
        dialog.getWindow().setAttributes(windowLayoutParams);
        dialog.findViewById(R.id.layout).requestLayout();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);

        return dialog;
    }

    private static int getWindowHeight(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }
}