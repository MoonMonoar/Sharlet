package com.moonslab.sharlet.custom;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Net {
    private static Handler netHandler;
    public Net(Handler handler) {
        netHandler = handler;
    }
    public void post(String url, String payload){
        new Thread(() -> {
            try {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);
                con.setDoInput(true);
                OutputStream os = con.getOutputStream();
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Message message = netHandler.obtainMessage();
                message.obj = response.toString();
                netHandler.sendMessage(message);
            }
            catch (Exception e){
                Message message = netHandler.obtainMessage();
                message.obj = e.toString();
                netHandler.sendMessage(message);
            }
        }).start();
    }
}
