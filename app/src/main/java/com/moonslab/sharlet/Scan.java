package com.moonslab.sharlet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.os.Bundle;

import android.content.Intent;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Scan extends AppCompatActivity {
    private static SecretKeySpec secretKey;
    private static byte[] key;
    private static final String ALGORITHM = "AES";
    ////NEVER CHANGE THIS !!!!!!!!
    //THIS WILL NEVER BE CHANGED -- EVEN IN THE UPCOMING UPDATES
    public static String QR_DECODER_KEY = "uZ3x4OCmn*Xe&l1Ychs$pyrv^5pcoMh3gqUW&JE&lUCKM@3e!d";
    ////NEVER CHANGE THIS !!!!!!!!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        setContentView(R.layout.activity_scan);
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setPrompt("");
        intentIntegrator.setCaptureActivity(Scan_helper.class);
        intentIntegrator.setOrientationLocked(false);
        intentIntegrator.setBeepEnabled(false);
        intentIntegrator.initiateScan();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                finish();
                Toast.makeText(getBaseContext(), "Canceled", Toast.LENGTH_SHORT).show();
            } else {
                String qr_data = intentResult.getContents();
                //DO STUFF -- Decrypt
                String payload = decrypt(qr_data, QR_DECODER_KEY);
                if(null == payload){
                    Toast.makeText(Scan.this, "Invalid QR code!", Toast.LENGTH_LONG).show();
                    this.finish();
                }
                else {
                    Intent intent = new Intent(Scan.this, Receiver_initiator.class);
                    intent.putExtra("payload", payload);
                    this.startActivity(intent);
                    this.finish();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    public static String decrypt(String strToDecrypt, String secret) {
        try {
            prepareSecreteKey(secret);
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(android.util.Base64.decode(strToDecrypt, android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING)));
        } catch (Exception e) {
            return null;
        }
    }
    public static void prepareSecreteKey(String myKey) {
        MessageDigest sha;
        try {
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}