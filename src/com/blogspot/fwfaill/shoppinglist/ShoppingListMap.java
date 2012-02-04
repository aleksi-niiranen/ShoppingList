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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ShoppingListMap extends MapActivity {

	private MapView mMapView;
	private List<Overlay> mMapOverlays;
	private Drawable mDrawable;
	private ShoppingListItemizedOverlay mItemizedOverlay;
	private ShoppingListDbAdapter mDbHelper;
	private Geocoder mGeocoder;

	private static final int LIST_ID = R.id.showlist;

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
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.getBoolean("renderOne")) addMarker(extras.getInt("lat"), extras.getInt("lon"));	
			if (extras.getBoolean("renderAll")) fillMap();
		}
	}

	private void fillMap() {
		Cursor cursor = mDbHelper.fetchAllShoppingLists();
		new FillMapTask().execute(cursor);
	}

	private void addMarker(int lat, int lon) {
		GeoPoint point = new GeoPoint(lat, lon);
		OverlayItem overlayItem = new OverlayItem(point, "", "");
		mItemizedOverlay.addOverlay(overlayItem);
		mMapOverlays.add(mItemizedOverlay);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mapmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case LIST_ID:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	private class FillMapTask extends AsyncTask<Cursor, Void, Void> {

		// runs on a background thread
		@Override
		protected Void doInBackground(Cursor... params) {
			startManagingCursor(params[0]);

			if (params[0].moveToFirst()) {
				do {
					if (params[0].isNull(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT)) ||
							params[0].isNull(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON))) {
						// coordinates not found in database, geocode from location name
						try {
							String location = params[0].getString(params[0].getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LOCATION));
							List<Address> address = mGeocoder.getFromLocationName(location, 1);
							if (!address.isEmpty()) {
								int lat = (int) (address.get(0).getLatitude() * 1e6);
								int lon = (int) (address.get(0).getLongitude() * 1e6);
								GeoPoint point = new GeoPoint(lat, lon);
								OverlayItem overlayItem = new OverlayItem(point, "", "");
								mItemizedOverlay.addOverlay(overlayItem);
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

				} while (params[0].moveToNext());
				if (mItemizedOverlay.size() > 0) mMapOverlays.add(mItemizedOverlay);
			}
			mDbHelper.close();
			return null;
		}
	}
}
