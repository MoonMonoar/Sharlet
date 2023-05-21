package com.moonslab.sharlet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Video_player extends AppCompatActivity {
    RelativeLayout body, top;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        TextView back = findViewById(R.id.back_button);
        VideoView video = findViewById(R.id.main_video);
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
            video.start();
        }
        else {
            Toast.makeText(getApplicationContext(), "No file to show!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        back.setOnClickListener(v -> finish());

        int orientation = getApplicationContext().getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            rotate_check(true);
        }
        else {
            rotate_check(false);
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
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            rotate_check(true);
        }
        else {
            rotate_check(false);
        }
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
}