package com.touchmusicfree;

import java.util.ArrayList;
import java.util.Collections;

import com.touchmusicfree.R;
import com.touchmusicfree.TMSInterface;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;

public class TouchMusic extends Activity {

	private GestureDetector gd;
	private Cursor cursor;
	protected static Handler clock = new Handler();
	protected static TextView artist, length, playerstatus;
	protected static ImageView art;
	protected static ProgressBar slider;

	protected static ArrayList<String> songs = new ArrayList<String>();
	protected static int position = 0;
	protected static int where = 0;
	protected static int songsize = 0;
	protected static int duration = 0;
	protected static CharSequence songinfo = "";

	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private static final int SWIPE_OFF_PATH = 250;

	// Menu Item Variables
	public static final int MENU_SETTINGS = Menu.FIRST;
	public static final int MENU_QUIT = Menu.FIRST + 1;
	
	// User Preferences
	protected static boolean invVert = false;
	protected static boolean invHoriz = false;
	protected static boolean forceLand = false;
	protected static int seekTime = 10;
	protected static int restartTime = 10;
	
	
	// Set up The Background Service
	private static TMSInterface mpInterface;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		invVert = settings.getBoolean((String)"invVert", false);
		invHoriz = settings.getBoolean((String)"invHoriz", false);
		forceLand = settings.getBoolean((String)"forceLand", false);
		//seekTime = settings.getInt((String)"seekTime", 10000);
		//restartTime = settings.getInt((String)"restartTime", 30000);
		
		if(seekTime < 1000) seekTime = seekTime * 1000;
		if(restartTime < 1000) restartTime = restartTime * 1000;
		
		if(!forceLand){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		gd = new GestureDetector(this, new touch());
		length = (TextView) findViewById(R.id.length);
		artist = (TextView) findViewById(R.id.artist);
		playerstatus = (TextView) findViewById(R.id.playerstatus);
		//art = (ImageView) findViewById(R.id.album_art);
		slider = (ProgressBar) findViewById(R.id.slider);
		
		if(savedInstanceState != null){
			position = savedInstanceState.getInt("position");
			where = savedInstanceState.getInt("where");
			songinfo = savedInstanceState.getCharSequence("songinfo");
			duration = savedInstanceState.getInt("length");
		}
       	
		TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		this.bindService(new Intent(TouchMusic.this, TMService.class),
				mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
        outState.putInt("position", position);
        outState.putInt("where", (int) duration);
        outState.putCharSequence("songinfo", songinfo);
        outState.putInt("length", (int) slider.getMax());
        super.onSaveInstanceState(outState);
    }
	
	public void updateSongList() {
		// Lets get the music from the DB so we have meta data
		Uri media = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		String[] projection = { // The columns we want
		MediaStore.Audio.Media._ID, // 0
				MediaStore.Audio.Media.ARTIST, // 1
				MediaStore.Audio.Media.TITLE, // 2
				MediaStore.Audio.Media.DATA, // 3
				MediaStore.Audio.Media.DISPLAY_NAME, // 4
				MediaStore.Audio.Media.DURATION, // 5
				MediaStore.Audio.Media.ALBUM}; //, // 6
				//MediaStore.Audio.Media.ALBUM_ART}; // 7
		
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

		cursor = this.managedQuery(media, projection, selection, null, null);

		while (cursor.moveToNext()) {
			songs.add(cursor.getString(0) + "||" + cursor.getString(1) + "||"
					+ cursor.getString(2) + "||" + cursor.getString(3) + "||"
					+ cursor.getString(4) + "||" + cursor.getString(5) + "||"
					+ cursor.getString(6) + "||"); // + cursor.getString(7));
		}

		songsize = songs.size();
		Collections.shuffle(songs);
	}


	// Set up the Gesture Detector Listener
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (gd.onTouchEvent(ev)) {
			return true;
		}
		return super.onTouchEvent(ev);
	}

	private boolean nowPlaying() {
		boolean isit = false;
		try {
			isit = mpInterface.isPlaying();
		} catch (RemoteException e) {
			Log.e(getString(R.string.app_name), e.getMessage());
		}
		return isit;
	}

	// Pause the music if the phone starts ringing
	private PhoneStateListener mPhoneListener = new PhoneStateListener() {
		public void onCallStateChanged(int state, String incomingNumber){
			try {
				switch (state){
	            	case TelephonyManager.CALL_STATE_RINGING:
	            		mpInterface.pause();
	                    break;
				}
			} catch (RemoteException e) {}
		}
	};
	
	// Override the GDL to have touch controls
	class touch extends GestureDetector.SimpleOnGestureListener {

