package com.moonslab.sharlet;

import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
public class Scan_helper extends CaptureActivity {
    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_scan_helper);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        return findViewById(R.id.zxing_barcode_scanner);
    }
}