package com.moonslab.sharlet;

import static com.moonslab.sharlet.Music_application_class.CHANNEL_MUSIC;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Music_player_service extends Service {
    //File and session
    Music_player_actions music_action;
    public static MediaPlayer player;
    int duration;
    public static int duration_cache = 100;

    public static final String ACTION_PREV = "PREVIOUS";
    public static final String ACTION_NEXT = "NEXT";
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_FAVOURITE = "FAV";
    public static final String ACTION_LOOPER = "LOOP";

    //Controls and info
    private static TextView name, back, time_decreasing, time_increasing;
    private static ImageView play, album_art, shuffle_on, shuffle_off, loop_off, loop_on, loop_one;
    private static Button prev, next;
    private static SeekBar seekbar;
    private static MediaSessionCompat mediaSession;
    private static ConstraintLayout body;
    private static Window window;
    private static String file_name = "Unknown";
    private static String file_path;
    private static Activity player_activity;
    private static ImageView fav_add, fav_remove;

    //Timer
    private static Boolean timer_started = false;
    final Handler timer_thread = new Handler();
    private final long timer_interval = 1; //ms - 1ms

    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder{
        Music_player_service getService(){
            return Music_player_service.this;
        }
        public Boolean is_playing(){
            if(player != null){
                return true;
            }
            return false;
        }
        //Method to set element controls
        public void set_controls
            (Activity e_activity, TextView e_name, TextView e_back,
             TextView e_time_inc, TextView e_time_dec,
             ImageView e_play, ImageView e_art,
             Button e_prev, Button e_next,
             SeekBar e_seekbar, MediaSessionCompat e_media_session,
             ConstraintLayout e_body, Window e_window, String e_file_name,
             String e_file_path, ImageView e_fav_add, ImageView e_fav_remove,
             ImageView e_shuffle_on, ImageView e_shuffle_off,
             ImageView e_loop_on, ImageView e_loop_off, ImageView e_loop_one)
                {
                    name = e_name;
                    back = e_back;
                    time_increasing = e_time_inc;
                    time_decreasing = e_time_dec;
                    play = e_play;
                    album_art = e_art;
                    prev = e_prev;
                    next = e_next;
                    seekbar = e_seekbar;
                    mediaSession = e_media_session;
                    body = e_body;
                    window = e_window;
                    file_name = e_file_name;
                    file_path = e_file_path;
                    player_activity = e_activity;
                    fav_add = e_fav_add;
                    fav_remove = e_fav_remove;
                    shuffle_on = e_shuffle_on;
                    shuffle_off = e_shuffle_off;
                    loop_one = e_loop_one;
                    loop_off = e_loop_off;
                    loop_on = e_loop_on;
                    //Update auto
                    update_media_info();
                    //Timer - if not exists
                    timer_starter();
                }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        file_path = intent.getStringExtra("File_path");
        if (file_path != null) {
            //CREATE MEDIA PLAYER
            Create_media_player(new File(file_path), false);
            return START_STICKY;
        }
        else {
            String actionName = intent.getStringExtra("Action");
            if (null != actionName) {
                switch (actionName) {
                    case ACTION_PLAY:
                        playOrPause(false);
                        break;
                    case ACTION_PREV:
                        music_prev();
                        break;
                    case ACTION_NEXT:
                        music_next(true);
                        break;
                    case ACTION_FAVOURITE:
                        fav_alter_current();
                        break;
                    case ACTION_LOOPER:
                        loop_alter();
                        break;
                }
            }
        }
        return START_STICKY;
    }

    private void loop_alter() {
        if(null == file_path || null == file_name){
            file_info_reset();
        }
        DBHandler dbHandler = new DBHandler(this);
        int pause_play = R.drawable.ic_baseline_play_arrow_24;
        float playback_speed = 0F;
        if(null != player && player.isPlaying()){
            pause_play = R.drawable.ic_baseline_pause_24;
            playback_speed = 1F;
        }
        int fav_icon = R.drawable.fav_add;
        String last_path = dbHandler.get_settings("last_music_path");
        if(last_path != null){
            if(dbHandler.fav_exists(last_path)){
                fav_icon = R.drawable.fav_remove;
            }
        }
        String shuffle_test = dbHandler.get_settings("music_shuffle");
        int loop_button;
        String loop_test = dbHandler.get_settings("music_loop");
        if(null != loop_test && loop_test.equals("off")){
            //On - s1
            dbHandler.add_setting("music_loop", "on");
            dbHandler.add_setting("music_shuffle", "off");
            loop_button = R.drawable.loop_notif_on;
            loop_on.setVisibility(View.VISIBLE);
            loop_off.setVisibility(View.GONE);
            loop_one.setVisibility(View.GONE);
            shuffle_off.setVisibility(View.GONE);
            shuffle_on.setVisibility(View.VISIBLE);
            show_notification(pause_play, fav_icon, loop_button, file_name, folderFromPath(file_path, file_name), playback_speed);
            return;
        }
        if(null != loop_test && loop_test.equals("on") && (shuffle_test == null || shuffle_test.equals("off")))
        {
            //One - s2
            dbHandler.add_setting("music_loop", "one");
            loop_button = R.drawable.loop_notif_one;
            loop_off.setVisibility(View.GONE);
            loop_on.setVisibility(View.GONE);
            loop_one.setVisibility(View.VISIBLE);
            show_notification(pause_play, fav_icon, loop_button, file_name, folderFromPath(file_path, file_name), playback_speed);
            return;
        }
        //Loop on - check single - overwrite
        if(null != loop_test && loop_test.equals("one")){
            //Shuffle on(with loop on) - s3
            dbHandler.add_setting("music_loop", "on");
            dbHandler.add_setting("music_shuffle", "on");
            loop_button = R.drawable.shuffle_notif;
            shuffle_on.setVisibility(View.GONE);
            shuffle_off.setVisibility(View.VISIBLE);
            loop_on.setVisibility(View.VISIBLE);
            loop_off.setVisibility(View.GONE);
            loop_one.setVisibility(View.GONE);
            show_notification(pause_play, fav_icon, loop_button, file_name, folderFromPath(file_path, file_name), playback_speed);
            return;
        }
        if(null != shuffle_test && shuffle_test.equals("on")){
            //Finally off the loop
            dbHandler.add_setting("music_loop", "off");
            dbHandler.add_setting("music_shuffle", "off");
            shuffle_off.setVisibility(View.GONE);
            shuffle_on.setVisibility(View.VISIBLE);
            loop_one.setVisibility(View.GONE);
            loop_off.setVisibility(View.VISIBLE);
            loop_on.setVisibility(View.GONE);
            loop_button = R.drawable.loop_notif_off;
            show_notification(pause_play, fav_icon, loop_button, file_name, folderFromPath(file_path, file_name), playback_speed);
        }
    }

    private void fav_alter_current() {
        //File name, File path must be retaken as notification can also call that
        if(null == file_path || null == file_name){
            file_info_reset();
        }
        DBHandler dbHandler = new DBHandler(this);
        String last_path = dbHandler.get_settings("last_music_path");
        int fav_icon = R.drawable.fav_add;
        float playback_speed = 0F;
        int pause_play = R.drawable.ic_baseline_play_arrow_24;
        if(null != player && player.isPlaying()){
            pause_play = R.drawable.ic_baseline_pause_24;
            playback_speed = 1F;
        }
        if(last_path != null){
            if(dbHandler.fav_exists(last_path)){
                dbHandler.fav_music_remove(last_path);
                fav_add.setVisibility(View.VISIBLE);
                fav_remove.setVisibility(View.GONE);
            }
            else {
                fav_icon = R.drawable.fav_remove;
                dbHandler.add_new_fav_music(last_path);
                fav_remove.setVisibility(View.VISIBLE);
                fav_add.setVisibility(View.GONE);
            }
        }
        int loop_button = get_looper_icon_id();
        show_notification(pause_play, fav_icon, loop_button, file_name, folderFromPath(file_path, file_name), playback_speed);
    }

    //Methods
    public void Create_media_player(File file, Boolean self_call) {
        releaseMediaPlayer();
        if(file.exists()) {
            file_path = file.getPath();
            file_name = file.getName();
            //Store last path
            DBHandler dbHandler = new DBHandler(this);
            dbHandler.add_setting("last_music_path", file.getPath());
            Boolean fav_check = dbHandler.fav_exists(file.getPath());
            if(fav_check){
                fav_remove.setVisibility(View.VISIBLE);
                fav_add.setVisibility(View.GONE);
            }
            else {
                fav_add.setVisibility(View.VISIBLE);
                fav_remove.setVisibility(View.GONE);
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "File lost!", Toast.LENGTH_SHORT).show();
            //Should play next
            return;
        }
        //Reset seekbar
        seekbar.setProgress(0);
        player = new MediaPlayer();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());
        try {
            player.setDataSource(getApplicationContext(), Uri.fromFile(file));
            player.prepare();
            player.setOnPreparedListener(mp -> {
                //Check previous time
                DBHandler dbHandler = new DBHandler(this);
                String old_pos = dbHandler.get_settings("last_music_progress");
                if(old_pos != null && !old_pos.equals("0")){
                    int pos = Integer.parseInt(old_pos);
                    if(pos < player.getDuration()){
                        player.seekTo(pos);
                    }
                }
                duration_cache = player.getDuration();
                if(!self_call){
                    playOrPause(true);
                }
                update_media_info();
                timer_starter();
            });
            player.setOnCompletionListener(mp -> {
                //Check if looping available
                music_next(false);
            });
            duration = player.getDuration();
        }
        catch (IOException e)
        {
            Toast.makeText(getApplicationContext(), "Can't play! Try again.", Toast.LENGTH_SHORT).show();
        }

    }

    public void timer_starter(){
        Runnable timer_task = new Runnable() {
            @Override
            public void run() {
                update_playing_time();
                timer_thread.postDelayed(this, timer_interval);
            }
        };
        if(!timer_started){
            timer_thread.post(timer_task);
            timer_started = true;
        }
    }

    public String ms_format(long milliseconds){
        String minutes_t = "0", second_t = "0";
        long minutes
                = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds
                = (TimeUnit.MILLISECONDS.toSeconds(milliseconds)
                % 60);
        minutes_t = Long.toString(minutes);
        if(minutes < 10){
            minutes_t = "0"+ minutes;
        }
        second_t = Long.toString(seconds);
        if(seconds < 10){
            second_t = "0"+ seconds;
        }
        return minutes_t+":"+second_t;
    }

    public void update_media_info(){
        if(null == player){
            cancelNotification(1000);
            return;
        }
        //File name, File path must be retaken as notification can also call that
        if(null == file_path || null == file_name){
            file_info_reset();
        }
        int remaining = player.getDuration()-player.getCurrentPosition();
        time_increasing.setText(ms_format(remaining));
        time_decreasing.setText(ms_format(player.getCurrentPosition()));
        name.setText(file_name);
        name.setSelected(true);
        seekbar.setMax(duration_cache);
        int fav_button = R.drawable.fav_add;
        //Fav check
        DBHandler dbHandler = new DBHandler(this);
        String last_path = dbHandler.get_settings("last_music_path");
        if(last_path != null){
            if(dbHandler.fav_exists(last_path)){
                fav_button = R.drawable.fav_remove;
            }
        }
        //Loop check
        int loop_button = get_looper_icon_id();
        if(player.isPlaying()){
            play.setImageResource(R.drawable.pause);
            show_notification(R.drawable.ic_baseline_pause_24, fav_button, loop_button, file_name, folderFromPath(file_path, file_name), 1F);
        }
        else {
            play.setImageResource(R.drawable.play);
            show_notification(R.drawable.ic_baseline_play_arrow_24, fav_button, loop_button, file_name, folderFromPath(file_path, file_name), 0F);
        }
    }

    public void update_notif_info(){
        //File name, File path must be retaken as notification can also call that
        if(null == file_path || null == file_name){
            file_info_reset();
        }
        int fav_button = R.drawable.fav_add;
        //Fav check
        DBHandler dbHandler = new DBHandler(this);
        String last_path = dbHandler.get_settings("last_music_path");
        if(last_path != null){
            if(dbHandler.fav_exists(last_path)){
                fav_button = R.drawable.fav_remove;
            }
        }
        //Loop check
        int loop_button = get_looper_icon_id();
        if(player.isPlaying()){
            play.setImageResource(R.drawable.pause);
            show_notification(R.drawable.ic_baseline_pause_24, fav_button, loop_button, file_name, folderFromPath(file_path, file_name), 1F);
        }
        else {
            play.setImageResource(R.drawable.play);
            show_notification(R.drawable.ic_baseline_play_arrow_24, fav_button, loop_button, file_name, folderFromPath(file_path, file_name), 0F);
        }
    }

    public void update_playing_time(){
        if(player != null){
                duration = player.getDuration();
                int time_now = player.getCurrentPosition();
                int remaining = duration - player.getCurrentPosition();
                //Store position
                DBHandler dbHandler = new DBHandler(this);
                int pos = player.getCurrentPosition();
                if(pos > 0) {
                    dbHandler.add_setting("last_music_progress", Integer.toString(pos));
                }
                Music_player.update_call(ms_format(time_now), ms_format(remaining), pos);
        }
    }

    @SuppressLint("SetTextI18n")
    public void releaseMediaPlayer(){
        if(player != null){
            player.release();
            player = null;
        }
        time_decreasing.setText("00:00");
        time_increasing.setText("00:00");
        name.setText("");
        play.setImageResource(R.drawable.play);
    }

    public void playOrPause(Boolean self_start) {
        new Thread(()-> {
            if (null == player) {
                cancelNotification(1000);
                return;
            }
            //File name, File path must be retaken as notification can also call that
            if(null == file_path || null == file_name){
                file_info_reset();
            }
            int fav_button = R.drawable.fav_add;
            //Fav check
            DBHandler dbHandler = new DBHandler(this);
            String last_path = dbHandler.get_settings("last_music_path");
            if(last_path != null){
                if(dbHandler.fav_exists(last_path)){
                    fav_button = R.drawable.fav_remove;
                }
            }
            //Loop check
            int loop_button = get_looper_icon_id();
            if (!player.isPlaying() || self_start) {
                //Play
                player.start();
                player_activity.runOnUiThread(() -> play.setImageResource(R.drawable.pause));
                int finalFav_button = fav_button;
                player_activity.runOnUiThread(()->show_notification(R.drawable.ic_baseline_pause_24, finalFav_button, loop_button, file_name, folderFromPath(file_path, file_name), 1F));
                return;
            }
            if (player.isPlaying()) {
                //Pause at once
                //Also make notification cleanable
                player.pause();
                player_activity.runOnUiThread(() -> play.setImageResource(R.drawable.play));
                int finalFav_button1 = fav_button;
                player_activity.runOnUiThread(()->show_notification(R.drawable.ic_baseline_play_arrow_24, finalFav_button1, loop_button, file_name, folderFromPath(file_path, file_name), 0F));
            }
        }).start();
    }

    private void file_info_reset() {
        String last_path = Music_player.get_last_path();
        if(null == last_path){
            Toast.makeText(getApplicationContext(), "Can't play! Try again", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(last_path);
        if(file.exists()){
            file_path = last_path;
            file_name = file.getName();
        }
        else {
            Toast.makeText(getApplicationContext(), "Can't play! File lost", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private int get_looper_icon_id(){
        DBHandler dbHandler = new DBHandler(this);
        int loop_button = R.drawable.loop_notif_off;
        String loop_test = dbHandler.get_settings("music_loop");
        if(null != loop_test && loop_test.equals("on")){
            loop_button = R.drawable.loop_notif_on;
        }
        if(null != loop_test && loop_test.equals("on")) {
            //Loop on - check shuffle - overwrite
            String shuffle_test = dbHandler.get_settings("music_shuffle");
            if (null != shuffle_test && shuffle_test.equals("on")) {
                loop_button = R.drawable.shuffle_notif;
            }
        }
        //Loop on - check single - overwrite
        if(null != loop_test && loop_test.equals("one")){
            loop_button = R.drawable.loop_notif_one;
        }
        return loop_button;
    }

    public void seekTo(int pos){
        if(player != null) {
            player.seekTo(pos);
            //Update notification too
            if(null == file_path || null == file_name){
                file_info_reset();
            }
            int fav_button = R.drawable.fav_add;
            //Fav check
            DBHandler dbHandler = new DBHandler(this);
            String last_path = dbHandler.get_settings("last_music_path");
            if(last_path != null){
                if(dbHandler.fav_exists(last_path)){
                    fav_button = R.drawable.fav_remove;
                }
            }
            //Loop check
            int loop_button = get_looper_icon_id();
            float speed = 0F;
            int play_pause = R.drawable.ic_baseline_play_arrow_24;
            if(player.isPlaying()){
                play_pause = R.drawable.ic_baseline_pause_24;
                speed = 1F;
            }
            show_notification(play_pause, fav_button, loop_button, file_name, folderFromPath(file_path, file_name), speed);
        }
    }

    public void setCallBack(Music_player_actions actions){
        this.music_action = actions;
    }

    public void show_notification(int play_pause_button, int fav_button, int loop_button, String name_of_file, String artist, float playbackSpeed) {
        //Update file path
        DBHandler dbHandler = new DBHandler(this);
        String path_test = dbHandler.get_settings("last_music_path");
        if(null != path_test){
            file_path = path_test;
        }

        //Destination intent
        Intent intent = new Intent(this, Music_player.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        //Action intents
        Intent prev_intent = new Intent(this, Music_notification_control.class).setAction(ACTION_PREV);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent prev_pending_intent = PendingIntent.getBroadcast(this, 0, prev_intent,
                PendingIntent.FLAG_IMMUTABLE);

        Intent next_intent = new Intent(this, Music_notification_control.class).setAction(ACTION_NEXT);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent next_pending_intent = PendingIntent.getBroadcast(this, 0, next_intent,
                PendingIntent.FLAG_IMMUTABLE);

        Intent play_intent = new Intent(this, Music_notification_control.class).setAction(ACTION_PLAY);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent play_pending_intent = PendingIntent.getBroadcast(this, 0, play_intent,
                PendingIntent.FLAG_IMMUTABLE);

        Intent fav_intent = new Intent(this, Music_notification_control.class).setAction(ACTION_FAVOURITE);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent fav_pending_intent = PendingIntent.getBroadcast(this, 0, fav_intent,
                PendingIntent.FLAG_IMMUTABLE);

        Intent loop_intent = new Intent(this, Music_notification_control.class).setAction(ACTION_LOOPER);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent loop_pending_intent = PendingIntent.getBroadcast(this, 0, loop_intent,
                PendingIntent.FLAG_IMMUTABLE);

        Bitmap cover;
        try {
            android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(file_path);
            byte[] data = mmr.getEmbeddedPicture();
            if (data != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                cover = bitmap;
                album_art.setImageBitmap(bitmap);
            }
            else {
                cover = BitmapFactory.decodeResource(getResources(), R.drawable.card_background);
                album_art.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_music_cover));
            }
        }
        catch (Exception e){
            cover = BitmapFactory.decodeResource(getResources(), R.drawable.card_background);
            album_art.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_music_cover));
        }

        //After pausing, fav button will convert to close button, so vars will be overwritten

        //Notification
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_MUSIC)
                .setLargeIcon(cover)
                .setSmallIcon(R.drawable.ic_baseline_audio_file_24)
                .setContentTitle(name_of_file)
                .setContentText(artist)
                .addAction(loop_button, "Loop", loop_pending_intent)
                .addAction(R.drawable.ic_baseline_skip_previous_24, "Previous", prev_pending_intent)
                .addAction(play_pause_button, "Play", play_pending_intent)
                .addAction(R.drawable.ic_baseline_skip_next_24, "Next", next_pending_intent)
                .addAction(fav_button, "Favourite", fav_pending_intent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .build();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.getDuration())
                    .build());
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, player.getCurrentPosition(), playbackSpeed)
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                    .build());
            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                    playOrPause(false);
                    return super.onMediaButtonEvent(mediaButtonEvent);
                }
                @Override
                public void onSeekTo(long pos) {
                    super.onSeekTo(pos);
                    player.seekTo((int)pos);
                    mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PLAYING, player.getCurrentPosition(), playbackSpeed)
                            .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                            .build());
                }
                @Override
                public void onSkipToNext() {
                    super.onSkipToNext();
                    music_next(true);
                }

                @Override
                public void onSkipToPrevious() {
                    super.onSkipToPrevious();
                    music_prev();
                }
            });
        }

        startForeground(1000, notification);

        if(playbackSpeed == 0F){
            stopForeground(false);
        }
    }

    public void cancelNotification(int notifyId) {
        NotificationManager nMgr = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(notifyId);
    }

    //Controls - copy paste of music player class method - except next has extra
    public void music_next(Boolean user_action) {
        if(null == player){
            cancelNotification(1000);
            return;
        }
        DBHandler dbHandler = new DBHandler(this);
        String loop_test = dbHandler.get_settings("music_loop");
        String last_path = dbHandler.get_settings("last_music_path");
        dbHandler.add_setting("last_music_progress", "0");
        if(null != last_path) {

            //User action
            if(user_action){

                //shuffle
                String shuffle_test = dbHandler.get_settings("music_shuffle");
                if(null != shuffle_test && shuffle_test.equals("on")){
                    //Play a random file
                    String next_path = dbHandler.get_music_path_random(last_path);
                    if(null != next_path){
                        play_target_file(next_path);
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
                if(next_id == 0 || (next_id < current_music_position)) {
                    //Start over!
                    int max = dbHandler.get_music_count();
                    int flag = 1;
                    boolean no_files = true;
                    while (flag <= max) {
                        next_path = dbHandler.get_music_path_by_id(flag);
                        if (null != next_path) {
                            play_target_file(next_path);
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
                    play_target_file(next_path);
                }
                return;
            }

            //Self action

            // Loop current
            if(null != loop_test && loop_test.equals("one")){
                //Loop current
                play_target_file(last_path);
                return;
            }

            //Shuffle
            if(null != loop_test && loop_test.equals("on")){
                //Looping is on, check for shuffle
                String shuffle_test = dbHandler.get_settings("music_shuffle");
                if (null != shuffle_test && shuffle_test.equals("on")) {
                    //Play a random file
                    String next_path = dbHandler.get_music_path_random(last_path);
                    if (null != next_path) {
                        play_target_file(next_path);
                    }
                    return;
                }
            }

            //Loop check can be done
            if(null != loop_test && loop_test.equals("on")){
                int current_music_position = dbHandler.get_music_id_by_path(last_path);
                String next_path = dbHandler.get_next_music_path(current_music_position, false);
                int next_id = dbHandler.get_music_id_by_path(next_path);
                if (next_id < current_music_position){
                    //Start over
                    int max = dbHandler.get_music_count();
                    int flag = 1;
                    Boolean no_files = true;
                    while (flag <= max) {
                        next_path = dbHandler.get_music_path_by_id(flag);
                        if (null != next_path) {
                            play_target_file(next_path);
                            no_files = false;
                            break;
                        }
                        flag++;
                    }
                    if (no_files) {
                        Toast.makeText(this, "No file to play next!", Toast.LENGTH_SHORT).show();
                    }

                }
                else {
                    play_target_file(next_path);
                }
            }

            if(null != loop_test && loop_test.equals("off")){
                Toast.makeText(this, "End of playback", Toast.LENGTH_SHORT).show();
                Create_media_player(new File(last_path), true);
            }

        }
        else
        {
            Toast.makeText(this, "Can't play! Music list not ready", Toast.LENGTH_SHORT).show();
        }
    }

    public void music_prev() {
        if(null == player){
            cancelNotification(1000);
            return;
        }
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
                        play_target_file(target);
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
                play_target_file(next_path);
            }
        }
        else
        {
            Toast.makeText(this, "Can't play! Music list not ready", Toast.LENGTH_SHORT).show();
        }
    }

    public void play_target_file(String file_path_x){
        Create_media_player(new File(file_path_x), false);
    }

    public static String folderFromPath(String path, String name){
        if(null == name || null == path){
            return "Unknown";
        }
        String r_str = "", r_n_str = "";
        char ch;
        path = path.substring(0, path.indexOf(name)-1);
        for (int i=0; i< path.length(); i++)
        {
            ch = path.charAt(i);
            r_str = ch+r_str;
        }
        r_str = r_str.substring(0, r_str.indexOf("/"));
        int i = r_str.length()-1;
        while(i >= 0){
            ch = r_str.charAt(i);
            r_n_str = r_n_str+ch;
            i--;
        }
        return r_n_str;
    }
}