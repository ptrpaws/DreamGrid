package com.basti564.dreamgrid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.Gravity;
import android.view.WindowManager;

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
}