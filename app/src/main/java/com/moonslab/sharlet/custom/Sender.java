package com.moonslab.sharlet.custom;

import static com.moonslab.sharlet.Home.cancel_notification;
import static com.moonslab.sharlet.Home.default_username;
import static com.moonslab.sharlet.Home.get_app_home_bundle_data_store;
import static com.moonslab.sharlet.Home.get_appdata_location_root;
import static com.moonslab.sharlet.Music_application_class.CHANNEL_DEFAULT;
import static org.apache.commons.net.io.Util.copyStream;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.wifi.WifiInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.moonslab.sharlet.DBHandler;
import com.moonslab.sharlet.Home;
import com.moonslab.sharlet.R;
import com.moonslab.sharlet.Receive;
import com.moonslab.sharlet.Send;
import com.squareup.picasso.Picasso;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.io.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class Sender extends Service {
    //COMPONENTS
    static String server_address;
    String portal_id = "default";
    private DBHandler dbHandler;
    private static String main_payload = null;
    private long total_bytes = 0, bucket_size = 0;
    private  int packs = 0, bucket_count = 0;
    private long http_timestamp = 0, http_time_took = 0;
    private TextView top_title, portal_summary, pack_size, bucket_size_text, unusual;
    private final File bundle_file = new File(get_app_home_bundle_data_store() + "/Selection_bucket.txt");
    private boolean sending_mode_inited = false;
    private RelativeLayout waiting_view, sending_view;
    private LinearLayout qr_button;
    private static final List<Integer> notification_list = new ArrayList<>();
    public static WeakReference<Context> contextRef;
    static String ssid, link_speed;
    private TableLayout portal_files_table;
    private static Bitmap QR_bitmap;

    //FILE MANAGEMENT
    List<String> sent_paths_http = new ArrayList<>();
    List<String> sent_paths_main = new ArrayList<>();
    private final HashMap<String, View> file_all_child = new HashMap<>();
    private HashMap<String, View> file_all_child_reserved = new HashMap<>();
    private final List<File> portal_files = new ArrayList<>();

    private HttpServer server, main_server; //SERVERS

    private final int pc_ios_port = 3250;
    private String server_type = "https://";
    private static String main_pin;

    ////NEVER CHANGE THIS !!!!!!!!
    //THIS WILL NEVER BE CHANGED -- EVEN IN THE UPCOMING UPDATES
    final String PAYLOAD_DECODER_KEY = "uZ3x4OCmn*Xe&l1Ychs$pyrv^5pcoMh3gqUW&JE&lUCKM@3e!d";
    ////NEVER CHANGE THIS !!!!!!!!

    ////CHANGEABLE//
    final String SSL_JKS_PASSWORD = "97wbf37963vf78DV27%$289BD9_+-89bd8b54333^&8908H";


    //USER(THE SENDER)
    private static String user_name, last_file = "Waiting...";
    private static String user_photo_final;

    //ERROR FLAG
    boolean error_overall = false, running = false, turbo_active = false, no_token = false;

    //Thread handler
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public String getPCline(){
        if(server_type.equals("https://")) {
            return getString(R.string.static_ip_info) + System.lineSeparator() + System.lineSeparator() + "🔗 Link: " + server_type + server_address + ":3250";
        }
        return "⚡ Link: " + server_type + server_address + ":3250";
    }
    public boolean isError_overall() {
        return error_overall;
    }
    public Bitmap getQR_bitmap() {
        return QR_bitmap;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopServers();
        dbHandler.add_setting("portal_open", "false");
    }

    //Needed components ends
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dbHandler = new DBHandler(getApplicationContext());
        if(running){
            String init_check = dbHandler.get_settings("last_sender_init");
            if(init_check != null && init_check.equals("true")) {
                sending_mode_inited = false;
                mainHandler.post(Sender.this::sending_view_init);
                mainHandler.post(Sender.this::update_total_sent);
                pack_size_update();
            }
            return START_STICKY;
        }

        contextRef = new WeakReference<>(getApplicationContext());
        Global global = new Global(getApplicationContext());

        String turbo_check = dbHandler.get_settings("turbo_active");
        if(null != turbo_check && turbo_check.equals("true")){
            //CHECK TURBO TOKEN
            String token = dbHandler.get_settings("turbo_token");
            if(null != token){
                String count = global.decrypt(token, global.getDeviceEncryptionKey());
                if(null != count) {
                    int count_int = Integer.parseInt(count);
                    if(count_int > 0) {
                        server_type = "http://";
                        turbo_active = true;
                        count_int-= 1;
                        String turbo_token = global.encrypt(String.valueOf(count_int), global.getDeviceEncryptionKey());
                        //Save it
                        dbHandler.add_setting("turbo_token", turbo_token);
                        Toast.makeText(this, "Turbo token used("+count_int+" left)", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        no_token = true;
                    }
                }
            }
        }
        String user_image_path = get_appdata_location_root(getApplicationContext()) + "/user_image.png";

        //RESET
        dbHandler.add_setting("last_sender_init", "false");

        //PIN & QR SETUP
        main_pin = getRandomPIN(5); //5digit pin
        String dynamic_photo_pin = getRandomPIN(5);
        portal_id = getRandomString();

        server_address = global.getIpAddress();

        if (server_address == null) {
            error_overall = true;
            Log.d("SENDER-SERVICE-ERROR", "Connection unavailable!");
            Toast.makeText(getApplicationContext(), "Connection unavailable!", Toast.LENGTH_LONG).show();
            return START_STICKY;
        }

        //PORTS
        //Android
        //PC
        //may change, not changing will not cause error
        int main_port = 5693;

        //User setup(The sender)
        user_name = dbHandler.get_profile_data("user_name");
        if (user_name == null) {
            user_name = default_username;
        }
        File user_image = new File(user_image_path);
        //USER Photo
        if (user_image.exists()) {
            //Always http
            user_photo_final = "https://" + server_address + ":"+ main_port +"/"+ dynamic_photo_pin;
        } else {
            user_photo_final = null;
        }
        //User setup done

        WifiInfo wifi = Home.get_wifi_info(getApplicationContext());
        if (null != wifi) {
            ssid = wifi.getSSID();
            if (ssid.equals("<unknown ssid>")) {
                ssid = "Personal hotspot";
            }
        } else {
            ssid = "Personal hotspot";
        }
        if (null != wifi) {
            link_speed = wifi.getLinkSpeed() + "MBPS";
            if (wifi.getLinkSpeed() == -1) {
                link_speed = "Unknown";
            }
        } else {
            link_speed = "Unknown";
        }

        //SSL STUFF
        File key_main = get_keystore_file(getApplicationContext());
        if(key_main == null){
            error_overall = true;
            Log.d("SENDER-SERVICE-ERROR", "SSL CERTIFICATE NOT FOUND!");
            return START_STICKY;
        }
        //SSL CONFIGURATOR
        HttpsConfigurator configurator_main;
        try {
            SSLContext ssl_main = SSLContext.getInstance("TLS");

            KeyManagerFactory keyFactory_main = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore store_main = KeyStore.getInstance(KeyStore.getDefaultType());
            store_main.load(Files.newInputStream(key_main.toPath()), SSL_JKS_PASSWORD.toCharArray());
            keyFactory_main.init(store_main, SSL_JKS_PASSWORD.toCharArray());
            TrustManagerFactory trustFactory_main = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustFactory_main.init(store_main);
            ssl_main.init(keyFactory_main.getKeyManagers(), trustFactory_main.getTrustManagers(), new SecureRandom());
            configurator_main = new HttpsConfigurator(ssl_main);

            //MAIN SERVER(HTTPS) -- SECURE -- ONLY FOR ANDROID TO ANDROID -- ALWAYS HTTPS
            if(!turbo_active) {
                //Turbo inactive so - go to https mode(FOR DATA SAFETY)
                HttpsServer server2 = HttpsServer.create(new InetSocketAddress(main_port), 0);
                server2.setHttpsConfigurator(configurator_main);
                main_server = server2;
            }
            else {
                //TURBO MODE IS SAFE - SO GO AHEAD
                main_server = HttpServer.create(new InetSocketAddress(main_port), 0);
            }
        }
        catch (Exception e){
            error_overall = true;
            Log.d("SENDER-SERVICE-ERROR", "ANDROID SERVER START FAILED!");
            return START_STICKY;
        }

        //Handlers -- ONLY THE BUCKET
        main_server.createContext("/bucket", load_bucket(true));
        //PAYLOAD FOR DISCOVERY
        main_server.createContext("/", new android_discover()); //GENERAL DISCOVER
        if (user_photo_final != null) {
            //OPEN TO DISCOVER WITHOUT PIN
            main_server.createContext("/"+ dynamic_photo_pin, load_file_path(user_image_path, false, false, null)); //Don't log to history, so false
        }

        String payload_server_info = main_port +"-"+server_address+"-"+server_type+"-"+main_pin;
        QR_bitmap = Home.make_qr_code(payload_server_info, 500, Color.WHITE, Color.parseColor("#000000"));
        //Encrypt and set QR
        main_payload = global.encrypt(payload_server_info, PAYLOAD_DECODER_KEY);

        try {
            //PC SERVER
            //HTTPS server for normal send - for turbo HTTP
            if (!turbo_active){
                //HTTPS CONFIGURE
                HttpsServer server2 = HttpsServer.create(new InetSocketAddress(pc_ios_port), 0);
                //SSL
                server2.setHttpsConfigurator(configurator_main);
                notification_list.add(
                        Home.create_notification(getApplicationContext(), "Secured connection",
                                getString(R.string.static_ip_info), 21, NotificationCompat.PRIORITY_DEFAULT, true)
                );
                server = server2;
            } else {
                server = HttpServer.create(new InetSocketAddress(pc_ios_port), 0);
            }
        }
        catch (Exception e){
            error_overall = true;
            Log.d("SENDER-SERVICE-ERROR", "PC SERVER START FAILED!");
            return START_STICKY;
        }

        //PC SERVER COMPONENTS

        //A fixed context with http pin will give an array with status and auth_key
        //Pages -- only for the pc as http client needed.
        server.createContext("/", new http_index_handler()); //THE INDEX PAGE -- Default
        server.createContext("/info", new http_info_handler()); //THE INFO HANDLER
        server.createContext("/bucket", load_bucket(false)); //THE INFO HANDLER
        server.createContext("/auth", new http_auth_handler()); //THE AUTH HANDLER

        //Files
        String package_location = Home.get_appdata_location_root(this);
        //Css
        server.createContext("/css/main.css", load_file_path(package_location+"/http/css/main.css", false, false, null));
        server.createContext("/font.css", load_file_path(package_location+"/http/css/font.css", false, false, null));
        server.createContext("/trebuc.woff", load_file_path(package_location+"/http/css/trebuc.woff", false, false, null));
        server.createContext("/Trebuchet-MS-Italic.woff", load_file_path(package_location+"/http/css/Trebuchet-MS-Italic.woff", false, false, null));
        //Plugin
        server.createContext("/plugins/fontawesome/css/all.min.css", load_file_path(package_location+"/http/plugins/fontawesome/css/all.min.css", false, false, null));
        server.createContext("/plugins/fontawesome/webfonts/fa-solid-900.ttf", load_file_path(package_location+"/http/plugins/fontawesome/webfonts/fa-solid-900.ttf", false, false, null));
        server.createContext("/plugins/fontawesome/webfonts/fa-solid-900.woff2", load_file_path(package_location+"/http/plugins/fontawesome/webfonts/fa-solid-900.woff2", false, false, null));
        //Images
        server.createContext("/img/logo.png", load_file_path(package_location+"/http/img/logo.png", false, false, null));
        server.createContext("/img/favs/android-chrome-192x192.png", load_file_path(package_location+"/http/img/favs/android-chrome-192x192.png", false, false, null));
        server.createContext("/img/favs/android-chrome-384x384.png", load_file_path(package_location+"/http/img/favs/android-chrome-384x384.png", false, false, null));
        server.createContext("/img/favs/apple-touch-icon.png", load_file_path(package_location+"/http/img/favs/apple-touch-icon.png", false, false, null));
        server.createContext("/img/favs/browserconfig.xml", load_file_path(package_location+"/http/img/favs/browserconfig.xml", false, false, null));
        server.createContext("/img/favs/favicon.ico", load_file_path(package_location+"/http/img/favs/favicon.ico", false, false, null));
        server.createContext("/img/favs/favicon-16x16.png", load_file_path(package_location+"/http/img/favs/favicon-16x16.png", false, false, null));
        server.createContext("/img/favs/favicon-32x32.png", load_file_path(package_location+"/http/img/favs/favicon-32x32.png", false, false, null));
        server.createContext("/img/favs/mstile-150x150.png", load_file_path(package_location+"/http/img/favs/mstile-150x150.png", false, false, null));
        server.createContext("/img/favs/safari-pinned-tab.svg", load_file_path(package_location+"/http/img/favs/safari-pinned-tab.svg", false, false, null));
        server.createContext("/img/favs/site.webmanifest", load_file_path(package_location+"/http/img/favs/site.webmanifest", false, false, null));
        //Js
        server.createContext("/js/connect.js", load_file_path(package_location+"/http/js/connect.js", false, false, null));
        server.createContext("/js/main.js", load_file_path(package_location+"/http/js/main.js", false, false, null));
        server.createContext("/js/query.js", load_file_path(package_location+"/http/js/query.js", false, false, null));
        server.createContext("/js/jszip.min.js", load_file_path(package_location+"/http/js/jszip.min.js", false, false, null));
        server.createContext("/js/saver.js", load_file_path(package_location+"/http/js/saver.js", false, false, null));
        //HTML
        server.createContext("/receive", load_file_path(package_location+"/http/receive/index.html", false, false, null));
        server.createContext("/bucket_error.html", load_file_path(package_location+"/http/bucket_error.html", false, false, null));

        //PC SERVER COMPONENTS ENDS


        //SENDERS FILES READY
        //Sender ready files
        StringBuilder new_bucket = new StringBuilder();
        List<String> bucket_list = new ArrayList<>();
        if(bundle_file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(bundle_file));
                String line = reader.readLine();
                while (line != null) {
                    bucket_list.add(line);
                    new_bucket.append(line).append(System.lineSeparator());
                    // read next line
                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
                bucket_list = null;
            }
            if(null != bucket_list){
                new_bucket.append("[LINK_SET-SHARLET]").append(System.lineSeparator());
                for(String path : bucket_list){
                    String home = "/storage";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        home = Environment.getStorageDirectory().getPath();
                    }
                    File file = new File(home+path);
                    if(file.exists()) {
                        bucket_count++;
                        bucket_size += file.length();
                        portal_files.add(file);

                        //PASSWORD PROTECTED
                        String link_random = getRandomFilename(), file_password = getRandomFilename();
                        main_server.createContext("/"+ main_pin +"/"+link_random, load_file_path(home+path, true, true, file_password));
                        server.createContext("/"+main_pin+"/"+link_random, load_file_path(home+path, true, false, file_password));
                        new_bucket.append(link_random).append("-").append(file_password).append(System.lineSeparator());
                    }
                }
                try {
                    //Save the string again
                    PrintWriter writer = new PrintWriter(bundle_file.getPath(), "UTF-8");
                    writer.println(new_bucket);
                    writer.close();
                }
                catch (Exception e){
                    error_overall = true;
                    Log.d("SENDER-SERVICE-ERROR", "SELECTION LOAD FAILED!");
                    return START_STICKY;
                }
            }
        }

        //FINALLY START SERVERS

        //Multi thread enabled
        //Main server start -- To android
        main_server.setExecutor(Executors.newFixedThreadPool(10));
        main_server.start();

        //PC SERVER START -- TO PC and OTHERS
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        //Save portal id
        dbHandler.add_setting("last_portal", portal_id);
        dbHandler.add_setting("last_packs", String.valueOf(0));

        running = true;
        dbHandler.add_setting("portal_open", "true");

        //THE SERVICE NOTIFICATION
        Intent notificationIntent = new Intent(getApplicationContext(), Send.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String turbo_emoji = "⚡";
        if(!turbo_active){
            turbo_emoji = "🔗";
        }
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_DEFAULT)
                .setSmallIcon(R.drawable.logo_main)
                .setContentTitle("Visit on any device")
                .setContentText(turbo_emoji+" Link: "+server_type+server_address+":3250"+System.lineSeparator()+"🔑 Pin: "+main_pin)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1010, notification);

        return START_STICKY;
    }

    private void stopServers(){
        new Thread(()-> {
            if (null != main_server) {
                main_server.stop(0);
            }
            if (null != server) {
                server.stop(0);
            }
            for(Integer id : notification_list){
                cancel_notification(id, getApplicationContext());
            }
            //ALSO THE MAIN NOTIFICATIONS
            cancel_notification(1010, getApplicationContext());
        }).start();
    }

    //SERVER METHODS
    static class http_index_handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Headers h = t.getResponseHeaders();
            h.add("Cache-Control", "no-cache");
            h.add("Content-Type", "text/html; charset=UTF-8");
            File index = new File(Home.get_appdata_location_root(contextRef.get())+"/http/index.html");
            t.sendResponseHeaders(200, index.length());
            FileInputStream fis;
            fis = new FileInputStream(index);
            OutputStream os = t.getResponseBody();
            copyStream(fis, os);
            os.close();
            fis.close();
            t.close();
        }
    }

    static class http_info_handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String ajax_data = "{ssid: \""+ssid.replace("\"",  "")+"\",  link_speed: \""+link_speed+"\"}";
            t.sendResponseHeaders(200, ajax_data.length());
            OutputStream os = t.getResponseBody();
            os.write(ajax_data.getBytes());
            os.close();
        }
    }

    static class http_auth_handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String ajax_data = "{\"portal_pass\": true}";
            t.sendResponseHeaders(200, ajax_data.length());
            OutputStream os = t.getResponseBody();
            os.write(ajax_data.getBytes());
            os.close();
        }
    }

    private static String extractParameterValue(String requestBody, String parameterName) {
        String[] pairs = requestBody.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(parameterName)) {
                return decodeURL(keyValue[1]);
            }
        }
        return null;
    }
    private static String decodeURL(String url) {
        StringBuilder decoded = new StringBuilder();
        char[] chars = url.toCharArray();
        int length = chars.length;
        int i = 0;
        while (i < length) {
            char c = chars[i];
            if (c == '%' && i + 2 < length) {
                char hex1 = Character.toLowerCase(chars[i + 1]);
                char hex2 = Character.toLowerCase(chars[i + 2]);
                int digit1 = Character.digit(hex1, 16);
                int digit2 = Character.digit(hex2, 16);
                if (digit1 != -1 && digit2 != -1) {
                    decoded.append((char) ((digit1 << 4) + digit2));
                    i += 3;
                    continue;
                }
            }
            decoded.append(c);
            i++;
        }
        return decoded.toString();
    }

    static class android_discover implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ssid = ssid.replaceAll("\"", "");
            String ajax_data = "{payload:'" +main_payload+ "', user:'" +user_name+ "', photo: 'null', ssid:'"+ssid+"', link_speed:'"+link_speed+"'}";
            if(user_photo_final != null) {
                ajax_data = "{payload:'" +main_payload+ "', user:'" +user_name+ "', photo:'"+user_photo_final+"', ssid:'"+ssid+"', link_speed:'"+link_speed+"'}";
            }
            // Send a response
            exchange.sendResponseHeaders(200, ajax_data.getBytes(StandardCharsets.UTF_8).length);
            OutputStream responseBody = exchange.getResponseBody();
            responseBody.write(ajax_data.getBytes(StandardCharsets.UTF_8));
            responseBody.close();
        }
    }

    //FILE LOADER
    private HttpHandler load_file_path(String path, Boolean is_media_file, boolean is_main_server, String file_password){
        load_file file_loader = new load_file();
        file_loader.setOptions(path, is_media_file, is_main_server, file_password);
        return file_loader;
    }

    private class load_file implements HttpHandler {
        String path = null, password = null;
        Boolean media_file = false, main_server = false;
        public void setOptions(String path_of_file, Boolean is_media, boolean is_main_server, String file_password){
            path = path_of_file;
            media_file = is_media;
            main_server = is_main_server;
            password = file_password;
        }
        @SuppressLint("SetTextI18n")
        public void handle(HttpExchange t) throws IOException {

            //CHECK FOR THE PASSWORD AT PARAM p
            //IF MATCHES, GO AHEAD, otherwise error - ONLY MEDIA FILE
            if(media_file){
                // Read the request body
                InputStreamReader isr = new InputStreamReader(t.getRequestBody());
                BufferedReader br = new BufferedReader(isr);
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                br.close();
                isr.close();
                // Extract the value of the 'p' parameter
                String pValue = extractParameterValue(requestBody.toString(), "p");
                if (null == pValue || !pValue.equals(password)) {
                    String response = "DENIED: FAULTY OR MANIPULATED DATA REQUEST!";
                    // Send a response
                    t.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream responseBody = t.getResponseBody();
                    responseBody.write(response.getBytes(StandardCharsets.UTF_8));
                    responseBody.close();
                }
            }

            File file = new File(path);
            String content_type = "text/html; charset=UTF-8";
            if(!file.exists()){
                file = new File(Home.get_appdata_location_root(getApplicationContext())+"/http/404.html");
            }
            else {
                String ct = Files.probeContentType(Paths.get(file.getPath()));
                if (null != ct && !ct.isEmpty()) {
                    content_type = ct;
                }
            }

            Headers h = t.getResponseHeaders();
            h.add("Cache-Control", "no-cache");
            h.add("Content-Type", content_type);
            t.sendResponseHeaders(200, file.length());

            long timestamp = new Date().getTime();
            FileInputStream fis;

            OutputStream os = t.getResponseBody();
            if(!media_file) {
                fis = new FileInputStream(file);
                copyStream(fis, os);
                os.close();
                fis.close();
                t.close();
            }
            else {
                last_file = Receive.format_size(file.length())+" - "+file.getName();
                //Media transfer
                File finalFile1 = file;
                CopyStreamListener copyStreamListener = new CopyStreamListener() {
                    @Override
                    public void bytesTransferred(CopyStreamEvent event) {
                        //Do nothing
                    }
                    @Override
                    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                        total_bytes+= bytesTransferred;
                        long time_dif = new Date().getTime() - http_timestamp;
                        http_time_took += time_dif;
                        //Done
                        if (totalBytesTransferred == streamSize) {
                            packs++;
                            dbHandler.add_setting("last_packs", Integer.toString(packs));
                            pack_size_update();
                            //Update file list as complete
                            View child = file_all_child.get(finalFile1.getPath());
                            if (null != child) {
                                TextView file_state = child.findViewById(R.id.file_sate);
                                mainHandler.post(()->file_state.setText("\\\uf058"));
                                mainHandler.post(()->file_state.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_green)));
                                mainHandler.post(()->child.setOnClickListener(v -> Toast.makeText(getApplicationContext(), "Sent successfully", Toast.LENGTH_SHORT).show()));
                                file_all_child.remove(finalFile1.getPath());
                            }
                            mainHandler.post(Sender.this::update_total_sent);
                        }
                        else {
                            if(http_time_took%1000 == 0) {
                                mainHandler.post(Sender.this::update_total_sent);
                            }
                        }
                        http_timestamp = new Date().getTime();
                    }
                };
                http_timestamp = new Date().getTime();
                http_media_send(file, os, copyStreamListener, t);
            }
            //Add to history
            if(main_server){
                if (media_file && !sent_paths_main.contains(path)) {
                    dbHandler.add_new_file_history(portal_id, path, Long.toString(file.length()), Long.toString(timestamp), "No info", "Sharlet App");
                }
            }
            else {
                if (media_file && !sent_paths_http.contains(path)) {
                    dbHandler.add_new_file_history(portal_id, path, Long.toString(file.length()), Long.toString(timestamp), "No info", "iOs/PC");
                }
            }
        }
    }

    private void http_media_send(File sourceFile, OutputStream outputStream, CopyStreamListener progressMonitor, HttpExchange t) throws IOException {
        try (InputStream sourceFileIn = Files.newInputStream(sourceFile.toPath())) {
            try {
                Util.copyStream(sourceFileIn, outputStream, Util.DEFAULT_COPY_BUFFER_SIZE, sourceFile.length(), progressMonitor);
            } catch (Exception e) {
                t.close();
                check_file_error(sourceFile.getPath());
            } finally {
                t.close();
                outputStream.close();
            }
        } finally {
            t.close();
        }
    }

    private void check_file_error(String path) {
        if(file_all_child.containsKey(path)){
            View child = file_all_child.get(path);
            if(null != child){
                TextView file_state = child.findViewById(R.id.file_sate);
                mainHandler.post(()->file_state.setText("\\\uf057"));
                mainHandler.post(()->file_state.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.warning)));
                mainHandler.post(()->child.setOnClickListener(v->Toast.makeText(getApplicationContext(), "Could not send this file!", Toast.LENGTH_SHORT).show()));
                file_all_child.remove(path);
            }
        }
    }
    private void update_total_sent() {
        String in;
        in = get_time_span(http_time_took);
        String speed;
        float time = (float) http_time_took / (float) 1000;
        float ratio = (float) total_bytes / time;

        speed = Receive.format_size((long) ratio);
        speed += "/s";

        if((total_bytes > bucket_size)){
            unusual.setVisibility(View.VISIBLE);
        }

        String size2 = Receive.format_size(total_bytes);
        String s = size2 + " sent in " + in + " - " + speed;
        portal_summary.setText(s);
        notification_list.add(
                Home.create_notification(getApplicationContext(), "Sender is active",
                        "💪 "+s+System.lineSeparator()+"⬆️ Last: "+last_file, 105, NotificationCompat.PRIORITY_DEFAULT, true)
        );
    }
    private HttpHandler load_bucket(Boolean is_main_server){
        http_bucket_handler handler = new http_bucket_handler();
        handler.is_main(is_main_server);
        return handler;
    }
    private class http_bucket_handler implements HttpHandler {
        private boolean main_server = false;
        public void is_main(Boolean mode){
            main_server = mode;
        }
        public void handle(HttpExchange t) throws IOException {
            Headers h = t.getResponseHeaders();

            //Read request body
            InputStreamReader isr = new InputStreamReader(t.getRequestBody());
            BufferedReader br = new BufferedReader(isr);
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            br.close();
            isr.close();

            //Get val
            String pValue = extractParameterValue(requestBody.toString(), "p");
            if (null == pValue || !pValue.equals(main_pin)) {
                // Send a response
                String response = "WRONG-PIN";
                t.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream responseBody = t.getResponseBody();
                responseBody.write(response.getBytes(StandardCharsets.UTF_8));
                responseBody.close();
                return;
            }
            h.add("Cache-Control", "no-cache");
            h.add("Content-Type", "text/html");
            t.sendResponseHeaders(200, bundle_file.length());
            FileInputStream fis;
            fis = new FileInputStream(bundle_file);
            OutputStream os = t.getResponseBody();
            copyStream(fis, os);
            os.close();
            fis.close();
            mainHandler.post(Sender.this::sending_view_init);
            if(!main_server){
                notification_list.add(
                        Home.create_notification(getApplicationContext(), "PC/iOS device connected",
                                "Device connected with PC/iOS receiver.", 101, NotificationCompat.PRIORITY_DEFAULT, false)
                );
            }
            else {
                //Normal user connected
                notification_list.add(
                        Home.create_notification(getApplicationContext(), "Receiver connected",
                                "Device connected with receiver.", 102, NotificationCompat.PRIORITY_DEFAULT, false)
                );
            }
            unusual.setVisibility(View.GONE);
        }
    }
    private void sending_view_init() {
        if(sending_mode_inited){
            return;
        }
        sending_mode_inited = true;
        dbHandler.add_setting("last_sender_init", "true");
        packs = 0;
        //Init
        top_title.setText(R.string.sending);
        qr_button.setVisibility(View.VISIBLE);
        waiting_view.setVisibility(View.GONE);
        sending_view.setVisibility(View.VISIBLE);
        //Update sent files
        String target_portal = dbHandler.get_settings("last_portal");
        String last_packs = dbHandler.get_settings("last_packs");
        if(null != last_packs){
            packs+= Integer.parseInt(last_packs);
        }
        if (null != target_portal) {
            //Get the paths
            List<String> path_list = dbHandler.get_files_history_by_portal_id(target_portal);
            for (String path : path_list) {
                if(file_all_child_reserved.containsKey(path)) {
                    View child = file_all_child_reserved.get(path);
                    if(null == child){
                        continue;
                    }
                    TextView file_state = child.findViewById(R.id.file_sate);
                    file_state.setText("\\\uf058");
                    file_state.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_green));
                    child.setOnClickListener(v -> Toast.makeText(getApplicationContext(), "Sent successfully", Toast.LENGTH_SHORT).show());
                }
            }
        }
    }

    public static String get_time_span(long http_time_took) {
        int ms = (int) http_time_took;
        if(ms > 1000){
            int sec = (int)((float)ms/(float)1000);
            if(sec < 60){
                return sec+"s";
            }
            else {
                int min = (int)((float)sec/(float)60);
                if(min < 60){
                    return min+"m";
                }
                else {
                    int hr = (int)((float)min/(float)60);
                    return hr+"h";
                }
            }
        }
        else {
            return ms+"ms";
        }
    }
    private void pack_size_update() {
        String s;
        s ="Sent: "+packs+"/"+bucket_count;
        if(packs <= bucket_count) {
            mainHandler.post(()->pack_size.setText(s));
        }
        else {
            mainHandler.post(()->pack_size.setText(String.format("%s(copy)", s)));
        }
    }
    public static File get_keystore_file(Context context){
        String key_path = get_appdata_location_root(context)+"/ssl_key.jks";
        File key = new File(key_path);
        if(key.exists()){
            return key;
        }
        try {
            byte[] buff = new byte[1024];
            int read;
            try (InputStream in = context.getResources().openRawResource(R.raw.ssl_key); FileOutputStream out = new FileOutputStream(key_path)) {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            }
            if(!key.exists()){
                return null;
            }
        }
        catch (Exception e){
            return null;
        }
        return key;
    }

    private static String getRandomFilename(){
        final String ALLOWED_CHARACTERS2 ="0123456789qwertyuiopasdfghjklzxcvbnmABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(15);
        for(int i=0 ;i < 15; ++i){
            sb.append(ALLOWED_CHARACTERS2.charAt(random.nextInt(ALLOWED_CHARACTERS2.length())));

        }
        return sb.toString();
    }

    private void update_total_selection_size() {
        String size = Receive.format_size(bucket_size);
        mainHandler.post(()->bucket_size_text.setText(String.format("Total: %s", size)));
        if(portal_files.size() > 0){
            new Thread(()->{
                boolean first = false;
                for(int x = 0; x < portal_files.size(); x++){
                    File file = portal_files.get(x);
                    View child = View.inflate(getApplicationContext(), R.layout.sender_file_child, null);
                    TextView file_name = child.findViewById(R.id.file_name);
                    TextView file_path = child.findViewById(R.id.file_path);
                    ImageView file_image = child.findViewById(R.id.file_image);

                    String name = file.getName(), path = file.getPath(), main_path = file.getPath();

                    String file_type = Home.file_type(file.getName());
                    if (file_type.equals("app")) {
                        mainHandler.post(()->file_image.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_android_24)));
                        if(file.getPath().contains("/Sharlet/.Transfer_data")){
                            //Users own app
                            path = "Installed apps";
                        }
                    }

                    if(file_type.equals("photo")){
                        mainHandler.post(()->Picasso.get().load(file).placeholder(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(file_image));
                    }

                    if(file_type.equals("video")){
                        mainHandler.post(()->Glide.with(getApplicationContext())
                                .load(file)
                                .placeholder(R.drawable.ic_baseline_video_file_24)
                                .into(file_image));
                    }

                    if(file_type.equals("document")){
                        mainHandler.post(()->file_image.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.book_file)));
                    }

                    if(file_type.equals("audio")){
                        try {
                            android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                            mmr.setDataSource(file.getPath());
                            byte[] data = mmr.getEmbeddedPicture();
                            if (data != null) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                mainHandler.post(()->file_image.setImageBitmap(bitmap));
                            } else {
                                mainHandler.post(()->file_image.setImageResource(R.drawable.ic_baseline_audio_file_24));
                            }
                        } catch (Exception e) {
                            mainHandler.post(()->file_image.setImageResource(R.drawable.ic_baseline_audio_file_24));
                        }
                    }
                    mainHandler.post(()->file_name.setText(name));
                    String finalPath = path;
                    mainHandler.post(()->file_path.setText(finalPath));
                    if(!first) {
                        portal_files_table.removeAllViews();
                        first = true;
                    }
                    child.setOnClickListener(v-> Toast.makeText(getApplicationContext(), "File will be sent soon", Toast.LENGTH_SHORT).show());
                    mainHandler.post(()->portal_files_table.addView(child));
                    file_all_child.put(main_path, child);
                }
                file_all_child_reserved = file_all_child;
            }).start();
        }
        else {
            //Finish
            Toast.makeText(getApplicationContext(), "Error loading selection! Restart app with permissions", Toast.LENGTH_LONG).show();
            error_overall = true;
            Log.d("SENDER-SERVICE-ERROR", "FILE SELECTION LOAD FAILED!");
        }
    }

    public static String getRandomPIN(int size){
        final String ALLOWED_NUMBERS ="0123456789";
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(size);
        for(int i=0 ;i < size; ++i){
            sb.append(ALLOWED_NUMBERS.charAt(random.nextInt(ALLOWED_NUMBERS.length())));
        }
        return sb.toString();
    }

    private static String getRandomString(){
        final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnmABCDEFGHIJKLMNOPQRSTUVWXYZ_#@!?{}[]><,.:";
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(10);
        for(int i=0 ;i < 10; ++i){
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));

        }
        return sb.toString();
    }

    public String getLink(){
        return server_type + server_address + ":" + pc_ios_port;
    }

    public String getPin(){
        return main_pin;
    }

    public String getServer_type(){
        return server_type;
    }

    //Binder and others
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public Sender getService() {
            return Sender.this;
        }
        public void setComponents(TextView topTitle, RelativeLayout waitingView,
                                  RelativeLayout sendingView, TextView portalSummary, TextView bucketSizeText,
                                  TextView packSize, TableLayout portalFilesTable, TextView unusual_view, LinearLayout qr_button_main){
            top_title = topTitle;
            waiting_view = waitingView;
            sending_view = sendingView;
            portal_summary = portalSummary;
            bucket_size_text = bucketSizeText;
            pack_size = packSize;
            portal_files_table = portalFilesTable;
            unusual = unusual_view;
            qr_button = qr_button_main;

            update_total_selection_size();

        }
    }
    public boolean getNoToken() {
        return no_token;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return binder;
    }
}