package com.blogspot.fwfaill.shoppinglist;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ShoppingList extends ListActivity {
	private static final int ACTIVITY_CREATE = 0;
	private static final int INSERT_ID = Menu.FIRST;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

	/**
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	menu.add(0, INSERT_ID, 0, R.string.list_menu_add_list);
    	return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	switch (item.getItemId()) {
    		case INSERT_ID:
    			createList();
    			return true;
    	}
    	return super.onMenuItemSelected(featureId, item);
    }

	private void createList() {
		Intent i = new Intent(this, EditList.class);
		startActivityForResult(i, ACTIVITY_CREATE);
	}
}