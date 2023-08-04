package com.moonslab.sharlet;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Video_player extends AppCompatActivity {
    RelativeLayout body, top;
    boolean from_receiver = false;
    private VideoView video;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("FROM_RECEIVER")) {
            from_receiver = true;
        }

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        TextView back = findViewById(R.id.back_button);
        video = findViewById(R.id.main_video);
        TextView name = findViewById(R.id.video_name);
        top = findViewById(R.id.relativeLayout2);
        body = findViewById(R.id.video_rel);
        String video_path = read_from_file(this);
        if(null != video_path) {
            File video_file = new File(video_path);
            Intent intent1 = new Intent(this, Music_player_service.class);
            intent1.putExtra("Action", "PLAY");
            this.startService(intent1);
            name.setText(video_file.getName());
            Uri video_uri = Uri.fromFile(video_file);
            video.setVideoURI(video_uri);
            MediaController mediaController = new MediaController(this);
            video.setMediaController(mediaController);
            mediaController.setAnchorView(video);
            new Music_notification_control().pausePauseMusic(this);
            video.start();
        }
        else {
            Toast.makeText(getApplicationContext(), "No file to show!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        back.setOnClickListener(v -> {
            go_back();
        });

        int orientation = getApplicationContext().getResources().getConfiguration().orientation;
        rotate_check(orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    private void go_back() {
        if(!from_receiver) {
            finish();
        }
        else {
            Intent intent2 = new Intent(this, Receive.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent2);
        }
    }

    private String read_from_file(Context context) {
        try {
            String location = context.getFilesDir().getAbsolutePath()+"/"+ "Video_last.txt";
            return FileUtils.readFileToString(new File(location), StandardCharsets.UTF_8);
        }
        catch (IOException e){
            return null;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int orientation = newConfig.orientation;
        rotate_check(orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    private void rotate_check(Boolean rotated){
        if(rotated){
            top.setVisibility(View.GONE);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) body.getLayoutParams();
            p.setMargins(0, 0, 0, 0);
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else{
            top.setVisibility(View.VISIBLE);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) body.getLayoutParams();
            p.setMargins(0, Home.convertDpToPixels(55, getApplicationContext()), 0, 0);
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        video.stopPlayback();
        go_back();
    }
}