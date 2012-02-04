package com.blogspot.fwfaill.shoppinglist;

import java.io.IOException;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class EditList extends ListActivity {

	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;

	// for context menu
	private static final int EDIT_ID = R.id.edititem;
	private static final int DELETE_ID = R.id.deleteitem;

	private EditText mListTitleText;
	private EditText mLocationText;
	private Integer mLatitude;
	private Integer mLongitude;
	private Long mRowId;
	private ShoppingListDbAdapter mDbHelper;
	private ShoppingListCursorAdapter mListAdapter;
	private Geocoder mGeocoder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();

		setContentView(R.layout.editlist);
		setTitle(R.string.edit_list);

		mListTitleText = (EditText) findViewById(R.id.txtShopName);
		mLocationText = (EditText) findViewById(R.id.txtLocation);
		mLatitude = null;
		mLongitude = null;
		mGeocoder = new Geocoder(this);

		mRowId = (savedInstanceState == null) ? null : (Long) savedInstanceState.getSerializable(ShoppingListDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : null;
		}

		populateFields();
		registerForContextMenu(getListView());

		Button saveList = (Button) findViewById(R.id.btnSaveList);
		Button addItem = (Button) findViewById(R.id.btnAddItem);
		ImageButton locate = (ImageButton) findViewById(R.id.btnLocate);

		saveList.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_OK);
				finish();
			}
		});

		addItem.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// call saveState() to make sure the list exists in the database
				saveState();
				addItemToList();
			}
		});

		locate.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				saveState();
				showMap();
			}
		});
	}

	private void showMap() {
		Intent i = new Intent(this, ShoppingListMap.class);
		if (mLatitude != null && mLongitude != null) {
			i.putExtra("renderOne", true);
			i.putExtra("lat", mLatitude);
			i.putExtra("lon", mLongitude);
		}
		startActivity(i);
	}

	private void addItemToList() {
		Intent i = new Intent(this, EditItem.class);
		i.putExtra("listId", mRowId);
		startActivityForResult(i, ACTIVITY_CREATE);
	}

	private void populateFields() {
		if (mRowId != null) {
			Cursor shoppingList = mDbHelper.fetchShoppingList(mRowId);
			startManagingCursor(shoppingList);
			// fetch texts
			mListTitleText.setText(shoppingList.getString(shoppingList.getColumnIndexOrThrow(
					ShoppingListDbAdapter.KEY_TITLE)));
			mLocationText.setText(shoppingList.getString(shoppingList.getColumnIndexOrThrow(
					ShoppingListDbAdapter.KEY_LOCATION)));
			// fetch coordinates
			if (!shoppingList.isNull(shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT)))
				mLatitude = shoppingList.getInt(shoppingList.getColumnIndexOrThrow(
						ShoppingListDbAdapter.KEY_LAT));
			if (!shoppingList.isNull(shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON)))
				mLongitude = shoppingList.getInt(shoppingList.getColumnIndexOrThrow(
						ShoppingListDbAdapter.KEY_LON));
			// fetch list items
			Cursor listItemsCursor = mDbHelper.fetchItemsOnList(mRowId);
			startManagingCursor(listItemsCursor);
			mListAdapter = new ShoppingListCursorAdapter(this, listItemsCursor);
			setListAdapter(mListAdapter);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		outState.putSerializable(ShoppingListDbAdapter.KEY_ROWID, mRowId);
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateFields();
	}

	private void saveState() {
		String listTitle = mListTitleText.getText().toString();
		String location = mLocationText.getText().toString();
		if (listTitle.isEmpty()) listTitle = "default";
		
		// do geocoding in background thread
		// TODO: is this OK?
		new LocateTask().execute(location);
		
		if (mRowId == null) {
			long id = mDbHelper.createShoppingList(listTitle, location, mLatitude, mLongitude);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateShoppingList(mRowId, listTitle, location, mLatitude, mLongitude);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contextmenu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case EDIT_ID:
			info = (AdapterContextMenuInfo) item.getMenuInfo();
			Intent i = new Intent(this, EditItem.class);
			i.putExtra(ShoppingListDbAdapter.KEY_ROWID, info.id);
			i.putExtra("listId", mRowId);
			startActivityForResult(i, ACTIVITY_EDIT);
			return true;
		case DELETE_ID:
			info = (AdapterContextMenuInfo) item.getMenuInfo();
			mDbHelper.deleteShoppingListItem(info.id);
			populateFields();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		CheckedTextView nameText = (CheckedTextView) v.findViewById(R.id.itemtext1);
		TextView quantityText = (TextView) v.findViewById(R.id.itemtext2);
		nameText.setChecked(!nameText.isChecked());
		mDbHelper.updateShoppingListItem(id, nameText.isChecked() ? 1 : 0);
		if (nameText.isChecked()) {
			nameText.setPaintFlags(nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			quantityText.setPaintFlags(quantityText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
		} else {
			nameText.setPaintFlags(nameText.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
			quantityText.setPaintFlags(quantityText.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		populateFields();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
	}

	private class ShoppingListCursorAdapter extends ResourceCursorAdapter {

		private LayoutInflater mInflater;

		public ShoppingListCursorAdapter(Context context, Cursor cursor) {
			super(context, R.layout.itemrow, cursor, false);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mInflater.inflate(R.layout.itemrow, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			CheckedTextView nameText = (CheckedTextView) view.findViewById(R.id.itemtext1);
			TextView quantityText = (TextView) view.findViewById(R.id.itemtext2);

			nameText.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow(
					ShoppingListDbAdapter.KEY_PICKED_UP)) == 1 ? true : false);

			if (nameText.isChecked()) {
				nameText.setPaintFlags(nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				quantityText.setPaintFlags(quantityText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			}

			nameText.setText(cursor.getString(cursor.getColumnIndexOrThrow(
					ShoppingListDbAdapter.KEY_ITEM_TITLE)));
			quantityText.setText(cursor.getString(cursor.getColumnIndexOrThrow(
					ShoppingListDbAdapter.KEY_QUANTITY)));
		}
	}
	
	private class LocateTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			/* Is fetching coordinates from database faster than geocoding each time
			 * when map is displayed? */
			try {
				// get coordinates
				List<Address> address = mGeocoder.getFromLocationName(params[0], 1);
				if (!address.isEmpty()) {
					mLatitude = (int) (address.get(0).getLatitude() * 1e6);
					mLongitude = (int) (address.get(0).getLongitude() * 1e6);
				}
			} catch (IOException e) {
				Log.e("EditList", "service unavailable");
				mLatitude = null;
				mLongitude = null;
			} catch (IllegalArgumentException e) {
				Log.e("EditList", "location is null");
				mLatitude = null;
				mLongitude = null;
			} catch (IllegalStateException e) {
				Log.e("EditList", "no coordinates assigned to address");
				mLatitude = null;
				mLongitude = null;
			}
			return null;
		}
	}
}
