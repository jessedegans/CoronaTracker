package com.deGans.coronaTracker.BackgroundServices.RestartBroadcasts;

import android.app.job.JobParameters;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobService extends android.app.job.JobService {
    private static String TAG= JobService.class.getSimpleName();
    private static CurrentLocationRestarterBroadcastReceiver restartCurrentLocationReceiver;
    private static JobService instance;
    private static JobParameters jobParameters;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        ProcessMainClass bck = new ProcessMainClass();
        bck.launchService(this);
        registerRestarterReceiver();
        instance= this;
        JobService.jobParameters= jobParameters;

        return false;
    }

    private void registerRestarterReceiver() {

        // the context can be null if app just installed and this is called from restartsensorservice
        // https://stackoverflow.com/questions/24934260/intentreceiver-components-are-not-allowed-to-register-to-receive-intents-when
        // Final decision: in case it is called from installation of new version (i.e. from manifest, the application is
        // null. So we must use context.registerReceiver. Otherwise this will crash and we try with context.getApplicationContext
        if (restartCurrentLocationReceiver == null)
            restartCurrentLocationReceiver = new CurrentLocationRestarterBroadcastReceiver();
        else try{
            unregisterReceiver(restartCurrentLocationReceiver);
        } catch (Exception e){
            // not registered
        }
        // give the time to run
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // we register the  receiver that will restart the background service if it is killed
                // see onDestroy of Service
                IntentFilter filter = new IntentFilter();
                filter.addAction("com.deGans.coronaTracker.BackgroundServices.RestartBroadcasts");
                try {
                    registerReceiver(restartCurrentLocationReceiver, filter);
                } catch (Exception e) {
                    try {
                        getApplicationContext().registerReceiver(restartCurrentLocationReceiver, filter);
                    } catch (Exception ex) {

                    }
                }
            }
        }, 1000);

    }

    /**
     * called if Android kills the job service
     * @param jobParameters
     * @return
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.i(TAG, "Stopping job");
        Intent broadcastIntent = new Intent("com.deGans.coronaTracker.BackgroundServices.RestartBroadcasts");
        sendBroadcast(broadcastIntent);
        // give the time to run
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                unregisterReceiver(restartCurrentLocationReceiver);
            }
        }, 1000);

        return false;
    }


    /**
     * called when the tracker is stopped for whatever reason
     * @param context
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void stopJob(Context context) {
        if (instance!=null && jobParameters!=null) {
            try{
                instance.unregisterReceiver(restartCurrentLocationReceiver);
            } catch (Exception e){
                // not registered
            }
            Log.i(TAG, "Finishing job");
            instance.jobFinished(jobParameters, true);
        }
    }
}
