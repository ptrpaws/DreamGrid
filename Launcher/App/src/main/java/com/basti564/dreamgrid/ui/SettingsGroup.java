package com.basti564.dreamgrid.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.basti564.dreamgrid.R;

public class SettingsGroup extends LinearLayout {

    private ImageView mIcon;
    private TextView mText;

    public SettingsGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
        getAttributes(context, attrs, defStyle);
    }

    public SettingsGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttributes(context, attrs, 0);
    }

    public SettingsGroup(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_app, this);
        mText = root.findViewById(R.id.textLabel);
        mIcon = root.findViewById(R.id.imageLabel);
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingsGroup, defStyleAttr, 0);
        mText.setText(a.getString(R.styleable.SettingsGroup_text));
        mText.setSingleLine(false);
        mText.setLines(2);
        mIcon.setImageDrawable(a.getDrawable(R.styleable.SettingsGroup_icon));
        a.recycle();
    }

    public void setIcon(int resource) {
        mIcon.setImageResource(resource);
    }

    public void setText(String value) {
        mText.setText(value);
    }
}