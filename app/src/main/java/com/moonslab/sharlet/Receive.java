package com.moonslab.sharlet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Receive extends AppCompatActivity {

    //Page components
    private Dialog dialog;
    private String server_address = null;
    private String pc_ios_link, pc_ios_pin;
    private TextView current_file, total_received, pack_got, portal_summary, main_title;
    private TableLayout main_table;
    private DBHandler dbHandler;

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        if(!Home.create_app_folders()){
            Toast.makeText(this, "Storage unavailable, please reinstall Sharlet!", Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        dbHandler = new DBHandler(this);

        //Set components
        ProgressBar progress = findViewById(R.id.progress);
        ProgressBar total_progress = findViewById(R.id.total_progress);
        total_received = findViewById(R.id.total_received);
        pack_got = findViewById(R.id.pack_got);
        current_file = findViewById(R.id.current_file);
        main_table = findViewById(R.id.files_table);
        portal_summary = findViewById(R.id.portal_summary);
        main_title = findViewById(R.id.total_progress_title);

        TextView back_button = findViewById(R.id.back_button);
        TextView portal_info = findViewById(R.id.portal_info);

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
        //Main receive


    }

    //Events
    public void onBackPressed() {
        dialog.show();
    }

    //Needed
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
}