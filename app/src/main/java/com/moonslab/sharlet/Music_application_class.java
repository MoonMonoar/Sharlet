package com.moonslab.sharlet;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class Music_application_class extends Application {
    public static final String CHANNEL_MUSIC = "MUSIC_PLAYER";
    public static final String CHANNEL_DEFAULT = "DEFAULT";
    @Override
    public void onCreate() {
        super.onCreate();
        Create_notification_channel();
    }
    private void Create_notification_channel() {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_MUSIC,
                "Music player", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription("Sharlet - Default music player");
        notificationChannel.setSound(null, null);
        notificationChannel.setVibrationPattern(null);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
        NotificationChannel notificationChannel2 = new NotificationChannel(CHANNEL_DEFAULT,
                "Sharlet", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel2.setDescription("Sharlet - App notifications");
        NotificationManager notificationManager2 = getSystemService(NotificationManager.class);
        notificationManager2.createNotificationChannel(notificationChannel);
    }
}