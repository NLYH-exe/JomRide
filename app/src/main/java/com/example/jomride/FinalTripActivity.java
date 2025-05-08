package com.example.jomride;

import android.Manifest;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.ArrayList;
import java.util.List;

public class FinalTripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference dbRef;
    private String tripId;
    private String currentUserId;
    private FusedLocationProviderClient fusedLocationClient;
    private List<LatLng> routePoints = new ArrayList<>();
    private static final String TAG = "TripLiveMap";
    private ImageButton recenterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_trip);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize FusedLocationProviderClient for real-time location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        tripId = getIntent().getStringExtra("TRIP_ID");

        dbRef = FirebaseDatabase.getInstance().getReference();

        // Set up recenter button
        recenterButton = findViewById(R.id.recenterButton);
        recenterButton.setOnClickListener(new View.OnClickListener() {
            @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
            @Override
            public void onClick(View v) {
                recenterMapOnUserLocation();
            }
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
        loadTripDetails();
    }

    private void loadTripDetails() {
        DatabaseReference tripRef = dbRef.child("users").child(currentUserId).child("trips").child(tripId);
        tripRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double destLat = snapshot.child("destLat").getValue(Double.class);
                    double destLng = snapshot.child("destLng").getValue(Double.class);
                    LatLng destination = new LatLng(destLat, destLng);
                    routePoints.add(destination);

                    // Add current user's location (you) as the first point
                    addUserLocationToRoute();

                    // Get friends' locations
                    List<String> friendIds = new ArrayList<>();
                    for (DataSnapshot friendSnap : snapshot.child("friends").getChildren()) {
                        friendIds.add(friendSnap.getValue(String.class));
                    }
                    loadFriendLocations(friendIds, destination);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Trip load failed: " + error.getMessage());
            }
        });
    }

    private void loadFriendLocations(List<String> friendIds, LatLng destination) {
        DatabaseReference locationsRef = dbRef.child("locations");
        for (String friendId : friendIds) {
            locationsRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Double lat = snapshot.child("latitude").getValue(Double.class);
                    Double lng = snapshot.child("longitude").getValue(Double.class);
                    if (lat != null && lng != null) {
                        LatLng loc = new LatLng(lat, lng);
                        routePoints.add(loc); // add friend's location to route
                        mMap.addMarker(new MarkerOptions().position(loc).title("Friend"));
                        drawRoute();  // Update polyline after each friend's location is fetched
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Friend location load failed: " + error.getMessage());
                }
            });
        }
    }

    // Add the current user's location as the first marker
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void addUserLocationToRoute() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            routePoints.add(0, userLocation); // add user location first in the route
                            drawRoute();  // Draw route once user's location is added
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16)); // Adjust zoom level
                        }
                    }
                });
    }

    // Draw the route on the map
    private void drawRoute() {
        if (routePoints.size() < 2) return;

        StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        urlBuilder.append("origin=").append(routePoints.get(0).latitude).append(",").append(routePoints.get(0).longitude);
        urlBuilder.append("&destination=").append(routePoints.get(routePoints.size() - 1).latitude).append(",").append(routePoints.get(routePoints.size() - 1).longitude);

        if (routePoints.size() > 2) {
            urlBuilder.append("&waypoints=");
            for (int i = 1; i < routePoints.size() - 1; i++) {
                urlBuilder.append(routePoints.get(i).latitude).append(",").append(routePoints.get(i).longitude);
                if (i < routePoints.size() - 2) urlBuilder.append("|");
            }
        }

        urlBuilder.append("&key=Google_API_KEY"); // Replace with your actual Google Maps API key

        String url = urlBuilder.toString();

        new Thread(() -> {
            try {
                java.net.URL requestUrl = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) requestUrl.openConnection();
                conn.connect();

                java.io.InputStream inputStream = conn.getInputStream();
                java.io.InputStreamReader reader = new java.io.InputStreamReader(inputStream);
                com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
                com.google.gson.JsonObject json = parser.parse(reader).getAsJsonObject();

                String encodedPolyline = json
                        .getAsJsonArray("routes")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("overview_polyline")
                        .get("points").getAsString();

                List<LatLng> decodedPath = decodePolyline(encodedPolyline);

                runOnUiThread(() -> {
                    mMap.addPolyline(new PolylineOptions()
                            .addAll(decodedPath)
                            .color(0xFF0000FF));
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading directions: " + e.getMessage());
            }
        }).start();
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }



    // Recenter the map on user's location
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void recenterMapOnUserLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));
                        } else {
                            Toast.makeText(FinalTripActivity.this, "Unable to get your location.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
