package com.example.bustracking;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.health.connect.datatypes.ExerciseRoute;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddVehicle extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 1;

    private SwitchMaterial getLocation;
    private FirebaseStorage storage;
    private StorageReference mStorageref;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId,DocId;
    private EditText editBusNo,editBusRoute,editBusLocation;
    private Button uploadBtn;
    private TextView carLocation;
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;
    private String carLongitude,carLatitude,address;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_vehicle);
        firestore=FirebaseFirestore.getInstance();
        storage=FirebaseStorage.getInstance();
        mStorageref=storage.getReference();

        firebaseAuth=FirebaseAuth.getInstance();
        currentUserId=firebaseAuth.getCurrentUser().getUid();
        mStorageref=storage.getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        editBusNo=findViewById(R.id.editTextId);
        editBusRoute=findViewById(R.id.editTextRoute);
        uploadBtn=findViewById(R.id.uploadBtn);
        carLocation=findViewById(R.id.carLocation);
        getLocation=findViewById(R.id.locSwitch);

        getLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    checkedLocationPermission();
                }
            }
        });

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadBusInfo();
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
        final AlertDialog.Builder builder=new AlertDialog.Builder(AddVehicle.this);
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

                getAddressFromLatLong(AddVehicle.this,latitude,longitude);
            } else {
                carLocation.setText("Location not available. Try again.");
            }
        });
    }

    public void getAddressFromLatLong(Context context,double LATITUDE,double LONGITUDE){
        try{
            Geocoder geocoder=new Geocoder(context, Locale.getDefault());
            List<Address>addresses=geocoder.getFromLocation(LATITUDE,LONGITUDE,1);
            if(addresses!=null && addresses.size()>0){
                address=addresses.get(0).getAddressLine(0);
            }
            carLocation.setText(address);
            carLocation.setSelected(true);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void uploadBusInfo(){
        String busID=editBusNo.getText().toString().trim();
        String busRoute=editBusRoute.getText().toString().trim();

        if(TextUtils.isEmpty(busID) && TextUtils.isEmpty(busRoute)){
            Toast.makeText(this,"Please Fill all Fields",Toast.LENGTH_SHORT).show();
        }
        else{
            DocumentReference documentReference=firestore.collection("BusInfo").document();
            Driver driver=new Driver(busID,busRoute,carLatitude,carLongitude,"",currentUserId,address);
            documentReference.set(driver, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        if(task.isSuccessful()) {
                            DocId=documentReference.getId();
                            driver.setDocId(DocId);
                            documentReference.set(driver,SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        Toast.makeText(AddVehicle.this,"Upload Successful",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(AddVehicle.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(AddVehicle.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}