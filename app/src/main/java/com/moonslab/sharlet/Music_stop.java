package com.moonslab.sharlet;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class Music_stop extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_stop);
        try {
            Music_player_service.player.stop();
        }
        catch (Exception e){
            //Ignorable
        }
        Home.cancel_notification(1000, getApplicationContext());
        finish();
    }
}