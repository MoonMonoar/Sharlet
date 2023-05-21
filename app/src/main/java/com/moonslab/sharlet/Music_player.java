package com.moonslab.sharlet;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Music_player extends AppCompatActivity implements ServiceConnection, Music_player_actions {
    private TextView music_name;
    private TextView back;
    private static TextView time_decreasing;
    private static TextView time_increasing;
    private ImageView play, album_art;
    private Button prev, next;
    private static SeekBar music_seekbar;
    private static MediaSessionCompat mediaSession;
    boolean start_new;
    private static Boolean hold_seek = false, hold_destroy = false;

    //The service
    public static Music_player_service Music_service;
    String current_file_path = null;
    static ConstraintLayout body;
    static Window window;
    private static Activity activity;
    DisplayMetrics displayMetrics = new DisplayMetrics();

    private static ImageView fav_add, fav_remove, shuffle_on, shuffle_off, loop_off, loop_on, loop_one, share_file;

    @Override
    protected void onPause() {
        super.onPause();
        if(!hold_destroy) {
            this.finish();
        }
        else {
            hold_destroy = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        Bundle signal_bundle = this.getIntent().getExtras();
        activity = this;

        DBHandler dbHandler = new DBHandler(this);
        String last_path = dbHandler.get_settings("last_music_path");

        if(null != last_path){
            File test_file = new File(last_path);
            if(!test_file.exists()){
                Toast.makeText(this, "Audio file not found!", Toast.LENGTH_LONG).show();
                finish();
            }
            else {
                current_file_path = last_path;
            }
        }
        else {
            Toast.makeText(this, "No audio to play!", Toast.LENGTH_LONG).show();
            finish();
        }

        //Start signal
        start_new = null != signal_bundle && !signal_bundle.getString("start_new").isEmpty();

        //Elements
        window = getWindow();
        music_name = findViewById(R.id.music_name);
        body = findViewById(R.id.music_body);
        back = findViewById(R.id.back_button);
        play = findViewById(R.id.music_play);
        prev = findViewById(R.id.music_previous);
        next = findViewById(R.id.music_next);
        music_seekbar = findViewById(R.id.music_seek_bar);
        time_decreasing = findViewById(R.id.time_decreasing);
        time_increasing = findViewById(R.id.time_increasing);
        album_art = findViewById(R.id.album_art);
        share_file = findViewById(R.id.share_file);

        share_file.setOnClickListener((view)-> {
            String target_path = get_last_path();
            if(null != target_path){
                File f = new File(target_path);
                if(f.exists()){
                    Uri uri = Uri.parse(f.getPath());
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("audio/*");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    hold_destroy = true;
                    startActivity(Intent.createChooser(share, "Sharlet Music - Share"));
                }
                else {
                    Toast.makeText(this, "File dose not exists!", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        });

        //Dynamically change album art height, width
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int display_height = displayMetrics.heightPixels;
        int display_width = displayMetrics.widthPixels;
        double height_ratio = 0.15;
        double width_ratio = 0.27;
        int i_height = (int) Math.round((double) display_height * height_ratio);
        int i_width = (int) Math.round((double) display_width * width_ratio);
        album_art.requestLayout();
        album_art.getLayoutParams().height = Home.convertDpToPixels(i_height, this);
        album_art.getLayoutParams().width = Home.convertDpToPixels(i_width, this);

        //Player - sessions
        mediaSession = new MediaSessionCompat(this, "Sharlet - Audio");

        //READY
        if (null != current_file_path) {
            //Elements click handler
            TextView list_button = findViewById(R.id.list_button);
            back.setOnClickListener(Listener);
            play.setOnClickListener(Listener);
            prev.setOnClickListener(Listener);
            next.setOnClickListener(Listener);
            list_button.setOnClickListener(Listener);

            //Must check again
             music_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                 @Override
                 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                     if(fromUser) {
                         Music_service.seekTo(progress);
                     }
                 }
                 @Override
                 public void onStartTrackingTouch(SeekBar seekBar) {
                     hold_seek = true;
                 }
                 @Override
                 public void onStopTrackingTouch(SeekBar seekBar) {
                     hold_seek = false;
                 }
             });

        }

        //Ready the database
        new Thread(()->{
            List<File> file_list = All_files(this);
            if(null == file_list || file_list.size() == 0){
                Toast.makeText(this, "Audio list update failed!", Toast.LENGTH_SHORT).show();
            }
            for(File file:file_list){
                if(file.exists()){
                   dbHandler.add_new_music(file.getPath());
                }
            }
        }).start();

        //Player features

        //Shuffle
        shuffle_on = findViewById(R.id.shuffle_on);
        shuffle_off = findViewById(R.id.shuffle_off);
        String shuffle_test = dbHandler.get_settings("music_shuffle");
        if(null != shuffle_test && shuffle_test.equals("on")){
            shuffle_on.setVisibility(View.GONE);
            shuffle_off.setVisibility(View.VISIBLE);
        }

        shuffle_on.setOnClickListener(v->{
            shuffle_on.setVisibility(View.GONE);
            shuffle_off.setVisibility(View.VISIBLE);
            dbHandler.add_setting("music_shuffle", "on");
            //also turn on loop
            loop_on.setVisibility(View.VISIBLE);
            loop_off.setVisibility(View.GONE);
            loop_one.setVisibility(View.GONE);
            dbHandler.add_setting("music_loop", "on");
            Toast.makeText(this, "Shuffle on", Toast.LENGTH_SHORT).show();
            Music_service.update_notif_info();
        });

        shuffle_off.setOnClickListener(v->{
            shuffle_off.setVisibility(View.GONE);
            shuffle_on.setVisibility(View.VISIBLE);
            dbHandler.add_setting("music_shuffle", "off");
            Toast.makeText(this, "Shuffle off", Toast.LENGTH_SHORT).show();
            Music_service.update_notif_info();
        });

        //Loop
        loop_on = findViewById(R.id.loop_on);
        loop_off = findViewById(R.id.loop_off);
        loop_one = findViewById(R.id.loop_one);

        String loop_test = dbHandler.get_settings("music_loop");
        if(null != loop_test && loop_test.equals("off")){
            loop_off.setVisibility(View.VISIBLE);
            loop_on.setVisibility(View.GONE);
            loop_one.setVisibility(View.GONE);
        }
        if(null != loop_test && loop_test.equals("on")){
            loop_on.setVisibility(View.VISIBLE);
            loop_off.setVisibility(View.GONE);
            loop_one.setVisibility(View.GONE);
        }
        if(null != loop_test && loop_test.equals("one")){
            loop_one.setVisibility(View.VISIBLE);
            loop_off.setVisibility(View.GONE);
            loop_on.setVisibility(View.GONE);
        }

        loop_off.setOnClickListener(v->{
            loop_on.setVisibility(View.VISIBLE);
            loop_off.setVisibility(View.GONE);
            loop_one.setVisibility(View.GONE);
            dbHandler.add_setting("music_loop", "on");
            Toast.makeText(this, "Looping playlist", Toast.LENGTH_SHORT).show();
            Music_service.update_notif_info();
        });

        loop_on.setOnClickListener(v->{
            loop_off.setVisibility(View.GONE);
            loop_on.setVisibility(View.GONE);
            loop_one.setVisibility(View.VISIBLE);
            dbHandler.add_setting("music_loop", "one");
            Toast.makeText(this, "Looping current track", Toast.LENGTH_SHORT).show();
            Music_service.update_notif_info();
        });

        loop_one.setOnClickListener(v->{
            loop_one.setVisibility(View.GONE);
            loop_off.setVisibility(View.VISIBLE);
            loop_on.setVisibility(View.GONE);
            dbHandler.add_setting("music_loop", "off");
            //Also shuffle off
            shuffle_off.setVisibility(View.GONE);
            shuffle_on.setVisibility(View.VISIBLE);
            dbHandler.add_setting("music_shuffle", "off");
            Toast.makeText(this, "Looping off", Toast.LENGTH_SHORT).show();
            Music_service.update_notif_info();
        });

        //Favourite
        fav_add = findViewById(R.id.fav_add);
        fav_remove = findViewById(R.id.fav_remove);
        Boolean fav_check = dbHandler.fav_exists(current_file_path);
        if(fav_check){
            fav_remove.setVisibility(View.VISIBLE);
            fav_add.setVisibility(View.GONE);
        }
        fav_add.setOnClickListener(v-> {
            String target_path = dbHandler.get_settings("last_music_path");
            fav_remove.setVisibility(View.VISIBLE);
            fav_add.setVisibility(View.GONE);
            dbHandler.add_new_fav_music(target_path);
            Music_service.update_notif_info();
        });
        fav_remove.setOnClickListener(v-> {
            String target_path = dbHandler.get_settings("last_music_path");
            fav_add.setVisibility(View.VISIBLE);
            fav_remove.setVisibility(View.GONE);
            dbHandler.fav_music_remove(target_path);
            Music_service.update_notif_info();
        });
    }

    public static String get_last_path(){
        DBHandler dbHandler = new DBHandler(activity);
        return dbHandler.get_settings("last_music_path");
    }

    private View.OnClickListener Listener = v -> {
        switch (v.getId()) {
            case R.id.back_button:
                finish();
                break;
            case R.id.music_play:
                music_play();
                break;
            case R.id.music_next:
                music_next();
                break;
            case R.id.music_previous:
                music_prev();
                break;
            case R.id.list_button:
                Intent intent = new Intent(this, Home.class);
                intent.putExtra("music_tab", true);
                finish();
                startActivity(intent);
                break;
            default:
                break;
        }
    };


    //Service starts and dismisses from here
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, Music_player_service.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        startService(intent);
    }

   // @Override
    //protected void onPause() {
     //   super.onPause();
     //   unbindService(this);
    //    this.finish();
   // }

    @Override
    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        Music_player_service.LocalBinder binder = (Music_player_service.LocalBinder) iBinder;
        Music_service = binder.getService();
        Music_service.setCallBack(Music_player.this);
        File current_file = new File(current_file_path);
        //Pass controls and info
        binder.set_controls(activity, music_name, back, time_increasing, time_decreasing,
                            play, album_art, prev, next, music_seekbar,
                            mediaSession, body, window, current_file.getName(),
                            current_file.getPath(), fav_add, fav_remove,
                            shuffle_on, shuffle_off, loop_on, loop_off, loop_one);
        //Play the music
        if(start_new){
            //Reset to 0
            DBHandler dbHandler = new DBHandler(this);
            dbHandler.add_setting("last_music_progress", "0");
            Music_service.Create_media_player(new File(current_file_path), false);
        }
        else {
            if(!binder.is_playing()){
                Music_service.Create_media_player(new File(current_file_path), true);
            }
        }
    }

    public static void update_call(String time_now, String time_remain, int pos){
        activity.runOnUiThread(() -> {
            time_decreasing.setText(time_now);
            time_increasing.setText(time_remain);
            if(!hold_seek) {
                music_seekbar.setProgress(pos);
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name){
        Music_service = null;
    }

    //Player methods
    @Override
    public void music_next() {
        DBHandler dbHandler = new DBHandler(this);
        String last_path = dbHandler.get_settings("last_music_path");
        dbHandler.add_setting("last_music_progress", "0");
        if(null != last_path){
        //shuffle
        String shuffle_test = dbHandler.get_settings("music_shuffle");
        if(null != shuffle_test && shuffle_test.equals("on")){
            //Play a random file
            String next_path = dbHandler.get_music_path_random(last_path);
            if(null != next_path){
                Music_service.play_target_file(next_path);
            }
            return;
        }
        int current_music_position = dbHandler.get_music_id_by_path(last_path);
        String next_path = dbHandler.get_next_music_path(current_music_position, false);
        int next_id = 0;
        if(null != next_path){
            //Start from 0
            next_id = dbHandler.get_music_id_by_path(next_path);
        }
        //Loop check can be done
        if(next_id == 0 || (next_id < current_music_position)){
            //Start over!
            int max = dbHandler.get_music_count();
            int flag = 1;
            Boolean no_files = true;
            while (flag <= max) {
                next_path = dbHandler.get_music_path_by_id(flag);
                if (null != next_path) {
                    Music_service.play_target_file(next_path);
                    no_files = false;
                    break;
                }
                flag++;
            }
            if(no_files) {
                Toast.makeText(this, "No file to play next!", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Music_service.play_target_file(next_path);
        }
        }
        else {
            Toast.makeText(this, "Can't play! Music list not ready", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void music_prev(){
        DBHandler dbHandler = new DBHandler(this);
        String last_path = dbHandler.get_settings("last_music_path");
        dbHandler.add_setting("last_music_progress", "0");
        if(null != last_path){
            int current_music_position = dbHandler.get_music_id_by_path(last_path);
            String next_path = dbHandler.get_next_music_path(current_music_position, true);
            if(null == next_path){
                //No loop test needed
                //Play the last file
                Boolean no_prev = true;
                int last_path1 = dbHandler.get_music_count(); //max is the last file
                while (last_path1 > 1){
                    String target = dbHandler.get_music_path_by_id(last_path1);
                    if(null != target){
                        Music_service.play_target_file(target);
                        no_prev = false;
                        break;
                    }
                    last_path1--;
                }
                if(no_prev) {
                    Toast.makeText(this, "No previous file to play!", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Music_service.play_target_file(next_path);
            }
        }
        else
        {
            Toast.makeText(this, "Can't play! Music list not ready", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void music_play() {
        Music_service.playOrPause(false);
    }

    //Passive code
    public List<File> All_files(Context context) {
        List<File> files = new ArrayList<>();
        List<String> file_paths = new ArrayList<>();
        try {
            final String[] columns = {
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DATE_ADDED
            };
            //Sort
            String sort_audio = MediaStore.Audio.Media.DATE_ADDED + " DESC";

            //Query sets
            MergeCursor cursor;
            cursor = new MergeCursor(new Cursor[]{
                    context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, null, null, sort_audio)
            });

            cursor.moveToFirst();
            files.clear();
            file_paths.clear();
            while (!cursor.isAfterLast()){
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                int lastPoint = path.lastIndexOf(".");
                path = path.substring(0, lastPoint) + path.substring(lastPoint).toLowerCase();
                files.add(new File(path));
                file_paths.add(path);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            //ERROR GETTING FILES
            //Handle
            return null;
        }
        return files;
    }
    //Passive code -- ends
}