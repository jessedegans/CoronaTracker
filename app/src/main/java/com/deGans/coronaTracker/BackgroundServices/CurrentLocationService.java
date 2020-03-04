package com.deGans.coronaTracker.BackgroundServices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;

import com.deGans.coronaTracker.BackgroundServices.RestartBroadcasts.CurrentLocationRestarterBroadcastReceiver;
import com.deGans.coronaTracker.Database.AppDatabase;
import com.deGans.coronaTracker.MainActivity;
import com.deGans.coronaTracker.Models.LocationDto;
import com.deGans.coronaTracker.R;

public class CurrentLocationService extends Service
{
    public static final String BROADCAST_ACTION = "Hello World";
    private static final int THIRTY_SECONDS = 1000 * 30;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int FIVE_MINUTES = 1000 * 60 * 5;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;
    public AppDatabase db;

    Intent intent;
    int counter = 0;

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
                    .setContentTitle("Corona Tracker - Keeping you safe...")
                    .setContentText("").setSmallIcon(R.drawable.ic_security_24px).build();


            startForeground(1, notification);
            startTracking();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid)
    {

        startTracking();
        return startid;
    }

    public void startTracking(){
        if(listener==null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            listener = new MyLocationListener();
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TWO_MINUTES, 100, listener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TWO_MINUTES, 100, listener);
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }


    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onDestroy() {
        // handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
        Intent broadcastIntent = new Intent(this, CurrentLocationRestarterBroadcastReceiver.class);

        sendBroadcast(broadcastIntent);
//        Log.v("STOP_SERVICE", "DONE");
//        if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
//            Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_SHORT).show();
//        }
//        locationManager.removeUpdates(listener);
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

    public class MyLocationListener implements LocationListener
    {

        public void onLocationChanged(final Location loc)
        {
            Log.i("*********************", "Location changed");
            if(isBetterLocation(loc, previousBestLocation)) {
                loc.getLatitude();
                loc.getLongitude();

                //save to local database
                LocationDto _locDto = new LocationDto();
                _locDto.latitude =  loc.getLatitude();
                _locDto.longitude = loc.getLongitude();
                _locDto.time = System.currentTimeMillis();
                final LocationDto locDto = _locDto;
                final Runnable UpdateDbRunnable = new Runnable() {
                    public void run() {
                        System.out.println("Background Task here");
                        db.locationDao().insert(locDto);
                    }
                };
                performOnBackgroundThread(UpdateDbRunnable);

                //send broadcasts to services

                intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Provider", loc.getProvider());
                sendBroadcast(intent);

                Intent RTReturn = new Intent(MainActivity.RECEIVE_JSON);
                RTReturn.putExtra("Latitude", loc.getLatitude());
                RTReturn.putExtra("Longitude", loc.getLongitude());
                RTReturn.putExtra("Provider", loc.getProvider());
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(RTReturn);

            }
        }

        public void onProviderDisabled(String provider)
        {
//           ? Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }


        public void onProviderEnabled(String provider)
        {
//            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

    }
}
