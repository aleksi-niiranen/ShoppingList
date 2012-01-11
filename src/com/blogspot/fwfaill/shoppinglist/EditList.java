package com.blogspot.fwfaill.shoppinglist;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class EditList extends Activity {
	
	private EditText mShopText;
	private Long mRowId;
	private ShoppingListDbAdapter mDbHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();
		
		setContentView(R.layout.editlist);
		setTitle(R.string.edit_list);
		
		mShopText = (EditText) findViewById(R.id.txtShopName);
		
		mRowId = (savedInstanceState == null) ? null :
			(Long) savedInstanceState.getSerializable(ShoppingListDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ShoppingListDbAdapter.KEY_ROWID) : null;
		}
		
		populateFields();
		
		Button saveButton = (Button) findViewById(R.id.btnSaveList);
		
		saveButton.setOnClickListener(new View.OnClickListener() {
			
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
			mShopText.setText(shoppingList.getString(
					shoppingList.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_SHOP)));
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
		String shop = mShopText.getText().toString();
		
		if (mRowId == null) {
			long id = mDbHelper.createShoppingList(shop);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateShoppingList(mRowId, shop);
		}
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mDbHelper.close();
    }
}
