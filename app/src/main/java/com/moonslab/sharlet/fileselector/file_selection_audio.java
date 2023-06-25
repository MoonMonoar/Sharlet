package com.moonslab.sharlet.fileselector;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.moonslab.sharlet.Home.grid_select_all_child;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.moonslab.sharlet.DBHandler;
import com.moonslab.sharlet.File_selection;
import com.moonslab.sharlet.Home;
import com.moonslab.sharlet.R;
import com.moonslab.sharlet.See_all_files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class file_selection_audio extends Fragment {
    Context context;
    TableLayout table;
    Boolean load_hidden = true;
    private DBHandler dbHandler;
    public void set_Context(Context context_target){
        context = context_target;
    }
    //Passive code
    public List<File> All_files(Context context) {
        List<File> files = new ArrayList<>();
        List<String> file_paths = new ArrayList<>();
        try {
            final String[] columns = {
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DATE_ADDED
            };
            //Sort
            String sort_audio = MediaStore.Audio.Media.DATE_ADDED + " DESC";

            //Query sets
            MergeCursor cursor;
            cursor = new MergeCursor(new Cursor[]{
                    context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, null, null, sort_audio)
            });
            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                int lastPoint = path.lastIndexOf(".");
                path = path.substring(0, lastPoint) + path.substring(lastPoint).toLowerCase();
                files.add(new File(path));
                file_paths.add(path);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            //ERROR GETTING FILES
            //Handle
            return null;
        }
        return files;
    }
    public String folderFromPath(String path, String name){
        StringBuilder r_str = new StringBuilder();
        StringBuilder r_n_str = new StringBuilder();
        char ch;
        path = path.substring(0, path.indexOf(name)-1);
        for (int i=0; i< path.length(); i++)
        {
            ch = path.charAt(i);
            r_str.insert(0, ch);
        }
        r_str = new StringBuilder(r_str.substring(0, r_str.indexOf("/")));
        int i = r_str.length()-1;
        while(i >= 0){
            ch = r_str.charAt(i);
            r_n_str.append(ch);
            i--;
        }
        return r_n_str.toString();
    }
    View main_view;
    View empty;
    //Passive code -- ends
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        dbHandler = new DBHandler(context);
        // Inflate the layout for this fragment
        main_view = inflater.inflate(R.layout.fragment_file_selection_audio, container, false);
        table = main_view.findViewById(R.id.files_table);
        File_selection.loading_state.setVisibility(View.INVISIBLE);
        File_selection.loading_state.setProgress(0);
        //Separate thread
        new Thread(() -> {
            //Ready data
            List<File> files = All_files(context);
            if(null == files){
                return;
            }
            if(files.size() == 0){
                empty = inflater.inflate(R.layout.no_files, container, false);
                main_view.post(() -> table.addView(empty));
                return;
            }
            HashMap<String, List> folders = new HashMap<String, List>();
            String folder = "";
            for (File file : files) {
                //Look thorough every file and store data
                String name = file.getName();
                String path = file.getPath();
                if (null != name && null != path) {
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
                        List<File> new_files = new ArrayList<File>();
                        new_files.add(file);
                        folders.put(folder, new_files);
                    } else {
                        old_files.add(file);
                        folders.put(folder, old_files);
                    }
                }
            }
            for(int f = 0; f < folders.size(); f++) {
                String[] keys = folders.keySet().toArray(new String[folders.keySet().size()]);
                String folder_target = keys[f];
                List<File> files_in_folder = folders.get(folder_target);
                View title_view = inflater.inflate(R.layout.music_list_title,null);
                CheckBox checkBox = title_view.findViewById(R.id.select_all);
                TextView title = title_view.findViewById(R.id.album_name);
                TextView see_all = title_view.findViewById(R.id.see_all);
                title.setText(folder_target+" ("+files_in_folder.size()+")");
                main_view.post(() -> table.addView(title_view));
                if(files_in_folder.size() > 4) {
                    see_all.setTextColor(context.getColor(R.color.primary));
                    see_all.setOnClickListener(v -> {
                            ArrayList<String> files_paths_to_pass = new ArrayList<>();
                            for(int i = 4; i < files_in_folder.size(); i++){
                                File target = files_in_folder.get(i);
                                files_paths_to_pass.add(target.getPath());
                            }
                            Bundle b = new Bundle();
                            b.putStringArrayList("file_paths", files_paths_to_pass);
                            b.putString("folder_name", folder_target + " (" + files_in_folder.size() + ")");
                            Intent i = new Intent(context, See_all_files.class);
                            i.putExtras(b);
                            startActivity(i);
                    });
                }
                else {
                    see_all.setOnClickListener(v -> Toast.makeText(context, "No more files in this folder!", Toast.LENGTH_SHORT).show());
                }
                int x = 0;
                List<View> child_list = new ArrayList<>();
                for (File target : files_in_folder){
                    if(x == 4){
                        break;
                    }
                    if(target.exists()){
                        View child = inflater.inflate(R.layout.audio_selection_style, null);
                        TextView name = child.findViewById(R.id.file_name);
                        TextView info = child.findViewById(R.id.file_info);
                        name.setText(target.getName());
                        info.setText(folderFromPath(target.getPath(), target.getName()));
                        child.setOnClickListener(v -> {
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

                            Boolean selected_already = false;
                            //Not null, so look for the file
                            //if exists, its selected
                            for (String path : bucket_list) {
                                String path0 = target.getPath();
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

                            if(!selected_already) {
                                child.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                                //Add to the list convert to text and save
                                String path0 = target.getPath();
                                String home = "/storage";
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    home = Environment.getStorageDirectory().getPath();
                                }
                                path0 = path0.substring(home.length());
                                bucket_list.add(path0);
                                //New bucket
                                String new_data = null;
                                for(String path : bucket_list){
                                    if(path.equals("empty")){
                                        continue;
                                    }
                                    if(null == new_data){
                                        new_data = path;
                                    }
                                    else {
                                        new_data+= System.lineSeparator()+path;
                                    }
                                }
                                if(null != new_data){
                                    //Save the string
                                    try {
                                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                                        outputStreamWriter.write(new_data);
                                        outputStreamWriter.close();
                                    }
                                    catch (Exception e){
                                        Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            else {
                                child.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                                if(bucket_list.size() == 1){
                                    //Empty and return
                                    main_file.delete();
                                    File_selection.selection_update();
                                    return;
                                }
                                //New bucket
                                String new_data = null;
                                for(String path : bucket_list){
                                    String path0 = target.getPath();
                                    String home = "/storage";
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        home = Environment.getStorageDirectory().getPath();
                                    }
                                    path0 = path0.substring(home.length());
                                    if(path.equals("empty") || path.equals(path0)){
                                        continue;
                                    }
                                    if(null == new_data){
                                        new_data = path;
                                    }
                                    else {
                                        new_data+= System.lineSeparator()+path;
                                    }
                                }
                                if(null != new_data){
                                    //Save the string
                                    try {
                                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(main_file.toPath()));
                                        outputStreamWriter.write(new_data);
                                        outputStreamWriter.close();
                                    }
                                    catch (Exception e){
                                        Toast.makeText(context, "Can't unselect!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            File_selection.selection_update();
                        });
                        child.setOnLongClickListener(v -> {
                            Intent in = Home.get_music_intent(dbHandler, target.getPath(), context);
                            in.setFlags(FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(in);
                            return true;
                        });
                        main_view.post(() -> table.addView(child));
                        child_list.add(child);
                    }
                    x++;
                }
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    int count = files_in_folder.size();
                    File[] final_selection = new File[count];
                    int v = 0;
                    for(File file : files_in_folder){
                        final_selection[v] = file;
                        v++;
                    }
                    if(v == 0){
                        return;
                    }
                    if(!isChecked){
                        grid_select_all_child(null, final_selection, false, context);
                        for(View child : child_list){
                            child.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                        }
                    }
                    else {
                        grid_select_all_child(null, final_selection, true, context);
                        for(View child : child_list){
                            child.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                        }
                    }
                });
                float x2 = f+1, y = folders.size(), r = (x2/y)*100;
                int p = Math.round(r);
                if(load_hidden) {
                    main_view.post(()-> File_selection.loading_state.setVisibility(View.VISIBLE));
                    load_hidden = false;
                }
                main_view.post(()-> File_selection.loading_state.setProgress(p));
                if(folders.size() == x2){
                    main_view.post(()-> {
                        File_selection.loading_state.setVisibility(View.INVISIBLE);
                        File_selection.loading_state.setProgress(0);
                        load_hidden = true;
                    });
                }
            }
        }).start();
        //Return the view
        return main_view;
    }
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
}