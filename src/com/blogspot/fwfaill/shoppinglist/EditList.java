package com.blogspot.fwfaill.shoppinglist;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class EditList extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.editlist);
		setTitle(R.string.edit_list);
		
		Button saveButton = (Button) findViewById(R.id.btnSaveList);
		
		saveButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setResult(RESULT_OK);
				finish();
			}
		});
	}
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
