package com.blogspot.fwfaill.shoppinglist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 * @author Aleksi
 *
 */
public class ShoppingListDbAdapter {

	public static final String KEY_TITLE = "title";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_ITEM_TITLE = "item_title";
	public static final String KEY_QUANTITY = "quantity";
	public static final String KEY_PICKED_UP = "picked_up";
	public static final String KEY_LIST_ID = "list_id";
	
	private static final String TAG = "ShoppingListDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String DATABASE_NAME = "shoppinglistdb";
	private static final String DATABASE_TABLE_LIST = "shoppinglist";
	private static final String DATABASE_TABLE_ITEM = "shoppinglistitem";
	private static final int DATABASE_VERSION = 3;
	
	private final Context mContext;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private Context context;

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			this.context = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			// db.execSQL(DATABASE_CREATE);
			String[] sql = context.getString(R.string.shoppinglistdb_onCreate).split("\n");
	        db.beginTransaction();
	        try {
	            // Create tables & test data
	            execMultipleSQL(db, sql);
	            db.setTransactionSuccessful();
	        } catch (SQLException e) {
	            Log.e("Error creating tables and debug data", e.toString());
	        } finally {
	            db.endTransaction();
	        }
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading to database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			String[] sql = context.getString(
	                R.string.shoppinglistdb_onUpgrade).split("\n");
	        db.beginTransaction();
	        try {
	            // drop tables & test data
	            execMultipleSQL(db, sql);
	            db.setTransactionSuccessful();
	        } catch (SQLException e) {
	            Log.e("Error creating tables and debug data", e.toString());
	        } finally {
	            db.endTransaction();
	        }
			onCreate(db);
		}
		
		/**
		 * Execute all the SQL statements in the String[] array
		 * @param db The database on which to execute the statements
		 * @param sql An array of SQL statements to execute
		 */
		private void execMultipleSQL(SQLiteDatabase db, String[] sql) {
			for (String s : sql) {
				if (s.trim().length() > 0)
					db.execSQL(s);
			}
		}
	}
	
	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * @param context the Context within which to work
	 */
	public ShoppingListDbAdapter(Context context) {
		this.mContext = context;
	}
	
	/**
	 * Open the shopping list database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * @return this (self reference, allowing this to be chained in an
	 * 		   initialization call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	public ShoppingListDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}
	
	/**
	 * Create a new shopping list using the title and body provided. If the shopping list is
	 * successfully created return the new rowId for that shopping list, otherwise return
	 * a -1 to indicate failure
	 * @param title the list title
	 * @return rowId or -1 if failed
	 */
	public long createShoppingList(String title) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TITLE, title);
		
		return mDb.insert(DATABASE_TABLE_LIST, null, initialValues);
	}
	
	/**
	 * Create a new shopping list item.
	 * @param itemTitle the title of the item
	 * @param quantity the quantity of the item
	 * @param pickedUp the value for determining if the item is picked up
	 * @param listId the id of the list the item belongs to
	 * @return rowId or -1 if failed
	 */
	public long createShoppingListItem(String itemTitle, String quantity, int pickedUp, long listId) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ITEM_TITLE, itemTitle);
		initialValues.put(KEY_QUANTITY, quantity);
		initialValues.put(KEY_PICKED_UP, pickedUp);
		initialValues.put(KEY_LIST_ID, listId);
		
		return mDb.insert(DATABASE_TABLE_ITEM, null, initialValues);
	}
	
	/**
	 * Delete the shopping list with the given rowId
	 * @param rowId
	 * @return
	 */
	public boolean deleteShoppingList(long rowId) {
		return mDb.delete(DATABASE_TABLE_LIST, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	/**
	 * Delete the shopping list item with the given rowId
	 * @param rowId
	 * @return
	 */
	public boolean deleteShoppingListItem(long rowId) {
		return mDb.delete(DATABASE_TABLE_ITEM, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	/**
	 * Return a Cursor over the list of all shopping lists in the database
	 * @return Cursor over all shopping lists
	 */
	public Cursor fetchAllShoppingLists() {
		return mDb.query(DATABASE_TABLE_LIST, new String[] {KEY_ROWID, KEY_TITLE}, null, null, null, null, null);
	}
	
	/**
	 * Return a Cursor over the list of all shopping list items on certain list in the database
	 * @param listId
	 * @return Cursor over all shopping list items on a certain list
	 */
	public Cursor fetchItemsOnList(int listId) {
		return mDb.query(DATABASE_TABLE_ITEM, new String[] {KEY_ROWID, KEY_ITEM_TITLE, KEY_QUANTITY, KEY_PICKED_UP, KEY_LIST_ID}, 
				KEY_LIST_ID + "=" + listId, null, null, null, null);
	}
	
	/**
	 * Return a Cursor positioned at the shopping list that matches the given rowId
	 * @param rowId id of the shopping list to retrieve
	 * @return Cursor positioned to matching shopping list, if found
	 * @throws SQLException if shopping list could not be found/retrieved
	 */
	public Cursor fetchShoppingList(long rowId) throws SQLException {
		Cursor cursor = mDb.query(true, DATABASE_TABLE_LIST, 
				new String[] {KEY_ROWID, KEY_TITLE}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}
	
	/**
	 * Return a Cursor positioned at the shopping list item that matches the given rowId
	 * @param rowId id of the shopping list item to retrieve
	 * @return Cursor positioned to matching shopping list item, if found
	 * @throws SQLException if shopping list item could not be found/retrieved
	 */
	public Cursor fetchShoppingListItem(long rowId) throws SQLException {
		Cursor cursor = mDb.query(true, DATABASE_TABLE_ITEM, new String[] {KEY_ROWID, KEY_ITEM_TITLE, KEY_QUANTITY, KEY_PICKED_UP, KEY_LIST_ID},
				KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}
	
	/**
	 * Update the shopping list using the details provided. The shopping list to
	 * be updated is specified using the rowId, and it is altered to use the title
	 * value passed in
	 * @param rowId id of the shopping list to update
	 * @param title value to set title to
	 * @return true if the shopping list was successfully updated, false otherwise
	 */
	public boolean updateShoppingList(long rowId, String title) {
		ContentValues args = new ContentValues();
		args.put(KEY_TITLE, title);
		
		return mDb.update(DATABASE_TABLE_LIST, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	/**
	 * Update the shopping list item using the details provided. The shopping list item
	 * to be updated is specified using the rowId, and it is altered to use the item title,
	 * quantity and picked up values passed in
	 * @param rowId id of the shopping list item to update
	 * @param itemTitle value to set item title to
	 * @param quantity value to set quantity to
	 * @param pickedUp value to set picked up to
	 * @return true if the shopping list item was successfully updated, false otherwise
	 */
	public boolean updateShoppingListItem(long rowId, String itemTitle, String quantity, int pickedUp) {
		ContentValues args = new ContentValues();
		args.put(KEY_ITEM_TITLE, itemTitle);
		args.put(KEY_QUANTITY, quantity);
		args.put(KEY_PICKED_UP, pickedUp);
		
		return mDb.update(DATABASE_TABLE_ITEM, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
}
