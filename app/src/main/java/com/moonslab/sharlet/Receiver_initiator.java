package com.moonslab.sharlet;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    //Read and try to get the info
    private String server_address = null, pc_address_main = null, pc_pin_main = null;
    private DBHandler dbHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_initiator);
        //Database
        dbHandler = new DBHandler(this);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        Bundle extras = getIntent().getExtras();
        String payload = extras.getString("payload");
        String main_address = null, pc_address = null, pc_pin = null;

        if(null != payload) {
            Scanner scanner = new Scanner(payload);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.contains("MAIN: ")){
                    main_address = line.replace("MAIN: ", "");
                }
                if(line.contains("PC: ")){
                    pc_address = line.replace("PC: ", "");
                }
                if(line.contains("PIN: ")){
                    pc_pin = line.replace("PIN: ", "");
                }
            }
            scanner.close();
        }
        else {
            Toast.makeText(this, "Invalid connection information!", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        //Bundle location can be changed!(Currently saving in data-folder)
        if (null == main_address || null == pc_address || null == pc_pin) {
            Toast.makeText(this, "Invalid connection information!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        else {
            File incoming_bundle = new File(this.getFilesDir(), "Selection_incoming.txt");
            server_address = main_address.replace(System.lineSeparator(), "");
            pc_address_main = pc_address.replace(System.lineSeparator(), "");
            pc_pin_main = pc_pin.replace(System.lineSeparator(), "");
            //Critical
            //Sharlet can allow all ssl
            HttpsTrustManager.allowAllSSL();
            String bucket_link = server_address + "/bucket";
            Download_bucket bucket = new Download_bucket();
            bucket.setSave_location(incoming_bundle.getPath());
            bucket.execute(bucket_link);
        }
    }

    private void go_to_receiver(){
        //Read the bucket as text
        StringBuilder raw_paths = new StringBuilder();
        StringBuilder links = new StringBuilder();
        boolean link_mode = false, error = false;
        File bundle = new File(this.getFilesDir(), "Selection_incoming.txt");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(bundle));
            String line = reader.readLine();
            while (line != null) {
                if(line.equals("[LINK_SET-SHARLET]")){
                    link_mode = true;
                }
                else {
                    if (!link_mode) {
                        raw_paths.append(line).append(System.lineSeparator());
                    } else {
                        links.append(line).append(System.lineSeparator());
                    }
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            error = true;
        }
        if(error){
            Toast.makeText(this, "Can't receive!", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        //SAVE TO SQL
        dbHandler.add_setting("receive_raw_paths", raw_paths.toString());
        dbHandler.add_setting("receive_links", links.toString());
        Intent intent = new Intent(Receiver_initiator.this, Receive.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("address", server_address);
        intent.putExtra("pc_link", pc_address_main);
        intent.putExtra("pc_pin", pc_pin_main);
        this.startActivity(intent);
        finish();
    }

    private void handle_connection_error(){
        runOnUiThread(() -> {
            finish();
            Toast.makeText(getApplicationContext(), "Connection unavailable or closed by sender!", Toast.LENGTH_LONG).show();
            //Maybe go to a information page to keep devices in save network.
        });
    }

    private class Download_bucket extends AsyncTask<String, String, String> {
        String save_location;
        boolean error = false;
        public void setSave_location(String location){
            save_location = location;
        }
        @Override
        protected String doInBackground(String... f_url) {
            if(null == save_location){
                return null;
            }
            try {
                URL url = new URL(f_url[0]);
                FileUtils.copyURLToFile(url, new File(save_location),
                        50000,
                        50000);
            } catch (Exception e) {
                error = true;
                handle_connection_error();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String file_url) {
            //Completed download
            if(!error) {
                go_to_receiver();
            }
            else {
                handle_connection_error();
            }
        }
    }
}