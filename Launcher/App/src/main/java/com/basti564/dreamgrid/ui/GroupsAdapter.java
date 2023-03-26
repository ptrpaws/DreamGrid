package com.basti564.dreamgrid.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.basti564.dreamgrid.MainActivity;
import com.basti564.dreamgrid.R;
import com.basti564.dreamgrid.SettingsProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupsAdapter extends BaseAdapter {
    public static final int MAX_GROUPS = 12;
    public static final String HIDDEN_GROUP = "HIDDEN!";

    private final MainActivity mainActivity;
    private final List<String> appGroups;
    private final Set<String> selectedGroups;
    private final SettingsProvider settingsProvider;
    private final boolean isEditMode;

    /**
     * Create new adapter
     */
    public GroupsAdapter(MainActivity activity, boolean editMode) {
        mainActivity = activity;
        isEditMode = editMode;
        settingsProvider = SettingsProvider.getInstance(activity);

        SettingsProvider settings = SettingsProvider.getInstance(mainActivity);
        appGroups = settings.getAppGroupsSorted(false);
        if (editMode) {
            appGroups.add(HIDDEN_GROUP);
            appGroups.add("+ " + mainActivity.getString(R.string.add_group));
        }
        selectedGroups = settings.getSelectedGroups();
    }

    public int getCount() {
        return appGroups.size();
    }

    public String getItem(int position) {
        return appGroups.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.lv_group, parent, false);

        if (position >= MAX_GROUPS - 1) {
            itemView.setVisibility(View.GONE);
        }

        // set menu action
        View menu = itemView.findViewById(R.id.menu);
        Drawable drawable = menu.getContext().getDrawable(R.drawable.ic_info);
        assert drawable != null;
        drawable.setColorFilter(Color.parseColor("#90000000"), PorterDuff.Mode.SRC_ATOP);
        menu.setBackground(drawable);
        menu.setOnClickListener(view -> {

            final Map<String, String> apps = settingsProvider.getAppList();
            final Set<String> appGroupsList = settingsProvider.getAppGroups();
            final String oldGroupName = settingsProvider.getAppGroupsSorted(false).get(position);

            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setView(R.layout.dialog_group_details);

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
            dialog.show();

            final EditText groupNameInput = dialog.findViewById(R.id.group_name);
            groupNameInput.setText(oldGroupName);

            dialog.findViewById(R.id.ok).setOnClickListener(view1 -> {
                String newGroupName = groupNameInput.getText().toString();
                if (newGroupName.length() > 0) {
                    appGroupsList.remove(oldGroupName);
                    appGroupsList.add(newGroupName);
                    Map<String, String> updatedAppList = new HashMap<>();
                    for (String packageName : apps.keySet()) {
                        if (apps.get(packageName).compareTo(oldGroupName) == 0) {
                            updatedAppList.put(packageName, newGroupName);
                        } else {
                            updatedAppList.put(packageName, apps.get(packageName));
                        }
                    }
                    HashSet<String> selectedGroup = new HashSet<>();
                    selectedGroup.add(newGroupName);
                    settingsProvider.setSelectedGroups(selectedGroup);
                    settingsProvider.setAppGroups(appGroupsList);
                    settingsProvider.setAppList(updatedAppList);
                    mainActivity.reloadUI();
                }
                dialog.dismiss();
            });

            dialog.findViewById(R.id.group_delete).setOnClickListener(view12 -> {
                HashMap<String, String> newAppList = new HashMap<>();
                for (String packageName : apps.keySet()) {
                    if (oldGroupName.equals(apps.get(packageName))) {
                        newAppList.put(packageName, HIDDEN_GROUP);
                    } else {
                        newAppList.put(packageName, apps.get(packageName));
                    }
                }
                settingsProvider.setAppList(newAppList);

                appGroupsList.remove(oldGroupName);
                settingsProvider.setAppGroups(appGroupsList);

                Set<String> firstSelectedGroup = new HashSet<>();
                firstSelectedGroup.add(settingsProvider.getAppGroupsSorted(false).get(0));
                settingsProvider.setSelectedGroups(firstSelectedGroup);
                mainActivity.reloadUI();
                dialog.dismiss();
            });
        });

        // set the look
        setLook(position, itemView, menu);

        // set drag and drop
        itemView.setOnDragListener((view, event) -> {
            if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                int[] colors = new int[]{Color.argb(192, 128, 128, 255), Color.TRANSPARENT};
                GradientDrawable.Orientation orientation = GradientDrawable.Orientation.LEFT_RIGHT;
                itemView.setBackground(new GradientDrawable(orientation, colors));
            } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                setLook(position, view, menu);
            } else if (event.getAction() == DragEvent.ACTION_DROP) {
                // add group or hidden group selection
                String name = appGroups.get(position);
                List<String> appGroupsList = settingsProvider.getAppGroupsSorted(false);
                if (appGroupsList.size() + 1 == position) {
                    name = settingsProvider.addGroup();
                } else if (appGroupsList.size() == position) {
                    name = HIDDEN_GROUP;
                }

                // move app into group
                String packageName = mainActivity.getSelectedPackage();
                Set<String> selectedGroup = settingsProvider.getSelectedGroups();
                Map<String, String> apps = settingsProvider.getAppList();
                apps.remove(packageName);
                apps.put(packageName, name);
                settingsProvider.setAppList(apps);

                // false to dragged icon fly back
                return !selectedGroup.contains(name);
            }
            return true;
        });

        // set value into textview
        TextView textView = itemView.findViewById(R.id.textLabel);
        if (HIDDEN_GROUP.equals(appGroups.get(position))) {
            textView.setText(" -  " + mainActivity.getString(R.string.apps_hidden));
        } else {
            textView.setText(appGroups.get(position));
        }

        return itemView;
    }

    private void setLook(int position, View itemView, View menu) {
        boolean isSelected = selectedGroups.contains(appGroups.get(position));

        if (isSelected) {
            boolean isLeft = (position == 0) || !selectedGroups.contains(appGroups.get(position - 1));
            boolean isRight = (position + 1 >= appGroups.size()) || !selectedGroups.contains(appGroups.get(position + 1));

            int shapeResourceId;
            if (isLeft && isRight) {
                shapeResourceId = R.drawable.selected_tab;
            } else if (isLeft) {
                shapeResourceId = R.drawable.left_selected_tab;
            } else if (isRight) {
                shapeResourceId = R.drawable.right_selected_tab;
            } else {
                shapeResourceId = R.drawable.middle_selected_tab;
            }
            itemView.setBackgroundResource(shapeResourceId);
            TextView textView = itemView.findViewById(R.id.textLabel);
            textView.setTextColor(Color.parseColor("#FF000000")); // set selected tab text color
            if (isEditMode && (position < getCount() - 2)) {
                menu.setVisibility(View.VISIBLE);
            } else {
                menu.setVisibility(View.GONE);
            }
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
            TextView textView = itemView.findViewById(R.id.textLabel);
            textView.setTextColor(Color.parseColor("#90000000")); // set unselected tab text color
            menu.setVisibility(View.GONE);
        }
    }
}