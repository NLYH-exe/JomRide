package com.example.jomride;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendSelectAdapter extends RecyclerView.Adapter<FriendSelectAdapter.ViewHolder> {

    private final Context context;
    private final List<UserModal> friends;

    public FriendSelectAdapter(Context context, List<UserModal> friends) {
        this.context = context;
        this.friends = friends;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.friend_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModal friend = friends.get(position);
        holder.username.setText(friend.getUsername());
        // If you want to let them “Add” or “Remove” from here, you can.
        holder.actionButton.setText("Add");
        holder.actionButton.setOnClickListener(v -> {
            // no-op or callback into your activity
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        Button actionButton;

        ViewHolder(View itemView) {
            super(itemView);
            username     = itemView.findViewById(R.id.friend_username);
            actionButton = itemView.findViewById(R.id.action_button);
        }
    }
}
