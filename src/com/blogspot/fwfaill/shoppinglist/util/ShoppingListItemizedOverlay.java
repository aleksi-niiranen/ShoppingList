/*
 * Copyright 2012 Aleksi Niiranen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.blogspot.fwfaill.shoppinglist.util;

import java.util.ArrayList;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;

import com.blogspot.fwfaill.shoppinglist.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class ShoppingListItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private MapView mMapView;
	private BalloonOverlayView mBalloonView;
	private View mClickRegion;
	private View mCloseRegion;
	private int mBalloonBottomOffset;
	private int mCurrentFocusedIndex;
	private OverlayItem mCurrentFocusedItem;
	private MapController mController;

	public ShoppingListItemizedOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}
	
	public ShoppingListItemizedOverlay(Drawable defaultMarker, MapView mapView) {
		super(boundCenterBottom(defaultMarker));
		mMapView = mapView;
		mBalloonBottomOffset = ((BitmapDrawable) defaultMarker).getBitmap().getHeight();
		mController = mMapView.getController();
	}
	
	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	
	@Override
	protected boolean onTap(int index) {
		mCurrentFocusedIndex = index;
		mCurrentFocusedItem = mOverlays.get(index);
		setLastFocusedIndex(index);
		
		onBalloonOpen(index);
		createAndDisplayBalloonOverlay();
		
		mController.animateTo(mCurrentFocusedItem.getPoint());
		mController.setZoom(18);
		return true;
	}
	
	public void clear() {
		mOverlays.clear();
		populate();
	}

	private boolean createAndDisplayBalloonOverlay() {
		boolean isRecycled;
		if (mBalloonView == null) {
			mBalloonView = createBalloonOverlayView();
			mClickRegion = mBalloonView.findViewById(R.id.balloon_inner);
			mClickRegion.setOnTouchListener(createBalloonTouchListener());
			mCloseRegion = mBalloonView.findViewById(R.id.balloon_close);
			if (mCloseRegion != null) {
				mCloseRegion.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						hideBalloon();
					}
				});
			}
			isRecycled = false;
		} else {
			isRecycled = true;
		}
		
		mBalloonView.setVisibility(View.GONE);
		
		if (mCurrentFocusedItem != null) mBalloonView.setData(mCurrentFocusedItem);
		
		GeoPoint point = mCurrentFocusedItem.getPoint();
		MapView.LayoutParams params = new MapView.LayoutParams(LayoutParams.WRAP_CONTENT, 
				LayoutParams.WRAP_CONTENT, point, MapView.LayoutParams.BOTTOM_CENTER);
		params.mode = MapView.LayoutParams.MODE_MAP;
		
		mBalloonView.setVisibility(View.VISIBLE);
		
		if (isRecycled) {
			mBalloonView.setLayoutParams(params);
		} else {
			mMapView.addView(mBalloonView, params);
		}
		
		return isRecycled;
	}

	private OnTouchListener createBalloonTouchListener() {
		return new OnTouchListener() {
			float startX;
			float startY;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				View l = ((View) v.getParent()).findViewById(R.id.balloon_main);
				Drawable d = l.getBackground();
				
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					int[] states = {android.R.attr.state_pressed};
					if (d.setState(states)) {
						d.invalidateSelf();
					}
					return true;
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					int newStates[] = {};
					if (d.setState(newStates)) {
						d.invalidateSelf();
					}
					if (Math.abs(startX - event.getX()) < 40 &&
							Math.abs(startY - event.getY()) < 40) {
						onBalloonTap(mCurrentFocusedIndex, mCurrentFocusedItem);
					}
					return true;
				} else {
					return false;
				}
			}
		};
	}

	protected boolean onBalloonTap(int index, OverlayItem item) {
		return false;
	}
	
	protected void onBalloonOpen(int index) {
		
	}
	
	public void hideBalloon() {
		if (mBalloonView != null) {
			mBalloonView.setVisibility(View.GONE);
		}
		mCurrentFocusedItem = null;
	}
	
	@Override
	public OverlayItem getFocus() {
		return mCurrentFocusedItem;
	}
	
	@Override
	public void setFocus(OverlayItem item) {
		super.setFocus(item);
		mCurrentFocusedIndex = getLastFocusedIndex();
		mCurrentFocusedItem = item;
		if (mCurrentFocusedItem == null) {
			hideBalloon();
		} else {
			createAndDisplayBalloonOverlay();
		}
	}

	private BalloonOverlayView createBalloonOverlayView() {
		return new BalloonOverlayView(mMapView.getContext(), mBalloonBottomOffset);
	}
}
