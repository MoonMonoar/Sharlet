package com.moonslab.sharlet;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Welcome extends AppCompatActivity {
    Button location, sd_card, file_all;
    private int current_slide = 1;
    private boolean skip_now_prev = false;
    private GestureDetector gestureDetector;
    boolean perm_check = false;
    private DBHandler dbHandler;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.slide1));
        List<LinearLayout> slides = new ArrayList<>();
        slides.add(findViewById(R.id.slide_one));
        slides.add(findViewById(R.id.slide_two));
        slides.add(findViewById(R.id.slide_three));
        slides.add(findViewById(R.id.slide_four));
        ConstraintLayout main_body = findViewById(R.id.slide_background);

        //DATABASE
        dbHandler = new DBHandler(Welcome.this);

        LinearLayout prev = findViewById(R.id.prev);
        prev.setOnClickListener(v->right_swipe());
        findViewById(R.id.continue_app).setOnClickListener(v-> continue_app());
        findViewById(R.id.slide_next).setOnClickListener(v -> {
            current_slide++;
            if(current_slide == 2){
                //Previous
                prev.setVisibility(View.VISIBLE);
            } else if (current_slide == 1) {
                prev.setVisibility(View.GONE);
            }
            if(current_slide <= 4){
                int x = 1;
                while (x <= 4){
                    if(current_slide != x){
                        //Hide it
                        slides.get(x-1).setVisibility(View.GONE);
                    }
                    x++;
                }
            }
            else {
                //No more slides
                current_slide = 1;
            }
            //New slide
            slides.get(current_slide-1).setVisibility(View.VISIBLE);
            perm_check = false;
            if(current_slide == 2){
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.slide2));
                main_body.setBackground(ContextCompat.getDrawable(this, R.color.slide2));
            }
            else if(current_slide == 3){
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.slide3));
                main_body.setBackground(ContextCompat.getDrawable(this, R.color.slide3));
            }
            else if(current_slide == 4){
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.slide4));
                main_body.setBackground(ContextCompat.getDrawable(this, R.color.slide4));
                perm_check = true;
            }
            else {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.slide1));
                main_body.setBackground(ContextCompat.getDrawable(this, R.color.slide1));
            }
            if(current_slide == 4){
                findViewById(R.id.slide_next).setVisibility(View.GONE);
                findViewById(R.id.continue_app).setVisibility(View.VISIBLE);
            }
            else {
                if(findViewById(R.id.slide_next).getVisibility() == View.GONE) {
                    findViewById(R.id.slide_next).setVisibility(View.VISIBLE);
                }
                if(findViewById(R.id.continue_app).getVisibility() == View.VISIBLE) {
                    findViewById(R.id.continue_app).setVisibility(View.GONE);
                }
            }
        });
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            //Right
                            right_swipe();
                        } else {
                            // Swipe left
                            if(current_slide == 4){
                                return true;
                            }
                            findViewById(R.id.slide_next).performClick();
                        }
                        result = true;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        });

        findViewById(R.id.slide_background).setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        //Permissions
        file_all = findViewById(R.id.file_all);
        file_all.setOnClickListener(v-> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if(!Environment.isExternalStorageManager()){
                    //ASK PERMISSION
                    Intent permission_intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,  Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                    startActivity(permission_intent);
                    Toast.makeText(getBaseContext(), "Please turn on files permission.", Toast.LENGTH_LONG).show();
                    perm_check = true;
                }
                else {
                    file_all.setText(R.string.granted);
                }
            }
            else {
                Toast.makeText(getBaseContext(), "Not needed", Toast.LENGTH_LONG).show();
                file_all.setText(R.string.no_need);
            }
        });

        sd_card = findViewById(R.id.Sd_card);
        sd_card.setOnClickListener(v-> {
            if(!shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
               ! shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                sd_card.setText(R.string.open_settings);
                Toast.makeText(this, "Allow from settings", Toast.LENGTH_SHORT).show();
                sd_card.setOnClickListener(v2-> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                });
                return;
            }
            if(this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) < 0
                    || this.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) < 0) {
                        ActivityCompat.requestPermissions(this, new String[]{
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                android.Manifest.permission.READ_EXTERNAL_STORAGE,

                        },
                        0);
                //APKS
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(this.checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) < 0) {
                        ActivityCompat.requestPermissions(this, new String[]{
                                        Manifest.permission.QUERY_ALL_PACKAGES
                                },
                                1);
                    }
                }
            }
            else {
                sd_card.setText(R.string.granted);
            }
        });

        location = findViewById(R.id.Location_permission);
        location.setOnClickListener(v-> {
            if(!shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ! shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                location.setText(R.string.open_settings);
                Toast.makeText(this, "Allow from settings", Toast.LENGTH_SHORT).show();
                location.setOnClickListener(v2-> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                });
                return;
            }
            if(this.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) < 0
                    || this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) < 0) {
                    ActivityCompat.requestPermissions(this, new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        0);
            }
            else {
                location.setText(R.string.granted);
            }
        });
    }

    private void right_swipe() {
        if(current_slide == 2){
            current_slide = 0;
            findViewById(R.id.slide_next).performClick();
        } else if (current_slide == 3) {
            current_slide = 1;
            findViewById(R.id.slide_next).performClick();
        } else if (current_slide == 4) {
            current_slide = 2;
            findViewById(R.id.slide_next).performClick();
        }
    }

    private void continue_app() {
        dbHandler.add_setting("intro_done", "true");
        //Newly start the app
        Intent intent = new Intent(this, Home.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()){
                if(perm_check) {
                    Toast.makeText(this, "File permission missing!", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                file_all.setText(R.string.granted);
                file_all.setOnClickListener(null);
            }
        }
        if(!(this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) < 0
                || this.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) < 0)) {
            sd_card.setText(R.string.granted);
            sd_card.setOnClickListener(null);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) < 0 || this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) < 0)) {
                location.setText(R.string.granted);
                location.setOnClickListener(null);
            }
        }
    }
}