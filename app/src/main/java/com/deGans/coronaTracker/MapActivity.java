package com.deGans.coronaTracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.deGans.coronaTracker.Database.AppDatabase;
import com.deGans.coronaTracker.Models.LocationDto;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.internal.VisibilityAwareImageButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.view.View.GONE;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback{
    public TextView Location, Subtitle;
    public AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initStyle();
        setBottomNavigationListeners();
        setTexts();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.googleMaps);
        mapFragment.getMapAsync(this);
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "CoronaDB").fallbackToDestructiveMigration().allowMainThreadQueries().build();
    }


    @Override
    public void onMapReady(GoogleMap map) {

        LatLng loc = new LatLng(0, 0);

        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        switch(sharedPrefs.getInt("corona_status",0)) {
            case 0:
                LocationDto lastLoc = db.locationDao().getLast();
                if(lastLoc == null){
                    Location.setText("No location have been saved yet");
                    return;
                }
                loc = new LatLng(lastLoc.latitude,lastLoc.longitude);
                break;
            case 1:
            case 2:
                loc = new LatLng(Double.valueOf(sharedPrefs.getString("infect_loc_lat","0")),Double.valueOf(sharedPrefs.getString("infect_loc_lon","0") ));
                break;
        }

        map.addMarker(new MarkerOptions().position(loc)
                .title("Last saved location"));

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc,16));
    }
    private void setTexts(){
        Subtitle =  findViewById(R.id.infection_subtitle);
        Location =  findViewById(R.id.location);
        // Creating date format
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");

        // Creating date from milliseconds
        // using Date() constructor

        SharedPreferences sharedPrefs = getSharedPreferences("corona", MODE_PRIVATE);
        Date result = new Date(sharedPrefs.getLong("infect_time", 0));
        switch(sharedPrefs.getInt("corona_status",0)) {
            case 0:
                //not infected
                Subtitle.setVisibility(GONE);
                Location.setText("Your last saved location");
                break;
            case 1:
            case 2:
                //posible infection
                Subtitle.setVisibility(View.VISIBLE);
                Subtitle.setText("Time of contact: " + simple.format(result) + "\n Source: "  + sharedPrefs.getString("infect_reason", "unknown" ));

                Location.setText("Location of contact");
                break;
        }
    }
    private void setBottomNavigationListeners(){
        final BottomNavigationView itemView = findViewById(R.id.bottom_navigation);
        itemView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.overview:
                                finish();
                                overridePendingTransition(0, 0);
                                break;
                            case R.id.donate:
                                NavigateTo(DonateActivity.class, R.id.donate);
                                break;
                        }
                        return true;
                    }
                });
    }
    private void NavigateTo(Class<?> cls, int item){
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();

    }
    private void initStyle(){
        View mainView = findViewById(R.id.mainBackground);
        BottomNavigationView itemView = findViewById(R.id.bottom_navigation);

        //set bg of navigationbar
        itemView.setBackgroundColor(Color.parseColor(getRightBackgroundAccentColor()));

        //set bg of mainview
        mainView.setBackgroundColor(Color.parseColor(getRightBackgroundColor()));

        itemView.setSelectedItemId(R.id.map);
        //change notificationbar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(getRightBackgroundAccentColor()));
        }
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
}
