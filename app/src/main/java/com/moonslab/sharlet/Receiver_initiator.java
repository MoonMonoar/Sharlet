package com.moonslab.sharlet;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.moonslab.sharlet.custom.Net;

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
import java.util.Scanner;

public class Receiver_initiator extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_initiator);
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
        Handler bucket_handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String data = msg.obj.toString();
                if(data.equals("WRONG-PIN")){
                    Toast.makeText(Receiver_initiator.this, "Invalid connection!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                //The bucket




            }
        };
        //Allowing all ssl(to allow SSL anyway - not much issue)
        HttpsTrustManager.allowAllSSL();
        Net net = new Net(bucket_handler);
        net.post(server+"/bucket", "p="+pin);
    }
}