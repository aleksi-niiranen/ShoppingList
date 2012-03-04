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

package com.blogspot.fwfaill.shoppinglist;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/**
 * 
 * @author Aleksi Niiranen
 * 
 */
public class ShoppingListMap extends MapActivity {

	private MapView mMapView;
	private List<Overlay> mMapOverlays;
	private Drawable mGreenMarker;
	private Drawable mOrangeMarker;
	private Drawable mRedMarker;
	private ShoppingListDbAdapter mDbHelper;
	private Geocoder mGeocoder;
	private FillMapTask mFillMapTask;
	private long mRowId;

	private static final String TAG = "ShoppingListMap";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();
		setContentView(R.layout.mapmain);
		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapOverlays = mMapView.getOverlays();
		mGreenMarker = this.getResources().getDrawable(R.drawable.androidmarkergreen);
		mOrangeMarker = this.getResources().getDrawable(R.drawable.androidmarkerorange);
		mRedMarker = this.getResources().getDrawable(R.drawable.androidmarkerred);
		mGeocoder = new Geocoder(this);
		Bundle extras = getIntent().getExtras();
		mRowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : 0;
		if (mRowId > 0)
			fillMap(mRowId);
		else
			fillMap();
	}

	private void fillMap(long rowId) {
		Cursor cursor;
		cursor = mDbHelper.fetchShoppingList(rowId);
		mFillMapTask = new FillMapTask();
		mFillMapTask.execute(cursor);
	}

	private void fillMap() {
		Cursor cursor;
		cursor = mDbHelper.fetchAllShoppingLists();
		mFillMapTask = new FillMapTask();
		mFillMapTask.execute(cursor);
	}

	private void clearMapOverlays() {
		if (!mMapOverlays.isEmpty()) {
			mMapOverlays.clear();
			mMapView.invalidate();
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mFillMapTask.cancel(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mRowId > 0)
			fillMap(mRowId);
		else
			fillMap();
	}

	private class FillMapTask extends AsyncTask<Cursor, Void, Void> {

		private ShoppingListItemizedOverlay mGreenOverlay;
		private ShoppingListItemizedOverlay mOrangeOverlay;
		private ShoppingListItemizedOverlay mRedOverlay;
		private long mTodayInMilliseconds;

		public FillMapTask() {
			mGreenOverlay = new ShoppingListItemizedOverlay(mGreenMarker, mMapView);
			mOrangeOverlay = new ShoppingListItemizedOverlay(mOrangeMarker, mMapView);
			mRedOverlay = new ShoppingListItemizedOverlay(mRedMarker, mMapView);
			final Calendar c = Calendar.getInstance();
			mTodayInMilliseconds = c.getTimeInMillis();
		}

		@Override
		protected Void doInBackground(Cursor... params) {
			startManagingCursor(params[0]);

			if (params[0].moveToFirst()) {
				do {
					long dueDateInMilliseconds = params[0].getLong(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_DUE_DATE)) * 1000;
					OverlayItem item = null;
					// check for nulls in latitude and longitude columns
					if (params[0].isNull(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT)) || 
							params[0].isNull(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON))) {
						// coordinates not found in database, geocode from location name
						try {
							String location = params[0].getString(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LOCATION));
							List<Address> address = mGeocoder.getFromLocationName(location, 1);
							if (!address.isEmpty()) {
								Log.i(TAG, "geocoding");
								int lat = (int) (address.get(0).getLatitude() * 1e6);
								int lon = (int) (address.get(0).getLongitude() * 1e6);
								GeoPoint point = new GeoPoint(lat, lon);
								item = new OverlayItem(point, params[0].getString(params[0].getColumnIndexOrThrow(
										ShoppingListDbAdapter.KEY_TITLE)), params[0].getString(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LOCATION)));
								// update coordinates in database
								mDbHelper.updateShoppingList(params[0].getLong(params[0].getColumnIndexOrThrow(
										ShoppingListDbAdapter.KEY_ROWID)), lat, lon);
							}
						} catch (IOException e) {
							Log.e(TAG, "service unavailable");
						} catch (IllegalArgumentException e) {
							Log.e(TAG, "location is null");
						} catch (IllegalStateException e) {
							Log.e(TAG, "no coordinates assigned to address");
						}
					} else {
						// coordinates found from database
						int lat = params[0].getInt(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT));
						int lon = params[0].getInt(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON));
						GeoPoint point = new GeoPoint(lat, lon);
						item = new OverlayItem(point, params[0].getString(params[0].getColumnIndexOrThrow(
								ShoppingListDbAdapter.KEY_TITLE)), params[0].getString(params[0].getColumnIndexOrThrow(
										ShoppingListDbAdapter.KEY_LOCATION)));
					}
					
					if (item != null) {
						if (dueDateInMilliseconds < mTodayInMilliseconds) {
							mRedOverlay.addOverlay(item);
						} else if (dueDateInMilliseconds == mTodayInMilliseconds || 
								(dueDateInMilliseconds - mTodayInMilliseconds) < 432000000) { // 5 days in milliseconds
							mOrangeOverlay.addOverlay(item);
						} else {
							mGreenOverlay.addOverlay(item);
						}
					}
					
					publishProgress();
				} while (params[0].moveToNext());
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... params) {
			clearMapOverlays();
			if (mGreenOverlay.size() > 0) mMapOverlays.add(mGreenOverlay);
			if (mOrangeOverlay.size() > 0) mMapOverlays.add(mOrangeOverlay);
			if (mRedOverlay.size() > 0) mMapOverlays.add(mRedOverlay);
		}
	}
}
