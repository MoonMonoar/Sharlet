package com.moonslab.sharlet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.moonslab.sharlet.custom.Sender;

public class Send extends AppCompatActivity implements ServiceConnection {
    private Dialog dialog, pc_dialogue, no_token;
    public Sender sender;
    private LinearLayout qr_button;
    private RelativeLayout waiting_view, sending_view;
    private TextView portal_summary, top_title, bucket_size_text, pack_size, unusual, file_update, payload_pin;
    private TableLayout portal_files_table;
    private ImageView qr_image;
    private String qr_link, qr_body;


    private boolean self_destroy = false;

    @Override
    protected void onPause() {
        super.onPause();
        this.finish();
        if(!self_destroy) {
            Toast.makeText(getApplicationContext(), "Sending in background", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint({"HardwareIds", "UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        Context context = this;

        if(!Home.create_app_folders()) {
            Toast.makeText(this, "Permission is required! Restart or reinstall app", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        //Sending mode
        qr_button = findViewById(R.id.qr_prompt);
        top_title = findViewById(R.id.top_title);
        waiting_view = findViewById(R.id.waiting_view);
        sending_view = findViewById(R.id.sending_view);
        portal_summary = findViewById(R.id.portal_summary);
        bucket_size_text = findViewById(R.id.total_bucket_size);
        pack_size = findViewById(R.id.pack_sent);
        portal_files_table = findViewById(R.id.files_table);
        unusual = findViewById(R.id.unusual);
        qr_image = findViewById(R.id.waiting_design);
        file_update = findViewById(R.id.file_update);
        payload_pin = findViewById(R.id.payload_pin);
        LinearLayout pc_button = findViewById(R.id.pc_button1);

        //SENDER SERVICE
        Intent intent = new Intent(this, Sender.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        startService(intent);

        qr_button.setOnClickListener(v -> {
            if (null == sender) {
                Toast.makeText(context, "Sender not ready yet!", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap QR_bitmap = sender.getQR_bitmap();
            if (null == QR_bitmap) {
                Toast.makeText(context, "QR code not ready!", Toast.LENGTH_SHORT).show();
                return;
            }
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.qr_button_popup);
            dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
            dialog.setCanceledOnTouchOutside(false);
            Window d_window = dialog.getWindow();
            d_window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ImageView qr_image = d_window.findViewById(R.id.qr_image);
            qr_image.setImageBitmap(QR_bitmap);
            dialog.show();
        });

        //Confirm dialogue
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.confirm_dialog_layout);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView d_title = dialog.getWindow().findViewById(R.id.title);
        TextView d_des = dialog.getWindow().findViewById(R.id.description);

        //Customize
        d_title.setText("Close portal?");
        d_des.setText("If you exit, your selection will be lost and the portal will be closed.");

        Button Okay = dialog.findViewById(R.id.btn_okay);
        Button Cancel = dialog.findViewById(R.id.btn_cancel);
        Okay.setText("Close");

        Okay.setOnClickListener(v -> {
            this.stopService(new Intent(this, Sender.class));
            self_destroy = true;
            this.finish();
        });

        Cancel.setOnClickListener(v -> dialog.dismiss());

        findViewById(R.id.back_button).setOnClickListener(v-> dialog.show());

        //PC dialogue
        //Confirm dialogue
        pc_dialogue = new Dialog(this);
        pc_dialogue.setContentView(R.layout.pc_button_dialogue);
        pc_dialogue.getWindow().setBackgroundDrawable(getDrawable(R.drawable.confirm_dialog_background));
        pc_dialogue.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pc_dialogue.setCanceledOnTouchOutside(false);
        TextView d_des2 = pc_dialogue.getWindow().findViewById(R.id.description);

        Button Copy = pc_dialogue.findViewById(R.id.btn_copy);
        Button Cancel2 = pc_dialogue.findViewById(R.id.btn_got_it);
        Copy.setText("Copy");

        Copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Sharlet link", qr_link);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), "Link copied", Toast.LENGTH_SHORT).show();
        });

        Cancel2.setOnClickListener(v -> pc_dialogue.dismiss());

        //No token
        no_token = new Dialog(this);
        no_token.setContentView(R.layout.no_token_dialogue);
        no_token.getWindow().setBackgroundDrawable(getDrawable(R.drawable.token_alert_back));
        no_token.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        no_token.setCanceledOnTouchOutside(false);

        LinearLayout got_it = no_token.findViewById(R.id.btn_got_it);

        got_it.setOnClickListener(v -> no_token.dismiss());

        //Dialogue ends
        //Others
        View.OnClickListener qr_action = v-> {
            if (!Home.is_http_ready(getApplicationContext())) {
                Toast.makeText(getApplicationContext(), "Plugin not found! Try again or Restart app", Toast.LENGTH_SHORT).show();
                Home.setup_http_plugin(getApplicationContext());
                return;
            }
            d_des2.setText(qr_body);
            pc_dialogue.show();
        };
        pc_button.setOnClickListener(qr_action);

        //Others
        unusual.setOnClickListener(v-> {
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle("Excessive data requirement");
            alert.setMessage("This is happening maybe because the users device is slow or the device is far from this wifi/hotspot range!");
            alert.setNegativeButton("Okay",
                    (dialog, whichButton) -> {
                        //Do nothing
                    });
            alert.show();
        });
    }
    public void onBackPressed() {
        dialog.show();
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Sender.LocalBinder binder = (Sender.LocalBinder) service;
        sender = binder.getService();
        binder.setComponents(top_title, waiting_view, sending_view, portal_summary, bucket_size_text, pack_size, portal_files_table, unusual, qr_button);
        //Check error
        if(sender.isError_overall()){
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            this.stopService(new Intent(this, Sender.class));
            self_destroy = true;
            this.finish();
        }
        else {
            //UPDATE THE QR
            Bitmap QR_bitmap = sender.getQR_bitmap();
            if(null != QR_bitmap) {
                qr_image.setImageBitmap(QR_bitmap);
            }
            String pc_line = sender.getPCline();
            if(null != pc_line){
                file_update.setText(String.format("%s%s", System.lineSeparator(), pc_line));
            }
            qr_link = sender.getLink();
            String body = "Link: " + qr_link + System.lineSeparator() + "Pin: " + sender.getPin();
            if (sender.getServer_type().equals("https://")) {
                body += System.lineSeparator() + System.lineSeparator() + "Please note as a static certificate, the HTTPS connection may show insecure by the browser. Please ignore this warning and proceed if occurs.";
            } else {
                body += System.lineSeparator() + System.lineSeparator() + "⚡ Turbo mode: Please make sure you are using 5GHz band for your hotspot. With 2.4GHz band, Turbo mode is limited up to 8MB/S.";
            }
            qr_body = body;
            String p_pin = sender.getPin();
            if(null != p_pin){
                payload_pin.setText(String.format("%s%s", getString(R.string.key), p_pin));
            }
        }
        //No token error
        if(sender.getNoToken()){
            //Show no token dialogue
            no_token.show();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        sender = null;
        Toast.makeText(this, "Sender disconnected!", Toast.LENGTH_SHORT).show();
        this.finish();
    }
}