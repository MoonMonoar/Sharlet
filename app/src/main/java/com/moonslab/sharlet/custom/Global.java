package com.moonslab.sharlet.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.provider.Settings;
import android.widget.Toast;

import com.moonslab.sharlet.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Global {
    private static final String ALGORITHM = "AES";
    private static SecretKeySpec secretKey;
    final String ENCRYPTION_KEY = "zA4YCR^6ZTeud$7cIypfe876863%^taP#I#3js1wp$Otq1BM^y5Kc6v13%4V";
    private final Context context;
    public Global(Context target_context){
        this.context = target_context;
    }
    public void writeStringAsFile(final String fileContents, String fileName) {
        try {
            FileWriter out = new FileWriter(new File(context.getFilesDir(), fileName));
            out.write(fileContents);
            out.close();
        } catch (IOException e) {
            //Cant save
            Toast.makeText(context, "Failed to information", Toast.LENGTH_SHORT).show();
        }
    }
    public String getIpAddress() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        if(null == ip) {
                            ip = inetAddress.getHostAddress();
                        }
                    }
                }
            }

        } catch (SocketException e) {
            return null;
        }
        return ip;
    }

    public String getENCRYPTION_KEY() {
        return ENCRYPTION_KEY;
    }

    public Context getContext(){
        return this.context;
    }
    public void prepareSecreteKey(String myKey) {
        MessageDigest sha;
        try {
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    public String encrypt(String strToEncrypt, String secret) {
        try {
            prepareSecreteKey(secret);
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return android.util.Base64.encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)), android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING)
                    .replaceAll(System.lineSeparator(), "");
        } catch (Exception e) {
            Toast.makeText(context, R.string.failed_error_while_encrypting, Toast.LENGTH_SHORT).show();
        }
        return null;
    }
    public String decrypt(String strToDecrypt, String secret) {
        try {
            prepareSecreteKey(secret);
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(android.util.Base64.decode(strToDecrypt, android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING)));
        } catch (Exception e) {
            return null;
        }
    }
    public String getAndroidDeviceId(Context context) {
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }
    public String getDeviceEncryptionKey(){
        String key = getAndroidDeviceId(context);
        if(null != key){
            return key+"-"+ENCRYPTION_KEY;
        }
        return ENCRYPTION_KEY;
    }
    public Bitmap makeDP(Bitmap originalBitmap, int desiredSize) {
        // Determine the dimensions of the original image
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        // Calculate the ratio for resizing
        float ratio;
        if (originalWidth > originalHeight) {
            ratio = (float) desiredSize / originalWidth;
        } else {
            ratio = (float) desiredSize / originalHeight;
        }

        // Calculate the new dimensions based on the ratio
        int newWidth = Math.round(originalWidth * ratio);
        int newHeight = Math.round(originalHeight * ratio);

        // Resize the image
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

        // Calculate the cropping coordinates
        int left = Math.max((newWidth - desiredSize) / 2, 0);
        int top = Math.max((newHeight - desiredSize) / 2, 0);
        int right = Math.min(left + desiredSize, newWidth);
        int bottom = Math.min(top + desiredSize, newHeight);

        // Create the cropped bitmap

        return Bitmap.createBitmap(resizedBitmap, left, top, right - left, bottom - top);
    }

}
