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
import com.moonslab.sharlet.custom.Net;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHandler = new DBHandler(this);
        setContentView(R.layout.activity_receiver_initiator);
        log = findViewById(R.id.log);
        //Database
        //Read and try to get the info
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        Bundle extras = getIntent().getExtras();
        String server = extras.getString("server");
        String pin = extras.getString("pin");
        if(null == server || null == pin){
            Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show();
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
                Pattern pattern = Pattern.compile("payload:'(.*?)',\\s*user:'(.*?)',\\s*photo:'(.*?)',\\s*ssid:'(.*?)',\\s*link_speed:'(.*?)'");
                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    TextView name = findViewById(R.id.user_name),
                             net_info = findViewById(R.id.net_info);
                    ImageView image = findViewById(R.id.user_image),
                            ph = findViewById(R.id.user_image_ph);
                    String user = matcher.group(2),
                            photo = matcher.group(3),
                            ssid = matcher.group(4),
                            link_speed = matcher.group(5);
                    name.setText(user);
                    if (null != photo && !photo.equals("null")) {
                        new Thread(()-> {
                            try {
                                URL url = new URL(photo);
                                Bitmap bm = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                runOnUiThread(()-> {
                                    image.setImageBitmap(bm);
                                    ph.setVisibility(View.GONE);
                                    image.setVisibility(View.VISIBLE);
                                    if(null != ssid && null != link_speed) {
                                        String net_info_text = ssid;
                                        net_info_text = link_speed+" - "+net_info_text;
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
                else {
                    Toast.makeText(Receiver_initiator.this, "Device not found!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        };
        //Allowing all ssl(to allow SSL anyway - not much issue)
        HttpsTrustManager.allowAllSSL();
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
                try{
                    Gson gson = new Gson();
                    fileOBJ[] fileOBJS = gson.fromJson(data, fileOBJ[].class);
                    dbHandler.incomingPut(fileOBJS);
                    Toast.makeText(Receiver_initiator.this, "Done", Toast.LENGTH_SHORT).show();
                }
                catch (Exception e){
                    Log.d("MOON-ERR", e.toString());
                    finish();
                    Toast.makeText(Receiver_initiator.this, "Invalid information!", Toast.LENGTH_SHORT).show();
                }
            }
        };
        //Allowing all ssl(to allow SSL anyway - not much issue)
        HttpsTrustManager.allowAllSSL();
        Net net = new Net(bucket_handler);
        net.post(server+"/bucket", "p="+pin);
    }
}