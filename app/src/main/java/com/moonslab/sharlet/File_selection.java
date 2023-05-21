package com.moonslab.sharlet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.tabs.TabLayout;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class File_selection extends AppCompatActivity{
    TabLayout tabs;
    ViewPager2 selection_fragment_viewer;
    File_selection_adapter selection_adapter;
    static TextView select_count = null;
    public static ProgressBar loading_state;

    @Override
    protected void onResume() {
        super.onResume();
        selection_update();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_selection);

        loading_state = findViewById(R.id.files_loading);
        loading_state.setVisibility(View.INVISIBLE);
        loading_state.setProgress(0);

        //Free the selection bucket
        Home.clean_bundle_and_data();

        select_count = findViewById(R.id.select_file_title);
        LinearLayout done_button = findViewById(R.id.done_button);
        TextView back_button = findViewById(R.id.back_button);
        done_button.setOnClickListener(v -> {
            if(Home.done_selection(this, false)){
                this.finish();
            }
        });
        back_button.setOnClickListener(v -> finish());

        if(!all_permission_once()){
            Toast.makeText(getBaseContext(), "Storage permission/Restart is required!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            this.finish();
        }

        //Ready tabs
        //Push screen dimens
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        tabs = findViewById(R.id.tabs_layout);
        selection_fragment_viewer = findViewById(R.id.file_selection_fragment_view_pager);
        selection_adapter = new File_selection_adapter(this);
        selection_fragment_viewer.setAdapter(selection_adapter);
        selection_adapter.set_Context(this);
        selection_adapter.set_dimen(height, width);


        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selection_fragment_viewer.setCurrentItem(tab.getPosition());
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

    }
    //Check on result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK)
        {
            if(all_permission_once()){
                Intent intent = getIntent();
                this.finish();
                startActivity(intent);
            }
            else {
                Toast.makeText(getBaseContext(), "Permission is required!", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
        else {
            Toast.makeText(getBaseContext(), "Permission required. Try again.", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }
    //All permission handler
    //Return false on hold or no permission
    //Check on result

    //Return false on hold or no permission
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
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) < 0) {
                ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.QUERY_ALL_PACKAGES
                        },
                        01);
            }
        }

        return true;
    }

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
                r = false;
                break;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                r = false;
                break;
        }
        return r;
    }
    private void store_as_file(String file_name, String data , Context context) {
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
    private String read_from_file(String file_name, Context context) {
        try {
            String location = context.getFilesDir().getAbsolutePath()+"/"+file_name;
            return FileUtils.readFileToString(new File(location), StandardCharsets.UTF_8);
        }
        catch (IOException e){
            return null;
        }
    }

    public static void selection_update(){
        Home.selection_update_main();
    }

    public static File[] get_files(String path){
        File directory = new File(path);
        File[] files = directory.listFiles();
        return files;
    }
}