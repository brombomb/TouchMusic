package com.touchmusicfree;

import java.io.IOException;
import java.util.ArrayList;

import com.touchmusicfree.R;
import com.touchmusicfree.TMSInterface;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.IBinder;
import android.util.Log;

public class TMService extends Service {

	private MediaPlayer mp = new MediaPlayer();
	private ArrayList<String> msongs = TouchMusic.songs;
	String ns = Context.NOTIFICATION_SERVICE;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		mp.stop();
		mp.release();
	}

	public void onPause() {
		if (mp.isPlaying()) {
			TouchMusic.playerstatus.setText("||");
			mp.pause();
		}
	}

	public void onResume() {
		if (!mp.isPlaying()) {
			TouchMusic.playerstatus.setText("|>");
			mp.start();
		}
		TouchMusic.clock.postDelayed(mUpdateTimeTask, 100);
	}

	public void onStop() {
		TouchMusic.playerstatus.setText("[]");
		TouchMusic.clock.removeCallbacks(mUpdateTimeTask);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.cancelAll();
		mp.stop();
	}

	public IBinder getBinder() {
		return mBinder;
	}

	private void playSong(int song) {
		try {
			if (TouchMusic.songsize > 0) {
				mp.reset();
				String info = msongs.get(song);
				String[] file = info.split("\\|\\|");
				mp.setDataSource(file[3]);
				mp.prepare();
				TouchMusic.artist.setText(file[1] + "\n" + file[2] + "\n"
						+ file[6]);
				TouchMusic.songinfo = file[1] + "\n" + file[2] + "\n" + file[6];
				TouchMusic.playerstatus.setText("|>");
				mp.start();

				int icon = R.drawable.icon;
				CharSequence show = file[1] + " - " + file[2];
				long now = System.currentTimeMillis();
				Context context = getApplicationContext();

				NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

				Intent notificationIntent = new Intent(this, TouchMusic.class);
				PendingIntent contentIntent = PendingIntent.getActivity(this,
						0, notificationIntent, 0);

				Notification notification = new Notification(icon, show, now);
				notification.setLatestEventInfo(context, "TouchMusic!", show,
						contentIntent);

				mNotificationManager.notify(R.string.artist, notification);

				TouchMusic.clock.removeCallbacks(mUpdateTimeTask);
				TouchMusic.clock.postDelayed(mUpdateTimeTask, 100);

				TouchMusic.slider.setMax(Integer.parseInt(file[5]));
				TouchMusic.duration = Integer.parseInt(file[5]);

				mp.setOnCompletionListener(new OnCompletionListener() {
					public void onCompletion(MediaPlayer arg0) {
						nextSong();
					}
				});

				if (TouchMusic.playerstatus.getText() != "" ){
					TouchMusic.playerstatus.setText("");
				}
			} else {
				TouchMusic.artist.setText("No Media Found");
			}
		} catch (IOException e) {
			Log.e(getString(R.string.app_name), e.getMessage());
		}
	}

	private void nextSong() {
		// Check if last song or not
		if (++TouchMusic.position >= TouchMusic.songsize) {
			TouchMusic.position = 0;
		}
		TouchMusic.playerstatus.setText(">>");
		playSong(TouchMusic.position);
	}

	private void prevSong() {
		// If we are less than 10 seconds in, go to last song, else restart
		if (mp.getCurrentPosition() < TouchMusic.restartTime) {
			if (--TouchMusic.position <= 0) {
				TouchMusic.position = 0;
			}
			TouchMusic.playerstatus.setText("<<");
			playSong(TouchMusic.position);
		} else {
			playSong(TouchMusic.position);
		}
	}

	private void moveSong(int amount) {
		mp.pause();
		int skipto = mp.getCurrentPosition() + amount;
		if (skipto > mp.getDuration()) {
			nextSong();
		} else if (skipto < 0) {
			prevSong();
		} else if (skipto < mp.getDuration() || skipto > 0) {
			mp.seekTo(mp.getCurrentPosition() + amount);
		}
		mp.start();

		TouchMusic.clock.removeCallbacks(mUpdateTimeTask);
		TouchMusic.clock.postDelayed(mUpdateTimeTask, 100);
	}

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			if (mp.isPlaying()) {
				long millis = mp.getCurrentPosition();
				int seconds = (int) (millis / 1000);
				int minutes = seconds / 60;
				seconds = seconds % 60;

				if (seconds < 10) {
					TouchMusic.length.setText("" + minutes + ":0" + seconds);
				} else {
					TouchMusic.length.setText("" + minutes + ":" + seconds);
				}

				TouchMusic.clock.postDelayed(this, 200);
				TouchMusic.slider.setProgress((int) millis);
				TouchMusic.where = (int) millis;
			}
		}
	}; 
	
	private final TMSInterface.Stub mBinder = new TMSInterface.Stub() {

		public void playFile(int position) {
			try {
				playSong(TouchMusic.position);

			} catch (IndexOutOfBoundsException e) {
				Log.e(getString(R.string.app_name), e.getMessage());
			}
		}

		public void addSongPlaylist(String song) {
			msongs.add(song);
		}

		public void clearPlaylist() {
			msongs.clear();
		}

		public void skipBack() {
			prevSong();

		}

		public void skipForward() {
			nextSong();
		}

		public void moveAround(int amount) {
			moveSong(amount);
		}

		public void pause() {
			TouchMusic.clock.removeCallbacks(mUpdateTimeTask);
			mp.pause();
		}

		public void stop() {
			TouchMusic.clock.removeCallbacks(mUpdateTimeTask);
			mp.stop();
		}

		public boolean isPlaying() {
			return mp.isPlaying();
		}

		public void start() {
			mp.start();
			TouchMusic.clock.postDelayed(mUpdateTimeTask, 100);
		}

	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}