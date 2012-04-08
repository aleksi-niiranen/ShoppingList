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

package com.blogspot.fwfaill.shoppinglist.activities;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.location.Geocoder;
import android.net.Uri;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.blogspot.fwfaill.shoppinglist.R;
import com.blogspot.fwfaill.shoppinglist.util.ShoppingListDbAdapter;

/**
 * 
 * @author Aleksi Niiranen
 *
 */
public class EditList extends ListActivity {

	// for context menu
	private static final int EDIT_ID = R.id.edititem;
	private static final int DELETE_ID = R.id.deleteitem;

	private EditText mListTitleText;
	private EditText mLocationText;
	private Long mRowId;
	private ShoppingListDbAdapter mDbHelper;
	private ShoppingListCursorAdapter mListAdapter;
	// store the value of location column so we can determine
	// if the value was changed and new coordinates have to
	// be calculated
	private String mOldLocation;
	
	private int mYear;
	private int mMonth;
	private int mDay;
	static final int DATE_DIALOG_ID = 0;
	private Button mSaveList;
	private Button mDueDate;
	private Button mAddItem;
	private ImageButton mLocate;
	private Calendar mCalendar;
	
	private DatePickerDialog.OnDateSetListener mDateSetListener =
			new DatePickerDialog.OnDateSetListener() {
				
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear,
						int dayOfMonth) {
					mYear = year;
					mMonth = monthOfYear;
					mDay = dayOfMonth;
					updateDateButton();
				}
			};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();

		setContentView(R.layout.editlist);
		setTitle(R.string.edit_list);

		mListTitleText = (EditText) findViewById(R.id.txtShopName);
		mLocationText = (EditText) findViewById(R.id.txtLocation);
		new Geocoder(this);
		// initialize to empty string to avoid NullPointerException
		// when comparing old and new location
		mOldLocation = "";

		mRowId = (savedInstanceState == null) ? null : (Long) savedInstanceState.getSerializable(ShoppingListDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			Uri data = getIntent().getData();
			mRowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : null;
			if (mRowId != null && mRowId == 0) {
				Log.d("EditList", mRowId.toString());
				mRowId = data != null ? Long.decode(data.getPath().split("/")[1]) : null;
			}
		}
		
		mDueDate = (Button) findViewById(R.id.btnDueDate);
		mCalendar = Calendar.getInstance();
		mYear = mCalendar.get(Calendar.YEAR);
		mMonth = mCalendar.get(Calendar.MONTH);
		mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
		
		populateFields();
		registerForContextMenu(getListView());

		mSaveList = (Button) findViewById(R.id.btnSaveList);
		
		mAddItem = (Button) findViewById(R.id.btnAddItem);
		mLocate = (ImageButton) findViewById(R.id.btnLocate);

		mSaveList.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_OK);
				finish();
			}
		});
		
		mDueDate.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});

		mAddItem.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				addItemToList();
			}
		});

		mLocate.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				showMap();
			}
		});
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
		}
		return null;
	}
	
	private void updateDateButton() {
		mDueDate.setText(new StringBuilder()
		.append("Due date: ")
		.append(mDay).append(".")
		// Month is 0 based so add 1
		.append(mMonth + 1).append(".")
		.append(mYear));
	}

	private void showMap() {
		// call saveState() to make sure the list exists in the database
		saveState();
		Intent i = new Intent(this, ShoppingListMap.class);
		i.putExtra(ShoppingListDbAdapter.KEY_ROWID, mRowId);
		startActivity(i);
	}

	private void addItemToList() {
		// call saveState() to make sure the list exists in the database
		saveState();
		Intent i = new Intent(this, EditItem.class);
		i.putExtra("listId", mRowId);
		startActivity(i);
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
			mOldLocation = mLocationText.getText().toString();
			// fetch date
			long dateInMilliseconds = shoppingList.getLong(shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_DUE_DATE)) * 1000;
			mCalendar.setTimeInMillis(dateInMilliseconds);
			mYear = mCalendar.get(Calendar.YEAR);
			mMonth = mCalendar.get(Calendar.MONTH);
			mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
			updateDateButton();
			// fetch coordinates
			if (!shoppingList.isNull(shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LAT)))
				shoppingList.getInt(shoppingList.getColumnIndexOrThrow(
						ShoppingListDbAdapter.KEY_LAT));
			if (!shoppingList.isNull(shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_LON)))
				shoppingList.getInt(shoppingList.getColumnIndexOrThrow(
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
		mCalendar.set(Calendar.YEAR, mYear);
		mCalendar.set(Calendar.MONTH, mMonth);
		mCalendar.set(Calendar.DAY_OF_MONTH, mDay);
		long dateInMilliseconds = mCalendar.getTimeInMillis();
		// set default value if listTitle is empty string
		// list items with empty title can't be selected
		if (listTitle.isEmpty()) listTitle = "default";
		
		if (mRowId == null) {
			long id = mDbHelper.createShoppingList(listTitle, location, dateInMilliseconds);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			if (mOldLocation.equalsIgnoreCase(location)) {
				// location was changed, set latitude and longitude to null
				mDbHelper.updateShoppingList(mRowId, listTitle, location, null, null, dateInMilliseconds);
			}
			else {
				mDbHelper.updateShoppingList(mRowId, listTitle, location, dateInMilliseconds);
			}
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
			startActivity(i);
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
}
