package com.moonslab.sharlet.custom;

import static com.moonslab.sharlet.Home.cancel_notification;
import static com.moonslab.sharlet.Home.file_type;
import static com.moonslab.sharlet.Music_application_class.CHANNEL_SERVICE;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.moonslab.sharlet.DBHandler;
import com.moonslab.sharlet.Home;
import com.moonslab.sharlet.R;
import com.moonslab.sharlet.Receive;
import com.moonslab.sharlet.objects.fileOBJ;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;

public class Receiver extends Service {
    private static HashMap<Integer, Boolean> done_list = new HashMap<>();
    private static List<fileOBJ> success_list = new ArrayList<>();
    private static List<fileOBJ> fail_list = new ArrayList<>();
    private boolean skip_binder = false, running = false, done = false;
    private static List<Integer> notification_list = new ArrayList<>();
    Receive.Navigate navigate = new Receive.Navigate();

    //Static counters and cores
    private static long total_bytes_received = 0;
    private static long total_carry = 0;
    private static long cache_size = 0;
    private static long http_timestamp = 0, http_time_took = 0;
    private static List<fileOBJ> fileOBJS;
    private static boolean killer = false;
    private DBHandler dbHandler;

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Service destroyed
        for(Integer id : notification_list){
            cancel_notification(id, getApplicationContext());
        }
        //ALSO THE MAIN NOTIFICATIONS
        cancel_notification(2020, getApplicationContext());
        if(null != client) {
            client.dispatcher().cancelAll();
            client.connectionPool().evictAll();
        }
        killer = true;
        //RESET static vars
        fileOBJS = new ArrayList<>();
        done_list = new HashMap<>();
        success_list = new ArrayList<>();
        fail_list = new ArrayList<>();
        notification_list = new ArrayList<>();
        total_carry = 0;
        total_bytes_received = 0;
        http_timestamp = 0;
        http_time_took = 0;
        cache_size = 0;
        dbHandler.add_setting("receiver_opened", "false");
    }

    private TextView main_title, summery, current_file, pack_got, total_received;
    private ProgressBar total_progress, current_progress;
    private int run_download = 0;
    private static String server, pin;
    private TableLayout files_table;
    private static final ReceiveCore receiveCore = new ReceiveCore();
    private OkHttpClient client;
    private static HashMap<Integer, View> fileViews = new HashMap<>();
    //Gives access to the main thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        navigate.setContext(getApplicationContext());
        dbHandler = new DBHandler(getApplicationContext());

        killer = false;

        if(running){
            makefileList(true);
            update_total_received(true);
            success_list_reset();
            return START_STICKY;
        }

        //Service start
        if(null != fileOBJS){
            //On resume the page - start download from there
            makefileList(false);
        }
        //Notification
        Intent notificationIntent = new Intent(getApplicationContext(), Receive.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.logo_main)
                .setContentTitle("Receiver is active")
                .setContentText("Receiving files, tap to open portal.")
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(2020, notification);

        running = true;
        dbHandler.add_setting("receiver_opened", "true");
        return START_STICKY;
    }

    private void success_list_reset() {
        for(fileOBJ file:success_list){
            String file_type = file_type(file.getFile());
            String path = savePath(file_type, file.getFile());
            View child = fileViews.get(file.getId());
            successView(child, path, file_type);
        }
        for(fileOBJ file:fail_list){
            View child = fileViews.get(file.getId());
            failView(child);
        }
    }

    //Methods
    private void makefileList(boolean remake_page) {
        if(remake_page){
            mainHandler.post(()-> files_table.removeAllViews());
            fileViews = new HashMap<>();
        }
        AtomicBoolean cleaner = new AtomicBoolean(false);
        int loop = 0;
        for(fileOBJ fileOBJ:fileOBJS){
            File file = new File(fileOBJ.getFile());
            String name = file.getName();
            String file_type = Home.file_type(name);
            String path = savePath(file_type, name);

            //View
            View child = View.inflate(getApplicationContext(), R.layout.sender_file_child, null);
            TextView file_name = child.findViewById(R.id.file_name),
                    file_state = child.findViewById(R.id.file_sate),
                    file_path = child.findViewById(R.id.file_path);

            //Download default
            file_state.setText("\uf0ab");

            ImageView file_image = child.findViewById(R.id.file_image);

            switch (file_type){
                case "app":
                    mainHandler.post(()->file_image.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_android_24)));
                    break;
                case "photo":
                    mainHandler.post(()-> Picasso.get().load(file).placeholder(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(file_image));
                    break;
                case "video":
                    mainHandler.post(()-> Glide.with(getApplicationContext())
                            .load(file)
                            .placeholder(R.drawable.ic_baseline_video_file_24)
                            .into(file_image));
                    break;
                case "document":
                    mainHandler.post(()->file_image.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.book_file)));
                    break;
                case "audio":
                    mainHandler.post(()->file_image.setImageResource(R.drawable.ic_baseline_audio_file_24));
                    break;
            }
            child.setOnClickListener(v-> Toast.makeText(getApplicationContext(), "Wait to finish!", Toast.LENGTH_SHORT).show());

            mainHandler.post(()->file_name.setText(name));
            //Set save location
            mainHandler.post(()->file_path.setText(path));
            mainHandler.post(()->{
                if(!remake_page && !cleaner.get() && files_table.getChildCount() == 1){
                    files_table.removeAllViews();
                    cleaner.set(true);
                }
                files_table.addView(child);
            });
            //Keep the child into the view list to update on the fly
            fileViews.put(fileOBJ.getId(), child);
            if(loop == fileOBJS.size()-1 && null != main_title){
                main_title.setText(R.string.receiving);
                if(!remake_page){
                    downLoad(fileOBJS);
                }
            }
            loop++;
        }
        if(!remake_page) {
            //Start download
            run_download = 0;
        }
    }

    //Events
    //Service Binder
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public void setComponents(String server_get,
                                  String pin_get,
                                  List<fileOBJ> fileOBJS_set,
                                  TableLayout file_table_set,
                                  TextView currentFile,
                                  ProgressBar totalProgress_set,
                                  ProgressBar currentProgress,
                                  TextView portalSummary_set,
                                  TextView main_title_set,
                                  TextView packs,
                                  TextView totalSize){

            //Page components comes here
            fileOBJS = fileOBJS_set;
            files_table = file_table_set;
            server = server_get;
            total_progress = totalProgress_set;
            summery = portalSummary_set;
            main_title = main_title_set;
            current_file = currentFile;
            current_progress = currentProgress;
            pack_got = packs;
            total_received = totalSize;
            navigate.setContext(getApplicationContext());
            pin = pin_get;
            if(!skip_binder){
                makefileList(false);
                skip_binder = true;
            }
        }
    }
    public IBinder onBind(Intent intent){
        return binder;
    }
    public static String savePath(String type, String file_name){
        File address;
        switch (type){
            case "app":
                address = new File(Environment.getExternalStorageDirectory(), "Sharlet/Apps/"+file_name);
                return address.getPath();
            case "photo":
                address = new File(Environment.getExternalStorageDirectory(), "Sharlet/Photos/"+file_name);
                return address.getPath();
            case "video":
                address = new File(Environment.getExternalStorageDirectory(), "Sharlet/Videos/"+file_name);
                return address.getPath();
            case "document":
                address = new File(Environment.getExternalStorageDirectory(), "Sharlet/Documents/"+file_name);
                return address.getPath();
            case "audio":
                address = new File(Environment.getExternalStorageDirectory(), "Sharlet/Audio/"+file_name);
                return address.getPath();
            default:
                address = new File(Environment.getExternalStorageDirectory(), "Sharlet/Files/"+file_name);
                return address.getPath();
        }
    }
    public void downLoad(List<fileOBJ> fileOBJS) {
        mainHandler.post(()->{
            int num = run_download+1;
            pack_got.setText((num <= 1)?"Received: "+num+" file":"Received: "+num+" files");
        });
        fileOBJ fileOBJ = fileOBJS.get(run_download);
        File file = new File(fileOBJ.getFile());
        String file_type = Home.file_type(file.getName());
        String path = savePath(file_type, file.getName());

        String file_link = server + "/" + pin + "/" + fileOBJ.getLink();
        String password = fileOBJ.getPass();

        if(done_list.containsKey(fileOBJ.getId())) {
            //Contains so check if success or failed
            if (has_object(success_list, fileOBJ.getId())){
                View child = fileViews.get(fileOBJ.getId());
                successView(child, path, file_type);
                return;
            }
            if (has_object(fail_list, fileOBJ.getId())){
                View child = fileViews.get(fileOBJ.getId());
                failView(child);
                return;
            }
        }

        //Start
        current_file.setText(file.getName());
        http_timestamp = new Date().getTime();
        done_list.put(fileOBJ.getId(), true);
        client = receiveCore.downloadFile(file_link, path, password, new ReceiveCore.DownloadCallback() {
            private final static int PROGRESS_UPDATE_THRESHOLD = 5;
            @Override
            public void onProgressUpdate(int progress, long totalBytesRead, long totalSize) {
                if (progress % PROGRESS_UPDATE_THRESHOLD != 0) {
                    return;
                }
                current_progress.setProgress(progress);
                total_bytes_received = totalSize - (totalSize - totalBytesRead);

                long time_dif = new Date().getTime() - http_timestamp;
                http_time_took += time_dif;

                if(progress == 100){
                    total_carry += totalSize;
                }
                else {
                    mainHandler.post(() -> Receiver.this.update_total_received(false));
                }

                float ratio = (float)progress/100;
                float downs = run_download+ratio;

                int full_progress = (int) (((double) downs / fileOBJS.size()) * 100);
                mainHandler.post(()-> total_progress.setProgress(full_progress));

                http_timestamp = new Date().getTime();
            }
            @Override
            public void onSuccess() {

                if(killer){
                    return;
                }

                success_list.add(fileOBJ);
                //Notification update
                notification_list.add(Home.create_notification(getApplicationContext(), "Incoming file",
                        "✅ Last: Done - "+file.getName(), 2010, NotificationCompat.PRIORITY_DEFAULT, false));

                //Next
                run_download++;
                if(run_download < fileOBJS.size() && null != fileOBJS.get(run_download)){
                    downLoad(fileOBJS);
                }
                else {
                    mainHandler.post(()-> {
                        current_file.setText(R.string.done);
                        main_title.setText(R.string.received);
                        done = true;
                    });
                }

                View child = fileViews.get(fileOBJ.getId());
                successView(child, path, file_type);
            }

            @Override
            public void onFailure(Exception e) {

                if(killer){
                    return;
                }

                fail_list.add(fileOBJ);
                //Notification update
                notification_list.add(Home.create_notification(getApplicationContext(), "Incoming file",
                        "❌ Last: Failed - "+file.getName(), 2010, NotificationCompat.PRIORITY_DEFAULT, false));

                //Next
                run_download++;
                if(run_download < fileOBJS.size() && null != fileOBJS.get(run_download)){
                    downLoad(fileOBJS);
                }
                else {
                    run_download = 0;
                    mainHandler.post(()-> {
                        main_title.setText(R.string.unfinished);
                        current_file.setText(R.string.failed);
                    });
                }


                View child = fileViews.get(fileOBJ.getId());
                if(null != child) {
                    failView(child);
                }
            }
        });
    }
    private boolean has_object(List<fileOBJ> list, int id) {
        for(fileOBJ file : list){
            if(file.getId() == id){
                return true;
            }
        }
        return false;
    }
    private void failView(View child) {
        if(null != child) {
            TextView status = child.findViewById(R.id.file_sate);
            mainHandler.post(() -> {
                status.setText("\uf057");
                status.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.warning));
                child.setOnClickListener(v -> Toast.makeText(getApplicationContext(), "Could not receive!", Toast.LENGTH_SHORT).show());
            });
        }
    }
    private void successView(View child, String path, String file_type){
        if(null != child) {
            TextView status = child.findViewById(R.id.file_sate);
            TextView path_file = child.findViewById(R.id.file_path);
            mainHandler.post(()-> {

                status.setText("\uf058");
                status.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_green));

                File file = new File(path);

                path_file.setText(String.format("%s - %s", Receive.format_size((long) file.length()), path));

                ImageView file_image = child.findViewById(R.id.file_image);
                switch (file_type) {
                    case "app":
                        file_image.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_android_24));
                        child.setOnClickListener(v->{
                            Toast.makeText(getApplicationContext(), "Upcoming feature!", Toast.LENGTH_SHORT).show();
                        });
                        break;
                    case "photo":
                        Picasso.get().load(file).placeholder(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(file_image);
                        child.setOnClickListener(v->navigate.showImage(path));
                        break;
                    case "video":
                        Glide.with(getApplicationContext())
                                .load(file)
                                .placeholder(R.drawable.ic_baseline_video_file_24)
                                .into(file_image);
                        child.setOnClickListener(v-> navigate.playVideo(path));
                        break;
                    case "document":
                        file_image.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.book_file));
                        child.setOnClickListener(v->navigate.openFile(path));
                        break;
                    case "audio":
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
                        child.setOnClickListener(v-> navigate.playMusic(path));
                        break;
                    default:
                        child.setOnClickListener(v->navigate.openFile(path));
                        break;
                }
            });
        }
    }
    private void update_total_received(boolean use_cache) {
        String in;
        in = Sender.get_time_span(http_time_took);

        long sum = 0;
        if(!use_cache) {
            sum = total_bytes_received + total_carry;
        }
        else {
            sum = cache_size;
        }

        String speed;
        float time = (float) http_time_took / (float) 1000;
        float ratio = (float) (sum)  / time;

        speed = Receive.format_size((long) ratio);
        speed += "/s";

        String size2 = Receive.format_size(sum);
        String s = size2 + " received in " + in + " - " + speed;
        long finalSum = sum;
        mainHandler.post(()->{
            summery.setText(s);
            total_received.setText(MessageFormat.format("Total: {0}", Receive.format_size(finalSum)));
            pack_got.setText((run_download <= 1)?"Received: "+run_download+" file":"Received: "+run_download+" files");
            if(done) {
                current_file.setText(R.string.done);
                main_title.setText(R.string.received);
                current_progress.setProgress(100);
                total_progress.setProgress(100);
            }
        });
        cache_size = sum;
    }
}