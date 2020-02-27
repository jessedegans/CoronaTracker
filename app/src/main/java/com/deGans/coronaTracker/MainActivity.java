package com.deGans.coronaTracker;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import 	androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings.Secure;

import com.deGans.coronaTracker.BackgroundServices.CurrentLocationService;
import com.deGans.coronaTracker.Database.AppDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.wajahatkarim3.roomexplorer.RoomExplorerActivity;

import static com.wajahatkarim3.roomexplorer.RoomExplorerActivity.DATABASE_CLASS_KEY;
import static com.wajahatkarim3.roomexplorer.RoomExplorerActivity.DATABASE_NAME_KEY;

public class MainActivity extends AppCompatActivity {

    public static final String RECEIVE_JSON = "com.deGans.coronaTracker.RECEIVE_JSON";
    Button btnLocationSharing,btnSeeDb;
    TextView txtCoordinates, txtAddress;
    Double Latitude, Longitude;
    String Provider;
    BroadcastReceiver receiver;
    private String android_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        initReference();
        initListener();
        android_id = Secure.getString(getApplicationContext().getContentResolver(),
                Secure.ANDROID_ID);

        receiver  = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(RECEIVE_JSON)) {
                    Provider = intent.getStringExtra("Provider");
                    Latitude = (Double)intent.getExtras().get("Latitude");
                    Longitude = (Double)intent.getExtras().get("Longitude");
                    txtAddress.setText("Provider : "+Provider);
                    txtCoordinates.setText("Lat:" + Latitude + " ,Long:" + Longitude);

                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    txtAddress.setText("Provider : "+Provider);
                    DatabaseReference myRef = database.getReference("current").child(android_id).child("coordinates");
                    myRef.child("0").setValue(Latitude);
                    myRef.child("1").setValue(Longitude);

                    DatabaseReference historyRef = database.getReference("history").child(android_id).child(String.valueOf(System.currentTimeMillis()));
                    historyRef.child("0").setValue(Latitude);
                    historyRef.child("1").setValue(Longitude);
                }
            }
        };

        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_JSON);
        bManager.registerReceiver(receiver, intentFilter);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
        } else{
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    1);
        }
        ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION },
                1);

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

        btnLocationSharing = (Button)findViewById(R.id.btnLocationSharing);
        btnSeeDb = (Button)findViewById(R.id.btnSeeDb);
        txtAddress = (TextView) findViewById(R.id.txtAddress);
        txtCoordinates = (TextView) findViewById(R.id.txtCoordinates);
    }

    private void initListener() {
        btnLocationSharing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btnLocationSharing.getText().toString().equalsIgnoreCase("Start location sharing")){
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                                    PackageManager.PERMISSION_GRANTED) {
                    } else{
                        ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION },
                                1);
                    }
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION },
                            1);

                }else{
                }
            }
        });
        btnSeeDb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDbView();
            }
        });
        txtAddress = (TextView) findViewById(R.id.txtAddress);
        txtCoordinates = (TextView) findViewById(R.id.txtCoordinates);
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
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                        Toast.makeText(getApplicationContext(),"Service already running",Toast.LENGTH_SHORT).show();
                    }

                    btnLocationSharing.setText("Stop location sharing");

                } else {
                    Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}

