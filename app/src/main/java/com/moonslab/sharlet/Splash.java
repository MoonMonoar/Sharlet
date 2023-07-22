package com.moonslab.sharlet;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.moonslab.sharlet.custom.Global;

import java.util.Timer;
import java.util.TimerTask;

public class Splash extends AppCompatActivity {
    boolean intro = true;
    DBHandler dbHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Global global = new Global(this);
        dbHandler = new DBHandler(this);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        /*
        Dark mode kept for next update
        //Prevent auto
        if(dbHandler.get_settings("dark_mode") == null
                && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES){
                global.disableDarkMode();
        }
        if(global.wasDarkModeOn()){
            global.enableDarkMode();
        }
        else {
            global.disableDarkMode();
        }
         */


        String intro_check = dbHandler.get_settings("intro_done");
        if (null != intro_check && intro_check.equals("true")) {
            intro = false;
        }
        new Timer().schedule(new TimerTask() {
            public void run() {
                if (intro) {
                    startActivity(new Intent(Splash.this, Welcome.class));
                    finish();
                    return;
                }
                startActivity(new Intent(Splash.this, Home.class));
                finish();
            }
        }, 500);
    }
}