package com.example.bustracking;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassengerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private List<LatLng> predefinedRoute = Arrays.asList(
            new LatLng(24.9013, 67.1153),
            new LatLng(24.8933, 67.0882),
            new LatLng(24.8754, 67.0410),
            new LatLng(24.8718787, 67.0325772)
    );
    private Marker movingBusMarker;
    private int routeIndex = 0;
    private static final double BUS_NEARBY_THRESHOLD_KM = 0.5;
    private boolean hasNotifiedBusNearby = false;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_map);

        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "bus_channel", "Bus Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts when bus is nearby");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("MAP-READY", "Map is initialized");
        getCurrentLocationAndNearbyBuses();
        startBusRouteSimulation();

    }
    private void startBusRouteSimulation() {
        if (predefinedRoute == null || predefinedRoute.size() < 2) return;

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(predefinedRoute)
                .width(8)
                .color(Color.BLUE);
        mMap.addPolyline(polylineOptions);


        // Start from first location
        LatLng startPoint = predefinedRoute.get(0);
        movingBusMarker = mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Moving Bus")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        routeIndex = 1;
        moveMarkerStepByStep();
    }
    private LatLng userLatLng; // Store globally

    private void moveMarkerStepByStep() {
        if (routeIndex >= predefinedRoute.size()) return;

        LatLng nextPoint = predefinedRoute.get(routeIndex);

        // Check if bus is near user
        if (userLatLng != null && !hasNotifiedBusNearby) {
            double distanceToUser = distanceBetween(
                    userLatLng.latitude, userLatLng.longitude,
                    nextPoint.latitude, nextPoint.longitude
            );
            if (distanceToUser <= BUS_NEARBY_THRESHOLD_KM) {
                sendBusNearbyNotification();
                hasNotifiedBusNearby = true; // Prevent duplicate notifications
            }
        }

        animateMarkerTo(movingBusMarker, nextPoint, () -> {
            routeIndex++;
            new Handler().postDelayed(this::moveMarkerStepByStep, 2000);
        });
    }

    private void animateMarkerTo(final Marker marker, final LatLng finalPosition, final Runnable onAnimationEnd) {
        final long duration = 5000; // 1 second
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final LatLng startLatLng = marker.getPosition();

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lat = (finalPosition.latitude - startLatLng.latitude) * t + startLatLng.latitude;
                double lng = (finalPosition.longitude - startLatLng.longitude) * t + startLatLng.longitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                } else {
                    marker.setPosition(finalPosition);
                    if (onAnimationEnd != null) onAnimationEnd.run();
                }
            }
        });
    }


    private void getCurrentLocationAndNearbyBuses() {
        Log.d("LOCATION", "Getting current location...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d("LOCATION", "Got current location: " + location.getLatitude() + ", " + location.getLongitude());

                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                        mMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));

                        loadBusesNearby(userLatLng);
                    }
                });
    }

    private void loadBusesNearby(LatLng userLatLng) {
        firestore.collection("BusInfo").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String latStr = doc.getString("lat");
                    String lngStr = doc.getString("lng");
                    String busId = doc.getString("busId");

                    Log.d("BUS-DATA", "busId: " + busId + ", lat: " + latStr + ", lng: " + lngStr);

                    if (latStr != null && lngStr != null && !latStr.isEmpty() && !lngStr.isEmpty()) {
                        try {
                            double lat = Double.parseDouble(latStr);
                            double lng = Double.parseDouble(lngStr);

                            LatLng busLocation = new LatLng(lat, lng);
                            double distance = distanceBetween(userLatLng.latitude, userLatLng.longitude, lat, lng);

                            Log.d("BUS-LOCATION", "Distance from user: " + distance + " km");
                            mMap.addMarker(new MarkerOptions()
                                    .position(busLocation)
                                    .title("Bus " + busId)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                            Log.d("BUS-MAP", "Marker added for Bus " + busId);

                        } catch (Exception e) {
                            Log.e("PARSE-ERROR", "Error parsing lat/lng", e);
                        }
                    }
                }
            } else {
                Log.e("FIRESTORE-ERROR", "Failed to fetch bus data", task.getException());
            }
        });
    }
    private void sendBusNearbyNotification() {
        Toast.makeText(this, "A bus is near your location!", Toast.LENGTH_LONG).show();


    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "bus_channel")
            .setContentTitle("Bus Nearby")
            .setContentText("A bus is near your location. Get ready!")
            .setPriority(NotificationCompat.PRIORITY_HIGH);

    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        notificationManager.notify(1, builder.build());
    }

    }



    private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0; // convert to kilometers
    }
}

