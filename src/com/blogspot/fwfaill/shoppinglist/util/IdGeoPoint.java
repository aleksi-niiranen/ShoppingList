package com.blogspot.fwfaill.shoppinglist.util;

import com.google.android.maps.GeoPoint;

public class IdGeoPoint extends GeoPoint {

	private long id;
	
	public IdGeoPoint(long id, int latitudeE6, int longitudeE6) {
		this(latitudeE6, longitudeE6);
		this.id = id;
	}
	
	public IdGeoPoint(int latitudeE6, int longitudeE6) {
		super(latitudeE6, longitudeE6);
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
}
