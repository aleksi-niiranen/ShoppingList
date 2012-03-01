package com.blogspot.fwfaill.shoppinglist;

import com.google.android.maps.OverlayItem;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BalloonOverlayView extends FrameLayout {

	private LinearLayout mLayout;
	private TextView mItemTitle;
	private TextView mItemSnippet;
	
	public BalloonOverlayView(Context context, int balloonBottomOffset) {
		super(context);
		
		setPadding(0, 0, 0, balloonBottomOffset);
		
		mLayout = new LinearLayout(context);
		mLayout.setVisibility(VISIBLE);
		
		setupView(context, mLayout);
		
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;
		
		addView(mLayout, params);
	}

	private void setupView(Context context, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.balloonoverlay, parent);
		mItemTitle = (TextView) v.findViewById(R.id.balloon_item_title);
		mItemSnippet = (TextView) v.findViewById(R.id.balloon_item_snippet);
	}
	
	public void setData(OverlayItem item) {
		mLayout.setVisibility(VISIBLE);
		setBalloonData(item, mLayout);
	}
	
	private void setBalloonData(OverlayItem item, ViewGroup parent) {
		mItemTitle.setVisibility(VISIBLE);
		mItemTitle.setText(item.getTitle());
		mItemSnippet.setVisibility(VISIBLE);
		mItemSnippet.setText(item.getSnippet());
	}
}
