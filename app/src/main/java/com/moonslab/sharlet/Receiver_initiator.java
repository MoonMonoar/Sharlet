package com.moonslab.sharlet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.moonslab.sharlet.custom.Global;
import com.moonslab.sharlet.custom.Net;
import com.moonslab.sharlet.custom.Receiver;
import com.moonslab.sharlet.objects.deviceConnection;
import com.moonslab.sharlet.objects.fileOBJ;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Receiver_initiator extends AppCompatActivity {
    private TextView log;
    private DBHandler dbHandler;
    private Global global;
    private String qr_raw;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHandler = new DBHandler(this);
        global = new Global(this);
        setContentView(R.layout.activity_receiver_initiator);
        log = findViewById(R.id.log);
        //Database
        //Read and try to get the info
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        Bundle extras = getIntent().getExtras();
        String server = extras.getString("server");
        String pin = extras.getString("pin");
        qr_raw = extras.getString("qr_raw");
        if(null == server || null == pin){
            Toast.makeText(this, "Invalid Connection", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        //Discover style connect
        Handler bucket_handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                log.setText(R.string.connected);
                String data = msg.obj.toString();
                //data is a json object
                try {
                    Gson gson = new Gson();
                    deviceConnection deviceConnection = gson.fromJson(data, com.moonslab.sharlet.objects.deviceConnection.class);
                    String user = deviceConnection.getUser(),
                            photo = deviceConnection.getPhoto(),
                            ssid = deviceConnection.getSsid(),
                            link_speed = deviceConnection.getLink_speed();
                    TextView name = findViewById(R.id.user_name),
                            net_info = findViewById(R.id.net_info);
                    ImageView image = findViewById(R.id.user_image),
                            ph = findViewById(R.id.user_image_ph);
                    name.setText(user);
                    if (null != photo && !photo.equals("null")) {
                        new Thread(() -> {
                            try {
                                URL url = new URL(photo);
                                Bitmap bm = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                runOnUiThread(() -> {
                                    image.setImageBitmap(bm);
                                    ph.setVisibility(View.GONE);
                                    image.setVisibility(View.VISIBLE);
                                    if (null != ssid && null != link_speed) {
                                        String net_info_text = ssid;
                                        net_info_text = link_speed + " - " + net_info_text;
                                        net_info.setText(net_info_text);
                                    }
                                });
                            } catch (Exception e) {
                                //DO nothing
                                Log.d("RECEIVE-INITIATOR-ERROR", e.toString());
                            }
                        }).start();
                    }
                    //Finally read the bucket now
                    load_selection(server, pin);
                }
                catch (Exception e){
                    Toast.makeText(Receiver_initiator.this, "Device not found!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        };
        Net net = new Net(bucket_handler);
        //Discover server
        net.post(server+"/", "");
    }
    private void load_selection(String server, String pin){
        Handler bucket_handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String data = msg.obj.toString();
                if(data.equals("WRONG-PIN")){
                    Toast.makeText(Receiver_initiator.this, "Invalid connection!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                log.setText(R.string.starting);
                //The bucket -- save it into the database
                try {
                    Gson gson = new Gson();
                    fileOBJ[] fileOBJS = gson.fromJson(data, fileOBJ[].class);
                    dbHandler.incomingPut(fileOBJS);
                    dbHandler.add_knownIp(global.extractIPAddress(server));
                    runOnUiThread(()->{
                        Intent intent = new Intent(getApplicationContext(), Receive.class);
                        dbHandler.add_setting("sender_server_last", server);
                        dbHandler.add_setting("sender_pin_last", pin);
                        dbHandler.add_setting("sender_qr_last", qr_raw);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                }
                catch (Exception e){
                    finish();
                    Toast.makeText(Receiver_initiator.this, "Portal expired!", Toast.LENGTH_SHORT).show();
                }
            }
        };
        Net net = new Net(bucket_handler);
        net.post(server+"/bucket", "p="+pin);
    }
}