package com.deGans.coronaTracker.BackgroundServices;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.content.BroadcastReceiver;


public class LocServiceStarter extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Intent in = new Intent(context, CurrentLocationService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(in);
        } else {
            // Pre-O behavior.
            context.startService(in);
        }
    }
}