		// Toggle Play/Pause w/ Double tap
		@Override
		public boolean onDoubleTapEvent(MotionEvent ev) {
			if (nowPlaying()) {
				try {
					mpInterface.pause();
				} catch (RemoteException e) {
					Log.e(getString(R.string.app_name), e.getMessage());
				}
			} else {
				try {
					mpInterface.start();
				} catch (RemoteException e) {
					Log.e(getString(R.string.app_name), e.getMessage());
				}
			}
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			// Swipe Right -- Next Track
			if (velocityX > 0 // Right Movement
					&& e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE // Far Enough
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY // Fast Enough
					&& Math.abs(e1.getY() - e2.getY()) < SWIPE_OFF_PATH) { // Don't go vertically
				try {
					if(!invHoriz){
						mpInterface.skipForward();
					}
					else{
						mpInterface.skipBack();
					}
				} catch (RemoteException e) {
					Log.e(getString(R.string.app_name), e.getMessage());
				}
			}

			// Swipe Left -- Previous Track
			else if (velocityX < 0 // Left Movement
					&& e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE // Far Enough
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY // Fast Enough
					&& Math.abs(e1.getY() - e2.getY()) < SWIPE_OFF_PATH) { // Don't go vertically
				try {
					if(!invHoriz){
						mpInterface.skipBack();
					}
					else{
						mpInterface.skipForward();
					}
				} catch (RemoteException e) {
					Log.e(getString(R.string.app_name), e.getMessage());
				}
			}

			// Swipe Up (?) -- Seek Ahead 10 seconds
			else if (velocityY < 0 // Up Movement
					&& e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE // Far Enough
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY // Fast Enough
					&& Math.abs(e1.getX() - e2.getX()) < SWIPE_OFF_PATH) { // Don't go horizontally
				try {
					if(!invVert){
						mpInterface.moveAround(seekTime);
					}
					else{
						mpInterface.moveAround(-seekTime);
					}
				} catch (RemoteException e) {
					Log.e(getString(R.string.app_name), e.getMessage());
				}
			}

			// Swipe Down -- Seek Back 10 seconds
			else if (velocityY > 0 // Down Movement
					&& e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE // Far Enough
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY // Fast Enough
					&& Math.abs(e1.getX() - e2.getX()) < SWIPE_OFF_PATH) { // Don't go horizontally
				try {
					if(!invVert){
						mpInterface.moveAround(-seekTime);
					}
					else{
						mpInterface.moveAround(seekTime);
					}
				} catch (RemoteException e) {
					Log.e(getString(R.string.app_name), e.getMessage());
				}
			}
			return true;
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) 
		{
			mpInterface = TMSInterface.Stub.asInterface(service);
			updateSongList();
			artist.setText("Connecting...");

			if (songsize > 0) {
				try {
					if(where == 0){
						mpInterface.playFile(position);
					} else{
						artist.setText(songinfo);
						slider.setMax(duration);
					}
				} catch (RemoteException e) {
					Log.e(getString(R.string.app_name), e.getMessage());
				}
			} else {
				artist.setText("No Media Found\nPlease add some music to your SD Card\n");
			}
		}

		public void onServiceDisconnected(ComponentName className) 
		{ 
			try {
				mpInterface.stop();
			} catch (RemoteException e) {
				Log.e(getString(R.string.app_name), e.getMessage());
			}
			mpInterface = null;
			mConnection = null;
		}
	};	

	public static void doHeadSetUpdate()
	{
		try {
    		if(mpInterface.isPlaying()){
	    		mpInterface.pause();
    		}
		} catch (RemoteException e) {
			Log.e("TouchMusic", e.getMessage());
		}
	}
	
	public static class HSChange extends BroadcastReceiver{
	    @Override 
	    public void onReceive(Context context, Intent intent){
	    	doHeadSetUpdate();
	    	Log.e("TouchMusic", "Headset unplugged");
	   }
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //MenuItem itemSettings = menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings);
        //itemSettings.setIcon(android.R.drawable.ic_menu_preferences);
        //itemSettings.setIntent(new Intent(this, Settings.class));
        MenuItem itemQuit = menu.add(0, MENU_QUIT, 0, R.string.menu_quit);
        itemQuit.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }
	
	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_QUIT:
	        try {
	        	mpInterface.stop();
	        	unbindService(mConnection);
	        	this.finalize();
	        	this.finish();
			} catch (RemoteException e) {
				Log.e(getString(R.string.app_name), e.getMessage());
			} catch (Throwable e) {
				Log.e(getString(R.string.app_name), e.getMessage());
			}
	        return true;
	    }
	    return false;
	}  
}