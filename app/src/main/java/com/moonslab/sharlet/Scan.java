package com.moonslab.sharlet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

public class Scan extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private CodeScanner mCodeScanner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission granted, initialize scanner
            initializeScanner();
        }
    }
    private void initializeScanner() {
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            String connection_info = result.getText();
            String[] variables = connection_info.split("-");
            String main_server = variables[2] + variables[1] + ":" + variables[0];
            String main_pin = variables[3];
            if (main_pin == null) {
                Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            startActivity(new Intent(Scan.this, Receiver_initiator.class)
                    .putExtra("qr_raw", connection_info)
                    .putExtra("server", main_server)
                    .putExtra("pin", variables[3]));
        }));
        scannerView.setOnClickListener(view -> mCodeScanner.startPreview());
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
                initializeScanner();
            } else {
                // Camera permission not granted, show error alert
                showCameraPermissionError();
            }
        }
    }
    private void showCameraPermissionError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Camera Permission Required");
        builder.setMessage("Please grant camera permission to use the QR code scanner.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Open app settings
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
            finish();
        });
        builder.setOnCancelListener(dialog -> {
            // Open app settings
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
            finish();
        });
        builder.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mCodeScanner != null) {
            mCodeScanner.startPreview();
        }
    }
    @Override
    protected void onPause() {
        if (mCodeScanner != null) {
            mCodeScanner.releaseResources();
        }
        super.onPause();
    }
}