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
	private Drawable mDrawable;
	private ShoppingListItemizedOverlay mItemizedOverlay;
	private ShoppingListDbAdapter mDbHelper;
	private Geocoder mGeocoder;
	private FillMapTask mFillMapTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();
		setContentView(R.layout.mapmain);
		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapOverlays = mMapView.getOverlays();
		mDrawable = this.getResources().getDrawable(R.drawable.androidmarker);
		mItemizedOverlay = new ShoppingListItemizedOverlay(mDrawable);
		mGeocoder = new Geocoder(this);
		mFillMapTask = new FillMapTask();
		Bundle extras = getIntent().getExtras();
		long rowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : 0;
		fillMap(rowId);
	}

	private void fillMap(long rowId) {
		Cursor cursor;
		if (rowId == 0) {
			cursor = mDbHelper.fetchAllShoppingLists();
		} 
		else {
			cursor = mDbHelper.fetchShoppingList(rowId);
		}
		
		mFillMapTask.execute(cursor);
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
	public void onBackPressed() {
		// cancel FillMapTask
		mFillMapTask.cancel(true);
		super.onBackPressed();
	}
	
	private class FillMapTask extends AsyncTask<Cursor, Void, Void> {
		
		@Override
		protected Void doInBackground(Cursor... params) {
			startManagingCursor(params[0]);

			if (params[0].moveToFirst()) {
				do {
					// check for nulls in latitude and longitude columns
					if (params[0].isNull(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT)) ||
							params[0].isNull(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON))) {
						// coordinates not found in database, geocode from location name
						try {
							String location = params[0].getString(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LOCATION));
							List<Address> address = mGeocoder.getFromLocationName(location, 1);
							if (!address.isEmpty()) {
								Log.i("ShoppingListMap", "geocoding");
								int lat = (int) (address.get(0).getLatitude() * 1e6);
								int lon = (int) (address.get(0).getLongitude() * 1e6);
								GeoPoint point = new GeoPoint(lat, lon);
								OverlayItem overlayItem = new OverlayItem(point, "", "");
								mItemizedOverlay.addOverlay(overlayItem);
								// update coordinates in database
								mDbHelper.updateShoppingList(params[0].getLong(params[0].getColumnIndexOrThrow(
										ShoppingListDbAdapter.KEY_ROWID)), lat, lon);
							}
						} catch (IOException e) {
							Log.e("ShoppingListMap", "service unavailable");
						} catch (IllegalArgumentException e) {
							Log.e("ShoppingListMap", "location is null");
						} catch (IllegalStateException e) {
							Log.e("EditList", "no coordinates assigned to address");
						}
					} else {
						// coordinates found from database
						int lat = params[0].getInt(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT));
						int lon = params[0].getInt(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON));
						GeoPoint point = new GeoPoint(lat, lon);
						OverlayItem overlayItem = new OverlayItem(point, "", "");
						mItemizedOverlay.addOverlay(overlayItem);
					}
					publishProgress();
				} while (params[0].moveToNext());
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Void... params) {
			if (mItemizedOverlay.size() > 0) mMapOverlays.add(mItemizedOverlay);
		}
	}
}
