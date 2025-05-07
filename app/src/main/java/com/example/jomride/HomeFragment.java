package com.example.jomride;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.app.AlertDialog;
import android.widget.ImageButton;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private EditText searchBar;
    private Marker searchMarker;
    private PlacesClient placesClient;
    private LatLng selectedDestinationLatLng;
    private String selectedDestinationName;
    private ImageButton btnMyLocation;

    private DatabaseReference placesRef;  // Firebase reference for places

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        placesRef = database.getReference("places");

        // Initialize Places API
        Places.initialize(requireContext(), "AIzaSyA0orkTD5Y6vQaIQxb9LxWV5Fer3c2HZY8");
        placesClient = Places.createClient(requireContext());

        // Fused location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Search bar
        searchBar = view.findViewById(R.id.search_bar);
        searchBar.setOnClickListener(v -> openPlaceSearch());

        btnMyLocation = view.findViewById(R.id.btnMyLocation);
        btnMyLocation.setOnClickListener(v -> getCurrentLocation());

        // Load map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Get UiSettings object
        UiSettings uiSettings = mMap.getUiSettings();
        // Hide the my-location button (recenter button)
        uiSettings.setMyLocationButtonEnabled(false);

        // Custom map style
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
            if (!success) Log.e("MapStyle", "Style parsing failed.");
        } catch (Resources.NotFoundException e) {
            Log.e("MapStyle", "Can't find style. Error: ", e);
        }

        // Location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // POI click
        mMap.setOnPoiClickListener(this::showPoiPopup);

        // Marker click (for red pins)
        mMap.setOnMarkerClickListener(marker -> {
            if ("search_result".equals(marker.getTag())) {
                showPlacePopup(marker.getTitle(), marker.getPosition());
                return true;
            }
            return false;
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 15));
                } else {
                    Toast.makeText(requireContext(), "Turn on GPS", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openPlaceSearch() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext());
        startActivityForResult(intent, 100);
    }

    private void showPoiPopup(PointOfInterest poi) {
        if (poi == null) {
            Log.e("HomeFragment", "POI is null");
            return; // Exit if POI is null
        }

        Log.d("HomeFragment", "POI clicked: " + poi.name);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.popup_place, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView title = dialogView.findViewById(R.id.place_title);
        Button navigateBtn = dialogView.findViewById(R.id.navigate_button_popup);
        ImageView closeBtn = dialogView.findViewById(R.id.close_button);

        title.setText(poi.name);

        navigateBtn.setOnClickListener(v -> {
            selectedDestinationLatLng = poi.latLng;
            selectedDestinationName = poi.name;
            Intent intent = new Intent(getContext(), TripPlanActivity.class);
            intent.putExtra("dest_name", poi.name);
            intent.putExtra("dest_lat", poi.latLng.latitude);
            intent.putExtra("dest_lng", poi.latLng.longitude);
            startActivity(intent);
            dialog.dismiss();
        });

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void showPlacePopup(String name, LatLng latLng) {
        Log.d("HomeFragment", "showPlacePopup called with name: " + name + ", LatLng: " + latLng); // Added logging
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.popup_place, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView title = dialogView.findViewById(R.id.place_title);
        Button navigateBtn = dialogView.findViewById(R.id.navigate_button_popup);
        ImageView closeBtn = dialogView.findViewById(R.id.close_button);

        title.setText(name);

        navigateBtn.setOnClickListener(v -> {
            selectedDestinationLatLng = latLng;
            selectedDestinationName = name;
            Log.d("HomeFragment", "Navigating to destination: " + selectedDestinationName + " at " + selectedDestinationLatLng); // Added logging
            Intent intent = new Intent(getContext(), TripPlanActivity.class);
            intent.putExtra("dest_name", name);
            intent.putExtra("dest_lat", latLng.latitude);
            intent.putExtra("dest_lng", latLng.longitude);
            startActivity(intent);
            dialog.dismiss();
        });

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == AutocompleteActivity.RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            if (place.getLatLng() != null) {
                LatLng location = place.getLatLng();
                searchBar.setText(place.getName());

                if (searchMarker != null) searchMarker.remove();

                searchMarker = mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(place.getName())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                searchMarker.setTag("search_result");

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
            }
        }
    }

    // PlaceData class to store place info
    public static class PlaceData {
        public String name;
        public double latitude;
        public double longitude;

        public PlaceData(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
