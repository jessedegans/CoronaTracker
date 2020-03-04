package com.deGans.coronaTracker.BackgroundServices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);

        //first check if this device is infected alreayd
        if(sharedPrefs.getInt("corona_status",0) != 2){
            //if not then proceed with checking if extra intent with data is send
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
                        if(intent.getBooleanExtra("isDevice",false)){
                            covidLoc.time = o.optLong("time");
                        }

                        covidLocs.add(covidLoc);
                    }

                    //determine if its deivce or WHO
                    if(intent.hasExtra("isDevice")){
                        //if its a device run different algoritm
                        if(intent.getBooleanExtra("isDevice",false)){
                            DoBacktraceDevice(covidLocs);
                        } else{

                            DoBackTraceWHO(covidLocs);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else{
            // Stop foreground service and remove the notification.
            stopForeground(true);

            // Stop the foreground service.
            stopSelf();

        }

        return startid;
    }
    public void DoBackTraceWHO(final ArrayList<LocationDto> data){
        Log.i ("XXXXXXXXXXXXXXX", "Starting WHO backtracing service");
        final long lastchecked = getLastChecked();
        //only get records from db that are higher than last time we checked
        final Runnable UpdateDbRunnable = new Runnable() {
            public void run() {
                List<LocationDto> recentLocations = db.locationDao().loadAfterTime(lastchecked);
                ArrayList<LocationDto> infectedLocations = data;

                for(LocationDto infectedLoc: infectedLocations){
                    for(LocationDto recentLoc : recentLocations){

                        float[] results = new float[1];

                        //do some lon lat trick to check if its nearby
                        Location.distanceBetween(recentLoc.latitude, recentLoc.longitude, infectedLoc.latitude, infectedLoc.longitude, results);
                        float distanceInMeters = results[0];

                        boolean isWithin10km = distanceInMeters < 10000;
                        if(isWithin10km){
                            CoronaDetected(recentLoc, "WHO");
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
    public void DoBacktraceDevice(final ArrayList<LocationDto> data){
        Log.i ("XXXXXXXXXXXXXXX", "Starting DEVICE backtracing service");
        //only get records from db that are higher than last time we checked
        final Runnable UpdateDbRunnable = new Runnable() {
            public void run() {
                List<LocationDto> recentLocations = db.locationDao().getAll();
                ArrayList<LocationDto> infectedLocations = data;

                for(int i =0; i <  infectedLocations.size(); i++){

                    for(LocationDto recentLoc : recentLocations){

                        float[] results = new float[1];

                        //the time between the current infected loc and the next qualifies the history loc (time wise)
                        LocationDto curInfectedLoc = infectedLocations.get(i);
                        LocationDto nextInfectedLoc = curInfectedLoc;
                        if(i+1 == infectedLocations.size()){
                            nextInfectedLoc = infectedLocations.get(i);
                            nextInfectedLoc.time = nextInfectedLoc.time + 60000;
                        } else{
                            nextInfectedLoc = infectedLocations.get(i + 1);
                        }

//
//                        if(recentLoc.time >= curInfectedLoc.time && recentLoc.time <= nextInfectedLoc.time){
                            //infection possible time wise // otherwise you would just check the next one


                            //calculate distance so the distance that qualifies if the device could be infected
                            //depends on the time between the curInfectedLoc and the recentLoc.
                        long timeBetweenInMillis = 0;
                        //determine highest value
                        if(curInfectedLoc.time <= recentLoc.time){
                            timeBetweenInMillis = (recentLoc.time - curInfectedLoc.time);
                        }
                        if(curInfectedLoc.time >= recentLoc.time){
                            timeBetweenInMillis = (curInfectedLoc.time - recentLoc.time);
                        }
                        if(timeBetweenInMillis <= (60 * 1000 * 5)){
                            //standard
                            float distanceInMetersTreshold = 1000;
                            //seconds to millis is times 1000 right?
                            //keep in mind that only walkers are infected (walkers on trains and ppl who are in crowds)
                            if(timeBetweenInMillis <= (60 * 1000)){
                                //time difference is 1 minute lets say max you can walk in 1 minute is 200 meters
                                distanceInMetersTreshold = 200;
                            } else if (timeBetweenInMillis <= (120*1000)){
                                distanceInMetersTreshold = 400;
                            } else if(timeBetweenInMillis <= (180 * 1000)){
                                distanceInMetersTreshold = 600;
                            } else if(timeBetweenInMillis <= (240 * 1000)){
                                distanceInMetersTreshold = 800;
                            } else if(timeBetweenInMillis <= (300 * 1000)){
                                distanceInMetersTreshold = 1000;
                            } else if(timeBetweenInMillis <= (360 * 1000)){
                                distanceInMetersTreshold = 1200;
                            }
                            else if(timeBetweenInMillis <= (420 * 1000)){
                                distanceInMetersTreshold = 1400;
                            } else{
                                distanceInMetersTreshold = 1500;
                            }


                            //do some lon lat trick to check if its nearby
                            Location.distanceBetween(recentLoc.latitude, recentLoc.longitude, curInfectedLoc.latitude, curInfectedLoc.longitude, results);
                            float distanceInMeters = results[0];

                            boolean isWithinRange = distanceInMeters < distanceInMetersTreshold;

                            if(isWithinRange){
                                CoronaDetected(recentLoc, "DEVICE");
                            }
                        }

                    }
                }
                Log.i ("XXXXXXXXXXXXXXX", "DEVICE backtracing Finished");
                // Stop foreground service and remove the notification.
                stopForeground(true);

                // Stop the foreground service.
                stopSelf();

            }
        };
        performOnBackgroundThread(UpdateDbRunnable);

    }
    public void CoronaDetected(LocationDto location, String dataSource){

        //get shared prefs
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        SharedPreferences.Editor ed;
        ed = sharedPrefs.edit();

        //Indicate that the default shared prefs have been set
        ed.putInt("corona_status", 1);
        ed.putString("infect_loc_lat", location.latitude.toString());
        ed.putString("infect_loc_lon", location.longitude.toString());
        ed.putLong("infect_time", location.time);
        ed.putString("infect_reason", dataSource);

        ed.apply();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            showCoronaDetectedNotificationOreo();
        } else{
            showCoronaDetectedNotification();
        }
        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void showCoronaDetectedNotificationOreo() {
        CharSequence name = "Corona Tracker";
        String description = "Keeps you safe.";
        int importance  = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = new NotificationChannel("Corona Tracker", name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager1 = getSystemService(NotificationManager.class);
        notificationManager1.createNotificationChannel(channel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channel.getId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("CORONA HAS BEEN DETECTED.")
                .setContentText("Please seek medical attention and confirm in the app so we can warn other users")
                .setVibrate(new long[] {1000, 1000})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager1.notify(100, builder.build());

    }
    public void showCoronaDetectedNotification() {
        CharSequence name = "Corona Tracker";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notifyId = 1;
        String channelId = "some_channel_id";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "Corona Tracker")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("CORONA HAS BEEN DETECTED.")
                .setContentText("Please seek medical attention and confirm in the app so we can warn other users")
                .setVibrate(new long[] {1000, 1000})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(100, builder.build());

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
