package com.deGans.coronaTracker;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import 	androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.deGans.coronaTracker.BackgroundServices.CurrentLocationService;
import com.deGans.coronaTracker.BackgroundServices.InfectedService;
import com.deGans.coronaTracker.Database.AppDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.wajahatkarim3.roomexplorer.RoomExplorerActivity;

import org.w3c.dom.Text;

import static com.wajahatkarim3.roomexplorer.RoomExplorerActivity.DATABASE_CLASS_KEY;
import static com.wajahatkarim3.roomexplorer.RoomExplorerActivity.DATABASE_NAME_KEY;

public class MainActivity extends AppCompatActivity {

    public static final String RECEIVE_JSON = "com.deGans.coronaTracker.RECEIVE_JSON";
    Button btnSeeDb, btnGotCorona, btnConfirmCorona, btnFalseAlarm,btnCoronaSubmission;
    Double Latitude, Longitude;
    String Provider;
    BroadcastReceiver receiver;
    private String android_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);


        initReference();
        initStyle();
        initListener();
        initLocationReceiver();
        initPermissions();
        InitSubscribe();
        initTexts();
        setBottomNavigationListeners();

        firstTime();
        //  debugButtons();



    }
    private void firstTime(){
        SharedPreferences prefs = getSharedPreferences("firsttime", MODE_PRIVATE);
        boolean previouslyStarted = prefs.getBoolean("prevStarted", false);
        if(!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("prevStarted", Boolean.TRUE);
            edit.apply();
            startActivity(new Intent(this, WelcomeActivity.class));
        }
    }
    private void initTexts(){
        //set right texts
        TextView status = findViewById(R.id.status);
        TextView statusSubtitle = findViewById(R.id.status_subtitle);

        status.setText(getRightStatusText());
        statusSubtitle.setText(getRightStatusSubtitleText());

    }

    private void setBottomNavigationListeners(){
        final BottomNavigationView itemView = findViewById(R.id.bottom_navigation);
        itemView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.map:
                                NavigateTo(MapActivity.class);
                                break;
                            case R.id.donate:
                                NavigateTo(DonateActivity.class);
                                break;
                        }
                        return true;
                    }
                });
    }
    private void NavigateTo(Class<?> cls){
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        overridePendingTransition(0, 0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView itemView = findViewById(R.id.bottom_navigation);
        itemView.setSelectedItemId(R.id.overview);
        initStyle();
        initTexts();
    }

    private void initStyle(){
        View mainView = findViewById(R.id.mainBackground);
        BottomNavigationView itemView = findViewById(R.id.bottom_navigation);


        ImageView bgImage = findViewById(R.id.imageView);
        // Cast to a TextView instance if the menu item was found

        //set bg of navigationbar
        itemView.setBackgroundColor(Color.parseColor(getRightBackgroundAccentColor()));

        //set bg of mainview
        mainView.setBackgroundColor(Color.parseColor(getRightBackgroundColor()));

        //set color filter of bg image
        bgImage.setColorFilter(Color.parseColor(getRightIconColor()));



        itemView.setSelectedItemId(R.id.overview);

        //change notificationbar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(getRightBackgroundAccentColor()));
        }

        //show buttons or not
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        if(sharedPrefs.getInt("corona_status",0) == 1) {
            btnFalseAlarm.setVisibility(View.VISIBLE);
            btnConfirmCorona.setVisibility(View.VISIBLE);
        } else{
            btnFalseAlarm.setVisibility(View.GONE);
            btnConfirmCorona.setVisibility(View.GONE);
        }
    }
    private String getRightStatusText(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        switch(sharedPrefs.getInt("corona_status",0)) {
            case 0:
                return "Actively searching...";

            case 1:
                return "Possible infection detected, please confirm";

            case 2:
                //def infected
                return "Infected";

        }
        return "ERROR";
    }
    private String getRightStatusSubtitleText(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        switch(sharedPrefs.getInt("corona_status",0)) {
            case 0:
                return "No contact with a corona patient detected. \n \n if you have corona and would like to share your location history tap on the question mark";

            case 1:
                return "If you feel sick, please seek medical attention. \n Please confirm so we can inform people who may have been in contact with you";

            case 2:
                //def infected
                return "Thanks for confirming we have notified the other users. Get well soon!";

        }
        return "ERROR";
    }
    private String getRightIconColor(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        switch(sharedPrefs.getInt("corona_status",0)) {
            case 0:
                //kill service
                return "#98ee99";

            case 1:
                return "#ffdd71";

            case 2:
                //def infected
                return "#ff9168";

        }
        return "ERROR";
    }
    private String getRightBackgroundColor(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        switch(sharedPrefs.getInt("corona_status",0)) {
            case 0:
                //kill service
                return "#66bb6a";

            case 1:
                return "#ffab40";

            case 2:
                //def infected
                return "#DD613C";

        }
        return "ERROR";
    }
    private String getRightBackgroundAccentColor(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        switch(sharedPrefs.getInt("corona_status",0)) {
            case 0:
                //kill service
                return "#338a3e";

            case 1:
                return "#c77c02";

            case 2:
                //def infected
                return "#a53112";

        }
        return "ERROR";
    }
    private void initLocationReceiver(){
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(RECEIVE_JSON)) {
                    Provider = intent.getStringExtra("Provider");
                    Latitude = (Double) intent.getExtras().get("Latitude");
                    Longitude = (Double) intent.getExtras().get("Longitude");
//                    txtAddress.setText("Provider : " + Provider);
//                    txtCoordinates.setText("Lat:" + Latitude + " ,Long:" + Longitude);
                }
            }
        };

        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_JSON);
        bManager.registerReceiver(receiver, intentFilter);
    }
    private void initPermissions(){
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                1);
    }

    private void InitSubscribe(){
        FirebaseMessaging.getInstance().subscribeToTopic("all").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        });
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", serviceClass.getName() + "is running");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", serviceClass.getName() + "is offline");
        return false;
    }

    private void initReference() {
        btnSeeDb = (Button)findViewById(R.id.btnSeeDb);
        btnGotCorona = (Button)findViewById(R.id.CoronaIhave);
        btnConfirmCorona = (Button)findViewById(R.id.confirmCorona);
        btnFalseAlarm = (Button)findViewById(R.id.falseAlarm);
        btnCoronaSubmission = (Button)findViewById(R.id.coronaSubmission);

    }

    private void initListener() {
        btnFalseAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falseAlarm();
            }
        });

        btnConfirmCorona.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmCorona();
            }
        });
        btnCoronaSubmission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CoronaSubmission();
            }
        });
    }
    public void falseAlarm(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        SharedPreferences.Editor ed;
        ed = sharedPrefs.edit();

        //Indicate that the default shared prefs have been set
        ed.putInt("corona_status", 0);

        ed.apply();

        initStyle();
        initTexts();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                initTexts();
                initStyle();
            }
        }
    }
    public void CoronaSubmission() {
        Intent coronaRequest = new Intent(this,CoronaSubmission.class );
        startActivityForResult(coronaRequest,1);
    }
    public void confirmCorona(){
        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        SharedPreferences.Editor ed;
        ed = sharedPrefs.edit();

        //Indicate that the default shared prefs have been set
        ed.putInt("corona_status", 2);

        ed.apply();
        Intent intent = new Intent(MainActivity.this, InfectedService.class);
        Log.i ("XXXXXXXXXXXXXXX", "Starting InfectedService");
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            // Pre-O behavior.
            startService(intent);
        }

        initStyle();
        initTexts();
    }



    public void openDbView(){
        if (BuildConfig.DEBUG) {
            Intent ii = new Intent(getApplicationContext(), RoomExplorerActivity.class);
            ii.putExtra(DATABASE_CLASS_KEY, AppDatabase.class);
            ii.putExtra(DATABASE_NAME_KEY, "CoronaDB");
            ii.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(ii);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bottom_menu_items, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (!isMyServiceRunning(CurrentLocationService.class)) {
                        Intent intent = new Intent(MainActivity.this, CurrentLocationService.class);
                        if (Build.VERSION.SDK_INT >= 26) {
                            startForegroundService(intent);
                        } else {
                            // Pre-O behavior.
                            startService(intent);
                        }

                    } else{
                        //Toast.makeText(getApplicationContext(),"Service already running",Toast.LENGTH_SHORT).show();
                    }



                } else {
                    //Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    private void debugButtons(){
        btnGotCorona.setVisibility(View.VISIBLE);
        btnSeeDb.setVisibility(View.VISIBLE);
        btnSeeDb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDbView();
            }
        });

        btnGotCorona.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
                SharedPreferences.Editor ed;
                ed = sharedPrefs.edit();

                //Indicate that the default shared prefs have been set
                ed.putInt("corona_status", 1);
                ed.putString("infect_loc_lat", "52.082920");
                ed.putString("infect_loc_lon", "6.148990");
                ed.putLong("infect_time", 1583090413544L);
                ed.putString("infect_reason", "DEVICE");
                ed.apply();
                initStyle();
                initTexts();
            }
        });
    }

}

