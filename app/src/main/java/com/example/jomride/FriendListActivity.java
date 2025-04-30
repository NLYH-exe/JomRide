package com.example.jomride;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.List;

public class FriendListActivity extends AppCompatActivity {
    private RecyclerView rvSearch, rvFriends;
    private EditText searchBar;
    private Button btnSearch;
    private FriendListAdapter searchAdapter, friendsAdapter;
    private DatabaseReference usersRef;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        // Init views
        searchBar = findViewById(R.id.search_input);
        btnSearch = findViewById(R.id.search_button);
        rvSearch = findViewById(R.id.recycler_search_results);
        rvFriends = findViewById(R.id.recycler_friends);

        // Firebase setup
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUid = user.getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // RecyclerViews
        rvSearch.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        // Init adapters
        searchAdapter = new FriendListAdapter("add", this::addFriend);
        rvSearch.setAdapter(searchAdapter);

        friendsAdapter = new FriendListAdapter("remove", this::removeFriend);
        rvFriends.setAdapter(friendsAdapter);

        btnSearch.setOnClickListener(v -> searchUsers());
        loadFriends();
    }

    private void searchUsers() {
        String query = searchBar.getText().toString().trim();
        if (query.isEmpty()) return;

        usersRef.orderByChild("username").equalTo(query)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ds) {
                        List<UserModel> results = new ArrayList<>();
                        for (DataSnapshot snap : ds.getChildren()) {
                            String uid = snap.getKey();
                            String name = snap.child("username").getValue(String.class);
                            if (!uid.equals(currentUid)) results.add(new UserModel(uid, name));
                        }
                        searchAdapter.updateItems(results);
                        rvSearch.setVisibility(results.isEmpty()? View.GONE: View.VISIBLE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadFriends() {
        usersRef.child(currentUid).child("friends")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ds) {
                        List<UserModel> list = new ArrayList<>();
                        if (!ds.exists()) {
                            friendsAdapter.updateItems(list);
                            return;
                        }
                        long total = ds.getChildrenCount();
                        for (DataSnapshot f: ds.getChildren()) {
                            String uid = f.getKey();
                            usersRef.child(uid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snap) {
                                            String name = snap.child("username").getValue(String.class);
                                            list.add(new UserModel(uid, name));
                                            if (list.size() == total) {
                                                friendsAdapter.updateItems(list);
                                            }
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                                    });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void addFriend(String uid, String username) {
        usersRef.child(currentUid).child("friends").child(uid).setValue(true);
        usersRef.child(uid).child("friends").child(currentUid).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Added " + username, Toast.LENGTH_SHORT).show();
                        loadFriends();
                    }
                });
    }

    private void removeFriend(String uid, String username) {
        usersRef.child(currentUid).child("friends").child(uid).removeValue();
        usersRef.child(uid).child("friends").child(currentUid).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Removed " + username, Toast.LENGTH_SHORT).show();
                        loadFriends();
                    }
                });
    }
}