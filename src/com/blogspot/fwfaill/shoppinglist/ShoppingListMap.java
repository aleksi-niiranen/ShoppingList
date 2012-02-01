package com.blogspot.fwfaill.shoppinglist;

import java.io.IOException;
import java.util.List;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

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
		fillMap();
	}
	
	private void fillMap() {
		Cursor cursor = mDbHelper.fetchAllShoppingLists();
		startManagingCursor(cursor);
		
		if (cursor.moveToFirst()) {
			do {
				try {
					String location = cursor.getString(cursor.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LOCATION));
					List<Address> address = mGeocoder.getFromLocationName(location, 1);
					int lat = (int) (address.get(0).getLatitude() * 1e6);
					int lon = (int) (address.get(0).getLongitude() * 1e6);
					GeoPoint point = new GeoPoint(lat, lon);
					OverlayItem overlayItem = new OverlayItem(point, "", "");
					itemizedOverlay.addOverlay(overlayItem);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO: location name is null
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
			mapOverlays.add(itemizedOverlay);
		}
		mDbHelper.close();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
