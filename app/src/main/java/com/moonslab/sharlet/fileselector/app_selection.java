package com.moonslab.sharlet.fileselector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.moonslab.sharlet.DBHandler;
import com.moonslab.sharlet.File_selection;
import com.moonslab.sharlet.Home;
import com.moonslab.sharlet.R;
import com.moonslab.sharlet.Receive;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class app_selection extends Fragment {
    Context context;
    Boolean is_home = false;
    public void set_home(Boolean mode){
        is_home = mode;
    }
    public void set_Context(Context context_target){context = context_target;}
    View main_view;
    private DBHandler dbHandler;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        main_view = inflater.inflate(R.layout.app_selection, container, false);
        ScrollView main_scroll = main_view.findViewById(R.id.main_scroll);
        TableLayout apps_table = main_view.findViewById(R.id.apps_table);
        dbHandler = new DBHandler(context);

        if(is_home){
            Home.loading_state.setVisibility(View.INVISIBLE);
            Home.loading_state.setProgress(0);
        }
        else {
            File_selection.loading_state.setVisibility(View.INVISIBLE);
            File_selection.loading_state.setProgress(0);
        }

        List<ApplicationInfo> user_apps = new ArrayList<>();
        final List<ApplicationInfo>[] device_apps = new List[]{new ArrayList<>()};

        final PackageManager pm = context.getPackageManager();
        @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        if(null == packages || packages.size() == 0){
            Toast.makeText(context, "Unable to load apps!", Toast.LENGTH_LONG).show();
        }

        if(null != packages) {
            for (ApplicationInfo packageInfo : packages) {
                if ((packageInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0) {
                    device_apps[0].add(packageInfo);
                } else {
                    user_apps.add(packageInfo);
                }
            }
        }

        class MyThread extends Thread {
            @SuppressLint("SetTextI18n")
            public void run() {
                //Add title view
                int spin = 0;
                if(null == dbHandler.get_settings("hide_system_apps") || dbHandler.get_settings("hide_system_apps").equals("true")){
                    device_apps[0] = new ArrayList<>();
                }
                int spin_total = user_apps.size() + device_apps[0].size();
                if (user_apps.size() > 0) {
                    View selection_child = inflater.inflate(R.layout.app_selection_title_user, null);
                    TextView info = selection_child.findViewById(R.id.info);
                    info.setOnClickListener(v -> {
                        AlertDialog.Builder alert = new AlertDialog.Builder(context);
                        alert.setTitle("Installed apps");
                        alert.setMessage("These apps were downloaded and were not added by the vendor of this device.");
                        alert.setNegativeButton("Okay",
                                (dialog, whichButton) -> {
                                    //Do nothing
                                });
                        alert.show();
                    });
                    main_view.post(() -> apps_table.addView(selection_child));
                }
                if(is_home){
                    main_view.post(()-> Home.loading_state.setVisibility(View.VISIBLE));
                }
                else {
                    main_view.post(()-> File_selection.loading_state.setVisibility(View.VISIBLE));
                }

                for (int x = 0; x < user_apps.size(); x++) {
                    //Copy the .apk file to wherever
                    ApplicationInfo packageInfo = user_apps.get(x);
                    File file = new File(packageInfo.sourceDir);
                    //Add views directly to the table
                    View selection_child = inflater.inflate(R.layout.apk_selection_child, null);
                    TextView apk_name = selection_child.findViewById(R.id.apk_name);
                    TextView apk_info = selection_child.findViewById(R.id.apk_info);
                    ImageView apk_icon = selection_child.findViewById(R.id.apk_icon);
                    apk_info.setText(Receive.format_size(file.length()) + " - " + packageInfo.packageName);
                    apk_name.setText(pm.getApplicationLabel(packageInfo));
                    try {
                        apk_icon.setImageDrawable(pm.getApplicationIcon(packageInfo.packageName));
                    } catch (PackageManager.NameNotFoundException e) {
                        //Do nothing!
                    }
                    selection_child.setOnLongClickListener(v -> {
                        AlertDialog.Builder alert = new AlertDialog.Builder(context);
                        alert.setTitle("Sure open app?");
                        alert.setMessage("Do you want to open "+pm.getApplicationLabel(packageInfo)+" now?");
                        alert.setPositiveButton("Open", (dialog, whichButton) -> startActivity(pm.getLaunchIntentForPackage(packageInfo.packageName)));
                        alert.setNegativeButton("Close",
                                (dialog, whichButton) -> {
                                    //Do nothing
                                });
                        alert.show();
                        return true;
                    });
                    selection_child.setOnClickListener(v -> {
                        String location = Home.get_app_home_bundle_data_store()+"/"+pm.getApplicationLabel(packageInfo)+".apk";
                        File main_file = new File(location);
                        try{
                            Home.copy_file(file, main_file, context);
                            //Single select
                            select_app(main_file, selection_child);
                        }
                        catch (Exception e){
                            //Error
                            main_view.post(() -> Toast.makeText(context, "Failed to get app!", Toast.LENGTH_SHORT).show());
                        }
                    });
                    int finalSpin = spin;
                    main_view.post(() -> {
                        float x2 = finalSpin + 1, y = spin_total, r = (x2 / y) * 100;
                        int p = Math.round(r);
                        if(is_home) {
                            Home.loading_state.setProgress(p);
                        }
                        else {
                            File_selection.loading_state.setProgress(p);
                        }
                        apps_table.addView(selection_child);
                    });
                    spin++;
                }

                //Add title view
                if (device_apps[0].size() > 0) {
                    View selection_child = inflater.inflate(R.layout.app_selection_title_user, null);
                    TextView info = selection_child.findViewById(R.id.info);
                    TextView title = selection_child.findViewById(R.id.title);
                    title.setText("System apps");
                    info.setOnClickListener(v -> {
                        AlertDialog.Builder alert = new AlertDialog.Builder(context);
                        alert.setTitle("System apps");
                        alert.setMessage("These apps were added by the vendor of this device. Sharlet is not sure if these will work on other deices!");
                        alert.setNegativeButton("Okay",
                                (dialog, whichButton) -> {
                                    //Do nothing
                                });
                        alert.show();
                    });
                    main_view.post(() -> apps_table.addView(selection_child));
                }

                for (int x = 0; x < device_apps[0].size(); x++) {
                    //Copy the .apk file to wherever
                    ApplicationInfo packageInfo = device_apps[0].get(x);
                    File file = new File(packageInfo.sourceDir);
                    //Add views directly to the table
                    View selection_child = inflater.inflate(R.layout.apk_selection_child, null);
                    TextView apk_name = selection_child.findViewById(R.id.apk_name);
                    TextView apk_info = selection_child.findViewById(R.id.apk_info);
                    ImageView apk_icon = selection_child.findViewById(R.id.apk_icon);
                    String pname = packageInfo.packageName;
                    if(null == pname || pname.isEmpty()){
                        pname = "Unknown package";
                    }
                    else {
                            pname = " - " + pname;
                    }
                    apk_info.setText(Receive.format_size(file.length()) + pname);
                    apk_name.setText(pm.getApplicationLabel(packageInfo));
                    try {
                        apk_icon.setImageDrawable(pm.getApplicationIcon(packageInfo.packageName));
                    } catch (PackageManager.NameNotFoundException e) {
                        //Do nothing!
                    }
                    selection_child.setOnLongClickListener(v -> {
                        AlertDialog.Builder alert = new AlertDialog.Builder(context);
                        alert.setTitle("Can't open system app!");
                        alert.setMessage("Sorry, opening system apps directly may result in crashing!");
                        alert.setNegativeButton("Close",
                                (dialog, whichButton) -> {
                                    //Do nothing
                                });
                        alert.show();
                        return true;
                    });
                    selection_child.setOnClickListener(v -> {
                        String location = Home.get_app_home_bundle_data_store() + "/" + pm.getApplicationLabel(packageInfo) + ".apk";
                        File main_file = new File(location);
                        try {
                            Home.copy_file(file, main_file, context);
                            //Single select
                            select_app(main_file, selection_child);
                        } catch (Exception e) {
                            //Error
                            main_view.post(() -> Toast.makeText(context, "Failed to get app!", Toast.LENGTH_SHORT).show());
                        }
                    });
                    int finalSpin1 = spin;
                    main_view.post(() -> {
                        float x2 = finalSpin1 + 1, y = spin_total, r = (x2 / y) * 100;
                        int p = Math.round(r);
                        if (is_home) {
                            Home.loading_state.setProgress(p);
                        }
                        else {
                            File_selection.loading_state.setProgress(p);
                        }
                        apps_table.addView(selection_child);
                    });
                    spin++;
                }

                if(spin == spin_total){
                    if(is_home){
                        main_view.post(()-> Home.loading_state.setVisibility(View.INVISIBLE));
                    }
                    else {
                        main_view.post(()-> File_selection.loading_state.setVisibility(View.INVISIBLE));
                    }
                    main_view.post(() -> main_scroll.setVisibility(View.VISIBLE));
                }
            }
        };
        new MyThread().start();

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
    private String read_from_file(String file_name, Context context) {
        try {
            String location = context.getFilesDir().getAbsolutePath()+"/"+file_name;
            return FileUtils.readFileToString(new File(location), StandardCharsets.UTF_8);
        }
        catch (IOException e){
            return null;
        }
    }
    private void select_app(File target_file, View view_child){
        List<String> bucket_list = new ArrayList<>();
        String location = Home.get_app_home_bundle_data_store()+"/Selection_bucket.txt";
        File main_file = new File(location);
        if(!main_file.exists()){
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(main_file));
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
                view_child.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            if(bucket_list.size() == 1){
                //Empty and return
                main_file.delete();
                if(!is_home) {
                    File_selection.selection_update();
                }
            }
            //New bucket
            String new_data = null;
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
                    new_data = path;
                }
                else {
                    new_data+= System.lineSeparator()+path;
                }
            }
            if(null != new_data){
                //Save the string
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(main_file));
                    outputStreamWriter.write(new_data);
                    outputStreamWriter.close();
                }
                catch (Exception e){
                    Toast.makeText(context, "Can't unselect!", Toast.LENGTH_SHORT).show();
                }
            }
            //Delete file
            if(target_file.exists()){
                target_file.delete();
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
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(main_file));
                    outputStreamWriter.write(new_data);
                    outputStreamWriter.close();
                }
                catch (Exception e){
                    Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        File_selection.selection_update();
    }
}