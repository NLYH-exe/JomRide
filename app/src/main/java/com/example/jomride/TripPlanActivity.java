package com.example.jomride;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TripPlanActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng destinationLatLng;
    private String destinationName;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker userMarker;
    private Polyline routePolyline;
    private TextView destTextView, timeTextView, dateTextView;
    private Button timePickerButton, datePickerButton, backButton, setTripButton, selectFriendsButton;
    private LinearLayout friendsLayout;
    private LatLng currentLocation;

    private List<Friend> allFriends = Arrays.asList(
            new Friend("Sidney", "2406, Jln Seksyen 2/11, 31900 Kampar, Perak"),
            new Friend("Nigel", "Kampar Lake Campus Condominium, Jln Kolej, Taman Bandar Baru, 31900 Kampar, Perak"),
            new Friend("Stanley", "Meadow Park, 31900, Kampar, Perak")
    );

    // Add a global variable to store the waypoints
    private List<LatLng> selectedWaypoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_plan);

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
        selectFriendsButton = findViewById(R.id.select_friends_btn);
        friendsLayout = findViewById(R.id.friends_section);

        destTextView.setText(destinationName);

        timePickerButton.setOnClickListener(v -> showTimePickerDialog());
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());
        backButton.setOnClickListener(v -> finish());

        // Fix here: Don't send the route immediately
        setTripButton.setOnClickListener(v -> openGoogleMapsNavigation());

        selectFriendsButton.setOnClickListener(v -> showFriendSelectionDialog());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.trip_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void showFriendSelectionDialog() {
        String[] friendNames = new String[allFriends.size()];
        boolean[] selected = new boolean[allFriends.size()];
        for (int i = 0; i < allFriends.size(); i++) {
            friendNames[i] = allFriends.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Friends")
                .setMultiChoiceItems(friendNames, selected, (dialog, which, isChecked) -> selected[which] = isChecked)
                .setPositiveButton("OK", (dialog, which) -> {
                    List<Friend> selectedFriends = new ArrayList<>();
                    for (int i = 0; i < selected.length; i++) {
                        if (selected[i]) selectedFriends.add(allFriends.get(i));
                    }
                    // Fix here: Only resolve the addresses and store them
                    resolveAddressesAndDrawRoute(selectedFriends);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resolveAddressesAndDrawRoute(List<Friend> selectedFriends) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<LatLng> waypoints = new ArrayList<>();

                // Add selected friends' locations
                for (Friend friend : selectedFriends) {
                    List<Address> results = geocoder.getFromLocationName(friend.address, 1);
                    if (!results.isEmpty()) {
                        Address addr = results.get(0);
                        waypoints.add(new LatLng(addr.getLatitude(), addr.getLongitude()));
                    }
                }

                // Add current location and destination location
                waypoints.add(currentLocation);
                waypoints.add(destinationLatLng);

                // Store the waypoints globally
                selectedWaypoints.clear();
                selectedWaypoints.addAll(waypoints);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendRouteToGoogleMaps(List<LatLng> waypoints) {
        // Create a string with all the waypoints
        StringBuilder waypointsParam = new StringBuilder();
        for (LatLng point : waypoints) {
            waypointsParam.append(point.latitude).append(",").append(point.longitude).append("|");
        }
        // Remove the last pipe
        if (waypointsParam.length() > 0) {
            waypointsParam.setLength(waypointsParam.length() - 1);
        }

        // Construct the Google Maps navigation URL
        String url = "google.navigation:q=" + waypoints.get(waypoints.size() - 1).latitude + "," + waypoints.get(waypoints.size() - 1).longitude +
                "&waypoints=" + waypointsParam.toString() +
                "&mode=d"; // 'd' stands for driving mode

        // Start the Google Maps navigation intent
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGoogleMapsNavigation() {
        if (!selectedWaypoints.isEmpty()) {
            selectedWaypoints.add(destinationLatLng);  // Add destination at the end
            sendRouteToGoogleMaps(selectedWaypoints);  // Send the selected waypoints to Google Maps
        } else {
            Toast.makeText(this, "No friends selected for the trip", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRouteWithWaypoints(LatLng origin, LatLng dest, List<LatLng> waypoints) {
        StringBuilder waypointsParam = new StringBuilder();
        for (LatLng point : waypoints) {
            waypointsParam.append(point.latitude).append(",").append(point.longitude).append("|");
        }

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + dest.latitude + "," + dest.longitude +
                (waypoints.isEmpty() ? "" : "&waypoints=" + waypointsParam.toString()) +
                "&key=AIzaSyA0orkTD5Y6vQaIQxb9LxWV5Fer3c2HZY8";

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
                    List<LatLng> decodedPath = PolyUtil.decode(points);

                    // Draw polyline on the map
                    runOnUiThread(() -> {
                        if (routePolyline != null) {
                            routePolyline.remove();
                        }
                        routePolyline = mMap.addPolyline(new PolylineOptions().addAll(decodedPath).color(Color.BLUE));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            timeTextView.setText(hourOfDay + ":" + minute1);
        }, hour, minute, true).show();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            dateTextView.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1);
        }, year, month, day).show();
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14));
                userMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Your Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                // Add destination marker
                mMap.addMarker(new MarkerOptions().position(destinationLatLng).title(destinationName));

                // Start listening for location updates
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setInterval(10000);
                locationRequest.setFastestInterval(5000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult != null) {
                            Location location = locationResult.getLastLocation();
                            if (location != null) {
                                LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                if (userMarker != null) {
                                    userMarker.setPosition(newLocation);
                                }
                            }
                        }
                    }
                };

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        });
    }

    // Class for Friend data
    public static class Friend {
        String name;
        String address;

        Friend(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }
}
