package com.example.jomride;

import com.google.android.gms.maps.model.LatLng;

public class LatLngWrapper {
    private double latitude;
    private double longitude;

    public LatLngWrapper() {
        // Default constructor required for Firebase deserialization
    }

    public LatLngWrapper(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // Convert to LatLng object
    public LatLng toLatLng() {
        return new LatLng(latitude, longitude);
    }

    // Static method to create LatLngWrapper from LatLng
    public static LatLngWrapper fromLatLng(LatLng latLng) {
        return new LatLngWrapper(latLng.latitude, latLng.longitude);
    }
}

