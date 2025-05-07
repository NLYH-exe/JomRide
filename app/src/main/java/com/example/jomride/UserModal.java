package com.example.jomride;

public class UserModal {
    private String uid;
    private String username;
    private String address; // ✅ Add this line

    // Zero‑argument constructor for Firebase
    public UserModal() { }

    // Convenience constructor
    public UserModal(String uid, String username) {
        this.uid = uid;
        this.username = username;
    }

    // UID
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    // Username
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    // Address ✅ Add getter and setter
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
