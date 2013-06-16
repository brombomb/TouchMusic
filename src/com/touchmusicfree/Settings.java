package com.touchmusicfree;

import com.touchmusicfree.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

public class Settings extends PreferenceActivity {

	// Menu Item Variables
	public static final int MENU_SAVE = Menu.FIRST;
	
	/** Called when the activity is first created. */
	@Override 
	public void onCreate(Bundle savedInstanceState) { 
	  super.onCreate(savedInstanceState); 
	    
	  addPreferencesFromResource(R.xml.settings); 
	}


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SAVE, 0, R.string.menu_save).setIcon(android.R.drawable.ic_menu_save);
        return true;
    }
		
	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_SAVE:
	    	this.finish();
	        return true;
	    }
	    return false;
	}
}