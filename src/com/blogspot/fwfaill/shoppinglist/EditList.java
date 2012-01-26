package com.blogspot.fwfaill.shoppinglist;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class EditList extends ListActivity {
	
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;
	
	// for menus
	private static final int DELETE_ID = Menu.FIRST;
	
	private EditText mListTitleText;
	private Long mRowId;
	private ShoppingListDbAdapter mDbHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();
		
		setContentView(R.layout.editlist);
		setTitle(R.string.edit_list);
		
		LayoutInflater inflater = getLayoutInflater();
		// inflate View from separate XML layout
		View footer = inflater.inflate(R.layout.itemlistfooter, null);
		// set footer to list
		getListView().addFooterView(footer);
		
		mListTitleText = (EditText) findViewById(R.id.txtShopName);
		
		mRowId = (savedInstanceState == null) ? null :
			(Long) savedInstanceState.getSerializable(ShoppingListDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : null;
		}
		
		populateFields();
		registerForContextMenu(getListView());
		
		Button saveList = (Button) findViewById(R.id.btnSaveList);
		Button addItem = (Button) findViewById(R.id.btnAddItem);
		
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
			mListTitleText.setText(shoppingList.getString(
					shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_TITLE)));
			// fetch list items
			Cursor listItemsCursor = mDbHelper.fetchItemsOnList(mRowId);
			startManagingCursor(listItemsCursor);
			// Create an array to specify the fields we want to display in the list
			String[] from = new String[] {ShoppingListDbAdapter.KEY_ITEM_TITLE, ShoppingListDbAdapter.KEY_QUANTITY};
			
			// and an array of the fields we want to bind those fields to
			int[] to = new int[]{R.id.itemtext1, R.id.itemtext2};
			
			// Now create a simple cursor adapter and set it to display
			SimpleCursorAdapter shoppingListItems =
					new SimpleCursorAdapter(this, R.layout.itemrow, listItemsCursor, from, to);
			setListAdapter(shoppingListItems);
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
		
		if (mRowId == null) {
			long id = mDbHelper.createShoppingList(listTitle);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateShoppingList(mRowId, listTitle);
		}
	}
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(0, DELETE_ID, 0, R.string.remove);
    }
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case DELETE_ID:
				AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
				mDbHelper.deleteShoppingListItem(info.id);
				populateFields();
				return true;
		}
		return super.onContextItemSelected(item);
	}
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, EditItem.class);
		i.putExtra(ShoppingListDbAdapter.KEY_ROWID, id);
		i.putExtra("listId", mRowId);
		startActivityForResult(i, ACTIVITY_EDIT);
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
    
    // TODO: override backbutton press to setResult(OK) and ignore empty field
}
