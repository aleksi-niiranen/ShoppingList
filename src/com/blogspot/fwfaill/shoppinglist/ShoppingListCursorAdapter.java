package com.blogspot.fwfaill.shoppinglist;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class ShoppingListCursorAdapter extends ResourceCursorAdapter {

	private ShoppingListDbAdapter mDbHelper;
	private LayoutInflater mInflater;

	public ShoppingListCursorAdapter(Context context, Cursor cursor) {
		super(context, R.layout.itemrow, cursor, false);
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mDbHelper = new ShoppingListDbAdapter(context);
		// open database
		mDbHelper.open();
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.itemrow, parent, false);
	}

	@Override
	public void bindView(View view, Context context, final Cursor cursor) {
		// open database
		mDbHelper.open();

		CheckBox pickedUp = (CheckBox) view.findViewById(R.id.pickedup);
		TextView nameText = (TextView) view.findViewById(R.id.itemtext1);
		TextView quantityText = (TextView) view.findViewById(R.id.itemtext2);

		pickedUp.setChecked(cursor.getInt(cursor
				.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_PICKED_UP)) == 1 ? true
				: false);

		if (pickedUp.isChecked()) {
			nameText.setEnabled(false);
			quantityText.setEnabled(false);
		}

		pickedUp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// update database
				mDbHelper.updateShoppingListItem(
						cursor.getLong(cursor
								.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_ROWID)),
						isChecked ? 1 : 0);
				// disable/enable textviews
				// nameText.setEnabled(!isChecked);
				// quantityText.setEnabled(!isChecked);
			}
		});

		nameText.setText(cursor.getString(cursor
				.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_ITEM_TITLE)));
		quantityText.setText(cursor.getString(cursor
				.getColumnIndexOrThrow(ShoppingListDbAdapter.KEY_QUANTITY)));

		// close database
		mDbHelper.close();
	}
}