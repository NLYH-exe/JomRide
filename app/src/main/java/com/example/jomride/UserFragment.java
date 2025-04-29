package com.example.jomride;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserFragment extends Fragment {

    private TextView usernameText, addressText;
    private Button logoutButton, userSettingsButton, friendListButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private static final String TAG = "UserFragment";

    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://jom-ride-6156b-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        // Initialize UI elements
        usernameText = view.findViewById(R.id.username_text);
        addressText = view.findViewById(R.id.address_text);
        logoutButton = view.findViewById(R.id.logout_btn);
        userSettingsButton = view.findViewById(R.id.user_settings_btn);
        friendListButton = view.findViewById(R.id.friend_btn);

        // Load user data
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            loadUserData(userId);
        } else {
            Log.e(TAG, "User not logged in");
            // Optionally redirect to login
        }

        // Set listeners
        logoutButton.setOnClickListener(v -> logout());
        userSettingsButton.setOnClickListener(v -> openUserSettings());
        friendListButton.setOnClickListener(v -> openFriendList());

        return view;
    }

    private void loadUserData(@NonNull String userId) {
        mDatabase.child("users").child(userId).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String address = dataSnapshot.child("address").getValue(String.class);

                        usernameText.setText(username != null ? username : "No Username");
                        addressText.setText(address != null ? address : "No Address");
                    } else {
                        Log.e(TAG, "No user data found");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load user data", e));
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void openUserSettings() {
        Intent intent = new Intent(getActivity(), UserSettingsActivity.class);
        startActivity(intent);
    }

    private void openFriendList() {
        Intent intent = new Intent(getActivity(), FriendListActivity.class);
        startActivity(intent);
    }
}
