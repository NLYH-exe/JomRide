package com.example.jomride;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserFragment extends Fragment {

    private RecyclerView rvTrips;
    private TripAdapter adapter;
    private List<Pair<String, TripData>> tripList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private DatabaseReference usersTripsRef;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Firebase setup
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return view;
        currentUserId = user.getUid();
        usersTripsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserId)
                .child("trips");

        // UI references
        TextView usernameText = view.findViewById(R.id.username_text);
        TextView addressText = view.findViewById(R.id.address_text);

        // Load user info
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    if (username != null) usernameText.setText(username);
                    if (address != null) addressText.setText(address);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user info", Toast.LENGTH_SHORT).show();
            }
        });

        // RecyclerView setup
        rvTrips = view.findViewById(R.id.rvTrips);
        rvTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TripAdapter(tripList, new TripAdapter.Listener() {
            @Override
            public void onGo(TripData trip, String key) {
                Intent i = new Intent(getContext(), FinalTripActivity.class);
                i.putExtra("TRIP_ID", key);
                startActivity(i);
            }

            @Override
            public void onCancel(TripData trip, String key) {
                cancelTrip(key, trip.getFriends());
            }
        });
        rvTrips.setAdapter(adapter);

        loadTrips();

        // Set onClickListener for Logout button
        Button logoutBtn = view.findViewById(R.id.logout_btn);
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            getActivity().finish();
        });


        // Set onClickListener for User Settings button
        Button settingsBtn = view.findViewById(R.id.user_settings_btn);
        settingsBtn.setOnClickListener(v -> {
            // Open the UserSettingsActivity
            Intent intent = new Intent(getActivity(), UserSettingsActivity.class);
            startActivity(intent);
        });

        // Set onClickListener for Friends button
        Button friendsBtn = view.findViewById(R.id.friend_btn);
        friendsBtn.setOnClickListener(v -> {
            // Open the FriendsActivity
            Intent intent = new Intent(getActivity(), FriendListActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadTrips() {
        usersTripsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                tripList.clear();
                for (DataSnapshot child : snap.getChildren()) {
                    String key = child.getKey();
                    TripData t = child.getValue(TripData.class);
                    tripList.add(new Pair<>(key, t));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.e("UserFragment", "Failed to load trips: " + e.getMessage());
            }
        });
    }

    private void cancelTrip(String tripKey, List<String> friends) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + currentUserId + "/trips/" + tripKey, null);
        for (String uid : friends) {
            updates.put("/users/" + uid + "/trips/" + tripKey, null);
        }
        root.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Trip cancelled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Error: " + task.getException().getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
