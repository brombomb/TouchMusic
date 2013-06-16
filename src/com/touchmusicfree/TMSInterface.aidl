package com.touchmusicfree;

interface TMSInterface 
{
	void clearPlaylist();
	void addSongPlaylist( in String song );
    void playFile( in int position );
    void start();
    void pause();
    void stop();
    void skipForward();
    void skipBack();
    void moveAround( in int amount );
    
    boolean isPlaying(); 
}