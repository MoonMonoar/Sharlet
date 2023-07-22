package com.moonslab.sharlet.custom;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

public class NetUtility {
    private Context context;
    private WifiManager wifiManager;
    public NetUtility(Context context_get){
        this.context = context_get;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
    public boolean isWiFiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }
    public boolean isHotspotEnabled(){
        try {
            // Get the hotspot state using reflection
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            Object result = method.invoke(wifiManager);
            if (result instanceof Boolean) {
                return (boolean) result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public String getWifiFrequency() {
        String frequency = "";
        if (isWiFiConnected()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                int frequencyInMHz = wifiInfo.getFrequency();
                if (frequencyInMHz >= 2400 && frequencyInMHz <= 2500) {
                    frequency = "2.4";
                } else if (frequencyInMHz >= 5000 && frequencyInMHz <= 6000) {
                    frequency = "5";
                }
            }
        }
        return frequency;
    }
    public String getCurrentWiFiName() {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSSID().replace("\"", "");
    }
    public boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }
}