package com.blogspot.fwfaill.shoppinglist;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

public class EditList extends Activity {
	
	private EditText mShopText;
	private Long mRowId;
	private ShoppingListDbAdapter mDbHelper;
	private static TableLayout tblItems;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ShoppingListDbAdapter(this);
		mDbHelper.open();
		
		setContentView(R.layout.editlist);
		setTitle(R.string.edit_list);
		
		mShopText = (EditText) findViewById(R.id.txtShopName);
		tblItems = (TableLayout) findViewById(R.id.tblItems);
		
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
				addItemRow();
				
			}
		});
	}
	
	private void addItemRow() {
		TableRow tr = new TableRow(this);
		tr.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		
		CheckBox pickedUp = new CheckBox(this);
		EditText itemName = new EditText(this);
		itemName.setWidth(295);
		EditText itemQuantity = new EditText(this);
		itemQuantity.setWidth(140);
		itemName.setText("uusi itemi");
		itemQuantity.setText("2 purkkia");
		
		tr.addView(pickedUp);
		tr.addView(itemName);
		tr.addView(itemQuantity);
		tblItems.addView(tr);
	}
	
	private void populateFields() {
		if (mRowId != null) {
			Cursor shoppingList = mDbHelper.fetchShoppingList(mRowId);
			startManagingCursor(shoppingList);
			mShopText.setText(shoppingList.getString(
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
