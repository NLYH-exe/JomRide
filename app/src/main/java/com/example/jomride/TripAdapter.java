package com.example.jomride;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Pair;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Pair<String, TripData>> tripList;
    private Listener listener;
    private Context context;

    public interface Listener {
        void onGo(TripData trip, String key);
        void onCancel(TripData trip, String key);
    }

    // Modify the constructor to accept List<Pair<String, TripData>>
    public TripAdapter(List<Pair<String, TripData>> tripList, Listener listener) {
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Pair<String, TripData> pair = tripList.get(position);
        TripData trip = pair.second;  // Get the TripData from the Pair
        String destination = trip.getDestination();
        String dateTime = trip.getDateTime();

        // Setting the values to the views
        holder.tvDestination.setText(destination);
        holder.tvDateTime.setText(dateTime);

        // Button for "Go"
        holder.btnGo.setOnClickListener(v -> {
            String tripKey = pair.first;  // Access the key (String) from the Pair
            listener.onGo(trip, tripKey);
        });

        // Button for "Cancel"
        holder.btnCancel.setOnClickListener(v -> {
            String tripKey = pair.first;  // Access the key (String) from the Pair
            listener.onCancel(trip, tripKey);

            // Access friends for cancellation logic
            List<String> friends = trip.getFriends();
            for (String friend : friends) {
                // You can process canceling the trip for each friend here
                // For example, remove them from the trip, notify them, etc.
            }
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {

        TextView tvDestination, tvDateTime;
        Button btnGo, btnCancel;

        public TripViewHolder(View itemView) {
            super(itemView);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            btnGo = itemView.findViewById(R.id.btnGo);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}
