package com.blogspot.fwfaill.shoppinglist;

import java.io.IOException;
import java.util.List;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
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

	private MapView mapView;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private ShoppingListItemizedOverlay itemizedOverlay;
	private ShoppingListDbAdapter mDbHelper;
	private Geocoder mGeocoder;

	private static final int LIST_ID = R.id.showlist;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();
		setContentView(R.layout.mapmain);
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		mapOverlays = mapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.androidmarker);
		itemizedOverlay = new ShoppingListItemizedOverlay(drawable);
		mGeocoder = new Geocoder(this);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.getBoolean("renderOne")) addMarker(extras.getInt("lat"), extras.getInt("lon"));	
			if (extras.getBoolean("renderAll")) fillMap();
		}
	}

	// TODO: move to another thread
	private void fillMap() {
		Cursor cursor = mDbHelper.fetchAllShoppingLists();
		startManagingCursor(cursor);

		if (cursor.moveToFirst()) {
			do {
				if (cursor.isNull(cursor.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT)) ||
						cursor.isNull(cursor.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON))) {
					// coordinates not found in database, geocode from location name
					try {
						String location = cursor.getString(cursor.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LOCATION));
						List<Address> address = mGeocoder.getFromLocationName(location, 1);
						if (!address.isEmpty()) {
							int lat = (int) (address.get(0).getLatitude() * 1e6);
							int lon = (int) (address.get(0).getLongitude() * 1e6);
							GeoPoint point = new GeoPoint(lat, lon);
							OverlayItem overlayItem = new OverlayItem(point, "", "");
							itemizedOverlay.addOverlay(overlayItem);
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
					int lat = cursor.getInt(cursor.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT));
					int lon = cursor.getInt(cursor.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON));
					GeoPoint point = new GeoPoint(lat, lon);
					OverlayItem overlayItem = new OverlayItem(point, "", "");
					itemizedOverlay.addOverlay(overlayItem);
				}

			} while (cursor.moveToNext());
			if (itemizedOverlay.size() > 0) mapOverlays.add(itemizedOverlay);
		}
		mDbHelper.close();
	}

	private void addMarker(int lat, int lon) {
		GeoPoint point = new GeoPoint(lat, lon);
		OverlayItem overlayItem = new OverlayItem(point, "", "");
		itemizedOverlay.addOverlay(overlayItem);
		mapOverlays.add(itemizedOverlay);
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
}
