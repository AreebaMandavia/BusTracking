package com.example.bustracking;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Garage extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private carInfoAdapter adapter;
    private Driver model;
    private ArrayList<Driver>carInfoList;
    private MaterialCardView addNewCar;
    private String longitude,latitude,address;
    private RecyclerView carRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_garage);

        carRecyclerView=findViewById(R.id.CarRecyclerView);

        firestore=FirebaseFirestore.getInstance();
        carInfoList=new ArrayList<>();
        carRecyclerView.setHasFixedSize(true);
        getCarInfo();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void getCarInfo() {
        firestore.collection("BusInfo").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        new Thread(() -> {
                            ArrayList<Driver> tempList = new ArrayList<>();

                            for (DocumentSnapshot d : queryDocumentSnapshots.getDocuments()) {
                                Driver model = d.toObject(Driver.class);
                                String latitude = model.getLat();
                                String longitude = model.getLng();

                                if (longitude != null && latitude != null) {
                                    try {
                                        double lat = Double.parseDouble(latitude);
                                        double lng = Double.parseDouble(longitude);
                                        String address = getAddressFromLatLong(Garage.this, lat, lng);
                                        model.setAddress(address);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                        model.setAddress("Invalid Coordinates");
                                    }
                                } else {
                                    model.setAddress("Location Not Available");
                                }

                                tempList.add(model);
                            }

                            runOnUiThread(() -> {
                                carInfoList.clear();
                                carInfoList.addAll(tempList);
                                adapter = new carInfoAdapter(Garage.this, carInfoList);
                                carRecyclerView.setAdapter(adapter);
                                carRecyclerView.setLayoutManager(new LinearLayoutManager(Garage.this));
                                adapter.notifyDataSetChanged();
                            });
                        }).start();
                    }
                })
                .addOnFailureListener(e -> runOnUiThread(() ->
                        Toast.makeText(Garage.this, "Failed to fetch data", Toast.LENGTH_SHORT).show()));
    }




    public String getAddressFromLatLong(Context context, double LATITUDE, double LONGITUDE){
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null && addresses.size() > 0) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown Location";
    }


}