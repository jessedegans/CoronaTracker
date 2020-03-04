package com.deGans.coronaTracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DonateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        initStyle();
        setBottomNavigationListeners();

        WebView wView = (WebView)findViewById(R.id.donateWebView);
        wView.loadUrl("https://www.gofundme.com/f/corona-tracker");
        WebSettings webSettings = wView.getSettings();
        webSettings.setJavaScriptEnabled(true);

    }
    private void setBottomNavigationListeners(){
        BottomNavigationView itemView = findViewById(R.id.bottom_navigation);
        itemView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.overview:
                                finish();
                                overridePendingTransition(0, 0);
                                break;
                            case R.id.map:
                                NavigateTo(MapActivity.class);
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
        finish();

    }
    private void initStyle(){
        View mainView = findViewById(R.id.mainBackground);
        BottomNavigationView itemView = findViewById(R.id.bottom_navigation);

        //set bg of navigationbar
        itemView.setBackgroundColor(Color.parseColor(getRightBackgroundAccentColor()));

        //set bg of mainview
        mainView.setBackgroundColor(Color.parseColor(getRightBackgroundColor()));

        itemView.setSelectedItemId(R.id.donate);
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
