package com.example.jomride;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserSettingsActivity extends AppCompatActivity {

    private TextView emailText;
    private EditText usernameInput, addressInput;
    private Button saveButton, cancelButton;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference usersRef;

    private static final String TAG = "UserSettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        // Initialize UI elements
        emailText = findViewById(R.id.email_text);
        usernameInput = findViewById(R.id.username_input);
        addressInput = findViewById(R.id.address_input);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();
        usersRef = FirebaseDatabase.getInstance("https://jom-ride-6156b-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users")
                .child(userId);

        // Set current email
        emailText.setText(currentUser.getEmail());

        // Load existing username/address if available
        usersRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String username = snapshot.child("username").getValue(String.class);
                String address = snapshot.child("address").getValue(String.class);

                if (username != null) usernameInput.setText(username);
                if (address != null) addressInput.setText(address);
            }
        });

        saveButton.setOnClickListener(v -> saveUserSettings());
        cancelButton.setOnClickListener(v -> finish()); // Just finish activity
    }

    private void saveUserSettings() {
        String newUsername = usernameInput.getText().toString().trim();
        String newAddress = addressInput.getText().toString().trim();

        if (newUsername.isEmpty() || newAddress.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Saving data: Username = " + newUsername + ", Address = " + newAddress);

        usersRef.child("username").setValue(newUsername);
        usersRef.child("address").setValue(newAddress)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Just go back (UserFragment will refresh automatically when loaded again)
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
