package com.blogspot.fwfaill.shoppinglist;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class EditItem extends Activity {

	private EditText mNameText;
	private EditText mQuantityText;
	// the id of the item
	private Long mRowId;
	// the list id which the item belongs to
	private Long mListRowId;
	private ShoppingListDbAdapter mDbHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();
		
		setContentView(R.layout.edititem);
		setTitle(R.string.edit_item);
		
		mNameText = (EditText) findViewById(R.id.item_title);
		mQuantityText = (EditText) findViewById(R.id.item_quantity);
		
		mListRowId = getIntent().getLongExtra("listId", 0);
		mRowId = (savedInstanceState == null) ? null :
			(Long) savedInstanceState.getSerializable(ShoppingListDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : null;
		}
		
		populateFields();
		
		Button saveItem = (Button) findViewById(R.id.btnSaveItem);
		
		saveItem.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setResult(RESULT_OK);
				finish();
			}
		});
	}

	private void populateFields() {
		if (mRowId != null) {
			Cursor shoppingList = mDbHelper.fetchShoppingList(mRowId);
			startManagingCursor(shoppingList);
			mNameText.setText(shoppingList.getString(
					shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_ITEM_TITLE)));
			mQuantityText.setText(shoppingList.getString(
					shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_QUANTITY)));
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
		String itemName = mNameText.getText().toString();
		String itemQuantity = mQuantityText.getText().toString();
		
		if (mRowId == null) {
			long id = mDbHelper.createShoppingListItem(itemName, itemQuantity, 0, mListRowId);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateShoppingListItem(mRowId, itemName, itemQuantity, 0);
		}
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mDbHelper.close();
    }
}
