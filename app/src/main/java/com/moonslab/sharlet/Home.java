package com.moonslab.sharlet;

import static com.moonslab.sharlet.Music_application_class.CHANNEL_DEFAULT;
import static com.moonslab.sharlet.Music_player_service.folderFromPath;
import static com.moonslab.sharlet.custom.Sender.PAYLOAD_DECODER_KEY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.gson.Gson;
import com.moonslab.sharlet.custom.Global;
import com.moonslab.sharlet.custom.NetUtility;
import com.moonslab.sharlet.custom.ScannedPeer;
import com.moonslab.sharlet.custom.Sender;
import com.moonslab.sharlet.musiclibrary.adapter;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import net.lingala.zip4j.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class Home extends AppCompatActivity {
    private NetUtility netUtility;
    private boolean update_daily_token = false;
    private Global global_class;
    public static int clicks = 0;
    private boolean direct_to_sender = false;
    private int total_child_history = 0;
    private LinearLayout send_now_button2 = null;
    private static Thread search_thread;
    private static Boolean query_busy = false;
    private RelativeLayout last_player_tag = null;
    private Context context;
    private TextView title;
    private TextView home, files, history, music, ad_icon;
    private FrameLayout home_frame;
    private LayoutInflater inflater;
    public static String current_tab = null;
    //Ignorable filed leak
    @SuppressLint("StaticFieldLeak")
    public static TextView send_button;
    public static String default_username = android.os.Build.MODEL;
    //Ignorable filed leak
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar loading_state;
    private DBHandler dbHandler;
    ImageView user_profile_image = null;
    private String user_image_path;
    private TextView reset_l;
    private Dialog token_info;
    //Ads -- native ad
    private static final String ADMOB_AD_UNIT_ID = "ca-app-pub-3865008552851810/7331691260"; //NATIVE
    private static final String AD_UNIT_ID2 = "ca-app-pub-3865008552851810/3056532195"; //Interstitial
    private static final String REWARD_AD_UNIT_TURBO_TOKEN = "ca-app-pub-3865008552851810/7505620460"; //Reward
    private NativeAd nativeAd;
    private RewardedAd rewardedAd;
    AdRequest adRequest;
    public static InterstitialAd interstitialAd;
    //Push screen dimens
    DisplayMetrics displayMetrics = new DisplayMetrics();
    private int display_height, display_width;
    private PackageManager pm;
    private ProgressBar ad_loading;
    //Ignorable filed leak
    //Selection update
    @SuppressLint("StaticFieldLeak")
    public static TextView send_now_count = null;
    //Ignorable filed leak
    @SuppressLint("StaticFieldLeak")
    public static RelativeLayout send_now = null;

    @Override
    public void onBackPressed() {
        vibrate(70, this);
        if(null != current_tab){
            if(current_tab.equals("local")){
                findViewById(R.id.button_home).performClick();
                return;
            }
            if(current_tab.equals("history")){
                findViewById(R.id.button_local).performClick();
                return;
            }
            if(current_tab.equals("music")){
                findViewById(R.id.button_history).performClick();
                return;
            }
        }
        //Ask for back
        //Show
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Quit Sharlet?");
        alert.setMessage("Do you really want to close Sharlet?");
        alert.setPositiveButton("Quit", (dialog, whichButton) -> super.onBackPressed());
        alert.setNegativeButton("Cancel",
                (dialog, whichButton) -> {
                    //Do nothing
                });
        alert.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_home);

        //Check for update
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if(appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        registerForActivityResult(
                                new ActivityResultContracts.StartIntentSenderForResult(),
                                result -> {
                                    if (result.getResultCode() != RESULT_OK) {
                                        Toast.makeText(getApplicationContext(), "Update failed!", Toast.LENGTH_SHORT).show();
                                    }
                                }), AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                                .setAllowAssetPackDeletion(true)
                                .build());
            }
        });


        global_class = new Global(this);
        context = this;
        netUtility = new NetUtility(this);
        inflater = LayoutInflater.from(context);
        super.onCreate(savedInstanceState);
        adRequest = new AdRequest.Builder().build();
        user_image_path = get_appdata_location_root(context)+"/user_image.png";
        pm = context.getPackageManager();
        //Ready ad
        loadInterstitialAd(this);

        //Display
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        display_height = displayMetrics.heightPixels;
        display_width = displayMetrics.widthPixels;

        //Database
        dbHandler = new DBHandler(Home.this);

        //First time install detect
        String install_timestamp = dbHandler.get_settings("install_timestamp");
        if(null == install_timestamp){
            //New user
            //GIVE 4 Turbo token for free
            String turbo_token = global_class.encrypt("4", global_class.getDeviceEncryptionKey());
            //Save it
            dbHandler.add_setting("turbo_token", turbo_token);
            //Daily timer
            //dbHandler.add_setting("last_turbo_daily", global_class.encrypt(String.valueOf(System.currentTimeMillis()+24*60*60*1000), global_class.getDeviceEncryptionKey()));

            //turn on loop playlist for music player(default setting)
            dbHandler.add_setting("music_loop", "on");

            //Store install timestamp
            dbHandler.add_setting("install_timestamp", Long.toString(get_timestamp()));
        }

        //Ready ads
        MobileAds.initialize(this, initializationStatus -> {});

        //Permissions
        if(all_permission_once()){
            //Create app specific folders
            create_app_folders();
            //Clean bundles
            clean_bundle_and_data();
        }

        //Components
        home_frame = findViewById(R.id.home_frame);

        title = findViewById(R.id.title);
        loading_state = findViewById(R.id.home_loading);


        //Buttons
        // Capture our button from layout
        Button button3 = findViewById(R.id.button_user_profile);
        home = findViewById(R.id.button_home);
        files = findViewById(R.id.button_local);
        history = findViewById(R.id.button_history);
        music = findViewById(R.id.button_music);
        Button menu = findViewById(R.id.button_menu);

        button3.setOnClickListener(Listener);
        menu.setOnClickListener(Listener);
        home.setOnClickListener(Listener);
        history.setOnClickListener(Listener);
        music.setOnClickListener(Listener);
        files.setOnClickListener(Listener);

        ImageView up = findViewById(R.id.button_user_profile_pic);
        up.setOnClickListener(v-> button3.performClick());


        //TAB switcher(default)
        Bundle tab_bundle = this.getIntent().getExtras();
        //Default page
        if(tab_bundle != null){
            if(tab_bundle.getBoolean("music_tab")){
                music.performClick();
            }
            else {
                load_home();
            }
        }
        else {
            load_home();
        }

        //Setup plugin -- always make it ready
        setup_http_plugin(this);
        //MAIN ENDS
        //Profile picture
        update_user_pic_view();

        //Turbo info dialogue
        token_info = new Dialog(this);
        token_info.setContentView(R.layout.no_token_dialogue);
        token_info.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.token_alert_back));
        token_info.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        token_info.setCanceledOnTouchOutside(true);
        TextView title = token_info.findViewById(R.id.title),
                desc = token_info.findViewById(R.id.description);
        title.setText(R.string.turbo_tokens);
        desc.setText(R.string.turbo_tokens_lets_you_to_use_turbo_mode_while_sending_files_you_can_get_tokens_by_claiming_daily_reward_or_watching_ads);

        LinearLayout got_it = token_info.findViewById(R.id.btn_got_it);
        got_it.setOnClickListener(v -> token_info.dismiss());
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        //Tab reload
        if(null != current_tab && current_tab.equals("music")){
            music.performClick();
        }
        if(null != current_tab && current_tab.equals("home")){
            home.performClick();
        }
        all_permission_once();
        if(receive_prompt){
            scan_network();
            return;
        }
        if(sender_prompt){
            if(direct_to_sender){
                sender_prompt = false;
                if(null != send_now_button2){
                    send_now_button2.performClick();
                }
                return;
            }
            if(null != send_button){
                send_button.performClick();
            }
        }
    }
    @Override
    protected void onDestroy() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        super.onDestroy();
    }

    private void update_user_pic_view() {
        File user_image_now = new File(user_image_path);
        Button ub = findViewById(R.id.button_user_profile);
        ImageView up = findViewById(R.id.button_user_profile_pic);
        if(user_image_now.exists()){
            Picasso.get().load(user_image_now).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).placeholder(R.drawable.ic_baseline_supervised_user_circle_24_white).resize(250, 250).centerCrop().into(up);
            ub.setVisibility(View.GONE);
            up.setVisibility(View.VISIBLE);
        }
        else {
            up.setVisibility(View.GONE);
            ub.setVisibility(View.VISIBLE);
        }
    }

    //Main click listener
    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener Listener = v -> {
        switch (v.getId()) {
            case R.id.button_home:
                showInterstitial(this);
                home.setBackground(ContextCompat.getDrawable(context, R.color.dark_primary));
                files.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                history.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                music.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                load_home();
                current_tab = "home";
                break;
            case R.id.button_local:
                showInterstitial(this);
                home.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                files.setBackground(ContextCompat.getDrawable(context, R.color.dark_primary));
                history.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                music.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                load_local();
                current_tab = "local";
                break;
            case R.id.button_history:
                showInterstitial(this);
                home.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                files.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                history.setBackground(ContextCompat.getDrawable(context, R.color.dark_primary));
                music.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                load_history();
                current_tab = "history";
                break;
            case R.id.button_music:
                showInterstitial(this);
                home.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                files.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                history.setBackground(ContextCompat.getDrawable(context, R.color.transparent));
                music.setBackground(ContextCompat.getDrawable(context, R.color.dark_primary));
                load_music();
                current_tab = "music";
                break;
            case R.id.button_menu:
                showInterstitial(this);
                startActivity(new Intent(Home.this, Settings_page.class));
                break;
            case R.id.send_button:
                //Check ip
                showInterstitial(context);
                direct_to_sender = false;
                //If hotspot on, or has 5GHz wifi connected -- or, prompt
                if((!netUtility.isWiFiConnected() && !netUtility.isHotspotEnabled())
                        || (netUtility.isWiFiConnected() &&
                        !netUtility.getWifiFrequency().equals("5"))){
                    //Redirect to sender steps
                    sender_prompt();
                    return;
                }
                sender_prompt = false;
                //Setup activity -- Should go to select files first
                //Check if already a portal open
                goto_sender();
                break;
            case R.id.scan_button:
                scan_network();
                break;
            case R.id.button_user_profile:
                //Profile
                Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.user_profile);
                dialog.setCanceledOnTouchOutside(false);
                Window d_window = dialog.getWindow();
                d_window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                //Info
                TextView user = d_window.findViewById(R.id.user_name),
                        normal_tip = d_window.findViewById(R.id.normal_tip),
                        hotspot_tip = d_window.findViewById(R.id.hotspot_tip),
                        wifi_tip = d_window.findViewById(R.id.wifi_tip),
                        close = d_window.findViewById(R.id.close_button),
                        reset_button = d_window.findViewById(R.id.reset_button),
                        net_type = d_window.findViewById(R.id.net_type),
                        net_name = d_window.findViewById(R.id.user_net);
                ImageView user_image = d_window.findViewById(R.id.user_photo);
                Button change_image = d_window.findViewById(R.id.change_photo),
                        change_name = d_window.findViewById(R.id.change_name);
                Boolean reset_need = false;

                File photo = new File(user_image_path);
                if(photo.exists()){
                    //New thread
                    new Thread(()-> runOnUiThread(()-> Picasso.get().load(photo).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).placeholder(R.drawable.ic_baseline_supervised_user_circle_24).resize(250, 250).centerCrop().into(user_image))).start();
                    change_image.setText(R.string.change_photo);
                    reset_need = true;
                }

                String user_name = dbHandler.get_profile_data("user_name");
                if(user_name == null){
                    user.setText(default_username);
                }
                else {
                    user.setText(user_name);
                    change_name.setText(R.string.change_name);
                    reset_need = true;
                }

                close.setOnClickListener(v13 -> dialog.dismiss());

                change_name.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Enter name");
                        builder.setMessage("Enter your/any full name within 32 characters(Appears when someone discovers your device).");
                        final EditText input = new EditText(context);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);
                        builder.setPositiveButton("Save", (dialog12, which) -> {
                            String m_Text = input.getText().toString();
                            if(m_Text.isEmpty()){
                                change_name.performClick();
                                runOnUiThread(()->Toast.makeText(context, "Please enter name!", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            if(m_Text.length() > 32) {
                                m_Text = m_Text.substring(0, 32);
                            }
                            dbHandler.add_profile_data("user_name", m_Text);
                            String finalM_Text = m_Text;
                            runOnUiThread(()-> {
                                        user.setText(finalM_Text);
                                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
                                    });
                            reset_button.setVisibility(View.VISIBLE);
                        });
                        builder.setNegativeButton("Cancel", (dialog1, which) -> dialog1.cancel());
                        builder.show();
                    }
                });
                if(reset_need) {
                    reset_button.setVisibility(View.VISIBLE);
                }

                reset_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHandler.delete_profile_data("user_name");
                        if(photo.exists()){
                            runOnUiThread(()-> {
                                if (!photo.delete()) {
                                    Toast.makeText(Home.this, "Can't remove old photo!", Toast.LENGTH_SHORT).show();
                                }
                                user_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_supervised_user_circle_24));
                                update_user_pic_view();
                            });
                        }
                        runOnUiThread(()-> {
                            change_name.setText(R.string.set_name);
                            change_image.setText(R.string.set_photo);
                            user.setText(default_username);
                            Toast.makeText(context, "Profile reset", Toast.LENGTH_SHORT).show();
                            reset_button.setVisibility(View.GONE);
                        });
                    }
                });

                //Get wifi data
                if(null != global_class.getIpAddress()){
                    normal_tip.setVisibility(View.GONE);
                    //We have connection
                    WifiInfo wifi = get_wifi_info(this);
                    if(wifi.getBSSID() != null){
                        //Wifi
                        wifi_tip.setVisibility(View.VISIBLE);
                        String ssid = wifi.getSSID().replace("\"", "");
                        if(ssid.equals("<unknown ssid>")){
                            ssid = "Network name is hidden";
                        }
                        net_name.setText(ssid);
                        if(wifi.getFrequency() < 5925) {
                            String n_t = "2.4G"; //Default is 5G+
                            if (wifi.getFrequency() >= 5000 && wifi.getFrequency() < 5925) {
                                n_t = "5G";
                            }
                            net_type.setText(n_t);
                        }
                        net_type.setVisibility(View.VISIBLE);
                    }
                    else {
                        //Hotspot
                        hotspot_tip.setVisibility(View.VISIBLE);
                        net_name.setText(R.string.hotspot_this_device_tap_to_setup_edit);
                        net_name.setOnClickListener(v1 -> {
                            final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                            intent.setComponent(cn);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
                    }
                }

                change_image.setOnClickListener(v12 -> {
                    user_profile_image = user_image;
                    reset_l = reset_button;
                    select_photo();
                });

                dialog.show();
                break;
            default:
                break;
        }
    };

    private void goto_sender() {
        String portal_open_already = dbHandler.get_settings("portal_open");
        if(null != portal_open_already && portal_open_already.equals("true")){
            startActivity(new Intent(Home.this, Send.class));
        }
        else {
            startActivity(new Intent(Home.this, File_selection.class));
        }
    }

    Boolean sender_prompt = false;
    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    private void sender_prompt() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.network_notice);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
        dialog.setCanceledOnTouchOutside(false);
        Window d_window = dialog.getWindow();
        d_window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Button wifi = d_window.findViewById(R.id.btn_wifi), hotspot = d_window.findViewById(R.id.btn_hotspot);
        TextView dis = d_window.findViewById(R.id.description);
        Button use_slow = d_window.findViewById(R.id.use_slow);

        use_slow.setOnClickListener(v-> {
            sender_prompt = false;
            goto_sender();
            dialog.dismiss();
        });

        View.OnClickListener hotspot_go = v -> {
            {
                final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                runOnUiThread(()-> Toast.makeText(context, "Turn on hotspot and ask the receiver to join.", Toast.LENGTH_LONG).show());
                dialog.dismiss();
                startActivity(intent);
            }
        };

        if(netUtility.isWiFiConnected()) {
            dis.setText("You are using a slow Wifi ("+netUtility.getCurrentWiFiName()+"). Please connect to a 5GHz Wifi. Or, setup your own Hotspot with 5GHz AP band.");
            use_slow.setVisibility(View.VISIBLE);
        }
        else {
            dis.setText("For better speed turn on your Hotspot with 5Ghz AP band and connect the receiver.");
            use_slow.setText("Turn on Hotspot");
            use_slow.setOnClickListener(hotspot_go);
        }

        //Buttons
        wifi.setOnClickListener(v -> {
            final Intent intent2 = new Intent(Intent.ACTION_MAIN, null);
            intent2.addCategory(Intent.CATEGORY_LAUNCHER);
            final ComponentName cn2 = new ComponentName("com.android.settings", "com.android.settings.wifi.WifiSettings");
            intent2.setComponent(cn2);
            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            runOnUiThread(()->Toast.makeText(context, "Join the wifi network same as the receiver.", Toast.LENGTH_LONG).show());
            dialog.dismiss();
            startActivity(intent2);
        });
        hotspot.setOnClickListener(hotspot_go);

        sender_prompt = true;
        dialog.show();

    }

    //Home code of sender
    Boolean receive_prompt = false;
    List<String> processed;
    Thread cache_thread;
    @SuppressLint("UseCompatLoadingForDrawables")
    private void scan_network() {
        if (null != cache_thread && cache_thread.isAlive()) {
            cache_thread.interrupt();
        }
        if(!netUtility.isWiFiConnected() && !netUtility.isHotspotEnabled()) {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.network_notice);
            dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
            dialog.setCanceledOnTouchOutside(false);
            Window d_window = dialog.getWindow();
            d_window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Button wifi = d_window.findViewById(R.id.btn_wifi),
                    join_wifi = d_window.findViewById(R.id.use_slow),
                    hotspot = d_window.findViewById(R.id.btn_hotspot);

            TextView title = d_window.findViewById(R.id.title),
                    des = d_window.findViewById(R.id.description);

            title.setText(R.string.connect_wifi_or_turn_on_hotspot);
            des.setText(R.string.connect_to_the_senders_wifi_or_turn_on_your_hotspot);

            View.OnClickListener open_wifi = v -> {
                final Intent intent2 = new Intent(Intent.ACTION_MAIN, null);
                intent2.addCategory(Intent.CATEGORY_LAUNCHER);
                final ComponentName cn2 = new ComponentName("com.android.settings", "com.android.settings.wifi.WifiSettings");
                intent2.setComponent(cn2);
                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Toast.makeText(context, "Join the wifi network same as the sender.", Toast.LENGTH_LONG).show();
                dialog.dismiss();
                startActivity(intent2);
            };

            wifi.setOnClickListener(open_wifi);
            join_wifi.setText(R.string.join_wifi);
            join_wifi.setOnClickListener(open_wifi);

            hotspot.setOnClickListener(v -> {
                final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Toast.makeText(context, "Turn on hotspot and ask the sender to join.", Toast.LENGTH_LONG).show();
                dialog.dismiss();
                startActivity(intent);
            });
            dialog.show();
            receive_prompt = true;
            return;
        }
        receive_prompt = false;
        processed = new ArrayList<>();
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.senders);
        dialog.setCanceledOnTouchOutside(false);
        Window d_window = dialog.getWindow();
        float h = display_height;
        d_window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ScrollView main_scroll = d_window.findViewById(R.id.scrollView2);
        TableLayout main_table = d_window.findViewById(R.id.user_table);
        main_scroll.setMinimumHeight(Math.round(h / 2));
        TextView back = d_window.findViewById(R.id.back_button);
        LinearLayout qr_button = d_window.findViewById(R.id.qr_button);
        LinearLayout sub_loader = d_window.findViewById(R.id.sub_loader);
        LinearLayout loading = d_window.findViewById(R.id.loading),
                empty = d_window.findViewById(R.id.empty);
        TextView load_text = d_window.findViewById(R.id.load_text);
        loading.setMinimumHeight(Math.round(h / 2));
        empty.setMinimumHeight(Math.round(h / 2));
        WifiInfo wifi = get_wifi_info(this);
        String ssid1 = "Network name hidden";
        if (wifi.getBSSID() != null) {
            //Wifi
            String ssid = wifi.getSSID().replace("\"", "");
            if (!ssid.equals("<unknown ssid>")) {
                load_text.setText(String.format("%s%s...", getString(R.string.scanning), ssid));
                ssid1 = ssid;
            }
        }

        String main_ip = global_class.getIpAddress();

        //Scan now
        Thread thread = start_scan(main_ip, loading, empty, ssid1, main_table, sub_loader);
        thread.start();
        cache_thread = thread;

        empty.setOnClickListener(v -> {
            dialog.dismiss();
            findViewById(R.id.scan_button).performClick();
        });
        back.setOnClickListener(v -> {
            dialog.dismiss();
            if (thread.isAlive()) {
                thread.interrupt();
            }
        });
        qr_button.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, Scan.class));
            dialog.dismiss();
        });
        dialog.show();
    }
    private Thread start_scan(String main_ip, LinearLayout loading, LinearLayout empty, String ssid, TableLayout main_table, LinearLayout sub_loader) {

        //IP SET
        String x = main_ip.substring(main_ip.indexOf(".") + 1),
                y = x.substring(x.indexOf(".")),
                main_net_2 = main_ip.replace(y, ""),
                z = main_ip.substring(main_net_2.length()),
                main_net_1 = main_net_2+z.substring(0, z.lastIndexOf("."));

        //Separate thread -- OF RANDOM SCAN
        return new Thread(() -> {
            //Scan from main_net_1, then main_net_2
            int size_k = 0;
            //Known ips
            List<String> knownIps = dbHandler.get_knownIp();
            if(null != knownIps){
                for (String ip:knownIps){
                    if(!ip.equals(main_ip)) {
                        try {

                            ////FLIPPED TO FALSE FOR TEST PURPOSE !!!!!!!
                            //Should be if (InetAddress.getByName(ip).isReach
                            if (!InetAddress.getByName(ip).isReachable(150)) {
                                //Host alive
                                process_host(ip, ssid, main_table, sub_loader);
                            }
                        } catch (IOException e) {
                            //Do nothing
                        }


                    }
                }
                size_k = knownIps.size();
            }

            Handler handler = new Handler(Looper.getMainLooper());

            long delay = 150*(long) size_k;

            handler.postDelayed(()-> new Thread(()-> {
                //1 - from 0 to 225 - Common
                int c = 225;
                for (int i = 1; i <= 255; i++) {
                    String host = main_net_1 + "." + i;
                    if (!host.equals(main_ip)) {
                        try {
                            if (InetAddress.getByName(host).isReachable(30)) {
                                //Host alive
                                process_host(host, ssid, main_table, sub_loader);
                            }
                        } catch (IOException e) {
                            //Do nothing
                        }
                    }
                    //Also from backwards
                    if (c > 0) {
                        String host2 = main_net_1 + "." + c;
                        if (!host.equals(main_ip)) {
                            try {
                                if (InetAddress.getByName(host2).isReachable(30)) {
                                    //Host alive
                                    process_host(host2, ssid, main_table, sub_loader);
                                }
                            } catch (IOException e) {
                                //Do nothing
                            }
                        }
                    }
                    c--;
                }

                //2(Deep) - unlikely happens
                for (int i = 1; i <= 255; i++) {
                    for (int j = 1; j <= 255; j++) {
                        String host = main_net_2 + "." + i + "." + j;
                        if (host.equals(main_ip)) {
                            continue;
                        }
                        try {
                            if (InetAddress.getByName(host).isReachable(30)) {
                                //Host alive
                                process_host(host, ssid, main_table, sub_loader);
                            }
                        } catch (IOException e) {
                            //Do nothing
                        }
                    }
                }
                done_scanning(empty, loading, sub_loader);
            }).start(), delay);
        });

    }

    private void done_scanning(LinearLayout empty, LinearLayout loading, LinearLayout sub_loader) {
        if(processed.size() == 0){
            runOnUiThread(()-> {
                empty.setVisibility(View.VISIBLE);
                loading.setVisibility(View.GONE);
                sub_loader.setVisibility(View.GONE);
            });
        }
    }
    private void process_host(String host, String ssid, TableLayout main_table, LinearLayout sub_loader) {
        if(processed.contains(host)){
            return;
        }
        String a = "http://"+host+":5693"; //Port is set as default
        load_host(a, main_table, ssid, sub_loader, host);
    }

    private void load_host(String a, TableLayout main_table,
                           String ssid, LinearLayout sub_loader, String host) {
        HttpsTrustManager.allowAllSSL();
        URL url;
        try {
            url = new URL(a);
            URLConnection conn;
            conn = url.openConnection();
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader
                    (conn.getInputStream(), StandardCharsets.UTF_8))) {
                int c;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }

            //Run mapping
            String data = textBuilder.toString();
            Gson gson = new Gson();
            ScannedPeer scannedPeer = gson.fromJson(data, ScannedPeer.class);

            View child = inflater.inflate(R.layout.sender_child, null);
            ImageView imageView = child.findViewById(R.id.user_image);
            TextView user = child.findViewById(R.id.user_name), net_name = child.findViewById(R.id.net_info);

            net_name.setText(ssid);
            user.setText(scannedPeer.getUser());

            if(scannedPeer.getPhoto() != null){
                URL url2 = new URL(scannedPeer.getPhoto());
                Bitmap bmp = BitmapFactory.decodeStream(url2.openConnection().getInputStream());
                imageView.setImageBitmap(bmp);
            }

            if (processed.size() == 0) {
                runOnUiThread(() -> {
                    main_table.removeAllViews();
                    sub_loader.setVisibility(View.VISIBLE);
                });
            }

            child.setOnClickListener(v -> {
                String payload = global_class.decrypt(scannedPeer.getPayload(), PAYLOAD_DECODER_KEY);
                if(null != payload){
                    String[] variables = payload.split("-");
                    String main_server = variables[2]+variables[1]+":"+variables[0];
                    String main_pin = variables[3];
                    if(null == main_pin){
                        Toast.makeText(global_class.getContext(), "Can not connect!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startActivity(new Intent(Home.this, Receiver_initiator.class)
                            .putExtra("server", main_server)
                            .putExtra("pin", variables[3]));
                }
            });

            processed.add(host);
            dbHandler.add_knownIp(host); // Save as known
            runOnUiThread(() -> main_table.addView(child));
        } catch (IOException e) {
            //Do nothing
            //Try https
            if(!a.equals("https://"+host+":5693")) {
                load_host("https://" + host + ":5693", main_table, ssid, sub_loader, host);
            }
        }
    }

    private void select_photo() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        activityResultLauncher.launch(i);
    }
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null
                            && data.getData() != null) {
                        //New thread
                        new Thread(()-> {
                            Uri selectedImageUri = data.getData();
                            Bitmap selectedImageBitmap = null;
                            try {
                                selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                                        this.getContentResolver(),
                                        selectedImageUri);
                            } catch (IOException e) {
                                runOnUiThread(()->Toast.makeText(context, "Something went wrong, try again!", Toast.LENGTH_SHORT).show());
                            }
                            if (null == selectedImageBitmap || null == user_profile_image) {
                                runOnUiThread(()->Toast.makeText(context, "Something went wrong, try again!", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            selectedImageBitmap = getCroppedBitmap(selectedImageBitmap);
                            Bitmap finalSelectedImageBitmap = selectedImageBitmap;
                            runOnUiThread(()-> user_profile_image.setImageBitmap(finalSelectedImageBitmap));
                            //Save it
                            try (FileOutputStream out = new FileOutputStream(user_image_path)) {
                                Bitmap compressedBitmap = global_class.makeDP(selectedImageBitmap, 320);
                                compressedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            } catch (IOException e) {
                                runOnUiThread(()->Toast.makeText(context, "Failed to save! Try again", Toast.LENGTH_SHORT).show());
                            }
                            runOnUiThread(()->{
                                Toast.makeText(context, "Photo changed", Toast.LENGTH_SHORT).show();
                                reset_l.setVisibility(View.VISIBLE);
                                update_user_pic_view();
                            });
                        }).start();
                    }
                }
            });


    //Tabs -- HOME
    private void load_music() {
        title.setText(R.string.by_moonslab_music);
        @SuppressLint("InflateParams") View music_view = inflater.inflate(R.layout.home_music_tabs, null);
        ViewPager2 tabs_view = music_view.findViewById(R.id.music_tabs);
        adapter music_adapter = new adapter(this);
        music_adapter.set_Context(context);
        music_adapter.set_dimen(display_height, display_width);
        tabs_view.setAdapter(music_adapter);
        TextView lib_button = music_view.findViewById(R.id.music_lib),
                fav_button = music_view.findViewById(R.id.music_fav);

        //Search
        ScrollView search_scroll = music_view.findViewById(R.id.search_scroll);
        TableLayout search_table = music_view.findViewById(R.id.search_table);

        //Empty
        @SuppressLint("InflateParams") View no_result = inflater.inflate(R.layout.no_files, null);
        TextView text = no_result.findViewById(R.id.text), icon = no_result.findViewById(R.id.icon);
        text.setText(R.string.no_result);
        icon.setText("\\\uf002");

        //Search panel
        LinearLayout main_panel = music_view.findViewById(R.id.main_panel),
                search_panel = music_view.findViewById(R.id.search_panel);
        TextView search_button = music_view.findViewById(R.id.search_open),
                search_close = music_view.findViewById(R.id.search_close);
        EditText search_input = music_view.findViewById(R.id.search_input);

        search_button.setOnClickListener(v->{
            if(null != search_thread && !search_thread.isInterrupted()){
                search_thread.interrupt();
            }
            main_panel.setVisibility(View.GONE);
            search_panel.setVisibility(View.VISIBLE);
            search_input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(search_input, InputMethodManager.SHOW_IMPLICIT);
            //Ready the table - hide tab
            tabs_view.setVisibility(View.GONE);
            search_scroll.setVisibility(View.VISIBLE);
            search_table.removeAllViews();
            search_table.addView(no_result);
        });

        final TextWatcher search_watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @SuppressLint("SetTextI18n")
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(query_busy){
                    return;
                }
                String query = s.toString();
                //Main searcher
                @SuppressLint("InflateParams") View empty = inflater.inflate(R.layout.no_files, null);
                TextView text = empty.findViewById(R.id.text), icon = empty.findViewById(R.id.icon);
                text.setText("Searching...");
                icon.setText("\\\uf002");
                search_table.removeAllViews();
                search_table.addView(empty);
                if(null != search_thread && !search_thread.isInterrupted()){
                    search_thread.interrupt();
                }
                search_thread = new Thread(()-> {
                    List<String> s_list = dbHandler.search_music_all(query);
                    if (s_list.size() > 0) {
                        runOnUiThread(search_table::removeAllViews);
                        for (String path : s_list) {
                            File target_file = new File(path);
                            if (target_file.exists()) {
                                @SuppressLint("InflateParams") View child = inflater.inflate(R.layout.audio_library_child, null);
                                TextView name = child.findViewById(R.id.file_name);
                                TextView info = child.findViewById(R.id.file_info);
                                ImageView image = child.findViewById(R.id.file_image);
                                RelativeLayout is_playing_tag = child.findViewById(R.id.playing_tag),
                                        fav_tag = child.findViewById(R.id.fav_tag);
                                //Check last playing path
                                String last_path = dbHandler.get_settings("last_music_path");
                                if (last_path != null && last_path.equals(target_file.getPath())) {
                                    is_playing_tag.setVisibility(View.VISIBLE);
                                    last_player_tag = is_playing_tag;
                                }
                                Boolean fav_check = dbHandler.fav_exists(target_file.getPath());
                                if (fav_check) {
                                    fav_tag.setVisibility(View.VISIBLE);
                                }
                                //Try getting the cover
                                try {
                                    android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                                    mmr.setDataSource(target_file.getPath());
                                    byte[] data = mmr.getEmbeddedPicture();
                                    if (data != null) {
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                        Drawable m = new BitmapDrawable(getResources(), bitmap);
                                        image.setImageDrawable(m);
                                        image.setBackground(null);
                                    }
                                }
                                catch (Exception e){
                                    //No action needed
                                }
                                name.setText(target_file.getName());
                                info.setText(folderFromPath(target_file.getPath(), target_file.getName()) + " - " + Home.convertTime(target_file.lastModified()));
                                //Events
                                child.setOnClickListener(v -> {
                                    dbHandler.add_setting("last_music_path", target_file.getPath());
                                    if (last_player_tag != null) {
                                        runOnUiThread(()->last_player_tag.setVisibility(View.GONE));
                                    }
                                    runOnUiThread(()->is_playing_tag.setVisibility(View.VISIBLE));
                                    last_player_tag = is_playing_tag;
                                    Intent i = new Intent(context, Music_player.class);
                                    Bundle b = new Bundle();
                                    b.putString("start_new", "1");
                                    i.putExtras(b);
                                    startActivity(i);
                                });
                                runOnUiThread(()->search_table.addView(child));
                            }
                        }
                    } else {
                        runOnUiThread(search_table::removeAllViews);
                        runOnUiThread(()->search_table.addView(no_result));
                    }
                    query_busy = false;
                });
                query_busy = true;
                search_thread.start();
            }
            public void afterTextChanged(Editable s) {
            }
        };
        search_input.addTextChangedListener(search_watcher);

        search_close.setOnClickListener(v->{
            if(null != search_thread && !search_thread.isInterrupted()){
                search_thread.interrupt();
            }
            main_panel.setVisibility(View.VISIBLE);
            search_panel.setVisibility(View.GONE);
            search_input.setText("");
            hide_keyboard(search_input, context);
            //Get old view back, all normal
            search_scroll.setVisibility(View.GONE);
            tabs_view.setVisibility(View.VISIBLE);
            search_table.removeAllViews();
        });

        //Music card
        LinearLayout music_card = music_view.findViewById(R.id.music_card);
        LinearLayout music_shuffle = music_view.findViewById(R.id.music_shuffle);
        TextView music_name = music_view.findViewById(R.id.track_name),
                album_name = music_view.findViewById(R.id.album_name);
        ImageView album_art = music_view.findViewById(R.id.album_art);

        String last_played = dbHandler.get_settings("last_music_path");
        if(null != last_played) {
            File last_file = new File(last_played);
            music_name.setText(last_file.getName());
            music_name.setSelected(true);
            album_name.setText(folderFromPath(last_file.getPath(), last_file.getName()));
            if(last_file.exists()){
                try {
                    android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(last_played);
                    byte[] data = mmr.getEmbeddedPicture();
                    if (data != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        album_art.setImageBitmap(bitmap);
                    }
                }
                catch (Exception e){
                    //Do nothing
                }
            }
            music_card.setOnClickListener(v-> startActivity(new Intent(this, Music_player.class)));
        }
        else {
            music_card.setVisibility(View.GONE);
            music_shuffle.setVisibility(View.VISIBLE);
            music_shuffle.setOnClickListener(v->{
                String random_path = dbHandler.get_music_path_random("none");
                if(random_path == null){
                    Toast.makeText(this, "Music library not ready yet!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent i = get_music_intent(dbHandler, random_path, this);
                    startActivity(i);
                }
            });
        }

        //On tab change
        tabs_view.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if(position < 1){
                    //Library
                    lib_button.setTextColor(ContextCompat.getColor(context, R.color.primary));
                    fav_button.setTextColor(ContextCompat.getColor(context, R.color.grey));
                }
                else {
                    //Favourite
                    fav_button.setTextColor(ContextCompat.getColor(context, R.color.primary));
                    lib_button.setTextColor(ContextCompat.getColor(context, R.color.grey));
                }
            }
        });
        lib_button.setOnClickListener(v-> tabs_view.setCurrentItem(0));
        fav_button.setOnClickListener(v-> tabs_view.setCurrentItem(1));
        replace_view(home_frame, music_view);
    }

    private boolean history_all_files_busy = false, send_again_busy = false;
    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    private void load_history() {
        title.setText("by MoonsLab · History");
        loading_state.setVisibility(View.VISIBLE);
        View history_view = inflater.inflate(R.layout.scrool_view_container,null);
        TableLayout history_table = history_view.findViewById(R.id.main_table);
        ScrollView history_scroll = history_view.findViewById(R.id.main_scroll);

        history_scroll.setBackgroundColor(ContextCompat.getColor(context, R.color.app_back));
        replace_view(home_frame, history_view);
        new Thread(()-> {
            //Get the history files
            List<String[]> list = dbHandler.get_files_history();
            if (list.size() > 0) {
                //Home table ready to get input now

                HashMap<String, List<String[]>> portals = new HashMap<>();
                for (String[] data_set : list) {
                    if (portals.containsKey(data_set[0])) {
                        //Already exists
                        List<String[]> old = portals.get(data_set[0]);
                        portals.remove(data_set[0]);
                        if(null != old) {
                            old.add(data_set);
                        }
                        portals.put(data_set[0], old);
                    } else {
                        List<String[]> new_set = new ArrayList<>();
                        new_set.add(data_set);
                        portals.put(data_set[0], new_set);
                    }
                }

                total_child_history = portals.size();
                //Process data
                runOnUiThread(history_table::removeAllViews);
                int flag = 0;
                for (int x = 0; x < portals.size(); x++) {
                    String[] keys = portals.keySet().toArray(new String[0]);
                    List<String[]> file_list = portals.get(keys[x]);
                    if (null != file_list && file_list.size() == 1) {
                        //portal_id, path, size, time, method, info, column id
                        String[] data_set = file_list.get(0);
                        if (data_set.length < 7) {
                            continue;
                        }
                        //Single child
                        @SuppressLint("InflateParams") View child = inflater.inflate(R.layout.history_child, null);
                        TextView file_name = child.findViewById(R.id.file_name);
                        TextView file_path = child.findViewById(R.id.file_path);
                        TextView file_info = child.findViewById(R.id.file_info); //format: Dec 12, 2022 - 0B - received by Android
                        TextView delete = child.findViewById(R.id.delete);
                        ImageView user = child.findViewById(R.id.user_icon);
                        TextView user_name = child.findViewById(R.id.receiver_name);
                        LinearLayout file_body = child.findViewById(R.id.ll2);
                        ImageView file_image = child.findViewById(R.id.file_image);

                        TextView send_again = child.findViewById(R.id.send_again);
                        send_again.setOnClickListener(v -> send_again_history(file_list));

                        if(data_set[4].equals("iOs/PC")){
                            user.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pc_receiver));
                            user_name.setText("iOs/PC receiver");
                        }

                        delete.setOnClickListener(v->{
                            dbHandler.remove_history_file(Integer.parseInt(data_set[6]));
                            runOnUiThread(()->child.setVisibility(View.GONE));
                            total_child_history--;
                            if(total_child_history <= 0){
                                //Empty!!
                                @SuppressLint("InflateParams") View empty = inflater.inflate(R.layout.history_empty, null);
                                runOnUiThread(()-> replace_view(home_frame, empty));
                            }
                        });

                        File target_file = new File(data_set[1]);

                        boolean is_app = false;
                        String name = target_file.getName(), path = target_file.getPath();
                        if(!target_file.exists()){
                            path = path+"(Deleted)";
                        }

                        //Check if apk of user
                        String file_type = file_type(target_file.getName());
                        if (file_type.equals("app")) {
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_android_24));
                            if(target_file.getPath().contains("/Sharlet/.Transfer_data")){
                                //Users own app
                                path = "Installed apps";
                                is_app = true;
                            }
                        }

                        if(file_type.equals("photo")){
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_photo_24));
                        }

                        if(file_type.equals("video")){
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_video_file_24));
                        }

                        if(file_type.equals("document")){
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.book_file));
                        }

                        if(file_type.equals("audio")){
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_audio_file_24));
                        }

                        file_path.setText(path);
                        file_name.setText(name);
                        if (target_file.exists()) {
                            file_body.setOnClickListener(v -> {
                                Toast.makeText(context, "Opening...", Toast.LENGTH_SHORT).show();
                                openFile(context, target_file);
                            });
                        } else {
                            if(is_app){
                                //App will always be removed
                                file_body.setOnClickListener(v-> new Thread(()->{
                                    runOnUiThread(()->Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show());
                                    @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> packages = pm.getInstalledApplications(0);
                                    if(null == packages || packages.size() == 0){
                                        runOnUiThread(()->Toast.makeText(context, "Can't open app from here!", Toast.LENGTH_LONG).show());
                                        return;
                                    }
                                    boolean flag2 = true;
                                    for (ApplicationInfo packageInfo : packages) {
                                        String temp = pm.getApplicationLabel(packageInfo)+".apk";
                                        if(temp.equals(name)){
                                            AlertDialog.Builder alert = new AlertDialog.Builder(context);
                                            alert.setTitle("Sure open app?");
                                            alert.setMessage("Do you want to open "+pm.getApplicationLabel(packageInfo)+" now?");
                                            alert.setPositiveButton("Open", (dialog, whichButton) -> startActivity(pm.getLaunchIntentForPackage(packageInfo.packageName)));
                                            alert.setNegativeButton("Close",
                                                    (dialog, whichButton) -> {
                                                        //Do nothing
                                                    });
                                            runOnUiThread(alert::show);
                                            flag2 = false;
                                            break;
                                        }
                                    }
                                    if(flag2) {
                                        runOnUiThread(() -> Toast.makeText(context, "App is not installed anymore!", Toast.LENGTH_SHORT).show());
                                    }
                                }).start());
                            }
                            else {
                                file_name.setTextColor(ContextCompat.getColor(context, R.color.primary));
                                file_body.setOnClickListener(v -> Toast.makeText(context, "File doesn't exists!", Toast.LENGTH_SHORT).show());
                            }
                        }
                        //Info
                        long ts = Long.parseLong(data_set[3]);
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy' 'hh:mmaa");
                        String info = sdf.format(ts) + " - " + Receive.format_size(Long.parseLong(data_set[2])) + " - received by " + data_set[4];
                        file_info.setText(info);

                        float d = portals.size();
                        if(d == 0){
                            d = 1;
                        }
                        float p = ((x+1)/d)*100;
                        int finalFlag = flag;
                        runOnUiThread(()-> {
                            if(finalFlag == 0){
                                View break_20dp = inflater.inflate(R.layout.break_20dp, null);
                                history_table.addView(break_20dp);
                            }
                            history_table.addView(child);
                            loading_state.setProgress(Math.round(p));
                        });
                    }
                    //Multiple file
                    else {
                        if(null == file_list){
                            continue;
                        }
                        View child_multi = inflater.inflate(R.layout.history_child_multiple, null);
                        TextView file_info = child_multi.findViewById(R.id.file_info); //format: Dec 12, 2022 - 0B - received by Android
                        TextView delete = child_multi.findViewById(R.id.delete);
                        ImageView user = child_multi.findViewById(R.id.user_icon);
                        TextView user_name = child_multi.findViewById(R.id.receiver_name),
                                see_all = child_multi.findViewById(R.id.see_all);

                        TextView send_again = child_multi.findViewById(R.id.send_again);
                        send_again.setOnClickListener(v -> send_again_history(file_list));


                        see_all.setOnClickListener(v-> {
                            if(history_all_files_busy){
                                runOnUiThread(() -> Toast.makeText(context, "Loading...please wait", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            history_all_files_busy = true;
                            new Thread(()-> {
                                View dialog_content = inflater.inflate(R.layout.history_see_all, null);
                                TableLayout file_table = dialog_content.findViewById(R.id.files_table);
                                if(file_list.size() > 50) {
                                    runOnUiThread(() -> Toast.makeText(context, "Loading...please wait", Toast.LENGTH_LONG).show());
                                }
                                //This code is minimized
                                AtomicBoolean flag2 = new AtomicBoolean(false);
                                for (String[] data_set : file_list) {
                                    View child = View.inflate(context, R.layout.sender_file_child, null);
                                    TextView file_name = child.findViewById(R.id.file_name);
                                    TextView file_path = child.findViewById(R.id.file_path);
                                    ImageView file_image = child.findViewById(R.id.file_image);
                                    LinearLayout file_body = child.findViewById(R.id.ll2);
                                    TextView file_state = child.findViewById(R.id.file_sate);
                                    file_state.setText("\\\uf058");

                                    File target_file = new File(data_set[1]);
                                    boolean is_app = false;
                                    String name = target_file.getName(), path = target_file.getPath();
                                    if (!target_file.exists()) {
                                        path = path + "(Deleted)";
                                    }
                                    //Check if apk of user
                                    String file_type = file_type(target_file.getName());
                                    if (file_type.equals("app")) {
                                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_android_24));
                                        if (target_file.getPath().contains("/Sharlet/.Transfer_data")) {
                                            //Users own app
                                            path = "Installed apps";
                                            is_app = true;
                                        }
                                    }
                                    if (file_type.equals("photo")) {
                                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_photo_24));
                                    }

                                    if (file_type.equals("video")) {
                                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_video_file_24));
                                    }

                                    if (file_type.equals("document")) {
                                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.book_file));
                                    }

                                    if (file_type.equals("audio")) {
                                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_audio_file_24));
                                    }

                                    file_path.setText(path);
                                    file_name.setText(name);

                                    if (target_file.exists()) {
                                        file_body.setOnClickListener(v2 -> {
                                            Toast.makeText(context, "Opening...", Toast.LENGTH_SHORT).show();
                                            openFile(context, target_file);
                                        });
                                    } else {
                                        if (is_app) {
                                            //App will always be removed
                                            file_body.setOnClickListener(v2 -> {
                                                new Thread(() -> {
                                                    runOnUiThread(() -> Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show());
                                                    @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> packages = pm.getInstalledApplications(0);
                                                    if (null == packages || packages.size() == 0) {
                                                        runOnUiThread(() -> Toast.makeText(context, "Can't open app from here!", Toast.LENGTH_LONG).show());
                                                        return;
                                                    }
                                                    boolean flag22 = true;
                                                    for (ApplicationInfo packageInfo : packages) {
                                                        String temp = pm.getApplicationLabel(packageInfo) + ".apk";
                                                        if (temp.equals(name)) {
                                                            AlertDialog.Builder alert = new AlertDialog.Builder(context);
                                                            alert.setTitle("Sure open app?");
                                                            alert.setMessage("Do you want to open " + pm.getApplicationLabel(packageInfo) + " now?");
                                                            alert.setPositiveButton("Open", (dialog2, whichButton) -> startActivity(pm.getLaunchIntentForPackage(packageInfo.packageName)));
                                                            alert.setNegativeButton("Close",
                                                                    (dialog2, whichButton) -> {
                                                                        //Do nothing
                                                                    });
                                                            runOnUiThread(alert::show);
                                                            flag22 = false;
                                                            break;
                                                        }
                                                    }
                                                    if (flag22) {
                                                        runOnUiThread(() -> Toast.makeText(context, "App is not installed anymore!", Toast.LENGTH_SHORT).show());
                                                    }
                                                }).start();
                                            });
                                        } else {
                                            file_name.setTextColor(ContextCompat.getColor(context, R.color.primary));
                                            file_body.setOnClickListener(v3 -> Toast.makeText(context, "File doesn't exists!", Toast.LENGTH_SHORT).show());
                                        }
                                    }
                                    if (!flag2.get()) {

                                        file_table.removeAllViews();
                                        flag2.set(true);

                                    }
                                    file_table.addView(child);
                                }
                                runOnUiThread(()->{
                                    Dialog dialog = new Dialog(context);
                                    dialog.setContentView(dialog_content);
                                    dialog.setCanceledOnTouchOutside(true);
                                    Window d_window = dialog.getWindow();
                                    d_window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    dialog.show();
                                });
                                history_all_files_busy = false;
                            }).start();
                        });

                        delete.setOnClickListener(v->{
                            for(String[] data_set : file_list){
                                dbHandler.remove_history_file(Integer.parseInt(data_set[6]));
                            }
                            runOnUiThread(()-> child_multi.setVisibility(View.GONE));
                            total_child_history--;
                            if(total_child_history <= 0){
                                //Empty!!
                                View empty = inflater.inflate(R.layout.history_empty, null);
                                runOnUiThread(()-> replace_view(home_frame, empty));
                            }
                        });

                        //Multi child
                        List<String[]>
                                photos = new ArrayList<>(),
                                videos = new ArrayList<>(),
                                audio = new ArrayList<>(),
                                docs = new ArrayList<>(),
                                apps = new ArrayList<>(),
                                files = new ArrayList<>();
                        long size_photo = 0,
                                size_video = 0,
                                size_audio = 0,
                                size_docs = 0,
                                size_apps = 0,
                                size_files = 0;
                        long size = 0;
                        String info = "Unknown";
                        String method = "Unknown";
                        long ts = 0;
                        for (String[] data_set : file_list) {
                            //portal_id, path, size, time, method, info
                            size += Long.parseLong(data_set[2]);
                            if (method.equals("Unknown")) {
                                method = data_set[4];
                            }
                            if (info.equals("Unknown")) {
                                info = data_set[5];
                            }
                            long nts = Long.parseLong(data_set[3]);
                            if (nts > ts) {
                                ts = nts;
                            }
                            File target_file = new File(data_set[1]);
                            if (file_type(target_file.getName()).equals("photo")) {
                                photos.add(data_set);
                                size_photo += Long.parseLong(data_set[2]);
                            } else if (file_type(target_file.getName()).equals("video")) {
                                videos.add(data_set);
                                size_video += Long.parseLong(data_set[2]);
                            } else if (file_type(target_file.getName()).equals("audio")) {
                                audio.add(data_set);
                                size_audio += Long.parseLong(data_set[2]);
                            } else if (file_type(target_file.getName()).equals("document")) {
                                docs.add(data_set);
                                size_docs += Long.parseLong(data_set[2]);
                            } else if (file_type(target_file.getName()).equals("app")) {
                                apps.add(data_set);
                                size_apps += Long.parseLong(data_set[2]);
                            } else {
                                files.add(data_set);
                                size_files += Long.parseLong(data_set[2]);
                            }
                        }
                        if (photos.size() > 0) {
                            LinearLayout pv = child_multi.findViewById(R.id.photos);
                            TextView pc = child_multi.findViewById(R.id.photo_count);
                            if (photos.size() > 1) {
                                pc.setText(photos.size() + " files");
                            } else {
                                pc.setText(photos.size() + " file");
                            }
                            TextView s = child_multi.findViewById(R.id.photo_size);
                            s.setText(Receive.format_size(size_photo));
                            pv.setVisibility(View.VISIBLE);
                        }
                        if (videos.size() > 0) {
                            LinearLayout pv = child_multi.findViewById(R.id.videos);
                            TextView pc = child_multi.findViewById(R.id.video_count);
                            if (photos.size() > 1) {
                                pc.setText(videos.size() + " files");
                            } else {
                                pc.setText(videos.size() + " file");
                            }
                            TextView s = child_multi.findViewById(R.id.video_size);
                            s.setText(Receive.format_size(size_video));
                            pv.setVisibility(View.VISIBLE);
                        }
                        if (audio.size() > 0) {
                            LinearLayout pv = child_multi.findViewById(R.id.audio);
                            TextView pc = child_multi.findViewById(R.id.audio_count);
                            if (photos.size() > 1) {
                                pc.setText(audio.size() + " files");
                            } else {
                                pc.setText(audio.size() + " file");
                            }
                            TextView s = child_multi.findViewById(R.id.audio_size);
                            s.setText(Receive.format_size(size_audio));
                            pv.setVisibility(View.VISIBLE);
                        }
                        if (apps.size() > 0) {
                            LinearLayout pv = child_multi.findViewById(R.id.apps);
                            TextView pc = child_multi.findViewById(R.id.apps_count);
                            if (photos.size() > 1) {
                                pc.setText(apps.size() + " files");
                            } else {
                                pc.setText(apps.size() + " file");
                            }
                            TextView s = child_multi.findViewById(R.id.apps_size);
                            s.setText(Receive.format_size(size_apps));
                            pv.setVisibility(View.VISIBLE);
                        }
                        if (docs.size() > 0) {
                            LinearLayout pv = child_multi.findViewById(R.id.docs);
                            TextView pc = child_multi.findViewById(R.id.docs_count);
                            if (photos.size() > 1) {
                                pc.setText(docs.size() + " files");
                            } else {
                                pc.setText(docs.size() + " file");
                            }
                            TextView s = child_multi.findViewById(R.id.doc_size);
                            s.setText(Receive.format_size(size_docs));
                            pv.setVisibility(View.VISIBLE);
                        }
                        if (files.size() > 0) {
                            LinearLayout pv = child_multi.findViewById(R.id.other);
                            TextView pc = child_multi.findViewById(R.id.other_count);
                            if (photos.size() > 1) {
                                pc.setText(files.size() + " files");
                            } else {
                                pc.setText(files.size() + " file");
                            }
                            TextView s = child_multi.findViewById(R.id.other_size);
                            s.setText(Receive.format_size(size_files));
                            pv.setVisibility(View.VISIBLE);
                        }

                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy' 'hh:mmaa");
                        String info2 = sdf.format(ts) + " - " + Receive.format_size(size) + " - received by " + method;
                        file_info.setText(info2);

                        if(method.equals("iOs/PC")){
                            user.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pc_receiver));
                            user_name.setText("iOs/PC receiver");
                        }

                        int finalX = x;
                        int finalFlag1 = flag;
                        runOnUiThread(()-> {
                            float x2 = finalX +1, y = file_list.size(), r = (x2/y)*100;
                            int p = Math.round(r);
                            if(finalFlag1 == 0){
                                View break_20dp = inflater.inflate(R.layout.break_20dp, null);
                                history_table.addView(break_20dp);
                            }
                            history_table.addView(child_multi);
                            loading_state.setProgress(Math.round(p));
                        });

                    }
                    flag++;
                    if (flag == 0) {
                        //Add empty view
                        View empty = inflater.inflate(R.layout.history_empty, null);
                        runOnUiThread(()-> replace_view(home_frame, empty));
                    }
                }
            }
            else {
                View empty = inflater.inflate(R.layout.history_empty, null);
                runOnUiThread(()-> replace_view(home_frame, empty));
            }
            runOnUiThread(()-> {
                loading_state.setVisibility(View.INVISIBLE);
                loading_state.setProgress(0);
            });
        }).start();
    }

    //History helper action
    private void send_again_history(List<String[]> file_list) {
        if(send_again_busy){
            runOnUiThread(() -> Toast.makeText(context, "Loading...please wait", Toast.LENGTH_SHORT).show());
            return;
        }
        send_again_busy = true;
        runOnUiThread(() -> Toast.makeText(context, "Loading...please wait", Toast.LENGTH_SHORT).show());
        //Clean bundle
        clean_bundle_and_data();

        List<File> f_cache = new ArrayList<>();

        for(String[] data_set : file_list){
            File target_file = new File(data_set[1]);
            if(target_file.isFile() && target_file.exists()){
                f_cache.add(target_file);
            }
        }

        if(f_cache.size() == 0){
            runOnUiThread(() -> Toast.makeText(context, "All files are deleted!", Toast.LENGTH_SHORT).show());
            send_again_busy = false;
        }
        else {
            File[] final_selection = new File[f_cache.size()];
            int x1 = 0;
            for(File file : f_cache){
                final_selection[x1] = file;
                x1++;
            }
            runOnUiThread(()->{
                grid_select_all_child(null, final_selection, true, context);
                send_again_busy = false;
                Intent i = new Intent(context, Send.class);
                Bundle b = new Bundle();
                b.putString("from_home", "1");
                i.putExtras(b);
                context.startActivity(i);
            });
        }
    }


    //Local files
    private void load_local() {
        //Permission check
        if(!all_permission_once()){
            Toast.makeText(getBaseContext(), "Storage permission/Restart is required!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            load_home();
            return;
        }

        //Free the selection bucket
        clean_bundle_and_data();

        title.setText(R.string.by_moonslab_files);
        @SuppressLint("InflateParams") View local_view = inflater.inflate(R.layout.tab_files, null);
        ViewPager2 selection_fragment_viewer;
        TabLayout tabs = local_view.findViewById(R.id.files_tabs);
        selection_fragment_viewer = local_view.findViewById(R.id.files_pager);
        Files_tabs tab_class = new Files_tabs(this);
        selection_fragment_viewer.setAdapter(tab_class);
        tab_class.set_Context(context);
        tab_class.set_dimen(display_height, display_width);

        //Send now button
        send_now = local_view.findViewById(R.id.send_now);
        send_now_count = local_view.findViewById(R.id.send_now_file_count);
        LinearLayout send_now_button = local_view.findViewById(R.id.send_now_button);

        send_now_button2 = send_now_button;

        send_now_button.setOnClickListener(v->{
            direct_to_sender = true;
            if(global_class.getIpAddress() == null){
                //Redirect to sender steps
                sender_prompt();
                return;
            }
            sender_prompt = false;
            if(done_selection(this, true)){
                this.finish();
            }
        });

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selection_fragment_viewer.setCurrentItem(tab.getPosition());
                showInterstitial(context);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //DO NOTHING
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //DO NOTHING
            }
        });
        selection_fragment_viewer.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabs.getTabAt(position).select();
            }
        });
        replace_view(home_frame, local_view);
    }

    private void load_home() {
        title.setText(R.string.by_moonslab_home);
        loading_state.setVisibility(View.INVISIBLE);
        loading_state.setProgress(0);
        View home_view = inflater.inflate(R.layout.tab_home, null);
        send_button = home_view.findViewById(R.id.send_button);
        home_view.<TextView>findViewById(R.id.send_button).setOnClickListener(Listener);
        home_view.<TextView>findViewById(R.id.scan_button).setOnClickListener(Listener);

        ScrollView main_scroll = home_view.findViewById(R.id.main_scroll);

        //Card 2
        RelativeLayout already_sending = home_view.findViewById(R.id.already_sending);
        String portal_open_already = dbHandler.get_settings("portal_open");
        if(null != portal_open_already && portal_open_already.equals("true")){
            already_sending.setOnClickListener(v-> {
                String test = dbHandler.get_settings("portal_open");
                if(null != test && test.equals("false")){
                    Toast.makeText(context, "Portal no longer exists", Toast.LENGTH_SHORT).show();
                    already_sending.setVisibility(View.GONE);
                    return;
                }
                startActivity(new Intent(Home.this, Send.class));
            });
            already_sending.setVisibility(View.VISIBLE);
        }


        //CARD 3 - Turbo mode
        TextView main_title = home_view.findViewById(R.id.tm_s),
                active_text = home_view.findViewById(R.id.active_text),
                main_dialogue = home_view.findViewById(R.id.tm_d);

        //Views
        LinearLayout location_needed = home_view.findViewById(R.id.location_needed),
                hotspot_needed = home_view.findViewById(R.id.own_hotspot_needed),
                turbo_active = home_view.findViewById(R.id.turbo_active);

        //Buttons
        Button hotspot_open = home_view.findViewById(R.id.Hotspot_open),
                location_perm = home_view.findViewById(R.id.Location_permission);

        View.OnClickListener open_hotspot = v -> {
            final Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
            intent.setComponent(cn);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        };

        /*
        Logic
        1. Check if Location permission given
        2. Check if hotspot is being used or not
        3. Check if 5GHz band active or not
        4. All true = Active
         */

        dbHandler.add_setting("turbo_active", "false");

        if(location_granted()) {
            //GRANTED -- goto step 2
            location_needed.setVisibility(View.GONE);
            boolean wifi5g = netUtility.isWiFiConnected() && netUtility.getWifiFrequency().equals("5");
            //Check if own hotspot or, wifi has 5GHz
            if(netUtility.isHotspotEnabled() || wifi5g){
                hotspot_needed.setVisibility(View.GONE);
                main_title.setText(R.string.turbo_mode_active);
                main_dialogue.setText(R.string.speed_boosted_up_to_32mb_s);
                if(wifi5g){
                    active_text.setText(R.string.turbo_mode_is_ready);
                }
                turbo_active.setVisibility(View.VISIBLE);
                dbHandler.add_setting("turbo_active", "true");
            }
            else {
                if(!netUtility.isWiFiConnected() && !netUtility.isHotspotEnabled()){
                    TextView tv = home_view.findViewById(R.id.tv_on_h);
                    tv.setText(String.format("%s", getString(R.string.set_up_hotspot_with_5ghz_ap_band_turn_wifi_off_first)));
                }
                //Need hotspot
                hotspot_open.setOnClickListener(open_hotspot);
                hotspot_needed.setVisibility(View.VISIBLE);
            }
        }
        else {
            //SET LOCATION BUTTON
            location_perm.setOnClickListener(v-> {
                //Check if granted
                if(location_granted()){
                    //Reload
                    load_home();
                    return;
                }
                if(!shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                        ! shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    location_perm.setText(R.string.open_settings);
                    Toast.makeText(this, "Allow from settings", Toast.LENGTH_SHORT).show();
                    location_perm.setOnClickListener(v2-> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    });
                    return;
                }
                if(this.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) < 0
                        || this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) < 0) {
                    ActivityCompat.requestPermissions(this, new String[]{
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            0);
                }
                else {
                    //Reload
                    load_home();
                }
            });
        }

        //CART 4, TURBO COUNT
        TextView turbo_count = home_view.findViewById(R.id.token_count),
                turbo_info = home_view.findViewById(R.id.turbo_token_info);
        String token = dbHandler.get_settings("turbo_token");
        int count_token = 0;
        if(null != token) {
            String count = global_class.decrypt(token, global_class.getDeviceEncryptionKey());
            if (null != count) {
                count_token = Integer.parseInt(count);
            }
        }
        if(count_token > 1){
            turbo_count.setText(String.format("%d %s", count_token, getString(R.string.token_multi)));
        }
        else {
            turbo_count.setText(String.format("%d %s", count_token, getString(R.string.token_single)));
        }
        turbo_info.setOnClickListener(v->token_info.show());

        ad_icon = home_view.findViewById(R.id.ad_icon);
        ad_loading = home_view.findViewById(R.id.ad_loading);
        home_view.findViewById(R.id.watch_ad).setOnClickListener(v-> {
            if(!netUtility.isInternetConnected()){
                Toast.makeText(context, "No internet!", Toast.LENGTH_SHORT).show();
                return;
            }
            ad_icon.setVisibility(View.GONE);
            ad_loading.setVisibility(View.VISIBLE);
            Show_token_ad();
        });
        TextView timer = home_view.findViewById(R.id.timer);

        LinearLayout watch_ad = home_view.findViewById(R.id.l3),
                claim_daily = home_view.findViewById(R.id.l4);

        long startTime = System.currentTimeMillis();
        String turbo_last = dbHandler.get_settings("last_turbo_daily");
        if(null != turbo_last){
            turbo_last = global_class.decrypt(turbo_last, global_class.getDeviceEncryptionKey());
            if(null != turbo_last){
                startTime = Long.parseLong(turbo_last);
            }
        }
        watch_ad.setVisibility(View.VISIBLE);
        timer.setVisibility(View.VISIBLE);
        claim_daily.setVisibility(View.GONE);
        Handler handler = new Handler();
        long finalStartTime = startTime;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long remainingTime = finalStartTime - System.currentTimeMillis();
                if (update_daily_token){
                    String turbo_last = dbHandler.get_settings("last_turbo_daily");
                    if(null != turbo_last){
                        turbo_last = global_class.decrypt(turbo_last, global_class.getDeviceEncryptionKey());
                        if(null != turbo_last){
                            remainingTime = Long.parseLong(turbo_last) - System.currentTimeMillis();
                        }
                    }
                    update_daily_token = false;
                }
                if (remainingTime <= 0) {
                    timer.setVisibility(View.GONE);
                    watch_ad.setVisibility(View.GONE);
                    //Add token logic
                    LinearLayout reward_get = home_view.findViewById(R.id.reward_get);
                    reward_get.setOnClickListener(v-> {
                        //Save current time
                        dbHandler.add_setting("last_turbo_daily", global_class.encrypt(String.valueOf(System.currentTimeMillis()+24*60*60*1000), global_class.getDeviceEncryptionKey()));
                        reward_get.setOnClickListener(null);
                        add_token(1);
                    });
                    claim_daily.setVisibility(View.VISIBLE);
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    timer.setText(String.format("Daily reward: %s", dateFormat.format(new Date(remainingTime))));
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);

        replace_view(home_frame, home_view);

        //Ads -- only in home view
        refreshAd(home_view);
    }

    private void replace_view(FrameLayout main_frame, View view){
        main_frame.removeAllViews();
        main_frame.addView(view);
    }

    //Ads
    private void refreshAd(View main_view){
        AdLoader.Builder builder = new AdLoader.Builder(this, ADMOB_AD_UNIT_ID);
        // OnLoadedListener implementation.
        builder.forNativeAd(
                nativeAd -> {
                    // If this callback occurs after the activity is destroyed, you must call
                    // destroy and return or you may get a memory leak.
                    boolean isDestroyed;
                    isDestroyed = isDestroyed();
                    if (isDestroyed || isFinishing() || isChangingConfigurations()) {
                        nativeAd.destroy();
                        return;
                    }
                    // You must call destroy on old ads when you are done with them,
                    // otherwise you will have a memory leak.
                    if (Home.this.nativeAd != null) {
                        Home.this.nativeAd.destroy();
                    }
                    Home.this.nativeAd = nativeAd;
                    FrameLayout frameLayout = main_view.findViewById(R.id.ad_frame_1);
                    NativeAdView adView =
                            (NativeAdView) getLayoutInflater().inflate(R.layout.ad_native_home, frameLayout, false);
                    populateNativeAdView(nativeAd, adView);
                    frameLayout.removeAllViews();
                    frameLayout.addView(adView);
                });

        //Muted
        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions =
                new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader =
                builder
                        .withAdListener(
                                new AdListener() {
                                    @Override
                                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                                        //Do nothing for now
                                    }
                                })
                        .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }
    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view.
        adView.setMediaView(adView.findViewById(R.id.ad_media));

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        VideoController vc = nativeAd.getMediaContent().getVideoController();

        // Updates the UI to say whether or not this ad has a video asset.
        if (nativeAd.getMediaContent() != null && nativeAd.getMediaContent().hasVideoContent()) {

            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    // Publishers should allow native ads to complete video playback before
                    // refreshing or replacing them with another ad in the same UI location.
                    super.onVideoEnd();
                }
            });
        }
    }

    //Read the ip of wifi, not internet - self

    //Night mode detection
    public boolean is_night(){
        boolean r = false;
        int nightModeFlags =
                this.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                r = true;
                break;

            case Configuration.UI_MODE_NIGHT_NO:

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                r = false;
                break;
        }
        return r;
    }
    //Return false on hold or no permission
    @SuppressLint("SuspiciousIndentation")
    public boolean all_permission_once(){
        //Checks
        //Files permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()){
                //ASK PERMISSION
                Intent permission_intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,  Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                startActivity(permission_intent);
                Toast.makeText(getBaseContext(), "Please turn on files permission.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        //READ - WRITE PERMISSION
        if(this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) < 0
                || this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) < 0
                || this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) < 0
                || this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) < 0)
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) < 0) {
                ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.QUERY_ALL_PACKAGES
                        },
                        1);
            }
        }
        return true;
    }

    public static void copy_file(File src, File dst, Context context) {
        class MyThread extends Thread {
            public void run() {
                try {
                    try (InputStream in = Files.newInputStream(src.toPath())) {
                        try (OutputStream out = Files.newOutputStream(dst.toPath())) {
                            // Transfer bytes from in to out
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        }
                    }
                }
                catch (Exception e){
                    new Thread(() -> Toast.makeText(context, "Copy operation failed!", Toast.LENGTH_SHORT).show());
                }
            }
        };
        new MyThread().start();
    }
    public static boolean create_app_folders(){
        boolean ok = false;
        if(make_folder("Sharlet")){
            if(make_folder("Sharlet/Photos")){
                if(make_folder("Sharlet/Videos")){
                    if(make_folder("Sharlet/Audio")){
                        if(make_folder("Sharlet/Documents")) {
                            if (make_folder("Sharlet/Apps")) {
                                if (make_folder("Sharlet/Files")) {
                                    if (make_folder("Sharlet/.Transfer_data")) {
                                        ok = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return ok;
    }

    private static boolean make_folder(String folder_main){
        boolean ok = true;
        try {
            File f = new File(Environment.getExternalStorageDirectory(), folder_main);
            if (!f.exists()) {
                f.mkdirs();
            }
        }
        catch (Exception e){
            ok = false;
        }
        return  ok;
    }
    public static WifiInfo get_wifi_info(Context context){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.getConnectionInfo();
    }

    public static String get_app_home_directory(){
        return Environment.getExternalStorageDirectory().toString()+"/Sharlet";
    }
    public static String get_app_home_bundle_data_store(){
        return Environment.getExternalStorageDirectory().toString()+"/Sharlet/.Transfer_data";
    }
    public static String get_storage_root(){
        return Environment.getExternalStorageDirectory().toString();
    }
    public static String get_appdata_location_root(Context context){
        return context.getFilesDir().getAbsolutePath();
    }
    public static void clean_bundle_and_data(){
        String dir_1 = ".Transfer_data";
        dir_clean(dir_1);
    }

    public static Integer create_notification(Context context, String title, String body, int id, int priority, Boolean ongoing){
        if(id == 0){
            id = Integer.parseInt(Sender.getRandomPIN(4)); // Random
        }
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_DEFAULT)
                .setSmallIcon(R.drawable.logo_main)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(priority)
                .setContentIntent(null)
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                .build();
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
        return id;
    }

    public static void vibrate(long ms, Context context){
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    }
    public static void cancel_notification(int id, Context context){
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) context.getSystemService(ns);
        nMgr.cancel(id);
    }
    public static void setup_http_plugin(Context context){
        String main_path = get_appdata_location_root(context)+"/http";
        File test = new File(main_path);
        if(false && test.isDirectory() && test.length() > 0){
            return;
        }
        new Thread(()-> {
            //DON'T CHANGE THIS PASSWORD
            String zip_password = "p9gd72bb-c3q73bc7q3b3fg7bfq9nec023@#$%%^&fwp8b";
            String plugin_temp = get_appdata_location_root(context) + "/http_plugin.zip";
            try {
                byte[] buff = new byte[1024];
                int read;
                try (InputStream in = context.getResources().openRawResource(R.raw.http_plugin); FileOutputStream out = new FileOutputStream(plugin_temp)) {
                    while ((read = in.read(buff)) > 0) {
                        out.write(buff, 0, read);
                    }
                }
                new ZipFile(plugin_temp, zip_password.toCharArray()).extractAll(main_path);
                store_as_file("http_plugin.txt", "available", context);
                File temp = new File(plugin_temp);
                if(temp.exists()){
                    temp.delete();
                }
            } catch (Exception e) {
                store_as_file("http_plugin.txt", "unavailable", context);
                File temp = new File(plugin_temp);
                if(temp.exists()){
                    temp.delete();
                }
            }
        }).start();
    }
    public static Boolean is_http_ready(Context context){
        String main_path = get_appdata_location_root(context)+"/http";
        File test = new File(main_path);
        if(test.isDirectory() && test.length() > 0){
            String test2 = read_from_file("http_plugin.txt", context);
            return test2 != null && test2.equals("available");
        }
        return false;
    }
    public static void store_as_file(String file_name, String data , Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file_name, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            //Write error,
            //DO NOTHING
            Toast.makeText(context, "Error loading file!", Toast.LENGTH_SHORT).show();
        }
    }
    public static String read_from_file(String file_name, Context context) {
        try {
            String location = context.getFilesDir().getAbsolutePath()+"/"+file_name;
            return FileUtils.readFileToString(new File(location), StandardCharsets.UTF_8);
        }
        catch (IOException e){
            return null;
        }
    }

    private static void dir_clean(String directory){
        //for safety, dir cleaner is set to home directory only
        File dir = new File(Environment.getExternalStorageDirectory()+"/Sharlet/"+directory);
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            if(null != children) {
                for (int i = 0; i < children.length; i++) {
                    new File(dir, children[i]).delete();
                }
            }
        }
    }
    public static Bitmap make_qr_code(String data, int dimension, int background_color, int main_color){
        QRGEncoder qrgEncoder = new QRGEncoder(data, null, QRGContents.Type.TEXT, dimension);
        qrgEncoder.setColorBlack(background_color);
        qrgEncoder.setColorWhite(main_color);
        // Getting QR-Code as Bitmap
        // Setting Bitmap to ImageView
        return qrgEncoder.getBitmap();
    }

    public static int convertDpToPixels(float dp, Context context){
        Resources resources = context.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                resources.getDisplayMetrics()
        );
    }

    public static void openFile(Context context, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(context.getApplicationContext()), BuildConfig.APPLICATION_ID + ".provider", file);
            String mime = context.getContentResolver().getType(uri);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        catch (Exception e){
            Toast.makeText(context, "Can't open that file!", Toast.LENGTH_SHORT).show();
        }
    }

    public static int getDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    public static Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
    public static String file_type(String file_name){
        String type = "file";
        String File_extension = FilenameUtils.getExtension(file_name).toLowerCase(Locale.ROOT);
        switch (File_extension) {
            case "png":
            case "jpg":
            case "gif":
            case "jpeg":
            case "heic":
            case "webp":
            case "tiff":
            case "raw":
                type = "photo";
                break;
            case "mp3":
            case "wav":
            case "ogg":
            case "m4a":
            case "aac":
            case "alac":
            case "aiff":
                type = "audio";
                break;
            case "mp4":
            case "mkv":
            case "flv":
            case "avi":
            case "webm":
            case "mov":
                type = "video";
                break;
            case "pdf":
            case "docx":
            case "doc":
            case "html":
            case "htm":
            case "xml":
            case "svg":
            case "txt":
            case "xls":
            case "xlsx":
                type = "document";
                break;
            case "apk":
                type = "app";
                break;
        }
        return type;
    }
    //Ad2
    private static void loadInterstitialAd(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, AD_UNIT_ID2, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Home.interstitialAd = interstitialAd;
                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            //Methods...
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when fullscreen content is dismissed.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                                Home.interstitialAd = null;
                                loadInterstitialAd(context);
                            }
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when fullscreen content failed to show.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                                Home.interstitialAd = null;
                                loadInterstitialAd(context);
                            }
                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                loadInterstitialAd(context);
                            }
                        });
                    }
                });
    }
    public static void showInterstitial(Context context) {
        //show ad per 10 clicks
        if(clicks < 9){
            clicks++;
            return;
        }
        Activity activity = (Activity) context;
        if (Home.interstitialAd != null) {
            Home.interstitialAd.show(activity);
            clicks = 0;
        }
        else {
            loadInterstitialAd(context);
        }
    }
    public static String convertTime(long time){
        Date date = new Date(time);
        @SuppressLint("SimpleDateFormat") Format format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return format.format(date);
    }
    public static long get_timestamp(){
        return System.currentTimeMillis()/1000;
    }
    public static Intent get_music_intent(DBHandler dbHandler, String path, Context context){
        dbHandler.add_setting("last_music_path", path);
        Intent i = new Intent(context, Music_player.class);
        Bundle b = new Bundle();
        b.putString("start_new", "1");
        i.putExtras(b);
        return i;
    }
    public static void hide_keyboard(View edit_text_view, Context context){
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_text_view.getWindowToken(), 0);
    }

    public static boolean done_selection(Context context, Boolean from_home){
        //Turbo check
        Global global = new Global(context);
        global.isTurboReady();
        //Get bucket count
        //Read the bucket
        String location = Home.get_app_home_bundle_data_store()+"/Selection_bucket.txt";
        File main_file = new File(location);
        if(!main_file.exists()){
            Toast.makeText(context, "Select at least one file!", Toast.LENGTH_SHORT).show();
        }
        else {
            List<String> bucket_list = new ArrayList<>();
            try {
                BufferedReader reader  = new BufferedReader(new FileReader(main_file));
                String line = reader.readLine();
                while (line != null) {
                    bucket_list.add(line);
                    //read next line
                    line = reader.readLine();
                }
                reader.close();
            }
            catch (IOException e){
                bucket_list = null;
            }
            if(null != bucket_list && bucket_list.size() > 0) {
                if(!from_home) {
                    context.startActivity(new Intent(context, Send.class));
                }
                else {
                    Intent i = new Intent(context, Send.class);
                    Bundle b = new Bundle();
                    b.putString("from_home", "1");
                    i.putExtras(b);
                    context.startActivity(i);
                }
                return true;
            }
            else {
                Toast.makeText(context, "Select at least one file!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    public static void selection_update_main(){
        //Get bucket count
        //Read the bucket

        TextView fs_sc = File_selection.select_count;

        List<String> bucket_list = new ArrayList<>();
        String location = Home.get_app_home_bundle_data_store()+"/Selection_bucket.txt";
        File main_file = new File(location);
        if(!main_file.exists()){
            if(null != fs_sc) {
                fs_sc.setText("Select files");
            }
            if(null != send_now){
                send_now.setVisibility(View.GONE);
            }
            if(null != send_now_count){
                send_now_count.setText("Send now");
            }
            return;
        }
        try {
            BufferedReader reader  = new BufferedReader(new FileReader(main_file));
            String line = reader.readLine();
            while (line != null) {
                bucket_list.add(line);
                // read next line
                line = reader.readLine();
            }
            reader.close();
        }
        catch (IOException e){
            bucket_list = null;
        }
        if(bucket_list != null){
            if(bucket_list.size() > 0) {
                if(null != fs_sc) {
                    File_selection.select_count.setText("Select files (" + bucket_list.size() + ")");
                }
                if(null != send_now){
                    send_now.setVisibility(View.VISIBLE);
                }
                if(null != send_now_count){
                    send_now_count.setText("Send now("+bucket_list.size()+")");
                }
            }
            else {
                if(null != fs_sc) {
                    File_selection.select_count.setText("Select files");
                }
                if(null != send_now){
                    send_now.setVisibility(View.GONE);
                }
                if(null != send_now_count){
                    send_now_count.setText("Send now");
                }
                String location2 = Home.get_app_home_bundle_data_store()+"/Selection_bucket.txt";
                File main_file2 = new File(location2);
                if(main_file2.exists()){
                    if(!main_file2.delete()){
                        //No action needed
                    }
                }
            }
        }
    }

    public static void grid_select_all_child(GridView grid, File[] file_list, Boolean check, Context context){
        //Read the bucket
        List<String> bucket_list = new ArrayList<>();
        String location = Home.get_app_home_bundle_data_store()+"/Selection_bucket.txt";
        File main_file = new File(location);
        if(!main_file.exists()){
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                outputStreamWriter.write("");
                outputStreamWriter.close();
            }
            catch (Exception e){
                Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
            }
        }
        try {
            BufferedReader reader  = new BufferedReader(new FileReader(main_file));
            String line = reader.readLine();
            while (line != null) {
                bucket_list.add(line);
                // read next line
                line = reader.readLine();
            }
            reader.close();
        }
        catch (IOException e){
            bucket_list = null;
        }
        if(null == bucket_list){
            //Error, so open the file instead
            Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
            return;
        }

        if(grid != null) {
            //Layout update
            for (int i = 0; i < grid.getChildCount(); i++) {
                View child = grid.getChildAt(i);
                View main_layout = child.findViewById(R.id.main_layout);
                if (check) {
                    main_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                } else {
                    main_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                }
            }
        }

        //Add-remove the files
        if(check){
            //Get them in the bucket
            for(File file : file_list){
                String t_path = file.getPath();
                String home = "/storage";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    home = Environment.getStorageDirectory().getPath();
                }
                t_path = t_path.substring(home.length());
                if(!bucket_list.contains(t_path)){
                    bucket_list.add(t_path);
                }
            }
        }
        else {
            for(File file : file_list){
                String t_path = file.getPath();
                String home = "/storage";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    home = Environment.getStorageDirectory().getPath();
                }
                t_path = t_path.substring(home.length());
                int position = bucket_list.indexOf(t_path);
                if(position >= 0){
                    bucket_list.remove(position);
                }
            }
        }
        //Update the list
        StringBuilder new_data = null;
        for(String path : bucket_list){
            if(path.equals("empty")){
                continue;
            }
            if(null == new_data){
                new_data = new StringBuilder(path);
            }
            else {
                new_data.append(System.lineSeparator()).append(path);
            }
        }
        if(bucket_list.size() > 0){
            if(null == new_data) {
                Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
            }
            else {
                //Save the string
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                    outputStreamWriter.write(new_data.toString());
                    outputStreamWriter.close();
                }
                catch (Exception e){
                    Toast.makeText(context, "Can't unselect!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else {
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                outputStreamWriter.write("");
                outputStreamWriter.close();
            }
            catch (Exception e){
                Toast.makeText(context, "Can't unselect!", Toast.LENGTH_SHORT).show();
            }
        }
        File_selection.selection_update();
    }
    private boolean location_granted(){
        return !(this.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) < 0)
                && !(this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) < 0);
    }
    private void Show_token_ad(){
        RewardedAd.load(this, REWARD_AD_UNIT_TURBO_TOKEN,
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Toast.makeText(Home.this, "Error loading ad!", Toast.LENGTH_LONG).show();
                        ad_loading.setVisibility(View.GONE);
                        ad_icon.setVisibility(View.VISIBLE);
                        rewardedAd = null;
                    }
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Activity activityContext = Home.this;
                        rewardedAd.show(activityContext, rewardItem -> {
                            // Handle the reward.
                            add_token(1);
                        });
                    }
                });
    }
    private void add_token(Integer add){
        update_daily_token = true;
        int count_token = 0;
        String token = dbHandler.get_settings("turbo_token");
        if (null != token) {
            token = global_class.decrypt(token, global_class.getDeviceEncryptionKey());
            if(null != token) {
                count_token = Integer.parseInt(token);
                count_token+= add;
            }
        }
        String d;
        if(add <= 1){
            d = "You got "+add+" token";
        }
        else {
            d = "You got "+add+" tokens";
        }
        String turbo_token = global_class.encrypt(String.valueOf(count_token), global_class.getDeviceEncryptionKey());
        dbHandler.add_setting("turbo_token", turbo_token);
        runOnUiThread(()-> Toast.makeText(Home.this, d, Toast.LENGTH_SHORT).show());
        load_home();
    }
}