package com.basti564.dreamgrid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.WindowManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class UpdateDetector {
    private final RequestQueue requestQueue;
    private final PackageManager packageManager;
    private final Context appContext;
    private final SharedPreferences sharedPreferences;

    public UpdateDetector(Context appContext, SharedPreferences sharedPreferences) {
        this.appContext = appContext;
        this.requestQueue = Volley.newRequestQueue(appContext);
        this.packageManager = appContext.getPackageManager();
        this.sharedPreferences = sharedPreferences;
    }

    public void checkForUpdate() {
        checkForUpdateIfIntervalElapsed(0);
    }

    public void checkForUpdateIfIntervalElapsed(long interval) {
        long lastUpdateCheckTime = sharedPreferences.getLong("lastUpdateCheckTime", 0);
        long currentTime = System.currentTimeMillis();

        if (interval == 0 || currentTime - lastUpdateCheckTime >= interval) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(
                        appContext.getPackageName(), PackageManager.GET_ACTIVITIES);

                StringRequest updateRequest = new StringRequest(
                        Request.Method.GET, "https://api.github.com/repos/basti564/DreamGrid/releases/latest",
                        response -> {
                            try {
                                JSONObject latestReleaseJson = (JSONObject) new JSONTokener(response).nextValue();
                                if (!("v" + packageInfo.versionName).equals(latestReleaseJson.getString("tag_name"))) {
                                    Log.v("DreamGrid", "New version available!!!!");

                                    AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(appContext);
                                    updateDialogBuilder.setTitle("An update is available!");
                                    updateDialogBuilder.setMessage("We recommend you to update to the latest version of DreamGrid (" +
                                            latestReleaseJson.getString("tag_name") + ")");
                                    updateDialogBuilder.setPositiveButton("View", (dialog, which) -> {
                                        Intent browserIntent = null;
                                        try {
                                            browserIntent = new Intent(Intent.ACTION_VIEW,
                                                    Uri.parse(latestReleaseJson.getString("html_url")));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        appContext.startActivity(browserIntent);
                                    });
                                    updateDialogBuilder.setNegativeButton("Dismiss", (dialog, which) -> {
                                        dialog.dismiss();
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putLong("lastUpdateCheckTime", currentTime);
                                        editor.apply();
                                    });
                                    AlertDialog updateAlertDialog = updateDialogBuilder.create();
                                    updateAlertDialog.show();
                                    updateAlertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                                } else {
                                    Log.i("DreamGrid", "DreamGrid is up to date :)");
                                }
                            } catch (JSONException e) {
                                Log.e("DreamGrid", "Received invalid JSON", e);
                            }
                        }, error -> Log.w("DreamGrid", "Couldn't get update info"));

                requestQueue.add(updateRequest);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
