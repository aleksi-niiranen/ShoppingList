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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.blogspot.fwfaill.shoppinglist.R;
import com.blogspot.fwfaill.shoppinglist.util.ShoppingListDbAdapter;

/**
 * 
 * @author Aleksi Niiranen
 *
 */
public class ShoppingListMain extends ListActivity {
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;

	// for menus
	private static final int INSERT_ID = R.id.item1;
	private static final int MAP_ID = R.id.item2;
	private static final int DELETE_ID = Menu.FIRST + 1;

	private ShoppingListDbAdapter mDbHelper;

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.listmain);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();
		fillData();
		registerForContextMenu(getListView());
	}

	/**
	 * Fills the list with data from the database
	 */
	private void fillData() {
		Cursor shoppingListsCursor = mDbHelper.fetchAllShoppingLists();
		startManagingCursor(shoppingListsCursor);

		// Create an array to specify the columns we want to display in the list
		String[] from = new String[] { ShoppingListDbAdapter.KEY_TITLE };

		// and an array of the fields we want to bind those fields to
		int[] to = new int[] { R.id.text1 };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter shoppingLists = new SimpleCursorAdapter(this,
				R.layout.shoppinglistrow, shoppingListsCursor, from, to);
		setListAdapter(shoppingLists);
	}

	/**
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	/**
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case INSERT_ID:
			createList();
			return true;
		case MAP_ID:
			Intent i = new Intent(this, ShoppingListMap.class);
			i.putExtra("renderAll", true);
			startActivity(i);
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, 
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.remove);
	}

	/**
	 * Starts activity for creating a new shopping list
	 */
	private void createList() {
		Intent i = new Intent(this, EditList.class);
		startActivityForResult(i, ACTIVITY_CREATE);
	}

	/**
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			mDbHelper.deleteShoppingList(info.id);
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, EditList.class);
		i.putExtra(ShoppingListDbAdapter.KEY_ROWID, id);
		startActivityForResult(i, ACTIVITY_EDIT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		fillData();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mDbHelper.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper.open();
	}
}