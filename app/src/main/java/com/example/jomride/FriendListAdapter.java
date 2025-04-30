package com.example.jomride;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {
    public interface OnActionListener {
        void onAction(String uid, String username);
    }

    private final List<UserModel> items = new ArrayList<>();
    private final String actionType; // "add" or "remove"
    private final OnActionListener listener;

    public FriendListAdapter(String actionType, OnActionListener listener) {
        this.actionType = actionType;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModel user = items.get(position);
        holder.usernameText.setText(user.getUsername());
        holder.actionButton.setText(actionType.equals("add") ? "Add" : "Remove");
        holder.actionButton.setOnClickListener(v -> listener.onAction(user.getUid(), user.getUsername()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Public method to update items list
     */
    public void updateItems(List<UserModel> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        Button actionButton;

        ViewHolder(View view) {
            super(view);
            usernameText = view.findViewById(R.id.friend_username);
            actionButton = view.findViewById(R.id.action_button);
        }
    }
}