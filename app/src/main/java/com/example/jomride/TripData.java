package com.example.jomride;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripData {
    private String destination;
    private String dateTime;
    private List<String> friends;
    private String driverUsername;
    private double destLat;
    private double destLng;
    private Map<String, String> friendsAddresses;

    // No-argument constructor required for Firebase
    public TripData() {
        // Empty constructor for Firebase
    }

    // Constructor for initializing data
    public TripData(String destination, String dateTime, List<String> friends, double destLat, double destLng) {
        this.destination = destination;
        this.dateTime = dateTime;
        this.friends = friends;
        this.destLat = destLat;
        this.destLng = destLng;
        this.friendsAddresses = friendsAddresses;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public String getDriverUsername() {
        return driverUsername;
    }

    public void setDriverUsername(String driverUsername) {
        this.driverUsername = driverUsername;
    }

    public double getDestLat() {
        return destLat;
    }

    public void setDestLat(double destLat) {
        this.destLat = destLat;
    }

    public double getDestLng() {
        return destLng;
    }

    public void setDestLng(double destLng) {
        this.destLng = destLng;
    }

    public Map<String, String> getFriendsAddresses() {
        return friendsAddresses;
    }

    public void setFriendsAddresses(Map<String, String> friendsAddresses) {
        this.friendsAddresses = friendsAddresses;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("destination", destination);
        result.put("dateTime", dateTime);
        result.put("driverUsername", driverUsername);
        result.put("friends", friends);
        result.put("destLat", destLat);
        result.put("destLng", destLng);
        result.put("friendsAddresses", friendsAddresses);
        return result;
    }
}
