package com.moonslab.sharlet.custom;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.TextView;

public class Receiver extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
        //Service start


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Service destroyed


    }

    //Service Binder
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public Receiver getService() {
            return Receiver.this;
        }
        public void setComponents(TextView top_etc){
            //Page components comes here

        }
    }
    public IBinder onBind(Intent intent){
        return binder;
    }
}
