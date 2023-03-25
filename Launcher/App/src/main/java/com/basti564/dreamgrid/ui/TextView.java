package com.basti564.dreamgrid.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;

import com.basti564.dreamgrid.R;

public class TextView extends androidx.appcompat.widget.AppCompatTextView {

	private float strokeWidth;
	private Integer strokeColor;
	
	private int[] lockedCompoundPadding;
	private boolean frozen = false;

	public TextView(Context context) {
		super(context);
		init(null);
	}
	public TextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	public TextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}
	
	public void init(AttributeSet attrs){
		if(attrs != null){
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TextView);

			if(a.hasValue(R.styleable.TextView_strokeColor)){
				strokeWidth = a.getDimensionPixelSize(R.styleable.TextView_strokeWidth, 1);
				strokeColor = a.getColor(R.styleable.TextView_strokeColor, 0xff000000);
			}
		}
	}

	@Override
	public void onDraw(Canvas canvas){
		String text = getText().toString();
		setText(text.toUpperCase());

		super.onDraw(canvas);
		
		freeze();
		Drawable restoreBackground = this.getBackground();
		Drawable[] restoreDrawables = this.getCompoundDrawables();
		int restoreColor = this.getCurrentTextColor();
		
		this.setCompoundDrawables(null,  null, null, null);
		this.setTextColor(restoreColor);

		if(strokeColor != null){
			TextPaint paint = this.getPaint();
			paint.setStyle(Style.STROKE);
			this.setTextColor(strokeColor);
			paint.setStrokeWidth(strokeWidth);
			super.onDraw(canvas);
			paint.setStyle(Style.FILL);
			this.setTextColor(restoreColor);
			super.onDraw(canvas);
		}
		
		if(restoreDrawables != null){
			this.setCompoundDrawablesWithIntrinsicBounds(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);
		}
		this.setBackgroundDrawable(restoreBackground);
		this.setTextColor(restoreColor);

		unfreeze();
	}
	
	// Keep these things locked while onDraw in processing
	public void freeze(){
		lockedCompoundPadding = new int[]{
				getCompoundPaddingLeft(),
				getCompoundPaddingRight(),
				getCompoundPaddingTop(),
				getCompoundPaddingBottom()
		};
		frozen = true;
	}
	
	public void unfreeze(){
		frozen = false;
	}
	
    
    @Override
    public void requestLayout(){
        if(!frozen) super.requestLayout();
    }
	
	@Override
	public void postInvalidate(){
		if(!frozen) super.postInvalidate();
	}
	
   @Override
    public void postInvalidate(int left, int top, int right, int bottom){
        if(!frozen) super.postInvalidate(left, top, right, bottom);
    }
	
	@Override
	public void invalidate(){
		if(!frozen)	super.invalidate();
	}
	
	@Override
	public void invalidate(Rect rect){
		if(!frozen) super.invalidate(rect);
	}
	
	@Override
	public void invalidate(int l, int t, int r, int b){
		if(!frozen) super.invalidate(l,t,r,b);
	}
	
	@Override
	public int getCompoundPaddingLeft(){
		return !frozen ? super.getCompoundPaddingLeft() : lockedCompoundPadding[0];
	}
	
	@Override
	public int getCompoundPaddingRight(){
		return !frozen ? super.getCompoundPaddingRight() : lockedCompoundPadding[1];
	}
	
	@Override
	public int getCompoundPaddingTop(){
		return !frozen ? super.getCompoundPaddingTop() : lockedCompoundPadding[2];
	}
	
	@Override
	public int getCompoundPaddingBottom(){
		return !frozen ? super.getCompoundPaddingBottom() : lockedCompoundPadding[3];
	}
}
