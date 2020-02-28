package com.deGans.coronaTracker.BackgroundServices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.deGans.coronaTracker.Database.AppDatabase;
import com.deGans.coronaTracker.R;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.deGans.coronaTracker.BackgroundServices.CurrentLocationService.BROADCAST_ACTION;

public class InfectedService extends Service {

    public AppDatabase db;
    public  Intent intent;
    public JSONObject data;
    @Override
    public void onCreate()
    {
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
                    .setContentTitle("Possible Infection detected...")
                    .setContentText("").setSmallIcon(R.drawable.ic_security_24px).build();


            startForeground(3, notification);
        }
    }
    private String infectedStatus(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        switch(sharedPrefs.getInt("corona_status",0)) {
            case 0:
                //kill service
                return "NON";

            case 1:
                return "POSSIBLE";

            case 2:
                //def infected
                return "CONFIRMED";

        }
        return "ERROR";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid)
    {
        if(infectedStatus().equals("CONFIRMED")){
            UploadLocationHistoryToCloud();
        }
        return startid;
    }

    public void UploadLocationHistoryToCloud(){
        String currentDBPath = getDatabasePath("CoronaDB").getAbsolutePath();
        //upload file on this location

        String uniqueID = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        // Create a storage reference from our app
        FirebaseStorage storage = FirebaseStorage.getInstance();
        final StorageReference storageRef = storage.getReference();
        //set reference to unique id
        final StorageReference ref = FirebaseStorage.getInstance().getReference(uniqueID + "_" + System.currentTimeMillis());

        Uri file = Uri.fromFile(new File(currentDBPath));

        Task<UploadTask.TaskSnapshot> uploadTask =ref.putFile(file);
        Log.i ("DDDDDDDDDDDDDDDD", "Starting uploading DB");
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                if (!task.isSuccessful()) {
                    Log.i ("DDDDDDDDDDDDDDDD", task.getException().toString());
                }
                Log.i ("DDDDDDDDDDDDDDDD", "Upload successful");
                // Continue with the task to get the download URL
                return ref.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull final Task<Uri> task) {

                Log.i ("ONCOMPLETE", task.getResult().toString());
                if (task.isSuccessful()) {
                    final Uri downloadUri = task.getResult();
                    Log.i ("DOWNLOAD URL", task.getResult().toString());
                    final Runnable SubmitDbUrlToApi = new Runnable() {
                        public void run() {
                            try {

                                String url = "https://coronatracker.azurewebsites.net/api/database";
                                URL urlObj = new URL(url);
                                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                                conn.setDoOutput(true);
                                conn.setRequestMethod("POST");
                                conn.setRequestProperty("Accept-Charset", "UTF-8");
                                conn.setRequestProperty("Content-Type","application/json");
                                conn.setReadTimeout(10000);
                                conn.setConnectTimeout(15000);

                                conn.connect();

                                String paramsString = downloadUri.toString();

                                JSONObject jsonParam = new JSONObject();
                                jsonParam.put("downloadURL", paramsString);
                                Log.i("JSON", jsonParam.toString());

                                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                                wr.writeBytes(jsonParam.toString());
                                wr.flush();
                                wr.close();
                                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                                Log.i("MSG" , conn.getResponseMessage());
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            // Stop foreground service and remove the notification.
                            stopForeground(true);

                            // Stop the foreground service.
                            stopSelf();
                        }
                    };

                    performOnBackgroundThread(SubmitDbUrlToApi);
                }
            }
        });

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }
}
