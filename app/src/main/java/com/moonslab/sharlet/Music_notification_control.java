package com.moonslab.sharlet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Music_notification_control extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent1 = new Intent(context, Music_player_service.class);
        if(null != intent.getAction()) {
            intent1.putExtra("Action", intent.getAction());
            context.startService(intent1);
        }
    }
}