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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class UpdateDetector {
    private static final String UPDATE_URL = "https://api.github.com/repos/basti564/DreamGrid/releases/latest";
    private static final String LAST_UPDATE_CHECK_TIME_KEY = "lastUpdateCheckTime";
    private static final String TAG = "DreamGrid";

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
        StringRequest updateRequest = new StringRequest(
                Request.Method.GET, UPDATE_URL,
                this::handleUpdateResponse, this::handleUpdateError);

        requestQueue.add(updateRequest);
    }

    public void checkForUpdateIfIntervalElapsed(long updateCheckInterval) {
        long lastUpdateCheckTime = sharedPreferences.getLong(LAST_UPDATE_CHECK_TIME_KEY, 0);
        long currentTime = System.currentTimeMillis();

        if (updateCheckInterval == 0 || currentTime - lastUpdateCheckTime >= updateCheckInterval) {
            StringRequest updateRequest = new StringRequest(
                    Request.Method.GET, UPDATE_URL,
                    this::handleUpdateResponse, this::handleUpdateError);

            requestQueue.add(updateRequest);
        }
    }

    private void handleUpdateResponse(String response) {
        try {
            JSONObject latestReleaseJson = new JSONObject(response);
            String tagName = latestReleaseJson.getString("tag_name");
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    appContext.getPackageName(), PackageManager.GET_ACTIVITIES);

            if (!("v" + packageInfo.versionName).equals(tagName)) {
                Log.v(TAG, "New version available!!!!");
                showUpdateDialog(latestReleaseJson, tagName);
            } else {
                Log.i(TAG, "DreamGrid is up to date :)");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Received invalid JSON", e);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
    }

    private void handleUpdateError(VolleyError error) {
        Log.w(TAG, "Couldn't get update info", error);
    }

    private void showUpdateDialog(JSONObject latestReleaseJson, String tagName) {
        AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(appContext);
        updateDialogBuilder.setTitle("An update is available!");
        updateDialogBuilder.setMessage("We recommend you to update to the latest version of DreamGrid (" +
                tagName + ")");
        updateDialogBuilder.setPositiveButton("View", (dialog, which) -> {
            try {
                String htmlUrl = latestReleaseJson.getString("html_url");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl));
                appContext.startActivity(browserIntent);
            } catch (JSONException e) {
                Log.e(TAG, "Unable to parse JSON for html_url", e);
            }
        });
        updateDialogBuilder.setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss());
        AlertDialog updateAlertDialog = updateDialogBuilder.create();
        updateAlertDialog.show();
        updateAlertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }
}