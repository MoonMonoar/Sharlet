package com.moonslab.sharlet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

public class Scan extends AppCompatActivity {
    private CodeScanner mCodeScanner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            String connection_info = result.getText();
            String[] variables = connection_info.split("-");
            String main_server = variables[2]+variables[1]+":"+variables[0];
            String main_pin = variables[3];
            if(null == main_pin){
                Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show();
                this.finish();
                return;
            }
            startActivity(new Intent(Scan.this, Receiver_initiator.class)
                    .putExtra("server", main_server)
                    .putExtra("pin", variables[3]));
        }));
        scannerView.setOnClickListener(view -> mCodeScanner.startPreview());
    }
    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }
    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
}