package com.moonslab.sharlet;

import static com.moonslab.sharlet.Home.read_from_file;
import static com.moonslab.sharlet.Home.store_as_file;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class Settings_page extends AppCompatActivity {
    private Context context;
    Boolean tip_1 = true;
    Boolean doc_edit_mood = false;
    private DBHandler dbHandler;
    String tester;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        dbHandler = new DBHandler(this);
        setContentView(R.layout.activity_settings_page);

        //Back
        TextView back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());

        //Components
        TextView sort_text = findViewById(R.id.sort_text);
        Switch sort_button = findViewById(R.id.sort_button);
        TextView hidden_text = findViewById(R.id.hidden_text);
        Switch hidden_button = findViewById(R.id.hidden_button);

        //Bottom links
        TextView link_privacy = findViewById(R.id.link_privacy);
        link_privacy.setOnClickListener(v -> {
            Intent intent = new Intent(context, notice_viewer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle b = new Bundle();
            b.putString("notice_code", "privacy_v1");
            intent.putExtras(b);
            context.startActivity(intent);
        });

        TextView link_legal = findViewById(R.id.link_legal);
        link_legal.setOnClickListener(v -> {
            Intent intent = new Intent(context, notice_viewer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle b = new Bundle();
            b.putString("notice_code", "legal_v1");
            intent.putExtras(b);
            context.startActivity(intent);
        });

        TextView link_feedback = findViewById(R.id.link_feedback);
        link_feedback.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/xtA9XNbbvdTYdci36"));
            startActivity(browserIntent);
        });

        TextView link_about = findViewById(R.id.link_about);
        link_about.setOnClickListener(v -> startActivity(new Intent(Settings_page.this, about.class)));

        TextView link_disclaimer = findViewById(R.id.link_disclaimer);
        link_disclaimer.setOnClickListener(v -> {
            Intent intent = new Intent(context, notice_viewer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle b = new Bundle();
            b.putString("notice_code", "disclaimer_v1");
            intent.putExtras(b);
            context.startActivity(intent);
        });

        //Latest
        tester = dbHandler.get_settings("latest_files");
        if(null != tester && tester.equals("false")){
            sort_button.setChecked(false);
        }
        sort_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                dbHandler.add_setting("latest_files", "true");
            }
            else {
                dbHandler.add_setting("latest_files", "false");
            }
        });

        //Hidden
        tester = dbHandler.get_settings("show_hidden");
        if(null != tester && tester.equals("true")){
            hidden_button.setChecked(true);
        }
        hidden_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                dbHandler.add_setting("show_hidden", "true");
            }
            else {
                dbHandler.add_setting("show_hidden", "false");
            }
        });

        //Texts
        hidden_text.setOnClickListener(v -> hidden_button.toggle());
        sort_text.setOnClickListener(v -> sort_button.toggle());

        //Doc path
        TextView doc_path = findViewById(R.id.doc_path);
        TextView doc_input = findViewById(R.id.doc_input);
        TextView doc_done = findViewById(R.id.doc_done);

        String d_path = Home.get_storage_root()+"/Download";
        String d = read_from_file("doc_path.txt", context);
        if(null != d){
            d_path = d;
        }
        doc_path.setText(d_path);
        doc_input.setText(d_path);
        doc_path.setOnClickListener(v->{
            doc_done.setText("\\\uf00c");
            doc_path.setVisibility(View.GONE);
            doc_input.setVisibility(View.VISIBLE);
            doc_input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(doc_input, InputMethodManager.SHOW_IMPLICIT);
            if(tip_1) {
                Toast.makeText(context, "Enter folder path", Toast.LENGTH_SHORT).show();
                tip_1 = false;
            }
            doc_edit_mood = true;
        });
        String finalD_path = d_path;
        doc_done.setOnClickListener(v->{
            if(!doc_edit_mood){
                doc_path.performClick();
                return;
            }
            String path = doc_input.getText().toString();
            if(null != path){
                File test = new File(path);
                if(test.exists() && test.isDirectory()){
                    store_as_file("doc_path.txt", path, context);
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
                    doc_path.setText(path);
                    Home.hide_keyboard(doc_input, context);
                }
                else {
                    doc_input.setText(finalD_path);
                    Toast.makeText(context, "Invalid path", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(context, "Invalid path", Toast.LENGTH_SHORT).show();
            }
            doc_path.setVisibility(View.VISIBLE);
            doc_input.setVisibility(View.GONE);
            doc_done.setText("\\\uf044");
            doc_edit_mood = false;
        });


        //System apps
        TextView system_apps_text = findViewById(R.id.system_apps_text);
        Switch system_apps_button = findViewById(R.id.system_apps_button);

        tester = dbHandler.get_settings("hide_system_apps");
        if(null != tester && tester.equals("false")){
            system_apps_button.setChecked(false);
        }
        system_apps_text.setOnClickListener(v-> system_apps_button.toggle());
        system_apps_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                //Unset
                dbHandler.add_setting("hide_system_apps", "true");
            }
            else {
                dbHandler.add_setting("hide_system_apps", "false");
            }
        });


        //HTTPS
        TextView https_text = findViewById(R.id.https_text);
        Switch https_button = findViewById(R.id.https_button);
        tester = dbHandler.get_settings("use_https");
        if(null != tester && tester.equals("true")){
            https_button.setChecked(true);
        }
        https_text.setOnClickListener(v-> https_button.toggle());
        https_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                //Unset
                dbHandler.add_setting("use_https", "true");
            }
            else {
                dbHandler.add_setting("use_https", "false");
            }
        });

        Switch all_secure = findViewById(R.id.all_secure);
        TextView enc_all = findViewById(R.id.enc_all_text);
        enc_all.setOnClickListener(v-> all_secure.toggle());
        all_secure.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(context,"For security over public networks, you can't turn this feature off!", Toast.LENGTH_LONG).show();
            all_secure.setChecked(true);
        });


        LinearLayout reset = findViewById(R.id.reset_button);
        reset.setOnClickListener(v -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle("Reset to default settings?");
            alert.setMessage("Your current settings will be lost and default settings will be applied!");
            alert.setPositiveButton("Reset", (dialog, whichButton) -> {
                //Reset
                String def_doc = Home.get_storage_root()+"/Download";
                store_as_file("set_latest.txt", "yes", context);
                store_as_file("set_show_hidden.txt", "no", context);
                store_as_file("doc_path.txt", def_doc, context);
                dbHandler.add_setting("hide_system_apps", "true");
                dbHandler.add_setting("use_https", "false");

                //Visual
                sort_button.setChecked(true);
                hidden_button.setChecked(false);
                doc_path.setText(def_doc);
                doc_input.setText(def_doc);
                hidden_button.setChecked(false);
                https_button.setChecked(false);
                system_apps_button.setChecked(true);

                //Confirm
                Toast.makeText(context, "Settings reset", Toast.LENGTH_SHORT).show();
            });
            alert.setNegativeButton("Cancel",
                    (dialog, whichButton) -> {
                        //Do nothing
                    });
            alert.show();
        });

    }
}