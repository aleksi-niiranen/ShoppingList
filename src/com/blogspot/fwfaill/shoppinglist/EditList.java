package com.blogspot.fwfaill.shoppinglist;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

public class EditList extends Activity {
	
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;
	
	// for menus
	private static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;
	
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
		
		mListTitleText = (EditText) findViewById(R.id.txtShopName);
		
		mRowId = (savedInstanceState == null) ? null :
			(Long) savedInstanceState.getSerializable(ShoppingListDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : null;
		}
		
		populateFields();
		
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
		// pass the id of the list as argument
		i.putExtra("listId", mRowId);
		startActivityForResult(i, ACTIVITY_CREATE);
	}
	
	private void populateFields() {
		// TODO fetch list items
		if (mRowId != null) {
			Cursor shoppingList = mDbHelper.fetchShoppingList(mRowId);
			startManagingCursor(shoppingList);
			mListTitleText.setText(shoppingList.getString(
					shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_TITLE)));
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
    protected void onDestroy() {
    	super.onDestroy();
    	mDbHelper.close();
    }
}
