package com.moonslab.sharlet.fileselector;

import static com.moonslab.sharlet.Home.grid_select_all_child;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.moonslab.sharlet.File_selection;
import com.moonslab.sharlet.File_selection_grid_adapter;
import com.moonslab.sharlet.Home;
import com.moonslab.sharlet.R;
import com.moonslab.sharlet.See_all_files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class file_selection_photo extends Fragment {
    Context context;
    int next_start;
    int scroll_total_folders = 0;
    boolean scroll_loader_busy = false;
    boolean scroll_ended = false;
    public void set_Context(Context context_target){
        context = context_target;
    }
    //Passive code
    public List<File> All_files(Context context) {
        List<File> files = new ArrayList<>();
        try {
            final String[] columns = {
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
            //Sort
            String sort_image = MediaStore.Images.Media.DATE_ADDED + " DESC";

            //Query sets
            MergeCursor cursor = null;
            cursor = new MergeCursor(new Cursor[]{
                    context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, sort_image)
            });

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
        if(folder_name.equals("Screenshots")
                || folder_name.equals("Pictures")
                || folder_name.equals("Images")){
            return "\\\uf302";
        }
        else if(folder_name.equals("Camera")
                || folder_name.equals("DCIM")){
            return "\\\uf030";
        }
        else if(folder_name.equals("Downloads") ||
                folder_name.equals("Download")){
            return "\\\uf019";
        }
        else if(folder_name.equals("Telegram")){
            return "\\\uf1d8";
        }
        else {
            return "\\\uf07b";
        }
    }
    //Passive code -- ends
    View main_view;
    View empty;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        main_view = inflater.inflate(R.layout.fragment_file_selection_photo, container, false);
        empty = inflater.inflate(R.layout.no_files, container, false);
        ScrollView main_scroll = main_view.findViewById(R.id.main_scroll);
        //Separate thread
        View finalMain_view = main_view;
        new Thread(() -> {
            //Ready data
            List<File> files = All_files(context);
            if(null == files){
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
            //SORT BY FOLDER DONE
            //Pagination control
            int ender = 15;
            scroll_total_folders = folders.keySet().size(); //This is the total spin
            if(scroll_total_folders < 15){
                ender = scroll_total_folders;
            }
            //Show 20 at a time
            next_start = pagination(0, ender, folders, inflater, finalMain_view); //Will return the last element id
            //Start the listener
            main_scroll.getViewTreeObserver()
                    .addOnScrollChangedListener(() -> {
                        if(scroll_ended){
                            return;
                        }
                        if (main_scroll.getChildAt(0).getBottom()
                                <= (main_scroll.getHeight() + main_scroll.getScrollY())) {
                            //scroll view is at bottom
                            //Load more from the previous last value
                            if(!scroll_loader_busy){
                                //Add next if has any
                                int ender1 = next_start+15;
                                if(ender1 > scroll_total_folders){
                                    //No more needed
                                    scroll_ended = true;
                                    ender1 = scroll_total_folders;
                                }
                                next_start = pagination(next_start, ender1, folders, inflater, finalMain_view); //Will return the last element id
                            }
                        }
                    });
                    main_view.post(()-> {
                        File_selection.loading_state.setVisibility(View.INVISIBLE);
                        File_selection.loading_state.setProgress(0);
                    });
        }).start();

        //Return the view
        return main_view;
    }

    //Pagination
    public int pagination(int start, int end, HashMap<String, List> folders,
                          LayoutInflater inflater, View main_view){
        int flag_count = 0;
        scroll_loader_busy = true;
        for (int f = start; f < end; f++) {

            String[] keys = folders.keySet().toArray(new String[folders.keySet().size()]);
            String folder_target = keys[f];

            List<File> files_in_folder = folders.get(folder_target);
            //ADD NOT MORE THEN 8 FILES
            int total_files = files_in_folder.size();
            if (total_files == 0) {
                continue;
                //No need to bother for empty folders
            }
            View table_element = inflater.inflate(R.layout.files_selection_table_elements, null);

            File[] files_to_make_grid, files_to_select;
            files_to_make_grid = new File[files_in_folder.size()];
            files_in_folder.toArray(files_to_make_grid);
            files_to_select = files_to_make_grid;
            int final_total_files = total_files;
            if (total_files > 4) {
                if (total_files > 8) {
                    total_files = 8;
                }
                //BUT ONLY KEEP FIRST 8 THEN BREAK
                files_to_make_grid = Arrays.copyOfRange(files_to_make_grid, 0, total_files);
            }
            File_selection_grid_adapter adapter = new File_selection_grid_adapter(context, files_to_make_grid);

            TextView see_all = table_element.findViewById(R.id.see_all);
            if(final_total_files > 8) {
                //Set see all
                see_all.setOnClickListener(v -> {
                    ArrayList<String> files_paths_to_pass = new ArrayList<>();
                    int flag_count1 = 0;
                    for(File file_x : files_to_select){
                        if(flag_count1 < 8){
                            flag_count1++;
                            continue;
                        }
                        files_paths_to_pass.add(file_x.getPath());
                    }
                    Bundle b = new Bundle();
                    b.putStringArrayList("file_paths", files_paths_to_pass);
                    b.putString("folder_name", folder_target + " (" + final_total_files + ")");
                    Intent i = new Intent(context, See_all_files.class);
                    i.putExtras(b);
                    startActivity(i);
                });
            }
            else {
                see_all.setTextColor(ContextCompat.getColor(context, R.color.grey));
                see_all.setOnClickListener(v -> Toast.makeText(context, "No more files in this folder!", Toast.LENGTH_SHORT).show());
            }

            //Set icon
            TextView virtual_icon_name = (TextView) table_element.findViewById(R.id.folder_icon);
            virtual_icon_name.setText(get_folder_icon(folder_target));
            //Set folder name
            TextView virtual_folder_name = (TextView) table_element.findViewById(R.id.folder_name);
            virtual_folder_name.setText("(" + final_total_files + ") " + folder_target);

            //Set grid
            GridView virtual_grid = (GridView) table_element.findViewById(R.id.files_grid);
            if(total_files > 4){
                ViewGroup.LayoutParams layoutParams = virtual_grid.getLayoutParams();
                layoutParams.height = Home.convertDpToPixels(265, context);
                virtual_grid.setLayoutParams(layoutParams);
            }

            virtual_grid.setAdapter(adapter);

            //On check
            CheckBox check_all = table_element.findViewById(R.id.select_all);
            check_all.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    grid_select_all_child(virtual_grid, files_to_select, true, context);
                }
                else
                {
                    grid_select_all_child(virtual_grid, files_to_select, false, context);
                }
            });

            main_view.post(() -> {
                TableLayout files_table = main_view.findViewById(R.id.files_table);
                RelativeLayout loader = main_view.findViewById(R.id.file_selection_loading);
                files_table.addView(table_element);
                if(start == 0) {
                    loader.setVisibility(View.GONE);
                }
            });
            flag_count++;
        }
        scroll_loader_busy = false;
        if(flag_count == 0 && start == 0){
            main_view.post(() -> {
                TableLayout files_table = main_view.findViewById(R.id.files_table);
                files_table.removeAllViews();
                RelativeLayout loader = main_view.findViewById(R.id.file_selection_loading);
                loader.setVisibility(View.GONE);
                files_table.addView(empty);
            });
        }
        return start+flag_count;
    }
}