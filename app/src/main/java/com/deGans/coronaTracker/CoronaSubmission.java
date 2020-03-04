package com.deGans.coronaTracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.sqlite.db.SimpleSQLiteQuery;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.deGans.coronaTracker.Database.AppDatabase;
import com.deGans.coronaTracker.Models.LocationDto;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
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
import java.util.LinkedList;
import java.util.List;

import javax.xml.datatype.Duration;

public class CoronaSubmission extends AppCompatActivity {

    public AppDatabase db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corona_submission);
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "CoronaDB").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        Button submitButton = (Button)findViewById(R.id.submit_form);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });
    }

    public void submitForm(){
        EditText nameField = (EditText) findViewById(R.id.name_field);
        EditText phoneNumberField = (EditText) findViewById(R.id.phone_field);
        setVars();
        String phoneNumber = phoneNumberField.getText().toString();
        String name = nameField.getText().toString();
        UploadLocationHistoryToCloud(name,phoneNumber);


    }
    public void setVars(){

        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        SharedPreferences.Editor ed;
        ed = sharedPrefs.edit();
        LocationDto loc = db.locationDao().getLast();
        //Indicate that the default shared prefs have been set
        ed.putInt("corona_status", 2);
        ed.putString("infect_loc_lat",loc.latitude.toString() );
        ed.putString("infect_loc_lon", loc.longitude.toString());
        ed.putLong("infect_time", loc.time);
        ed.putString("infect_reason", "Submission");
        ed.apply();
    }
    public void UploadLocationHistoryToCloud(final String name, final String phoneNumber){
        String currentDBPath = getDatabasePath("CoronaDB").getAbsolutePath();
        //upload file on this location

        //apparently we have to do some little trick to get locations
        db.locationDao().checkpoint(new SimpleSQLiteQuery("pragma wal_checkpoint(full)"));


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
                                setVars();
                                String url = "https://coronatracker.azurewebsites.net/api/database/request";
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
                                jsonParam.put("name", name);
                                jsonParam.put("phonenumber", phoneNumber);
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
                            Intent returnIntent = new Intent();
                            setResult(RESULT_OK, returnIntent);
                            finish();
                        }
                    };
                    Toast.makeText(getApplicationContext(),"Submission successful!",Toast.LENGTH_SHORT).show();
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
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();

        if (v != null &&
                (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) &&
                v instanceof EditText &&
                !v.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            v.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + v.getLeft() - scrcoords[0];
            float y = ev.getRawY() + v.getTop() - scrcoords[1];

            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom())
                hideKeyboard(this);
        }
        return super.dispatchTouchEvent(ev);
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }
}
