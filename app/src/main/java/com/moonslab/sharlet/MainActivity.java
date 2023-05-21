package com.moonslab.sharlet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Timer().schedule(new TimerTask() {
            public void run() {
                startActivity(new Intent(MainActivity.this, Home.class));
                finish();
            }
        }, 1000);
    }
}