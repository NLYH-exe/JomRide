package com.example.jomride;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

public class TripPlanActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng destinationLatLng;
    private String destinationName;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker userMarker;
    private Polyline routePolyline;
    private TextView destTextView, timeTextView, dateTextView;
    private Button timePickerButton, datePickerButton, backButton, setTripButton;
    private LinearLayout friendsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_plan);

        // Initialize views
        destinationName = getIntent().getStringExtra("dest_name");
        double lat = getIntent().getDoubleExtra("dest_lat", 0);
        double lng = getIntent().getDoubleExtra("dest_lng", 0);
        destinationLatLng = new LatLng(lat, lng);

        destTextView = findViewById(R.id.destination_text);
        timeTextView = findViewById(R.id.selected_time);
        dateTextView = findViewById(R.id.selected_date);
        timePickerButton = findViewById(R.id.pick_time_btn);
        datePickerButton = findViewById(R.id.pick_date_btn);
        backButton = findViewById(R.id.back_btn);
        setTripButton = findViewById(R.id.set_trip_btn);
        friendsLayout = findViewById(R.id.friends_section);

        destTextView.setText(destinationName);

        // Set up the time picker
        timePickerButton.setOnClickListener(v -> showTimePickerDialog());

        // Set up the date picker
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());

        // Set up back button to go back to the home screen
        backButton.setOnClickListener(v -> finish());

        // Set up the set trip button to open Google Maps navigation
        setTripButton.setOnClickListener(v -> openGoogleMapsNavigation());

        // Initialize FusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the Google Map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.trip_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // Show the time picker dialog
    private void showTimePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute1);
                    timeTextView.setText(time);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    // Show the date picker dialog
    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    dateTextView.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    // Open Google Maps in navigation mode to the destination
    private void openGoogleMapsNavigation() {
        if (destinationLatLng != null) {
            String uri = "google.navigation:q=" + destinationLatLng.latitude + "," + destinationLatLng.longitude + "&mode=d";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            );
            if (!success) {
                Log.e("MapStyle", "Style parsing failed.");
            }
        } catch (Exception e) {
            Log.e("MapStyle", "Can't find style. Error: ", e);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        mMap.setMyLocationEnabled(true);
        startLocationUpdates();
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (userMarker == null) {
                        userMarker = mMap.addMarker(new MarkerOptions()
                                .position(userLatLng)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                .title("You"));
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(userLatLng)
                                        .zoom(16f)
                                        .bearing(0)
                                        .tilt(0)
                                        .build()));
                        drawRoute(userLatLng, destinationLatLng);
                    } else {
                        userMarker.setPosition(userLatLng);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void drawRoute(LatLng origin, LatLng dest) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + origin.latitude + "," + origin.longitude
                + "&destination=" + dest.latitude + "," + dest.longitude
                + "&key=AIzaSyA0orkTD5Y6vQaIQxb9LxWV5Fer3c2HZY8";  // Replace with your API key

        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.connect();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }

                JSONObject json = new JSONObject(responseBuilder.toString());
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String points = overviewPolyline.getString("points");

                    List<LatLng> polylineList = PolyUtil.decode(points);

                    runOnUiThread(() -> {
                        if (routePolyline != null) {
                            routePolyline.remove();
                        }
                        routePolyline = mMap.addPolyline(new PolylineOptions()
                                .addAll(polylineList)
                                .width(15)
                                .color(Color.parseColor("#4285F4"))
                                .geodesic(true));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to fetch directions", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
