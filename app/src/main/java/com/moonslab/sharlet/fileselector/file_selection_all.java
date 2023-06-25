package com.moonslab.sharlet.fileselector;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.moonslab.sharlet.Home.grid_select_all_child;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import com.moonslab.sharlet.Music_player;
import com.moonslab.sharlet.Photo_view;
import com.moonslab.sharlet.R;
import com.moonslab.sharlet.Receive;
import com.moonslab.sharlet.Video_player;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class file_selection_all extends Fragment {
    Context context;
    List<String> selected_folder_paths = new ArrayList<>();
    TextView current_folder;
    TableLayout files_folders;
    LayoutInflater inflater2;
    File[] file_list = null;
    String current_path;
    ScrollView main_scroll;
    Boolean show_hidden = false;
    Boolean back_hold = false;
    Boolean latest_first = true;
    TextView folder_prev;
    Boolean home_call = false;
    private DBHandler dbHandler;

    public void set_Context(Context context_target){
        context = context_target;
    }

    public void is_from_home(Boolean flag){
        if(flag){
            home_call = true;
        }
    }

    View main_view;
    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        main_view = inflater.inflate(R.layout.fragment_file_selection_all, container, false);
        dbHandler = new DBHandler(context);
        //Components
        TextView capacity = main_view.findViewById(R.id.capacity);
        ProgressBar left_percent = main_view.findViewById(R.id.storage_left);
        current_folder = main_view.findViewById(R.id.current_folder);
        files_folders = main_view.findViewById(R.id.files_folders);
        main_scroll = main_view.findViewById(R.id.main_scroll);
        current_folder.setText("Home");
        inflater2 = inflater;

        if(home_call){
            Home.loading_state.setVisibility(View.INVISIBLE);
            Home.loading_state.setProgress(0);
        }
        else {
            //Default
            File_selection.loading_state.setVisibility(View.INVISIBLE);
            File_selection.loading_state.setProgress(0);
        }

        String tester = dbHandler.get_settings("latest_files");
        if(null != tester && tester.equals("false")){
            latest_first = false;
        }

        String tester4 = dbHandler.get_settings("show_hidden");
        if(null != tester4 && tester4.equals("true")){
            show_hidden = true;
        }

        RelativeLayout main = main_view.findViewById(R.id.storage_main);
        main.setOnClickListener(v -> {
            load_storage_location(Environment.getExternalStorageDirectory().toString());
            Toast.makeText(context, "Storage home", Toast.LENGTH_SHORT).show();
        });
        TextView settings = main_view.findViewById(R.id.storage_settings);
        settings.setOnClickListener(v -> {
            //Dialogue
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.storage_settings);
            dialog.getWindow().setBackgroundDrawable(context.getDrawable(R.drawable.confirm_dialog_background));
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            //Options
            CheckBox hidden_file_switch = dialog.getWindow().findViewById(R.id.show_hidden_files);
            TextView text_hidden_file = dialog.getWindow().findViewById(R.id.show_hidden_files_text);

            CheckBox sort = dialog.getWindow().findViewById(R.id.latest_first);
            TextView sort_text = dialog.getWindow().findViewById(R.id.latest_first_text);

            TextView close = dialog.getWindow().findViewById(R.id.close);

            //Pre check
            String tester3 = dbHandler.get_settings("show_hidden");
            if(null != tester3 && tester3.equals("true")){
                show_hidden = true;
                hidden_file_switch.setChecked(true);
            }
            else {
                show_hidden = false;
                hidden_file_switch.setChecked(false);
            }

            String tester2 = dbHandler.get_settings("latest_files");
            if(null != tester2 && tester2.equals("false")){
                latest_first = false;
                sort.setChecked(false);
            }
            else {
                latest_first = true;
                sort.setChecked(true);
            }

            sort_text.setOnClickListener(v12 -> sort.toggle());

            text_hidden_file.setOnClickListener(v1 -> hidden_file_switch.toggle());

            hidden_file_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked){
                    dbHandler.add_setting("show_hidden", "true");
                    show_hidden = true;
                }
                else {
                    dbHandler.add_setting("show_hidden", "false");
                    show_hidden = false;
                }
                //Concurrently reload current location
                load_storage_location(current_path);
            });

            sort.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked){
                    latest_first = true;
                    dbHandler.add_setting("latest_files", "true");
                }
                else {
                    latest_first = false;
                    dbHandler.add_setting("latest_files", "false");
                }
                //Concurrently reload current location
                load_storage_location(current_path);
            });

            close.setOnClickListener(v13 -> dialog.dismiss());
            dialog.show();
        });

        //Info
        File externalStorageDir = Environment.getExternalStorageDirectory();
        float free = externalStorageDir.getFreeSpace();
        float total = externalStorageDir.getTotalSpace();
        float used = total-free;
        float percent = 100 - Math.round((free/total)*100);
        capacity.setText(Receive.format_size((long) used)+"/"+Receive.format_size((long) total));
        left_percent.setProgress((int) percent);

        folder_prev = main_view.findViewById(R.id.folder_prev);
        folder_prev.setOnClickListener(v -> {
            if(back_hold){
                Toast.makeText(context, "Loading...Please wait", Toast.LENGTH_SHORT).show();
                return;
            }
            String new_path;
            String home = Environment.getExternalStorageDirectory().toString();
            if(!current_path.equals(home)) {
                new_path = current_path.substring(0, current_path.lastIndexOf("/"));
            }
            else {
                new_path = home;
                    Toast.makeText(context, "No more parent folders!", Toast.LENGTH_SHORT).show();
            }
            load_storage_location(new_path);
        });

        //Load home
        load_storage_location(Environment.getExternalStorageDirectory().toString());

        //Return the view
        return main_view;
    }

    @SuppressLint("SetTextI18n")
    private void load_storage_location(String path){
    if(back_hold){
            Toast.makeText(context, "Loading...Please wait", Toast.LENGTH_SHORT).show();
            return;
    }
    class MyThread extends Thread {
        public void run() {
            back_hold = true;
            main_view.post(() -> {folder_prev.setText("\\\uf2f1");
            if(home_call){
                Home.loading_state.setVisibility(View.VISIBLE);
            }
            else {
                File_selection.loading_state.setVisibility(View.VISIBLE);
            }
            });
            String home = Environment.getExternalStorageDirectory().toString();
            View empty = inflater2.inflate(R.layout.files_folders_load, null);
            TextView load_text;
            View finalEmpty1 = empty;
            main_view.post(() -> {
                        files_folders.addView(finalEmpty1);
            });
            file_list = File_selection.get_files(path);
            if (null != file_list) {
                current_path = path;
            } else {
                back_hold = false;
                main_view.post(() -> folder_prev.setText("\\\uf060"));
                return;
            }
            main_view.post(() -> current_folder.setText(path.replace(home, "Home").replace("/", " / ")));
            //Load the files
            if (file_list.length == 0) {
                    main_view.post(() -> {
                            files_folders.removeAllViews();
                    });
                    empty = inflater2.inflate(R.layout.files_folders_load, null);
                    load_text = empty.findViewById(R.id.files_loading);
                    TextView load_icon = empty.findViewById(R.id.load_icon);
                    load_text.setText("Empty folder");
                    load_icon.setText("\\\uf65d");
                    if(home_call){
                        main_view.post(() -> Home.loading_state.setVisibility(View.INVISIBLE));
                    }
                    else {
                        main_view.post(() -> File_selection.loading_state.setVisibility(View.INVISIBLE));
                    }
                    View finalEmpty = empty;
                    main_view.post(() -> {
                        files_folders.addView(finalEmpty);
                    });
                back_hold = false;
                main_view.post(() -> folder_prev.setText("\\\uf060"));
                return;
            }

            main_view.post(() -> files_folders.removeAllViews());

            //Sort by time
            if(latest_first) {
                File temp;
                for (int x = 0; x < file_list.length; x++) {
                    for (int y = x + 1; y < file_list.length; y++) {
                        if (file_list[x].lastModified() < file_list[y].lastModified()) {
                            temp = file_list[x];
                            file_list[x] = file_list[y];
                            file_list[y] = temp;
                        }
                    }
                }
            }

            //Folders first
            List<File> temp_files = new ArrayList<>();
            List<File> temp_folders = new ArrayList<>();
            for(File target : file_list){
                if(target.isDirectory()){
                    temp_folders.add(target);
                }
                else {
                    temp_files.add(target);
                }
            }
            //Re assign
            file_list = new File[temp_files.size()+temp_folders.size()];
            int z = 0;
            for(File file : temp_folders){
                file_list[z] = file;
                z++;
            }
            for(File file : temp_files){
                file_list[z] = file;
                z++;
            }

            int safety_flag = 0;
            for(int x = 0; x < file_list.length; x++){
                if(!show_hidden){
                    if(file_list[x].isHidden()){
                        safety_flag++;
                        continue;
                    }
                }
                if(file_list[x].isDirectory()){
                    //Its a directory
                    View view_child = inflater2.inflate(R.layout.files_child_folder, null);
                    TextView name_folder = view_child.findViewById(R.id.folder_name);
                    TextView info_folder = view_child.findViewById(R.id.folder_info);
                    name_folder.setText(file_list[x].getName());
                    File[] target = new File(file_list[x].getPath()).listFiles();
                    if(null == target){
                        continue;
                    }
                    String date = "";
                    int item_count = 0;
                    if(show_hidden) {
                        item_count = target.length;
                    }
                    else {
                        for(File file : target){
                            if(!file.isHidden()){
                                item_count++;
                            }
                        }
                    }
                    if(item_count > 0){
                        date = " - "+Home.convertTime(target[0].lastModified());
                    }
                    String item = " item";
                    if(item_count > 1){
                        item = " items";
                    }
                    info_folder.setText(item_count+item+date);
                    int finalX = x;
                    File[] finalFile_list1 = file_list;
                    view_child.setOnClickListener(v -> load_storage_location(finalFile_list1[finalX].getPath()));
                    File[] finalFile_list = file_list;
                    view_child.setOnLongClickListener(v -> {
                        explorer_select_folder(finalFile_list[finalX].getPath(), view_child);
                        return true;
                    });
                    //Pre check
                    if(selected_folder_paths.contains(file_list[finalX].getPath())){
                        //Was selected
                        view_child.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                    }
                    int finalX2 = x;
                    main_view.post(() -> {
                        files_folders.addView(view_child);
                        float x2 = finalX2 +1, y = file_list.length, r = (x2/y)*100;
                        int p = Math.round(r);
                        if(home_call){
                            Home.loading_state.setProgress(p);
                        }
                        else {
                            File_selection.loading_state.setProgress(p);
                        }
                    });
                }
                else {
                    //Must be file
                    File target_file = file_list[x];
                    if(null == target_file){
                        continue;
                    }
                    String file_name = target_file.getName();
                    String File_extension = FilenameUtils.getExtension(file_name).toLowerCase(Locale.ROOT);
                    View view_child = inflater2.inflate(R.layout.files_child_photo, null);
                    TextView name_file = view_child.findViewById(R.id.file_name);
                    TextView info_file = view_child.findViewById(R.id.file_info);
                    ImageView thumb = view_child.findViewById(R.id.thumb);
                    info_file.setText(Receive.format_size(target_file.length())+" - "+Home.convertTime(target_file.lastModified()));
                    if (File_extension.equals("png")
                            || File_extension.equals("jpg")
                            || File_extension.equals("gif")
                            || File_extension.equals("jpeg")
                            || File_extension.equals("heic")
                            || File_extension.equals("webp")
                            || File_extension.equals("tiff")
                            || File_extension.equals("raw")) {
                        //Image
                        main_view.post(() -> Picasso.get().load(target_file).placeholder(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(thumb));
                        name_file.setText(file_name);
                        view_child.setOnLongClickListener(v -> {
                            Intent intent = new Intent(context, Photo_view.class);
                            //Save the file first
                            store_as_file("Image_last.txt", target_file.getPath(), context);
                            context.startActivity(intent);
                            return true;
                        });
                    } else if (File_extension.equals("mp3")
                            || File_extension.equals("wav")
                            || File_extension.equals("ogg")
                            || File_extension.equals("m4a")
                            || File_extension.equals("aac")
                            || File_extension.equals("alac")
                            || File_extension.equals("aiff")) {
                        try {
                            android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                            mmr.setDataSource(target_file.getPath());
                            byte[] data = mmr.getEmbeddedPicture();
                            if (data != null) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                bitmap = Home.getCroppedBitmap(bitmap);
                                Drawable d = new BitmapDrawable(getResources(), bitmap);
                                thumb.setImageDrawable(d);
                            } else {
                                thumb.setImageResource(R.drawable.ic_baseline_audio_file_24);
                            }
                        }
                        catch (Exception e){
                            thumb.setImageResource(R.drawable.ic_baseline_audio_file_24);
                        }
                        name_file.setText(file_name);
                        view_child.setOnLongClickListener(v -> {
                            Intent in = Home.get_music_intent(dbHandler, target_file.getPath(), context);
                            in.setFlags(FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(in);
                            return true;
                        });
                    } else if (File_extension.equals("mp4")
                            || File_extension.equals("mkv")
                            || File_extension.equals("flv")
                            || File_extension.equals("avi")
                            || File_extension.equals("webm")
                            || File_extension.equals("mov")) {
                        main_view.post(() -> Glide.with(context)
                                .load(target_file)
                                .placeholder(R.drawable.ic_baseline_video_file_24)
                                .into(thumb));
                        name_file.setText(file_name);
                        view_child.setOnLongClickListener(v -> {
                            Intent intent = new Intent(context, Video_player.class);
                            store_as_file("Video_last.txt", target_file.getPath(), context);
                            context.startActivity(intent);
                            return true;
                        });
                    } else {
                        if(File_extension.equals("apk")){
                            thumb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_android_24));
                        }
                        name_file.setText(file_name);
                        view_child.setOnLongClickListener(v -> {
                            Toast.makeText(context, "Opening...", Toast.LENGTH_SHORT).show();
                            Home.openFile(context, target_file);
                            return true;
                        });
                    }
                    view_child.setOnClickListener(v -> explorer_select_single(target_file, view_child, target_file.getParent()));

                    //Recheck
                    if(is_selected(target_file, context)){
                        view_child.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                    }
                    else {
                        view_child.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                        view_child.setBackground(ContextCompat.getDrawable(context, R.drawable.line));
                    }

                    int finalX1 = x;
                    main_view.post(() -> {
                        files_folders.addView(view_child);
                        float x2 = finalX1 +1, y = file_list.length, r = (x2/y)*100;
                        int p = Math.round(r);
                        if(home_call){
                            Home.loading_state.setProgress(p);
                        }
                        else {
                            File_selection.loading_state.setProgress(p);
                        }
                    });

                }
                safety_flag++;
            }
            if(safety_flag == 0){
                //Empty
                main_view.post(() -> {
                    View empty1;
                    files_folders.removeAllViews();
                    empty1 = inflater2.inflate(R.layout.files_folders_load, null);
                    TextView load_text1, load_text2;
                    load_text1 = empty1.findViewById(R.id.files_loading);
                    load_text1.setText("Empty folder");
                    load_text2 = empty1.findViewById(R.id.load_icon);
                    load_text2.setText("\\\uf65d");
                    files_folders.addView(empty1);
                    if(home_call){
                        main_view.post(() -> Home.loading_state.setVisibility(View.INVISIBLE));
                    }
                    else {
                        main_view.post(() -> File_selection.loading_state.setVisibility(View.INVISIBLE));
                    }
                });
            }
                back_hold = false;
                main_view.post(() -> {
                    folder_prev.setText("\\\uf060");
                    if(home_call){
                        Home.loading_state.setVisibility(View.INVISIBLE);
                    }
                    else {
                        File_selection.loading_state.setVisibility(View.INVISIBLE);
                    }
                });
                //main_scroll.post(() -> main_scroll.fullScroll(View.FOCUS_UP));
            }
    }
    new MyThread().start();
    }

    //Helpers
    public static Boolean is_selected(File target_file, Context context){
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
                e.printStackTrace();
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

        Boolean selected_already = false;
        //Not null, so look for the file
        //if exists, its selected
        for (String path : bucket_list) {
            String path0 = target_file.getPath();
            String home = "/storage";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                home = Environment.getStorageDirectory().getPath();
            }
            path0 = path0.substring(home.length());
            if (path.equals(path0)) {
                selected_already = true;
                break;
            }
        }
        return selected_already;
    }
    private void explorer_select_single(File target_file, View view_child, String folder_path){
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
                e.printStackTrace();
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

        boolean selected_already = false;
        //Not null, so look for the file
        //if exists, its selected
        assert bucket_list != null;
        for (String path : bucket_list) {
            String path0 = target_file.getPath();
            String home = "/storage";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                home = Environment.getStorageDirectory().getPath();
            }
            path0 = path0.substring(home.length());
            if (path.equals(path0)) {
                selected_already = true;
                break;
            }
        }

        if(selected_already){
            view_child.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            view_child.setBackground(ContextCompat.getDrawable(context, R.drawable.line));
            selected_folder_paths.remove(folder_path);
            if(bucket_list.size() == 1){
                //Empty and return
                main_file.delete();
                File_selection.selection_update();
                return;
            }
            //New bucket
            StringBuilder new_data = null;
            for(String path : bucket_list){
                String path0 = target_file.getPath();
                String home = "/storage";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    home = Environment.getStorageDirectory().getPath();
                }
                path0 = path0.substring(home.length());
                if(path.equals("empty") || path.equals(path0)){
                    continue;
                }
                if(null == new_data){
                    new_data = new StringBuilder(path);
                }
                else {
                    new_data.append(System.lineSeparator()).append(path);
                }
            }
            if(null != new_data){
                //Save the string
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                    outputStreamWriter.write(new_data.toString());
                    outputStreamWriter.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        else {
            view_child.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
            String path0 = target_file.getPath();
            String home = "/storage";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                home = Environment.getStorageDirectory().getPath();
            }
            path0 = path0.substring(home.length());
            bucket_list.add(path0);
            //New bucket
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
            if(null != new_data){
                //Save the string
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                    outputStreamWriter.write(new_data.toString());
                    outputStreamWriter.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        File_selection.selection_update();
    }
    private void explorer_select_folder(String path, View view_child){
        File[] file_list = File_selection.get_files(path);
        int count = 0;
        if(null == file_list){
            return;
        }
        if(file_list.length == 0){
            return;
        }
        int flag = 0;
        boolean hidden_factor = false;
        List<File> selections = new ArrayList<>();
        for (File value : file_list) {
            if (value.isHidden() && !show_hidden) {
                hidden_factor = true;
                continue;
            }
            if (value.isFile()) {
                selections.add(value);
                flag++;
            }
        }
        count = selections.size();
        File[] final_selection = new File[count];
        int v = 0;
        for(File file : selections){
            final_selection[v] = file;
            v++;
        }
        if(flag == 0){
            if(!hidden_factor) {
                Toast.makeText(context, "Folder has only sub folders!", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, "Empty folder!", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        else {
            File_selection.selection_update();
        }
        if(selected_folder_paths.contains(path)){
            //Was selected
            view_child.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            view_child.setBackground(ContextCompat.getDrawable(context, R.drawable.line));
            //Remove
            selected_folder_paths.remove(path);
            grid_select_all_child(null, final_selection, false, context);
        }
        else {
            if(count > 0) {
                view_child.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                selected_folder_paths.add(path);
            }
            grid_select_all_child(null, final_selection, true, context);
        }
        File_selection.selection_update();
    }
    //Don't delete these


    private void store_as_file(String file_name, String data , Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file_name, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Toast.makeText(context, "Error loading file!", Toast.LENGTH_SHORT).show();
        }
    }
    private String read_from_file(String file_name, Context context) {
        try {
            String location = context.getFilesDir().getAbsolutePath()+"/"+file_name;
            return FileUtils.readFileToString(new File(location), StandardCharsets.UTF_8);
        }
        catch (IOException e){
            return null;
        }
    }
}