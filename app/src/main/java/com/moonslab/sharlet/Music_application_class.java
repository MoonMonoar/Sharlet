package com.moonslab.sharlet;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class Music_application_class extends Application {
    public static final String CHANNEL_MUSIC = "MUSIC_PLAYER";
    public static final String CHANNEL_DEFAULT = "DEFAULT";
    public static final String CHANNEL_SERVICE = "FOREGROUND";
    @Override
    public void onCreate() {
        super.onCreate();
        Create_notification_channel();
    }
    private void Create_notification_channel() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_MUSIC,
                "Music player", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription("Sharlet - Default music player");
        notificationChannel.setSound(null, null);
        notificationChannel.setVibrationPattern(null);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationChannel notificationChannel2 = new NotificationChannel(CHANNEL_DEFAULT,
                "Sharlet", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel2.setDescription("Sharlet - App notifications");
        notificationManager.createNotificationChannel(notificationChannel2);

        NotificationChannel notificationChannel3 = new NotificationChannel(CHANNEL_SERVICE,
                "Sharlet Sender/Receiver", NotificationManager.IMPORTANCE_LOW);
        notificationChannel3.setDescription("Sharlet - App services");
        notificationManager.createNotificationChannel(notificationChannel3);
    }
}