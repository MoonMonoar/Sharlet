package com.moonslab.sharlet;

import static com.moonslab.sharlet.Home.cancel_notification;
import static com.moonslab.sharlet.Home.default_username;
import static com.moonslab.sharlet.Home.get_appdata_location_root;
import static org.apache.commons.net.io.Util.copyStream;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class Send extends AppCompatActivity {
    //Servers
    //Payload(HTTP)
    private HttpServer payload_server;
    private int payload_port = 7394;

    //Main server(HTTPS) -- ANDROID TO ANDROID FILE SHARE
    private HttpsServer main_server;
    private int main_port = 5693;
    private String main_server_pin;

    //PC/iOS(HTTP/HTTPS)
    private HttpServer server;
    private int pc_ios_port = 3250;
    private String server_type = "http://";
    private String http_pin;

    private static SecretKeySpec secretKey;
    private static Context context;
    private TextView file_update;
    String home;
    LinearLayout pc;
    private static final String ALGORITHM = "AES";
    static String server_address;
    String portal_id = "default"; //Must be unique
    //A list of data_base added paths
    List<String> sent_paths_http = new ArrayList<>();
    List<String> sent_paths_main = new ArrayList<>();

    String android_id = "Unknown";
    static String ssid, link_speed;
    private File bundle_file;
    private String user_image_path;
    private static String user_name;
    private static String user_photo_final;
    private static String main_payload = null;

    private Dialog dialog;
    private DBHandler dbHandler;

    private boolean direct_from_home = false, stop_called = false, sender_stopped = false;

    //Sending mode
    private boolean sending_mode_inited = false;
    private LinearLayout qr_button;
    private TextView top_title, portal_summary, pack_size, bucket_size_text, unusual;
    private RelativeLayout waiting_view, sending_view;
    private long total_bytes = 0, bucket_size = 0;
    private  int packs = 0, bucket_count = 0;
    private TableLayout portal_files_table;
    private final List<File> portal_files = new ArrayList<>();
    private final HashMap<String, View> file_all_child = new HashMap<>();
    private final HashMap<String, Boolean> compleate_transfers = new HashMap<>();
    private long http_timestamp = 0, http_time_took = 0;

    private static final List<Integer> notification_list = new ArrayList<>();
    private static Bitmap QR_bitmap;

    ////NEVER CHANGE THIS !!!!!!!!
    //THIS WILL NEVER BE CHANGED -- EVEN IN THE UPCOMING UPDATES
    final String QR_DECODER_KEY = "uZ3x4OCmn*Xe&l1Ychs$pyrv^5pcoMh3gqUW&JE&lUCKM@3e!d";
    ////NEVER CHANGE THIS !!!!!!!!

    ////CHANGEABLE//
    final String SSL_JKS_PASSWORD = "97wbf37963vf78DV27%$289BD9_+-89bd8b54333^&8908H";
    ///JKS KEY///

    @Override
    protected void onDestroy() {
        //Clean notifications
        stop_servers(false);
        if(null != context) {
            for(Integer id : notification_list){
                cancel_notification(id, context);
            }
        }
        super.onDestroy();
    }

    @SuppressLint({"HardwareIds", "UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        Bundle signal_bundle = this.getIntent().getExtras();
        direct_from_home = null != signal_bundle && !signal_bundle.getString("from_home").isEmpty();

        dbHandler = new DBHandler(Send.this);
        WifiInfo wifi = Home.get_wifi_info(this);
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

        //Sending mode
        qr_button = findViewById(R.id.qr_prompt);
        top_title = findViewById(R.id.top_title);
        waiting_view = findViewById(R.id.waiting_view);
        sending_view = findViewById(R.id.sending_view);
        portal_summary = findViewById(R.id.portal_summary);
        bucket_size_text = findViewById(R.id.total_bucket_size);
        pack_size = findViewById(R.id.pack_sent);
        portal_files_table = findViewById(R.id.files_table);
        unusual = findViewById(R.id.unusual);

        unusual.setOnClickListener(v->{
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle("Excessive data requirement");
            alert.setMessage("This is happening maybe because the users device is slow or the device is far from this wifi/hotspot range!");
            alert.setNegativeButton("Okay",
                    (dialog, whichButton) -> {
                        //Do nothing
                    });
            alert.show();
        });

        qr_button.setOnClickListener(v -> {
            if (null == QR_bitmap) {
                Toast.makeText(context, "QR code not ready!", Toast.LENGTH_SHORT).show();
                return;
            }
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.qr_button_popup);
            dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
            dialog.setCanceledOnTouchOutside(true);
            Window d_window = dialog.getWindow();
            d_window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ImageView qr_image = d_window.findViewById(R.id.qr_image);
            qr_image.setImageBitmap(QR_bitmap);
            dialog.show();
        });

        //Read the bucket
        file_update = findViewById(R.id.file_update);
        TextView back_button = findViewById(R.id.back_button);
        pc = findViewById(R.id.pc_button1);
        android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        context = this;
        ImageView wait_design = findViewById(R.id.waiting_design);
        home = "/storage";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            home = Environment.getStorageDirectory().getPath();
        }

        //Username & photo - for payload server
        user_name = dbHandler.get_profile_data("user_name");
        if (user_name == null) {
            user_name = default_username;
        }
        user_image_path = get_appdata_location_root(this) + "/user_image.png";
        File user_image = new File(user_image_path);
        if (user_image.exists()) {
            //Always http
            user_photo_final = "http://" + server_address + ":7394/photo";
        } else {
            user_photo_final = null;
        }

        //Confirm dialogue
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.confirm_dialog_layout);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView d_title = dialog.getWindow().findViewById(R.id.title);
        TextView d_des = dialog.getWindow().findViewById(R.id.description);

        //Customize
        d_title.setText("Close portal?");
        d_des.setText("If you exit, your selection will be lost and the portal will be closed.");

        Button Okay = dialog.findViewById(R.id.btn_okay);
        Button Cancel = dialog.findViewById(R.id.btn_cancel);
        Okay.setText("Close");

        Okay.setOnClickListener(v -> {
            if(stop_called){
                if(!sender_stopped) {
                    Toast.makeText(this, "Closing...wait", Toast.LENGTH_SHORT).show();
                }
                else {
                    super.finish();
                }
                return;
            }
            stop_called = true;
            stop_servers(true);
            d_des.setText("Holding the horses, please wait...");
            //Finish and go to home
            Home.clean_bundle_and_data();
            if (direct_from_home) {
                startActivity(new Intent(this, Home.class));
            }
        });

        Cancel.setOnClickListener(v -> dialog.dismiss());

        //Dialogue ends

        if (!Home.create_app_folders()) {
            Toast.makeText(this, "Permission is required! Restart or reinstall app", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        //Bundle file
        bundle_file = new File(Home.get_app_home_bundle_data_store() + "/Selection_bucket.txt");

        //PC SERVER PIN
        http_pin = getRandomPIN(5); //5digit pin
        //MAIN SERVER PIN
        main_server_pin = getRandomPIN(5);
        portal_id = getRandomString();
        //Check ip
        server_address = getIpAddress();

        if (server_address == null) {
            //No connection
            Toast.makeText(this, "Connection unavailable!",
                    Toast.LENGTH_LONG).show();
            this.finish();
            if (direct_from_home) {
                startActivity(new Intent(this, Home.class));
            }
        }

        //Continue to setup server
        //Create from here
        file_update.setText("🪄 Starting connection...");
        //Ready servers

        //HTTP SERVERS
        start_servers();

        //This layout is for version 1.0
        String payload = "MAIN: https://" + server_address + ":" + main_port + "/" + main_server_pin
                + System.lineSeparator() +
                "PC: " + server_type + server_address + ":" + pc_ios_port
                +System.lineSeparator()+
                "PIN: " +http_pin;

        //Encrypt
        payload = encrypt(payload, QR_DECODER_KEY);
        main_payload = payload;

        Bitmap bmp = Home.make_qr_code(payload, 2000, Color.WHITE, Color.parseColor("#ff7f27"));
        QR_bitmap = bmp;
        wait_design.setImageBitmap(bmp);
        file_update.setText("👀 Ready! waiting for receivers.");
        //NOW LOOK FOR RECEIVERS...

        //Don't bother messing with these
        //Click events
        pc.setOnClickListener(v -> {
            if(!Home.is_http_ready(context)){
                Toast.makeText(context, "Plugin not found! Try again or Restart app", Toast.LENGTH_SHORT).show();
                Home.setup_http_plugin(context);
                return;
            }
            String link = server_type+server_address+":"+pc_ios_port;
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle("Visit on any device");
            String body = "Link: "+link+System.lineSeparator()+"Pin: "+http_pin;
            if(server_type.equals("https://")){
                body+= System.lineSeparator()+System.lineSeparator()+"Please note as a static certificate, the HTTPS connection may show insecure by the browser. Please ignore this warning and proceed if occurs.";
            }
            else {
                body+= System.lineSeparator()+System.lineSeparator()+"Warning: If you are not using your own password protected hotspot or trusted wifi, please enable HTTPS from settings.";
            }
            alert.setMessage(body);
            alert.setPositiveButton("Copy", (dialog, whichButton) -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Sharlet link", link);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show();
            });
            alert.setNegativeButton("Close",
                    (dialog, whichButton) -> {
                        //Do nothing
                    });
            alert.show();
        });
        back_button.setOnClickListener(v -> dialog.show());
    }

    private void sending_view_init() {
        if(sending_mode_inited){
            return;
        }
        sending_mode_inited = true;
        packs = 0;
        //Init
        runOnUiThread(()-> {
            top_title.setText("Sending ");
            qr_button.setVisibility(View.VISIBLE);
            waiting_view.setVisibility(View.GONE);
            sending_view.setVisibility(View.VISIBLE);
        });
    }

    //PIN GENERATORS
    private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnmABCDEFGHIJKLMNOPQRSTUVWXYZ_#@!?{}[]><,.:";
    private static String getRandomString(){
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(10);
        for(int i=0 ;i < 10; ++i){
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));

        }
        return sb.toString();
    }

    private static final String ALLOWED_CHARACTERS2 ="0123456789qwertyuiopasdfghjklzxcvbnmABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static String getRandomFilename(){
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(15);
        for(int i=0 ;i < 15; ++i){
            sb.append(ALLOWED_CHARACTERS2.charAt(random.nextInt(ALLOWED_CHARACTERS2.length())));

        }
        return sb.toString();
    }

    public static final String ALLOWED_NUMBERS ="0123456789";
    public static String getRandomPIN(int size){
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(size);
        for(int i=0 ;i < size; ++i){
            sb.append(ALLOWED_NUMBERS.charAt(random.nextInt(ALLOWED_NUMBERS.length())));
        }
        return sb.toString();
    }
    public void prepareSecreteKey(String myKey) {
        MessageDigest sha;
        try {
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    public String encrypt(String strToEncrypt, String secret) {
        try {
            prepareSecreteKey(secret);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return android.util.Base64.encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")), android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING);
        } catch (Exception e) {
            file_update.setText("❌ Failed, Error while encrypting.");
        }
        return null;
    }

    //Server
    //FUNCTIONS
    private void start_servers(){
        try {
            //PAYLOAD SERVER
            //Default for discovering -- payload
            payload_server = HttpServer.create(new InetSocketAddress(payload_port), 0);
            payload_server.createContext("/", new http_payload());
            if(user_photo_final != null) {
                payload_server.createContext("/photo", load_file_path(user_image_path, false, false)); //Don't log to history, so false
            }
            //Multi threaded -- PAYLOAD SERVER START
            payload_server.setExecutor(Executors.newFixedThreadPool(10));
            payload_server.start();

            //MAIN SERVER(HTTPS)
            main_server = HttpsServer.create(new InetSocketAddress(main_port), 0);
            SSLContext ssl_main = SSLContext.getInstance("TLS");
            File key_main = get_keystore_file(context);
            if(null == key_main){
                Toast.makeText(context, "Failed to encrypt the sender, aborted!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            KeyManagerFactory keyFactory_main = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore store_main = KeyStore.getInstance(KeyStore.getDefaultType());
            store_main.load(new FileInputStream(key_main), SSL_JKS_PASSWORD.toCharArray());
            keyFactory_main.init(store_main, SSL_JKS_PASSWORD.toCharArray());
            TrustManagerFactory trustFactory_main = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustFactory_main.init(store_main);
            ssl_main.init(keyFactory_main.getKeyManagers(),
                    trustFactory_main.getTrustManagers(), new SecureRandom());
            HttpsConfigurator configurator_main = new HttpsConfigurator(ssl_main);
            main_server.setHttpsConfigurator(configurator_main);

            //Handlers
            main_server.createContext("/"+main_server_pin+"/bucket", load_bucket(true));

            //PC SERVER
            //HTTPS server optional
            String is_http = dbHandler.get_settings("use_https");
            if(null != is_http && is_http.equals("true")) {
                HttpsServer server2 = HttpsServer.create(new InetSocketAddress(pc_ios_port), 0);
                //SSL
                SSLContext ssl = SSLContext.getInstance("TLS");
                File key = get_keystore_file(context);
                if(null == key){
                    Toast.makeText(context, "Failed to encrypt the sender, aborted!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
                store.load(new FileInputStream(key), SSL_JKS_PASSWORD.toCharArray());
                keyFactory.init(store, SSL_JKS_PASSWORD.toCharArray());
                TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustFactory.init(store);
                ssl.init(keyFactory.getKeyManagers(),
                        trustFactory.getTrustManagers(), new SecureRandom());
                HttpsConfigurator configurator = new HttpsConfigurator(ssl);
                server2.setHttpsConfigurator(configurator);
                server = server2;
                server_type = "https://";
            }
            else {
                server = HttpServer.create(new InetSocketAddress(pc_ios_port), 0);
                server_type = "http://";
            }

            //A fixed context with http pin will give an array with status and auth_key
            //Pages
            server.createContext("/", new http_index_handler()); //THE INDEX PAGE -- Deafult
            server.createContext("/"+http_pin+"/info", new http_info_handler()); //THE INFO HANDLER
            server.createContext("/"+http_pin+"/bucket", load_bucket(false)); //THE INFO HANDLER
            server.createContext("/"+http_pin+"/auth", new http_auth_handler()); //THE AUTH HANDLER

            //Files
            String package_location = Home.get_appdata_location_root(this);
            //Css
            server.createContext("/css/main.css", load_file_path(package_location+"/http/css/main.css", false, false));
            server.createContext("/font.css", load_file_path(package_location+"/http/css/font.css", false, false));
            server.createContext("/trebuc.woff", load_file_path(package_location+"/http/css/trebuc.woff", false, false));
            server.createContext("/Trebuchet-MS-Italic.woff", load_file_path(package_location+"/http/css/Trebuchet-MS-Italic.woff", false, false));
            //Plugin
            server.createContext("/plugins/fontawesome/css/all.min.css", load_file_path(package_location+"/http/plugins/fontawesome/css/all.min.css", false, false));
            server.createContext("/plugins/fontawesome/webfonts/fa-solid-900.ttf", load_file_path(package_location+"/http/plugins/fontawesome/webfonts/fa-solid-900.ttf", false, false));
            server.createContext("/plugins/fontawesome/webfonts/fa-solid-900.woff2", load_file_path(package_location+"/http/plugins/fontawesome/webfonts/fa-solid-900.woff2", false, false));
            //Images
            server.createContext("/img/logo.png", load_file_path(package_location+"/http/img/logo.png", false, false));
            server.createContext("/img/favs/android-chrome-192x192.png", load_file_path(package_location+"/http/img/favs/android-chrome-192x192.png", false, false));
            server.createContext("/img/favs/android-chrome-384x384.png", load_file_path(package_location+"/http/img/favs/android-chrome-384x384.png", false, false));
            server.createContext("/img/favs/apple-touch-icon.png", load_file_path(package_location+"/http/img/favs/apple-touch-icon.png", false, false));
            server.createContext("/img/favs/browserconfig.xml", load_file_path(package_location+"/http/img/favs/browserconfig.xml", false, false));
            server.createContext("/img/favs/favicon.ico", load_file_path(package_location+"/http/img/favs/favicon.ico", false, false));
            server.createContext("/img/favs/favicon-16x16.png", load_file_path(package_location+"/http/img/favs/favicon-16x16.png", false, false));
            server.createContext("/img/favs/favicon-32x32.png", load_file_path(package_location+"/http/img/favs/favicon-32x32.png", false, false));
            server.createContext("/img/favs/mstile-150x150.png", load_file_path(package_location+"/http/img/favs/mstile-150x150.png", false, false));
            server.createContext("/img/favs/safari-pinned-tab.svg", load_file_path(package_location+"/http/img/favs/safari-pinned-tab.svg", false, false));
            server.createContext("/img/favs/site.webmanifest", load_file_path(package_location+"/http/img/favs/site.webmanifest", false, false));
            //Js
            server.createContext("/js/connect.js", load_file_path(package_location+"/http/js/connect.js", false, false));
            server.createContext("/js/main.js", load_file_path(package_location+"/http/js/main.js", false, false));
            server.createContext("/js/query.js", load_file_path(package_location+"/http/js/query.js", false, false));
            server.createContext("/js/jszip.min.js", load_file_path(package_location+"/http/js/jszip.min.js", false, false));
            server.createContext("/js/saver.js", load_file_path(package_location+"/http/js/saver.js", false, false));
            //HTML
            server.createContext("/x-"+http_pin+"/receive", load_file_path(package_location+"/http/receive/index.html", false, false));
            server.createContext("/bucket_error.html", load_file_path(package_location+"/http/bucket_error.html", false, false));

            //Sender ready files
            String new_bucket = "";
            List<String> bucket_list = new ArrayList<>();
            if(bundle_file.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(bundle_file));
                    String line = reader.readLine();
                    while (line != null) {
                        bucket_list.add(line);
                        new_bucket+= line+System.lineSeparator();
                        // read next line
                        line = reader.readLine();
                    }
                    reader.close();
                } catch (IOException e) {
                    bucket_list = null;
                }
                if(null != bucket_list){
                    new_bucket+= "[LINK_SET-SHARLET]"+System.lineSeparator();
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
                            String link_random = getRandomFilename();
                            main_server.createContext("/"+main_server_pin+"/get_file/"+link_random, load_file_path(home+path, true, true));
                            server.createContext("/"+http_pin+"/"+link_random, load_file_path(home+path, true, false));
                            new_bucket+= link_random+System.lineSeparator();
                        }
                    }
                    update_total_selection_size();
                    //Save the string again
                    PrintWriter writer = new PrintWriter(bundle_file.getPath(), "UTF-8");
                    writer.println(new_bucket);
                    writer.close();
                }
            }

            //Multi thread enabled
            //Main server start
            main_server.setExecutor(Executors.newFixedThreadPool(10));
            main_server.start();

            //PC SERVER START
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            //Notification code 100
            notification_list.add(
                    Home.create_notification(this, "Visit on any device",
                    "Link: "+server_type+server_address+":3250"+System.lineSeparator()+"Pin: "+http_pin, 100, NotificationCompat.PRIORITY_DEFAULT, true)
            );

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            //ERROR CREATING HTTP SERVER, Abort
            Toast.makeText(context, "Failed to encrypt the sender, aborted! #001", Toast.LENGTH_SHORT).show();
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            this.finish();
            if(direct_from_home){
                startActivity(new Intent(this, Home.class));
            }
        } catch (UnrecoverableKeyException e) {
            Toast.makeText(context, "Failed to encrypt the sender, aborted! #011", Toast.LENGTH_SHORT).show();
            this.finish();
            if(direct_from_home){
                startActivity(new Intent(this, Home.class));
            }
        } catch (CertificateException e) {
            Toast.makeText(context, "Failed to encrypt the sender, aborted! #111", Toast.LENGTH_SHORT).show();
            this.finish();
            if(direct_from_home){
                startActivity(new Intent(this, Home.class));
            }
        }
    }

    private void stop_servers(boolean exit) {
        if(exit) {
            Toast.makeText(context, "Closing...", Toast.LENGTH_SHORT).show();
        }
        new Thread(()-> {
            if (null != main_server) {
                main_server.stop(0);
            }
            if (null != server) {
                server.stop(0);
            }
            if (null != payload_server) {
                payload_server.stop(0);
            }
            sender_stopped = true;
            if(exit){
                super.finish();
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void update_total_selection_size() {
        String size = Receive.format_size(bucket_size);
        runOnUiThread(()-> bucket_size_text.setText("Total: "+size));
        if(portal_files.size() > 0){
            new Thread(()->{
                boolean first = false;
                for(int x = 0; x < portal_files.size(); x++){
                    File file = portal_files.get(x);
                    View child = View.inflate(context, R.layout.sender_file_child, null);
                    TextView file_name = child.findViewById(R.id.file_name);
                    TextView file_path = child.findViewById(R.id.file_path);
                    ImageView file_image = child.findViewById(R.id.file_image);

                    String name = file.getName(), path = file.getPath(), main_path = file.getPath();

                    String file_type = Home.file_type(file.getName());
                    if (file_type.equals("app")) {
                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_android_24));
                        if(file.getPath().contains("/Sharlet/.Transfer_data")){
                            //Users own app
                            path = "Installed apps";
                        }
                    }

                    if(file_type.equals("photo")){
                        runOnUiThread(() -> Picasso.get().load(file).placeholder(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(file_image));
                    }

                    if(file_type.equals("video")){
                        runOnUiThread(()-> Glide.with(context)
                                .load(file)
                                .placeholder(R.drawable.ic_baseline_video_file_24)
                                .into(file_image));
                    }

                    if(file_type.equals("document")){
                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.book_file));
                    }

                    if(file_type.equals("audio")){
                        try {
                            android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                            mmr.setDataSource(file.getPath());
                            byte[] data = mmr.getEmbeddedPicture();
                            if (data != null) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                file_image.setImageBitmap(bitmap);
                            } else {
                                file_image.setImageResource(R.drawable.ic_baseline_audio_file_24);
                            }
                        } catch (Exception e) {
                            file_image.setImageResource(R.drawable.ic_baseline_audio_file_24);
                        }
                    }
                    file_name.setText(name);
                    file_path.setText(path);
                    if(!first) {
                        runOnUiThread(()-> portal_files_table.removeAllViews());
                        first = true;
                    }
                    child.setOnClickListener(v-> runOnUiThread(()-> Toast.makeText(context, "File will be sent soon", Toast.LENGTH_SHORT).show()));
                    runOnUiThread(()-> portal_files_table.addView(child));
                    file_all_child.put(main_path, child);
                }
            }).start();
        }
        else {
            //Finish
            Toast.makeText(context, "Error loading selection! Restart app with permissions", Toast.LENGTH_LONG).show();
            this.finish();
            if(direct_from_home){
                startActivity(new Intent(this, Home.class));
            }
        }
    }

    //HTTP Handlers
    static class http_payload implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String ajax_data = "user: " + user_name+System.lineSeparator()+"photo: null"+System.lineSeparator()+"payload: " + main_payload;
            if(user_photo_final != null) {
                ajax_data = "user: " + user_name+System.lineSeparator()+"photo: "+user_photo_final+System.lineSeparator()+"payload: " + main_payload;
            }
            t.sendResponseHeaders(200, ajax_data.length());
            OutputStream os = t.getResponseBody();
            os.write(ajax_data.getBytes());
            os.close();
            t.close();
        }
    }

    static class http_index_handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Headers h = t.getResponseHeaders();
            h.add("Cache-Control", "no-cache");
            h.add("Content-Type", "text/html; charset=UTF-8");
            File index = new File(Home.get_appdata_location_root(context)+"/http/index.html");
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

    private HttpHandler load_file_path(String path, Boolean is_media_file, boolean is_main_server){
        load_file file_loader = new load_file();
        file_loader.setOptions(path, is_media_file, is_main_server);
        return file_loader;
    }

    private class load_file implements HttpHandler {
        String path = null;
        Boolean media_file = false, main_server = false;
        public void setOptions(String path_of_file, Boolean is_media, boolean is_main_server){
            path = path_of_file;
            media_file = is_media;
            main_server = is_main_server;
        }
        @SuppressLint("SetTextI18n")
        public void handle(HttpExchange t) throws IOException {
            File file = new File(path);
            String content_type = "text/html; charset=UTF-8";
            if(!file.exists()){
                file = new File(Home.get_appdata_location_root(context)+"/http/404.html");
            }
            else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    String ct = Files.probeContentType(Paths.get(file.getPath()));
                    if (null != ct && !ct.isEmpty()) {
                        content_type = ct;
                    }
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
                                pack_size_update();
                                if(!compleate_transfers.containsKey(finalFile1.getPath())) {
                                    compleate_transfers.put(finalFile1.getPath(), true);
                                }
                                //Update file list as complete
                                View child = file_all_child.get(finalFile1.getPath());
                                if (null != child) {
                                    TextView file_state = child.findViewById(R.id.file_sate);
                                    runOnUiThread(() -> {
                                        file_state.setText("\\\uf058");
                                        file_state.setTextColor(ContextCompat.getColor(context, R.color.light_green));
                                        child.setOnClickListener(v -> {
                                            AlertDialog.Builder alert = new AlertDialog.Builder(context);
                                            alert.setTitle("Successful transaction");
                                            alert.setMessage("This file was sent successfully!");
                                            alert.setNegativeButton("Okay",
                                                    (dialog, whichButton) -> {
                                                        //Do nothing
                                                    });
                                            alert.show();
                                        });
                                    });
                                    file_all_child.remove(finalFile1.getPath());
                                }
                                update_total_sent();
                            }
                            else {
                                if(http_time_took%1000 == 0) {
                                    update_total_sent();
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

    private void check_file_error(String path) {
        if(file_all_child.containsKey(path)){
            View child = file_all_child.get(path);
            if(null != child){
                TextView file_state = child.findViewById(R.id.file_sate);
                runOnUiThread(()->{
                    file_state.setText("\\\uf057");
                    file_state.setTextColor(ContextCompat.getColor(context, R.color.warning));
                    child.setOnClickListener(v->{
                        AlertDialog.Builder alert = new AlertDialog.Builder(context);
                        alert.setTitle("Unsuccessful transaction");
                        alert.setMessage("Sharlet could not verify whether this file was successfully sent or not. Maybe the receiver closed the connection or left!");
                        alert.setNegativeButton("Okay",
                                (dialog, whichButton) -> {
                                    //Do nothing
                                });
                        alert.show();
                    });
                });
                file_all_child.remove(path);
            }
        }
    }

    private void http_media_send(File sourceFile, OutputStream outputStream, CopyStreamListener progressMonitor, HttpExchange t) throws IOException {
        InputStream sourceFileIn = new FileInputStream(sourceFile);
        try {
            try {
                Util.copyStream(sourceFileIn, outputStream, Util.DEFAULT_COPY_BUFFER_SIZE, sourceFile.length(), progressMonitor);
            }
            catch (Exception e){
                t.close();
                check_file_error(sourceFile.getPath());
            }
            finally {
                t.close();
                outputStream.close();
            }
        } finally {
            t.close();
            sourceFileIn.close();
        }
    }

    @SuppressLint("SetTextI18n")
    private void pack_size_update() {
        String s;
        s ="Sent: "+packs+"/"+bucket_count;
        if(packs <= bucket_count) {
            runOnUiThread(() -> pack_size.setText(s));
        }
        else {
            runOnUiThread(() -> pack_size.setText(s+"(copy)"));
        }
    }

    @SuppressLint("SetTextI18n")

    private void update_total_sent() {
            String in;
            in = get_time_span(http_time_took);
            String speed;
            float time = (float) http_time_took / (float) 1000;
            float ratio = (float) total_bytes / time;

            speed = Receive.format_size((long) ratio);
            speed += "/s";

            if((total_bytes > bucket_size)){
                runOnUiThread(() ->unusual.setVisibility(View.VISIBLE));
            }

            String size2 = Receive.format_size(total_bytes);
            String s = size2 + " sent in " + in + " - " + speed;
            runOnUiThread(() -> portal_summary.setText(s));
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
            h.add("Cache-Control", "no-cache");
            h.add("Content-Type", "text/html");
            t.sendResponseHeaders(200, bundle_file.length());
            FileInputStream fis;
            fis = new FileInputStream(bundle_file);
            OutputStream os = t.getResponseBody();
            copyStream(fis, os);
            os.close();
            fis.close();
            sending_view_init();
            if(!main_server){
                notification_list.add(
                        Home.create_notification(context, "PC/iOS device connected",
                                "Device connected with PC/iOS receiver.", 101, NotificationCompat.PRIORITY_DEFAULT, false)
                );
            }
            else {
                //Normal user connected
                notification_list.add(
                        Home.create_notification(context, "Receiver connected",
                                "Device connected with receiver.", 102, NotificationCompat.PRIORITY_DEFAULT, false)
                );
            }
            runOnUiThread(() ->unusual.setVisibility(View.GONE));
        }
    }
    //Server

    //Read the ip of wifi
    public static String getIpAddress() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        if(null == ip) {
                            ip = inetAddress.getHostAddress();
                        }
                    }
                }
            }

        } catch (SocketException e) {
            return null;
        }
        return ip;
    }

    //SSL READY -- FOR HTTP -- STATIC
    public static File get_keystore_file(Context context){
        String key_path = get_appdata_location_root(context)+"/ssl_key.jks";
        File key;
        try {
            byte[] buff = new byte[1024];
            int read;
            try (InputStream in = context.getResources().openRawResource(R.raw.ssl_key); FileOutputStream out = new FileOutputStream(key_path)) {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            }
            //Re initiate
            key = new File(key_path);
            if(!key.exists()){
                return null;
            }
        }
        catch (Exception e){
            return null;
        }
        return key;
    }

    //Back handler
    public void onBackPressed() {
        dialog.show();
    }
}