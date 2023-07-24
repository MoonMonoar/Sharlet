package com.moonslab.sharlet;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.moonslab.sharlet.Home.store_as_file;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.moonslab.sharlet.custom.Receiver;
import com.moonslab.sharlet.custom.Sender;
import com.moonslab.sharlet.objects.fileOBJ;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class Receive extends AppCompatActivity {
    private PowerManager.WakeLock wakeLock;
    private PowerManager powerManager;
    private ProgressBar progress, total_progress;
    //Page components
    private Dialog dialog;
    private List<fileOBJ> fileOBJS;
    private TextView current_file, total_received, pack_got, portal_summary, main_title;
    private TableLayout main_table;

    private String server, pin;
    private boolean self_destroy = false;

    private ServiceConnection serviceConnection;

    @Override
    protected void onPause() {
        super.onPause();
        this.finish();
        if(!self_destroy) {
            Toast.makeText(getApplicationContext(), "Receiving in background", Toast.LENGTH_SHORT).show();
        }
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Acquire wake lock
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sharlet:Receiver");
        wakeLock.acquire(20*60*1000L /*20 minutes*/);

        //Bind service
        Intent bindIntent = new Intent(this, Receiver.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        //Start service
        Intent intent = new Intent(this, Receiver.class);
        startService(intent);
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize PowerManager
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        setContentView(R.layout.activity_receive);

        if(!Home.create_app_folders()){
            Toast.makeText(this, "Storage unavailable, please reinstall Sharlet!", Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        DBHandler dbHandler = new DBHandler(this);

        server = dbHandler.get_settings("sender_server_last");
        pin = dbHandler.get_settings("sender_pin_last");

        if(null == server || null == pin){
            Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        //Retrieving the files
        try {
            fileOBJS = dbHandler.incomingGet();
            if (null == fileOBJS || fileOBJS.size() == 0) {
                Toast.makeText(this, "No files to receive!", Toast.LENGTH_LONG).show();
                this.finish();
                return;
            }
        }
        catch (Exception e){
            Toast.makeText(this, "Can not load file list!", Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        //Set components
        progress = findViewById(R.id.progress);
        total_progress = findViewById(R.id.total_progress);
        total_received = findViewById(R.id.total_received);
        pack_got = findViewById(R.id.pack_got);
        current_file = findViewById(R.id.current_file);
        main_table = findViewById(R.id.files_table);
        portal_summary = findViewById(R.id.portal_summary);
        main_title = findViewById(R.id.total_progress_title);

        TextView back_button = findViewById(R.id.back_button);

        //Critical
        //Sharlet can allow all ssl
        HttpsTrustManager.allowAllSSL();

        //Confirm dialogue
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.confirm_dialog_layout);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView d_title = dialog.getWindow().findViewById(R.id.title);
        TextView d_des = dialog.getWindow().findViewById(R.id.description);

        //Customize
        d_title.setText("Leave portal?");
        d_des.setText("If you exit, remaining file receiving will be canceled and you will leave the portal.");

        Button Okay = dialog.findViewById(R.id.btn_okay);
        Button Cancel = dialog.findViewById(R.id.btn_cancel);

        Okay.setText("Leave");

        Okay.setOnClickListener(v -> {
            this.stopService(new Intent(this, Receiver.class));
            dialog.dismiss();
            startActivity(new Intent(Receive.this, Home.class));
            self_destroy = true;
            finish();
        });

        Cancel.setOnClickListener(v -> dialog.dismiss());

        back_button.setOnClickListener(v -> dialog.show());

        //Dialogue ends
        //Connect the service
        //Methods
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Receiver.LocalBinder binder = (Receiver.LocalBinder) service;
                //Full access to the service methods now
                binder.setComponents(server, pin, fileOBJS, main_table,
                        current_file, total_progress, progress,
                        portal_summary, main_title, pack_got, total_received);
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(getApplicationContext(), "Receiver disconnected!", Toast.LENGTH_SHORT).show();
                finish();
            }
        };
        dbHandler.add_setting("receiver_opened", "true");
    }

    //Events
    public void onBackPressed() {
        dialog.show();
    }
    //Needed
    public static String format_size(long bytes){
        float returnable = 0;
        String fix = "KB";
        returnable = bytes/(float)1000; //Kb
        if(returnable >= 1000){
            //Megabyte range
            returnable = returnable/1000;
            fix  = "MB";
            if(returnable >= 1000){
                //Gigabyte range
                returnable = returnable/1000;
                fix  = "GB";
            }
        }
        return (Math.round(returnable * 100.0) / 100.0)+fix;
    }
    public static class Navigate {
        DBHandler dbHandler;
        Context context;
        public void setContext(Context context) {
            this.context = context;
        }
        public void playMusic(String path){
            dbHandler = new DBHandler(context);
            Intent in = Home.get_music_intent(dbHandler, path, context);
            in.setFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(in);
        }
        public void playVideo(String path){
            Intent intent = new Intent(context, Video_player.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("FROM_RECEIVER", "1");
            store_as_file("Video_last.txt", path, context);
            context.startActivity(intent);
        }
        public void showImage(String path){
            Intent intent = new Intent(context, Photo_view.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //Save the file first
            store_as_file("Image_last.txt", path, context);
            context.startActivity(intent);
        }
        public void openFile(String path){
            Home.openFile(context, new File(path));
            Toast.makeText(context, "Opening...", Toast.LENGTH_SHORT).show();
        }
    }
}