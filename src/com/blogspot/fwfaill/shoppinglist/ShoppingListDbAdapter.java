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

	public static final String KEY_SHOP = "shop";
	public static final String KEY_ROWID = "_id";
	
	private static final String TAG = "ShoppingListDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	/**
	 * Database creation sql statements
	 */
	private static final String DATABASE_CREATE = 
			"create table shoppinglist (_id integer primary key autoincrement, shop text not null);";
	private static final String DATABASE_NAME = "shoppinglistdb";
	private static final String DATABASE_TABLE_LIST = "shoppinglist";
	private static final int DATABASE_VERSION = 1;
	
	private final Context mContext;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading to database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("drop table if exists shoppinglist");
			onCreate(db);
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
	 * @param shop the shop name
	 * @return rowId or -1 if failed
	 */
	public long createShoppingList(String shop) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_SHOP, shop);
		
		return mDb.insert(DATABASE_TABLE_LIST, null, initialValues);
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
	 * Return a Cursor over the list of all shopping lists in the database
	 * @return Cursor over all shopping lists
	 */
	public Cursor fetchAllShoppingLists() {
		return mDb.query(DATABASE_TABLE_LIST, new String[] {KEY_ROWID, KEY_SHOP}, null, null, null, null, null);
	}
	
	/**
	 * Return a Cursor positioned at the shopping list that matches the given rowId
	 * @param rowId id of the shopping list to retrieve
	 * @return Cursor positioned to matching shopping list, if found
	 * @throws SQLException if shopping list could not be found/retrieved
	 */
	public Cursor fetchShoppingList(long rowId) throws SQLException {
		Cursor mCursor = mDb.query(true, DATABASE_TABLE_LIST, 
				new String[] {KEY_ROWID, KEY_SHOP}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	/**
	 * Update the shopping list using the details provided. The shopping list to
	 * be updated is specified using the rowId, and it is altered to use the shop
	 * value passed in
	 * @param rowId id of the shopping list to update
	 * @param shop value to set shop to
	 * @return true if the note was successfully updated, false otherwise
	 */
	public boolean updateShoppingList(long rowId, String shop) {
		ContentValues args = new ContentValues();
		args.put(KEY_SHOP, shop);
		
		return mDb.update(DATABASE_TABLE_LIST, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
}
