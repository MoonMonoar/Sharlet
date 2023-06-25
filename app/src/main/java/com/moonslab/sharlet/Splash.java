package com.moonslab.sharlet;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Timer;
import java.util.TimerTask;

public class Splash extends AppCompatActivity {
    boolean intro = true;
    DBHandler dbHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        dbHandler = new DBHandler(Splash.this);
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