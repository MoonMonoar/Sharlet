package com.moonslab.sharlet;

import static com.moonslab.sharlet.Home.store_as_file;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.moonslab.sharlet.custom.Sender;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Receive extends AppCompatActivity {
    private final HashMap<String, String> link_sets = new HashMap<>();
    private int path_index = 0;
    private Dialog dialog;
    private String[] raw_paths;
    private String server_address = null;
    private String pc_ios_link, pc_ios_pin;
    private Context context;
    private TextView current_file, total_received, pack_got, portal_summary, main_title;
    private final long previous_bps = 0;
    private final long total_incoming_size = 0;

    private TableLayout main_table;

    private long start_time;
    private long total_packs = 0;
    private final long current_pack = 0;
    private final HashMap<String, View> file_all_child = new HashMap<>();
    private boolean receive_complete = false;
    private DBHandler dbHandler;

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        //Elements
        Bundle extras = getIntent().getExtras();

        server_address = extras.getString("address");
        pc_ios_link = extras.getString("pc_link");
        pc_ios_pin = extras.getString("pc_pin");

        if (null == server_address || null == pc_ios_link || null == pc_ios_pin) {
            Toast.makeText(this, "Invalid connection information!", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        if(!Home.create_app_folders()){
            Toast.makeText(this, "Storage unavailable, please reinstall Sharlet!", Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }
        dbHandler = new DBHandler(this);

        ProgressBar progress = findViewById(R.id.progress);
        ProgressBar total_progress = findViewById(R.id.total_progress);
        total_received = findViewById(R.id.total_received);
        pack_got = findViewById(R.id.pack_got);
        current_file = findViewById(R.id.current_file);
        main_table = findViewById(R.id.files_table);
        portal_summary = findViewById(R.id.portal_summary);
        main_title = findViewById(R.id.total_progress_title);

        context = this;
        TextView back_button = findViewById(R.id.back_button);
        TextView portal_info = findViewById(R.id.portal_info);
        portal_info.setOnClickListener(v -> portal_info_dialog());

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
            dialog.dismiss();
            finish();
        });

        Cancel.setOnClickListener(v -> dialog.dismiss());

        back_button.setOnClickListener(v -> dialog.show());

        //Dialogue ends
        raw_paths = dbHandler.get_settings("receive_raw_paths").split(System.lineSeparator());
        String[] raw_links = dbHandler.get_settings("receive_links").split(System.lineSeparator());
        if(null != raw_paths) {
            for (int x = 0; x < raw_paths.length; x++) {
                link_sets.put(raw_paths[x], raw_links[x]);
            }
        }
        //Check the receive paths
        if(null == link_sets || link_sets.size() == 0){
            Toast.makeText(this, "Unable to receive!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        make_page_layout();
        //Initiate the receiving...
        start_time = new Date().getTime();
        assert link_sets != null;
        total_packs = link_sets.size();
        path_index = 0;
        new Thread(this::start_receive).start();
    }

    private void make_page_layout() {
        new Thread(()-> {
            boolean first = false;
            for (String path_main : raw_paths) {
                if (null != path_main) {
                    String file_name_main = path_main.substring(path_main.lastIndexOf("/") + 1);
                    View child = View.inflate(context, R.layout.sender_file_child, null);
                    TextView file_name = child.findViewById(R.id.file_name);
                    TextView file_path = child.findViewById(R.id.file_path);
                    ImageView file_image = child.findViewById(R.id.file_image);
                    TextView file_state = child.findViewById(R.id.file_sate);

                    file_state.setText("\\\uf358");
                    String location = Home.get_app_home_directory();

                    String file_type = Home.file_type(file_name_main);
                    switch (file_type) {
                        case "app":
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_android_24));
                            location += "/Apps/";
                            break;
                        case "photo":
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_photo_24));
                            location += "/Photos/";
                            break;
                        case "video":
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_video_file_24));
                            location += "/Videos/";
                            break;
                        case "document":
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.book_file));
                            location += "/Documents/";
                            break;
                        case "audio":
                            file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_audio_file_24));
                            location += "/Audio/";
                            break;
                        default:
                            location += "/Files/";
                            break;
                    }

                    String path = location + file_name_main;
                    file_name.setText(file_name_main);
                    file_path.setText(path);
                    if (!first) {
                        runOnUiThread(() -> main_table.removeAllViews());
                        first = true;
                    }
                    child.setOnClickListener(v -> open_file(path, file_type));
                    runOnUiThread(() -> main_table.addView(child));
                    file_all_child.put(path_main, child);
                }
            }
        }).start();
    }

    private void open_file(String path, String file_type) {
        if(receive_complete){
            switch (file_type) {
                case "photo": {
                    Intent intent = new Intent(context, Photo_view.class);
                    //Save the file first
                    store_as_file("Image_last.txt", path, context);
                    context.startActivity(intent);
                    break;
                }
                case "video": {
                    Intent intent = new Intent(context, Video_player.class);
                    store_as_file("Video_last.txt", path, context);
                    context.startActivity(intent);
                    break;
                }
                case "audio":
                    Intent in = Home.get_music_intent(dbHandler, path, context);
                    context.startActivity(in);
                    break;
                default:
                    Home.openFile(context, new File(path));
                    break;
            }
        }
        else {
            runOnUiThread(()->Toast.makeText(context, "Please wait to finish receiving", Toast.LENGTH_SHORT).show());
        }
    }

    @SuppressLint("SetTextI18n")
    private void start_receive() {
        if(link_sets.size() > 0) {
            String file_name_path = raw_paths[path_index];
            String path = link_sets.get(file_name_path);
            if (null != path) {
                current_file.setText(path);
                String location = Home.get_app_home_directory();
                String file_name = file_name_path.substring(file_name_path.lastIndexOf("/") + 1);
                if (Home.file_type(file_name).equals("photo")) {
                    location += "/Photos/";
                } else if (Home.file_type(file_name).equals("video")) {
                    location += "/Videos/";
                } else if (Home.file_type(file_name).equals("audio")) {
                    location += "/Audio/";
                } else if (Home.file_type(file_name).equals("document")) {
                    location += "/Documents/";
                } else if (Home.file_type(file_name).equals("app")) {
                    location += "/Apps/";
                } else {
                    location += "/Files/";
                }
                current_file.setText(file_name);

                try {
                    URL url = new URL(server_address + "/get_file/" + path);
                    String temporary_location = location + ".sharlet-"+file_name;
                    String final_location = location + file_name;

                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url).build();
                    Response response = client.newCall(request).execute();
                    FileOutputStream fos = new FileOutputStream(temporary_location);
                    assert response.body() != null;
                    fos.write(response.body().bytes());
                    fos.close();

                }
                catch (Exception ignored){
                }
            }
        }
        else {
            //Finished receive
            long diff = new Date().getTime() - start_time;
            String speed = Receive.format_size(previous_bps);
            speed += "/s";
            String status = format_size(total_incoming_size)+" received in "+ Sender.get_time_span(diff)+" - "+speed;
            runOnUiThread(()-> {
                   portal_summary.setText(status);
                   current_file.setText("Receiver inactive");
                   main_title.setText("Received 100%");
            });
            receive_complete = true;
        }
        if(total_packs > 0) {
            runOnUiThread(()-> {
                total_received.setText("Total: " + current_pack + "/" + total_packs);
                pack_got.setText("Received: " + format_size(total_incoming_size));
            });
        }
    }

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
    public void onBackPressed() {
        dialog.show();
    }
    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    private void portal_info_dialog(){
        Dialog dialog;
        //Connection info dialog
        //Confirm dialogue
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.portal_info_dialog);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView max_speed = dialog.getWindow().findViewById(R.id.max_speed);
        TextView total_band = dialog.getWindow().findViewById(R.id.total_transfer);
        TextView total_files = dialog.getWindow().findViewById(R.id.total_packages);
        TextView pc_link = dialog.getWindow().findViewById(R.id.http_address);
        TextView network_name = dialog.getWindow().findViewById(R.id.ssid);
        TextView link_speed = dialog.getWindow().findViewById(R.id.link_speed);
        TextView refresh = dialog.getWindow().findViewById(R.id.refresh);
        TextView pc_pin = dialog.getWindow().findViewById(R.id.http_pin);

        //Close
        TextView close = dialog.getWindow().findViewById(R.id.close);

        String ssid_name = Home.get_wifi_info(context).getSSID();
        String link_speed_main = "Unknown";
        if(ssid_name.equals("<unknown ssid>")){
            ssid_name = "Name hidden!";
        }
        int ls = Home.get_wifi_info(context).getLinkSpeed();
        if(ls < 0){
           ssid_name = "Name hidden!";
        }
        else {
            link_speed_main = ls+"MBPS";
        }

        //Customize
        max_speed.setText("Maximum speed: "+format_size(previous_bps)+"PS");
        total_band.setText("Total bandwidth: "+format_size(total_incoming_size));
        total_files.setText("Total packages: "+Math.round(total_packs));
        pc_link.setText("iOs/PC link: "+pc_ios_link);
        network_name.setText("Network: "+ssid_name);
        link_speed.setText("Link speed: "+link_speed_main);
        pc_pin.setText("Pin(iOs/PC): "+pc_ios_pin);

        close.setOnClickListener(v -> dialog.dismiss());

        refresh.setOnClickListener(v -> {
            dialog.dismiss();
            portal_info_dialog();
        });

        pc_link.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Sharlet link", pc_ios_link);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show();
        });

        //Dialogue ends
        dialog.show();
    }
    //Visuals done

    @SuppressLint("SetTextI18n")
    private void update_child(String location, String main_path, boolean success) {
        View child = file_all_child.get(main_path);
        if(null != child){
            TextView file_state = child.findViewById(R.id.file_sate);
            ImageView file_image = child.findViewById(R.id.file_image);
            TextView file_path = child.findViewById(R.id.file_path);
            File main_file = new File(location);
            File temp = new File(location.replace(".sharlet-", ""));
            if(main_file.renameTo(temp)) {
                main_file = temp;
            }
            File finalMain_file = main_file;
            if(!success){
                main_file.delete();
            }
            runOnUiThread(() -> {
                if(success) {
                    file_state.setText("\\\uf058");
                    file_state.setTextColor(ContextCompat.getColor(context, R.color.light_green));
                    String file_name_main = main_path.substring(main_path.lastIndexOf("/") + 1);
                    String file_type = Home.file_type(file_name_main);
                    if (file_type.equals("app")) {
                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_android_24));
                    }
                    if(file_type.equals("photo")) {
                        runOnUiThread(() -> Picasso.get().load(finalMain_file).placeholder(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(file_image));
                    }
                    if(file_type.equals("video")){
                        runOnUiThread(()-> Glide.with(context)
                                .load(finalMain_file)
                                .placeholder(R.drawable.ic_baseline_video_file_24)
                                .into(file_image));
                    }
                    if(file_type.equals("document")){
                        file_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.book_file));
                    }
                    if(file_type.equals("audio")){
                        try {
                            android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                            mmr.setDataSource(location);
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
                    String size = format_size(finalMain_file.length());
                    file_path.setText(size+" - "+finalMain_file.getPath());
                }
                else {
                    file_state.setText("\\\uf057");
                    file_state.setTextColor(ContextCompat.getColor(context, R.color.warning));
                    child.setOnClickListener(v->{
                        AlertDialog.Builder alert = new AlertDialog.Builder(context);
                        alert.setTitle("Unsuccessful transaction");
                        alert.setMessage("This file was not received due to unsafe filename or weak connection! Ask the sender to rename the file or check connection.");
                        alert.setNegativeButton("Okay",
                                (dialog, whichButton) -> {
                                    //Do nothing
                                });
                        alert.show();
                    });
                }
            });
        }
    }
}