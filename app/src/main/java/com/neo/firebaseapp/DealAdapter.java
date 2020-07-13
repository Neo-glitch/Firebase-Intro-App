package com.neo.firebaseapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.Nullable;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class DealAdapter extends RecyclerView.Adapter<DealAdapter.DealViewHolder>{

    private static final String TAG = "DealAdapter";

    ArrayList<TravelDeal> deals;
    //Firebase
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    private ImageView imageDeal;



    public DealAdapter(){
//        FirebaseUtil.OpenFbReference("traveldeals");
        mFirebaseDatabase = FirebaseUtil.sFirebaseDatabase;                  // gets the fbDb in the singleton class
        mDatabaseReference = FirebaseUtil.sDatabaseReference;

        deals = FirebaseUtil.sDeals;                                         // gets deals first from the db

        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {  // called when activity is started for first time
                TravelDeal travelDeal = snapshot.getValue(TravelDeal.class);
                Log.d(TAG, "onChildAdded: " + travelDeal.getTitle());
                travelDeal.setId(snapshot.getKey());                            // getsId of the pushId gen by Firebase
                deals.add(travelDeal);                                          // adds the detected travelDeal obj to the ArrayList
                notifyItemInserted(deals.size() -1);                   // notify rvView that item has been inserted at the specified pos
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        mDatabaseReference.addChildEventListener(mChildEventListener);
    }

    @NonNull
    @Override
    public DealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.rv_row, parent, false);
        return new DealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DealViewHolder holder, int position) {
        TravelDeal deal = deals.get(position);              // gets deal at curr pos
        holder.bind(deal);
    }

    @Override
    public int getItemCount() {
        return deals.size();
    }

    public class DealViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tvTitle, tvDescription, tvPrice;

        public DealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            imageDeal = itemView.findViewById(R.id.imageDeal);
            itemView.setOnClickListener(this);
        }

        /**
         * method for binding deal param to textView
         */
        public void bind(TravelDeal deal){
            tvTitle .setText(deal.getTitle());
            tvDescription.setText(deal.getDescription());
            tvPrice.setText(deal.getPrice());
            showImage(deal.getImageUrl());
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            Log.d(TAG, "onClick: Position is: " + position);
            TravelDeal selectedDeal = deals.get(position);                 // gets selected TravelDeal obj from pos
            Intent intent = new Intent(v.getContext(), DealActivity.class);
            intent.putExtra("Deal", selectedDeal);
            v.getContext().startActivity(intent);
        }

        /**
         * displays image, whose url is passed as an arg
         * @param url
         */
        private void showImage(String url){
            if(url != null && url.isEmpty() == false){
                // operations for resizing image and showing to user
                Picasso.get()
                        .load(url)
                        .resize(160, 160)
                        .centerCrop()
                        .into(imageDeal);
            }
        }
    }

}
