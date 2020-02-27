package com.deGans.coronaTracker.BackgroundServices.RestartBroadcasts;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.deGans.coronaTracker.BackgroundServices.CurrentLocationService;

public class ProcessMainClass {
    public static final String TAG = ProcessMainClass.class.getSimpleName();
    private static Intent serviceIntent = null;

    public ProcessMainClass() {
    }


    private void setServiceIntent(Context context) {
        if (serviceIntent == null) {
            serviceIntent = new Intent(context, CurrentLocationService.class);
        }
    }
    /**
     * launching the service
     */
    public void launchService(Context context) {
        if (context == null) {
            return;
        }
        setServiceIntent(context);
        // depending on the version of Android we eitehr launch the simple service (version<O)
        // or we start a foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Log.d(TAG, "ProcessMainClass: start service go!!!!");
    }
}
