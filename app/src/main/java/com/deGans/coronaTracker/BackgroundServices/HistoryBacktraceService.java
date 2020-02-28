package com.deGans.coronaTracker.BackgroundServices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.deGans.coronaTracker.Database.AppDatabase;
import com.deGans.coronaTracker.Models.LocationDto;
import com.deGans.coronaTracker.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.deGans.coronaTracker.BackgroundServices.CurrentLocationService.BROADCAST_ACTION;

public class HistoryBacktraceService extends Service {
    public AppDatabase db;
    public  Intent intent;
    public JSONArray data;
    @Override
    public void onCreate()
    {
//      should be callin firebase right here
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
        Toast.makeText(getApplicationContext(),"Corona Tracker is running...",Toast.LENGTH_SHORT).show();
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "CoronaDB").fallbackToDestructiveMigration().build();
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "coronatracker1";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Corona Tracker - Keeping you safe...",
                    NotificationManager.IMPORTANCE_DEFAULT );
            channel.setSound(null, null);


            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Backtracing in progress...")
                    .setContentText("").setSmallIcon(R.drawable.ic_security_24px).build();


            startForeground(2, notification);
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startid)
    {
        if(intent.hasExtra("data")) {
            String jsonArrayString = intent.getStringExtra("data");
            try {
                data = new JSONArray(jsonArrayString);
                ArrayList<LocationDto> covidLocs = new ArrayList<>();
                //parse
                for (int i=0;i<data.length();i++)
                {
                    LocationDto covidLoc = new LocationDto();
                    JSONObject o = data.optJSONObject(i);
                    covidLoc.latitude=o.optDouble("lat");
                    covidLoc.longitude=o.optDouble("lon");

                    covidLocs.add(covidLoc);
                }
                DoBackTraceWHO(covidLocs);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return startid;
    }
    public void DoBackTraceWHO(final ArrayList<LocationDto> data){
        Log.i ("XXXXXXXXXXXXXXX", "Starting backtracing service");
        final long lastchecked = getLastChecked();
        //only get records from db that are higher than last time we checked
        final Runnable UpdateDbRunnable = new Runnable() {
            public void run() {
                List<LocationDto> recentLocations = db.locationDao().loadAfterTime(lastchecked);
                ArrayList<LocationDto> infectedLocations = data;
                Log.i("*********************", "List contains:"+ data.toArray().toString());

                for(LocationDto infectedLoc: infectedLocations){

                    for(LocationDto recentLoc : recentLocations){

                        float[] results = new float[1];

                        //do some lon lat trick to check if its nearby
                        Location.distanceBetween(recentLoc.latitude, recentLoc.longitude, infectedLoc.latitude, infectedLoc.longitude, results);
                        float distanceInMeters = results[0];

                        boolean isWithin10km = distanceInMeters < 10000;
                        if(isWithin10km){
                            CoronaDetected();
                        }
                    }
                }
                // Stop foreground service and remove the notification.
                stopForeground(true);

                // Stop the foreground service.
                stopSelf();

            }
        };
        performOnBackgroundThread(UpdateDbRunnable);

    }
    public void CoronaDetected(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        SharedPreferences.Editor ed;
        if(!sharedPrefs.contains("corona_status")){
            ed = sharedPrefs.edit();

            //Indicate that the default shared prefs have been set
            ed.putInt("corona_status", 1);

            ed.apply();
        }

        Intent intent = new Intent(HistoryBacktraceService.this, InfectedService.class);
        Log.i ("XXXXXXXXXXXXXXX", "Starting InfectedService");
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            // Pre-O behavior.
            startService(intent);
        }
        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();

    }
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }
    private long getLastChecked(){
        SharedPreferences sharedPrefs = getSharedPreferences("backtraceLogging", MODE_PRIVATE);
        return sharedPrefs.getLong("backtrace",0);

    }
    private void saveBacktraceTimestamp(){
        SharedPreferences sharedPrefs = getSharedPreferences("backtraceLogging", MODE_PRIVATE);
        SharedPreferences.Editor ed;
        if(!sharedPrefs.contains("backtrace")){
            ed = sharedPrefs.edit();

            //Indicate that the default shared prefs have been set
            ed.putLong("backtrace", System.currentTimeMillis());

            ed.apply();
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
