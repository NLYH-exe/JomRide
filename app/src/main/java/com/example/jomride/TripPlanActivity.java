package com.example.jomride;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TripPlanActivity extends AppCompatActivity {

    private TextView destinationText, dateTimeText;
    private Button dateTimeButton, selectFriendsButton, scheduleTripButton;
    private RecyclerView friendRecycler;

    private List<UserModal> friendList = new ArrayList<>();
    private boolean[] friendChecked;
    private List<UserModal> selectedFriends = new ArrayList<>();

    private DatabaseReference usersRef, friendsRef;
    private String currentUserId, destination, selectedDateTime = "";
    private double destLat, destLng;
    private boolean friendsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_plan);

        // Wire up views
        destinationText = findViewById(R.id.destinationText);
        dateTimeText = findViewById(R.id.dateTimeText);
        dateTimeButton = findViewById(R.id.dateTimeButton);
        selectFriendsButton = findViewById(R.id.selectFriendsButton);
        scheduleTripButton = findViewById(R.id.scheduleTripButton);
        friendRecycler = findViewById(R.id.friendRecycler);

        // Get destination and coordinates from Intent
        destination = getIntent().getStringExtra("dest_name");
        destLat = getIntent().getDoubleExtra("dest_lat", 0);
        destLng = getIntent().getDoubleExtra("dest_lng", 0);

        // Check if destination and coordinates are valid
        if (destination == null || destLat == 0 || destLng == 0) {
            Toast.makeText(this, "Invalid destination or coordinates", Toast.LENGTH_SHORT).show();
            finish(); // Exit activity if data is invalid
            return;
        }

        destinationText.setText("Destination: " + (destination != null ? destination : "(none)"));

        // Firebase setup
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        friendsRef = usersRef.child(currentUserId).child("friends");

        // RecyclerView setup (optional)
        friendRecycler.setLayoutManager(new LinearLayoutManager(this));
        friendRecycler.setAdapter(new FriendSelectAdapter(this, friendList));

        // Disable selectFriendsButton until friends are loaded
        selectFriendsButton.setEnabled(false);

        // Fetch friends from Firebase
        fetchFriendsFromFirebase();

        // Button listeners
        dateTimeButton.setOnClickListener(v -> pickDateTime());
        selectFriendsButton.setOnClickListener(v -> showFriendDialog());
        scheduleTripButton.setOnClickListener(v -> scheduleTrip());
    }

    private void fetchFriendsFromFirebase() {
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                friendList.clear();
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    friendChecked = new boolean[0];
                    friendsLoaded = true;
                    selectFriendsButton.setEnabled(true);
                    return;
                }

                List<String> uids = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    uids.add(child.getKey());
                }
                friendChecked = new boolean[uids.size()];
                AtomicInteger remaining = new AtomicInteger(uids.size());

                for (String uid : uids) {
                    usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot ds) {
                            UserModal user = ds.getValue(UserModal.class);
                            if (user != null) {
                                user.setUid(ds.getKey());
                                friendList.add(user);
                            } else {
                                UserModal fallback = new UserModal(ds.getKey(), ds.getKey());
                                fallback.setUid(ds.getKey());
                                friendList.add(fallback);
                            }
                            friendRecycler.getAdapter().notifyDataSetChanged();
                            if (remaining.decrementAndGet() == 0) {
                                friendsLoaded = true;
                                selectFriendsButton.setEnabled(true);
                                Log.d("TripPlan", "All friends loaded: " + friendList.size());
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e("TripPlan", "Error loading friend " + uid, error.toException());
                            if (remaining.decrementAndGet() == 0) {
                                friendsLoaded = true;
                                selectFriendsButton.setEnabled(true);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("TripPlan", "Could not read friends node", error.toException());
                friendChecked = new boolean[0];
                friendsLoaded = true;
                selectFriendsButton.setEnabled(true);
            }
        });
    }

    private void showFriendDialog() {
        if (!friendsLoaded) {
            Toast.makeText(this, "Friends loading, please wait…", Toast.LENGTH_SHORT).show();
            return;
        }
        if (friendList.isEmpty()) {
            Toast.makeText(this, "No friends found", Toast.LENGTH_SHORT).show();
            return;
        }
        // Build names
        String[] names = new String[friendList.size()];
        for (int i = 0; i < friendList.size(); i++) {
            names[i] = friendList.get(i).getUsername();
        }
        new AlertDialog.Builder(this)
                .setTitle("Select Friends")
                .setMultiChoiceItems(names, friendChecked,
                        (dialog, which, isChecked) -> friendChecked[which] = isChecked)
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedFriends.clear();
                    for (int i = 0; i < friendChecked.length; i++) {
                        if (friendChecked[i]) {
                            selectedFriends.add(friendList.get(i));
                        }
                    }
                    Toast.makeText(this,
                            selectedFriends.size() + " friend(s) selected",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickDateTime() {
        final Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (v, y, m, d) -> {
            cal.set(y, m, d);
            new TimePickerDialog(this, (tp, h, min) -> {
                cal.set(Calendar.HOUR_OF_DAY, h);
                cal.set(Calendar.MINUTE, min);
                selectedDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(cal.getTime());
                dateTimeText.setText("When: " + selectedDateTime);
            },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false).show();
        },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void scheduleTrip() {
        if (selectedFriends.isEmpty()) {
            Toast.makeText(TripPlanActivity.this, "Please select at least one friend first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDateTime.isEmpty()) {
            Toast.makeText(TripPlanActivity.this, "Please select a date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> friendUids = new ArrayList<>();
        for (UserModal user : selectedFriends) {
            String uid = user.getUid();
            if (uid != null && !uid.isEmpty()) {
                friendUids.add(uid);
            } else {
                Log.e("TripPlan", "Skipping friend with invalid UID: " + user);
            }
        }

        usersRef.child(currentUserId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                String driverUsername = snap.getValue(String.class);

                TripData trip = new TripData(destination, selectedDateTime, friendUids, destLat, destLng);
                trip.setDriverUsername(driverUsername);
                Map<String, Object> tripValues = trip.toMap();

                DatabaseReference root = FirebaseDatabase.getInstance().getReference();
                DatabaseReference driverTripsRef = root.child("users").child(currentUserId).child("trips");
                String tripKey = driverTripsRef.push().getKey();

                Map<String, Object> updates = new HashMap<>();
                updates.put("/users/" + currentUserId + "/trips/" + tripKey, tripValues);
                for (String friendId : friendUids) {
                    updates.put("/users/" + friendId + "/trips/" + tripKey, tripValues);
                }

                root.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(TripPlanActivity.this, "Trip saved & shared!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Exception e = task.getException();
                        Log.e("TripPlan", "Batch write failed", e);
                        Toast.makeText(TripPlanActivity.this, "Failed to save trip", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TripPlan", "Failed to read driver username", error.toException());
                Toast.makeText(TripPlanActivity.this, "Failed to schedule trip", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
