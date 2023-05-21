package com.moonslab.sharlet;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class notice_viewer extends AppCompatActivity {
    @SuppressLint("SetJavaScriptEnabled")
    private static Boolean title_check = false;
    private static String title_code = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_viewer);
        RelativeLayout try_again = findViewById(R.id.try_again);
        WebView webView = findViewById(R.id.web);
        ProgressBar loader = findViewById(R.id.web_loading);
        TextView name = findViewById(R.id.notice_name), back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> this.finish());
        Bundle b = this.getIntent().getExtras();
        String link = null;
        if (null != b && null != b.getString("notice_code")) {
            String code = b.getString("notice_code");
            if (code.equals("privacy_v1")) {
                title_code = "Doc-privacy_v1";
                name.setText("Privacy agreement");
                link = "https://moonmonoar.github.io/MoonsLab-docs/Sharlet-v1/privacy.html";
            }
            else if(code.equals("disclaimer_v1")){
                title_code = "Doc-disclaimer_v1";
                name.setText("Disclaimer");
                link = "https://moonmonoar.github.io/MoonsLab-docs/Sharlet-v1/disclaimer.html";
            }
            else if(code.equals("legal_v1")){
                name.setText("Legal information");
                link = "https://moonmonoar.github.io/MoonsLab-docs/Sharlet-v1/legal.html";
            }
        } else {
            Toast.makeText(this, "Notice type invalid!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        loader.setVisibility(View.VISIBLE);
        String finalTitle_code = title_code;
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if(newProgress == 100){
                    if(title_check) {
                        loader.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                    }
                    else {
                        loader.setVisibility(View.GONE);
                        try_again.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if(null != finalTitle_code){
                    if(finalTitle_code.equals(title)){
                        title_check = true;
                    }
                }
            }
        });
        webView.loadUrl(link);
    }
}