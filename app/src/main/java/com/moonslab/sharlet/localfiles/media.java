package com.moonslab.sharlet.localfiles;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.moonslab.sharlet.Home.read_from_file;
import static com.moonslab.sharlet.Home.store_as_file;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.moonslab.sharlet.DBHandler;
import com.moonslab.sharlet.File_selection;
import com.moonslab.sharlet.Home;
import com.moonslab.sharlet.Photo_view;
import com.moonslab.sharlet.R;
import com.moonslab.sharlet.Receive;
import com.moonslab.sharlet.Video_player;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class media extends Fragment {
    String doc_path = Home.get_storage_root()+"/Download";
    Boolean load_busy = false;
    Context context;
    String media_mode = "Photo";
    Boolean show_hidden = false;
    Boolean no_selection = false;
    TextView sort_today,
            sort_week,
            sort_month,
            sort_all;
    List<File> cache_file_list = new ArrayList<>();
    ScrollView main_scroll;
    int height = 0, width = 0;
    private DBHandler dbHandler;
    public void set_dimen(int height_set, int width_set){
        height = height_set;
        width = width_set;
    }
    public void set_media_mood(String mode){
        media_mode = mode;
    }
    public void set_Context(Context context_target){context = context_target;}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View main_view = inflater.inflate(R.layout.fragment_media_controlable, container, false);
        dbHandler = new DBHandler(context);

        String d = read_from_file("doc_path.txt", context);
        if(null != d){
            doc_path = d;
        }

        main_scroll = main_view.findViewById(R.id.main_scroll);
        TableLayout files_table = main_view.findViewById(R.id.files_table);

        String tester4 = dbHandler.get_settings("show_hidden");
        if(null != tester4 && tester4.equals("true")){
            show_hidden = true;
        }

        Home.loading_state.setVisibility(View.INVISIBLE);
        Home.loading_state.setProgress(0);

        //Prefix scroller height
        main_scroll.setMinimumHeight(height);

        //Sort buttons
        sort_today = main_view.findViewById(R.id.sort_today);
        sort_week = main_view.findViewById(R.id.sort_week);
        sort_month = main_view.findViewById(R.id.sort_month);
        sort_all = main_view.findViewById(R.id.sort_all);

        sort_today.setOnClickListener(v -> {
            if(load_busy){
                Toast.makeText(context, "Loading...wait", Toast.LENGTH_SHORT).show();
                return;
            }
            long today_time = new Date().getTime() - (24*60*60*1000); //24hr past
            thread_loader(inflater, main_view, files_table, today_time, null);
            alter_position(v.getId(), main_view);
        });

        sort_week.setOnClickListener(v -> {
            if(load_busy){
                Toast.makeText(context, "Loading...wait", Toast.LENGTH_SHORT).show();
                return;
            }
            long today_time = new Date().getTime() - (7*24*60*60*1000); //7days past
            thread_loader(inflater, main_view, files_table, today_time, null);
            alter_position(v.getId(), main_view);
        });

        sort_month.setOnClickListener(v -> {
            if(load_busy){
                Toast.makeText(context, "Loading...wait", Toast.LENGTH_SHORT).show();
                return;
            }
            long today_time = new Date().getTime() - (30L *24*60*60*1000); //30days past
            thread_loader(inflater, main_view, files_table, today_time, null);
            alter_position(v.getId(), main_view);
        });

        sort_all.setOnClickListener(v -> {
            if(load_busy){
                Toast.makeText(context, "Loading...wait", Toast.LENGTH_SHORT).show();
                return;
            }
            thread_loader(inflater, main_view, files_table, null, null);
            alter_position(v.getId(), main_view);
        });

        //Default(All)
        thread_loader(inflater, main_view, files_table, null, null);

        return main_view;
    }

    private void alter_position(int id, View main_view) {
        List<Integer> buttons = new ArrayList<>();
        buttons.add(R.id.sort_today);
        buttons.add(R.id.sort_week);
        buttons.add(R.id.sort_month);
        buttons.add(R.id.sort_all);
        for(int id_target : buttons){
            if(id_target == id){
                //Active
                main_view.post(()-> main_view.findViewById(id_target).setBackground(ContextCompat.getDrawable(context, R.color.primary)));
            }
            else {
                main_view.post(()-> main_view.findViewById(id_target).setBackground(ContextCompat.getDrawable(context, R.color.tab_inactive)));
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void thread_loader(LayoutInflater inflater, View main_view, TableLayout files_table, Long sort_limit, List<File> pre_files){
        load_busy = true;
        Home.loading_state.setVisibility(View.VISIBLE);

        //Ads
        Home.showInterstitial(context);

        new Thread(() -> {
            List<File> file_list;
            if(pre_files == null) {
                if (cache_file_list.size() == 0) {
                    file_list = All_files(context);
                    if (null != file_list) {
                        cache_file_list = file_list;
                    } else {
                        no_file(inflater, main_view, files_table);
                        return;
                    }
                } else {
                    file_list = cache_file_list;
                }
            }
            else {
                file_list = pre_files;
            }
            int flag = 0;
            main_view.post(files_table::removeAllViews);

            if(null != pre_files){
                //Folders!, So insert folder control
                //Ready
                main_view.post(()-> main_view.findViewById(R.id.folder_navigator).setVisibility(View.VISIBLE));
                ScrollView.LayoutParams lp = (ScrollView.LayoutParams)main_scroll.getLayoutParams();
                lp.setMargins(0, Home.convertDpToPixels(115, context), 0, 0);
                main_view.post(()->main_scroll.setLayoutParams(lp));

                TextView prev = main_view.findViewById(R.id.folder_prev),
                         name = main_view.findViewById(R.id.current_folder);
                name.setText(folderFromPath(pre_files.get(0).getPath(), pre_files.get(0).getName()));
                prev.setOnClickListener(v -> sort_all.performClick());
            }
            else {
                //Ready
                main_view.post(()-> main_view.findViewById(R.id.folder_navigator).setVisibility(View.GONE));
                ScrollView.LayoutParams lp = (ScrollView.LayoutParams)main_scroll.getLayoutParams();
                lp.setMargins(0, Home.convertDpToPixels(65, context), 0, 0);
                main_view.post(()->main_scroll.setLayoutParams(lp));
            }

            if(null != sort_limit || null != pre_files) {
                for (int i = 0; i < file_list.size(); i++) {
                    File file = file_list.get(i);
                    if (null == pre_files && file.lastModified() < sort_limit) { //Equal or older then limit
                        continue;
                    }
                    if (null == file || !file.exists()) {
                        continue;
                    }
                    String File_name = file.getName();
                    String File_extension = FilenameUtils.getExtension(File_name).toLowerCase(Locale.ROOT);
                    View view = inflater.inflate(R.layout.all_files_child, null);
                    ImageView image;

                    image = view.findViewById(R.id.grid_image);
                    TextView name = view.findViewById(R.id.file_name);
                    TextView info = view.findViewById(R.id.file_info);
                    name.setText(file.getName());
                    info.setText(Receive.format_size(file.length())+" - "+ convertTime(file.lastModified()));
                    if (File_extension.equals("png")
                            || File_extension.equals("jpg")
                            || File_extension.equals("gif")
                            || File_extension.equals("jpeg")
                            || File_extension.equals("heic")
                            || File_extension.equals("webp")
                            || File_extension.equals("tiff")
                            || File_extension.equals("raw")) {
                            view.setOnLongClickListener(v -> {
                                Intent intent = new Intent(context, Photo_view.class);
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                //Save the file first
                                store_as_file("Image_last.txt", file.getPath(), context);
                                context.startActivity(intent);
                                return true;
                            });
                        try {
                            main_view.post(() -> Picasso.get().load(file).placeholder(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(image));
                        } catch (Exception e) {
                            main_view.post(() -> Picasso.get().load(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(image));
                        }
                    }
                    else if (File_extension.equals("mp4")
                            || File_extension.equals("mkv")
                            || File_extension.equals("3gp")
                            || File_extension.equals("flv")
                            || File_extension.equals("avi")
                            || File_extension.equals("webm")
                            || File_extension.equals("mov")) {
                            view.setOnLongClickListener(v -> {
                                Intent intent = new Intent(context, Video_player.class);
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                store_as_file("Video_last.txt", file.getPath(), context);
                                context.startActivity(intent);
                                return true;
                            });
                        try {
                            main_view.post(() -> Glide.with(context)
                                    .load(file)
                                    .placeholder(R.drawable.ic_baseline_video_file_24)
                                    .into(image));
                        } catch (Exception e) {
                            main_view.post(() -> Picasso.get().load(R.drawable.ic_baseline_video_file_24).resize(250, 250).centerCrop().into(image));
                        }
                    }
                    else if (File_extension.equals("mp3")
                            || File_extension.equals("wav")
                            || File_extension.equals("ogg")
                            || File_extension.equals("m4a")
                            || File_extension.equals("aac")
                            || File_extension.equals("alac")
                            || File_extension.equals("aiff")) {
                            view.setOnLongClickListener(v -> {
                                Intent in = Home.get_music_intent(dbHandler, file.getPath(), context);
                                in.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                startActivity(in);
                                return true;
                            });
                        main_view.post(() -> Picasso.get().load(R.drawable.ic_baseline_queue_music_24).placeholder(R.drawable.ic_baseline_queue_music_24).resize(200, 200).centerCrop().into(image));
                    }
                    else {

                        View view2 = inflater.inflate(R.layout.all_files_child_unknown, null);

                        TextView ext_holder = view2.findViewById(R.id.unknown_file_text);
                        if (File_extension.length() > 3) {
                            File_extension = File_extension.substring(0, 3);
                        }
                        ext_holder.setText(File_extension);
                        TextView name_x = view2.findViewById(R.id.file_name);
                        name_x.setText(file.getName());
                        TextView info_x = view2.findViewById(R.id.file_info);
                        info_x.setText(Receive.format_size(file.length())+" - "+ convertTime(file.lastModified()));
                            view2.setOnLongClickListener(v -> {
                                Toast.makeText(context, "Opening...", Toast.LENGTH_SHORT).show();
                                Home.openFile(context, file);
                                return true;
                            });
                        view = view2;
                    }

                    if (no_selection) {
                        view.setOnLongClickListener(v -> {
                            Toast.makeText(context, "Opening...", Toast.LENGTH_SHORT).show();
                            Home.openFile(context, file);
                            return false;
                        });
                    }

                    if (!no_selection) {
                        View finalConvertView = view;
                        view.setOnClickListener(v -> {
                            //Read the bucket
                            List<String> bucket_list = new ArrayList<>();
                            String location = Home.get_app_home_bundle_data_store() + "/Selection_bucket.txt";
                            File main_file = new File(location);
                            if (!main_file.exists()) {
                                try {
                                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                                    outputStreamWriter.write("");
                                    outputStreamWriter.close();
                                } catch (Exception e) {
                                    Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            try {
                                BufferedReader reader = new BufferedReader(new FileReader(main_file));
                                String line = reader.readLine();
                                while (line != null) {
                                    bucket_list.add(line);
                                    // read next line
                                    line = reader.readLine();
                                }
                                reader.close();
                            } catch (IOException e) {
                                bucket_list = null;
                            }

                            Boolean selected_already = false;
                            //Not null, so look for the file
                            //if exists, its selected
                            for (String path : bucket_list) {
                                String path0 = file.getPath();
                                String home = "/storage";
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    home = Environment.getStorageDirectory().getPath();
                                }
                                path0 = path0.substring(home.length());
                                if (path.equals(path0)) {
                                    selected_already = true;
                                    break;
                                }
                            }

                            LinearLayout main_layout = finalConvertView.findViewById(R.id.main_layout);

                            if (!selected_already) {
                                main_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                                //Add to the list convert to text and save
                                String path0 = file.getPath();
                                String home = "/storage";
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    home = Environment.getStorageDirectory().getPath();
                                }
                                path0 = path0.substring(home.length());
                                bucket_list.add(path0);
                                //New bucket
                                String new_data = null;
                                for (String path : bucket_list) {
                                    if (path.equals("empty")) {
                                        continue;
                                    }
                                    if (null == new_data) {
                                        new_data = path;
                                    } else {
                                        new_data += System.lineSeparator() + path;
                                    }
                                }
                                if (null != new_data) {
                                    //Save the string
                                    try {
                                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                                        outputStreamWriter.write(new_data);
                                        outputStreamWriter.close();
                                    } catch (Exception e) {
                                        Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                main_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                                if (bucket_list.size() == 1) {
                                    //Empty and return
                                    main_file.delete();
                                    File_selection.selection_update();
                                    return;
                                }
                                //New bucket
                                String new_data = null;
                                for (String path : bucket_list) {
                                    String path0 = file.getPath();
                                    String home = "/storage";
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        home = Environment.getStorageDirectory().getPath();
                                    }
                                    path0 = path0.substring(home.length());
                                    if (path.equals("empty") || path.equals(path0)) {
                                        continue;
                                    }
                                    if (null == new_data) {
                                        new_data = path;
                                    } else {
                                        new_data += System.lineSeparator() + path;
                                    }
                                }
                                if (null != new_data) {
                                    //Save the string
                                    try {
                                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                                        outputStreamWriter.write(new_data);
                                        outputStreamWriter.close();
                                    } catch (Exception e) {
                                        Toast.makeText(context, "Can't unselect!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            File_selection.selection_update();
                        });
                    }

                    //Pre check
                    List<String> bucket_list = new ArrayList<>();
                    String location = Home.get_app_home_bundle_data_store() + "/Selection_bucket.txt";
                    File main_file = new File(location);
                    if (!main_file.exists()) {
                        try {
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                            outputStreamWriter.write("");
                            outputStreamWriter.close();
                        } catch (Exception e) {
                            Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(main_file));
                        String line = reader.readLine();
                        while (line != null) {
                            bucket_list.add(line);
                            // read next line
                            line = reader.readLine();
                        }
                        reader.close();
                    } catch (IOException e) {
                        bucket_list = null;
                    }

                    Boolean selected_already = false;
                    //Not null, so look for the file
                    //if exists, its selected
                    for (String path : bucket_list) {
                        String path0 = file.getPath();
                        String home = "/storage";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            home = Environment.getStorageDirectory().getPath();
                        }
                        path0 = path0.substring(home.length());
                        if (path.equals(path0)) {
                            selected_already = true;
                            break;
                        }
                    }
                    if (selected_already) {
                        view.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                    }
                    View finalView = view;
                    int finalI = i;
                    main_view.post(() -> {
                        float x = finalI+1, y = file_list.size(), r = (x/y)*100;
                        int p = Math.round(r);
                        files_table.addView(finalView);
                        Home.loading_state.setProgress(Math.round(p));
                    });
                    flag++;
                }
                if (flag == 0) {
                    no_file(inflater, main_view, files_table);
                    return;
                }
            }
            else {
                //Sort by folders
                HashMap<String, List> folders = new HashMap<>();
                String folder = "";
                for (File file : file_list) {
                    //Look thorough every file and store data
                    String name = file.getName();
                    String path = file.getPath();
                    try {
                        folder = folderFromPath(path, name);
                    } catch (Exception e) {
                        //Error
                        //Add to Unknown album
                        folder = "Unknown album";
                    }
                    //Put files to hashmap
                    List<File> old_files = folders.get(folder);
                    if (null == old_files) {
                        List<File> new_files = new ArrayList<>();
                        new_files.add(file);
                        folders.put(folder, new_files);
                    } else {
                        old_files.add(file);
                        folders.put(folder, old_files);
                    }
                }
                //SORT BY FOLDER DONE
                if(folders.size() == 0){
                    no_file(inflater, main_view, files_table);
                    return;
                }
                String[] keys = folders.keySet().toArray(new String[0]);
                int flag2 = 0;
                main_view.post(files_table::removeAllViews);
                for(int x = 0; x < folders.size(); x++){
                    View view_child = inflater.inflate(R.layout.files_child_folder, null);
                    TextView name_folder = view_child.findViewById(R.id.folder_name);
                    TextView info_folder = view_child.findViewById(R.id.folder_info);
                    TextView folder_icon = view_child.findViewById(R.id.folder_icon);
                    ImageView folder_image = view_child.findViewById(R.id.folder_image);
                    if(media_mode.equals("Photo")) {
                        folder_icon.setTextColor(ContextCompat.getColor(context, R.color.primary));
                        folder_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_photo_library_24));
                    }
                    if(media_mode.equals("Video")){
                        folder_icon.setTextColor(ContextCompat.getColor(context, R.color.primary));
                        folder_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_video_library_24));
                    }
                    if(media_mode.equals("Audio")){
                        folder_icon.setTextColor(ContextCompat.getColor(context, R.color.primary));
                        folder_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_library_music_24));
                    }
                    if(media_mode.equals("Docs")){
                        folder_icon.setTextColor(ContextCompat.getColor(context, R.color.primary));
                        folder_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_library_books_24));
                    }
                    name_folder.setText(keys[x]);
                    File target = (File) folders.get(keys[x]).get(0);
                    List<File> target_folder = folders.get(keys[x]);
                    if(null == target){
                        continue;
                    }
                    folder_icon.setText(get_folder_icon(keys[x])+" ");
                    String date = "";
                    int item_count = 0;
                    if(show_hidden) {
                        item_count = target_folder.size();
                    }
                    else {
                        for(File file : target_folder){
                            if(!file.isHidden()){
                                item_count++;
                            }
                        }
                    }
                    if(item_count > 0){
                        date = " - "+convertTime(target.lastModified());
                    }
                    String item = " item";
                    if(item_count > 1){
                        item = " items";
                    }
                    info_folder.setText(item_count+item+date);

                    //Events
                    view_child.setOnClickListener(v -> {
                        thread_loader(inflater, main_view, files_table, null, target_folder);
                    });
                    int finalX = x;
                    main_view.post(()-> {
                        files_table.addView(view_child);
                        float x2 = finalX +1, y = file_list.size(), r = (x2/y)*100;
                        int p = Math.round(r);
                        Home.loading_state.setProgress(Math.round(p));
                    });
                    flag2++;
                }
                if(flag2 == 0){
                    no_file(inflater, main_view, files_table);
                }
                else {
                    if(media_mode.equals("Docs")){
                        View doc_info = inflater.inflate(R.layout.doc_notice, null);
                        TextView t = doc_info.findViewById(R.id.doc_path);
                        t.setText("Looking in \""+doc_path+"\". To change, go to settings.");
                        main_view.post(()-> files_table.addView(doc_info));
                    }
                }
            }
            load_busy = false;
            main_view.post(()->{
                Home.loading_state.setVisibility(View.INVISIBLE);
                Home.loading_state.setProgress(0);
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void no_file(LayoutInflater inflater, View main_view, TableLayout files_table){
        View view = inflater.inflate(R.layout.no_files, null);
        main_view.post(() -> {
            files_table.addView(view);
            Home.loading_state.setVisibility(View.INVISIBLE);
            Home.loading_state.setProgress(0);
        });
        if(media_mode.equals("Docs")){
            View doc_info = inflater.inflate(R.layout.doc_notice, null);
            TextView t = doc_info.findViewById(R.id.doc_path);
            t.setText("Looking in \""+doc_path+"\". To change, go to settings.");
            main_view.post(()-> files_table.addView(doc_info));
        }
        load_busy = false;
    }
    private Boolean is_doc(String name){
        Boolean mode = false;
        String[] doc_accepted_extensions = {".pdf", ".docs", ".docx", ".txt", ".htm", ".html", ".xml", ".xls", ".xlsx"};
        for (String t : doc_accepted_extensions){
            if(name.lastIndexOf(t) > 0){
                mode = true;
                break;
            }
        }
        return mode;
    }

    List<File> return_able_file_list = new ArrayList<>();
    private void get_files(String path){
        File[] temp_list = File_selection.get_files(path);
        if(null != temp_list) {
            for (File file : temp_list) {
                if(file.isFile()) {
                    if (is_doc(file.getName())) {
                        return_able_file_list.add(file);
                    }
                }
            }
        }
    }

    public List<File> All_files(Context context) {
        if(media_mode.equals("Docs")){
            //Load documents
            get_files(doc_path);
            return return_able_file_list;
        }
        List<File> files = new ArrayList<>();
        try {
            String[] columns = new String[]{
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
            String sort_image = MediaStore.Images.Media.DATE_ADDED + " DESC";
            MergeCursor cursor;
            cursor = new MergeCursor(new Cursor[]{
                    context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, sort_image)
            });
            if(media_mode.equals("Video")){
                String[] columns1 = new String[]{
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media.BUCKET_ID,
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME};
                String sort_video = MediaStore.Video.Media.DATE_ADDED + " DESC";
                cursor = new MergeCursor(new Cursor[]{
                        context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns1, null, null, sort_video)
                });

            }
            if(media_mode.equals("Audio")){
                String[] columns2 = new String[]{
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DATE_ADDED
                };
                String sort_audio = MediaStore.Audio.Media.DATE_ADDED + " DESC";
                cursor = new MergeCursor(new Cursor[]{
                        context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns2, null, null, sort_audio)
                });
            }

            cursor.moveToFirst();
            files.clear();

            while (!cursor.isAfterLast()){
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                int lastPoint = path.lastIndexOf(".");
                path = path.substring(0, lastPoint) + path.substring(lastPoint).toLowerCase();
                files.add(new File(path));
                cursor.moveToNext();
            }
        } catch (Exception e) {
            //ERROR GETTING FILES
            //Handle
            return null;
        }
        if(null == files){
            Toast.makeText(context, "Failed to load files!", Toast.LENGTH_SHORT).show();
            return null;
        }
        return files;
    }
    public String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return format.format(date);
    }
    public String folderFromPath(String path, String name){
        String r_str = "", r_n_str = "";
        char ch;
        path = path.substring(0, path.indexOf(name)-1);
        for (int i=0; i< path.length(); i++)
        {
            ch = path.charAt(i);
            r_str = ch+r_str;
        }
        r_str = r_str.substring(0, r_str.indexOf("/"));
        int i = r_str.length()-1;
        while(i >= 0){
            ch = r_str.charAt(i);
            r_n_str = r_n_str+ch;
            i--;
        }
        return r_n_str;
    }
    public String get_folder_icon(String folder_name){
        switch (folder_name) {
            case "Screenshots":
            case "Pictures":
            case "Images":
                return "\\\uf302";
            case "Camera":
            case "DCIM":
                return "\\\uf030";
            case "Downloads":
            case "Download":
                return "\\\uf019";
            case "Telegram":
                return "\\\uf1d8";
            default:
                return "\\\uf07b";
        }
    }
}