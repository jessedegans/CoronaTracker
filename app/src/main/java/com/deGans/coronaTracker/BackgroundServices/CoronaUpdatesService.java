package com.deGans.coronaTracker.BackgroundServices;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.deGans.coronaTracker.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CoronaUpdatesService extends FirebaseMessagingService {
    final String TAG = "Corona UPDATE";


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i("*********************", "Firebase Message Received");

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
//            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Map<String, String> params = remoteMessage.getData();

            String newCovidData = null;
            try {
                newCovidData = HTTPGetCall("https://" +params.get("url"));

            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONArray data = null;
            try {
                data = new JSONArray(newCovidData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //create intent
            Intent intent = new Intent(CoronaUpdatesService.this, HistoryBacktraceService.class);
            intent.putExtra("data", data.toString());
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                // Pre-O behavior.
                startService(intent);
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    protected String HTTPGetCall(String WebMethodURL) throws IOException, MalformedURLException
    {
        StringBuilder response = new StringBuilder();

        //Prepare the URL and the connection
        URL u = new URL(WebMethodURL);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            //Get the Stream reader ready
            BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()),8192);

            //Loop through the return data and copy it over to the response object to be processed
            String line = null;

            while ((line = input.readLine()) != null)
            {
                response.append(line);
            }

            input.close();
        }

        return response.toString();
    }
}
