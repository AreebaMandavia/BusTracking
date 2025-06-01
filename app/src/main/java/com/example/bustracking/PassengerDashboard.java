package com.example.bustracking;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Locale;

public class PassengerDashboard extends AppCompatActivity {

    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION = 1;
    private String carLongitude,carLatitude,address;
    private TextView carLocation;
    private Button trackBusBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_passenger_dashboard);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        carLocation=findViewById(R.id.carLocation);
        trackBusBtn=findViewById(R.id.trackBus);

        trackBusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(PassengerDashboard.this,PassengerMapActivity.class);
                startActivity(intent);
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void checkedLocationPermission() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            locationManager=(LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            onGPS();
        }else{
            getCurrentLocation();
        }
    }

    private void onGPS() {
        final AlertDialog.Builder builder=new AlertDialog.Builder(PassengerDashboard.this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        final AlertDialog dialog=builder.create();
        dialog.show();
    }
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                carLatitude=String.valueOf(latitude);
                carLongitude=String.valueOf(longitude);
                String loc=carLatitude + ", " + carLongitude;

                getAddressFromLatLong(PassengerDashboard.this,latitude,longitude);
            } else {
                carLocation.setText("Location not available. Try again.");
            }
        });
    }

    public void getAddressFromLatLong(Context context,double LATITUDE,double LONGITUDE){
        try{
            Geocoder geocoder=new Geocoder(context, Locale.getDefault());
            List<Address> addresses=geocoder.getFromLocation(LATITUDE,LONGITUDE,1);
            if(addresses!=null && addresses.size()>0){
                address=addresses.get(0).getAddressLine(0);
            }
            carLocation.setText(address);
            carLocation.setSelected(true);

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}