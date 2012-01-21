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
		
		Bundle extras = getIntent().getExtras();
		mListRowId = extras.getLong("listId");
		
		mRowId = (savedInstanceState == null) ? null :
			(Long) savedInstanceState.getSerializable(ShoppingListDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			mRowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : null;
			/* if extras does not contain KEY_ROWID value 0 will be assigned to mRowId
			 * this will cause CursorIndexOutOfBoundsException
			 */
			if (mRowId == 0) mRowId = null;
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
			Cursor shoppingListItem = mDbHelper.fetchShoppingListItem(mRowId);
			startManagingCursor(shoppingListItem);
			mNameText.setText(shoppingListItem.getString(
					shoppingListItem.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_ITEM_TITLE)));
			mQuantityText.setText(shoppingListItem.getString(
					shoppingListItem.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_QUANTITY)));
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
