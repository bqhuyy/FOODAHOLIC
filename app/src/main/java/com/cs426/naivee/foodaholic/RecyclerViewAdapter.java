package com.cs426.naivee.foodaholic;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cs426.naivee.foodaholic.R;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

    private ArrayList<Place> mPlaceArrayList;
    private Context mContext;

    public RecyclerViewAdapter(Context mContext, ArrayList<Place> mPlaceArrayList) {
        this.mPlaceArrayList = mPlaceArrayList;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_recyclerview_listitem,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Glide.with(mContext)
                .asBitmap()
                .load(mPlaceArrayList.get(position).Image)
                .into(holder.foodImage);
//        holder.foodImage.setImageBitmap(BitmapUtility.getImage(mPlaceArrayList.get(position).Image));
        holder.restaurantName.setText(mPlaceArrayList.get(position).Name);
        holder.restaurantAddress.setText(mPlaceArrayList.get(position).Address);
        holder.foodType.setText(mPlaceArrayList.get(position).foodType);
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext,InfoActivity.class);
//                intent.putExtra("info",mPlaceArrayList.get(position));
                intent.putExtra("position",mPlaceArrayList.get(position).Id);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPlaceArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView foodImage;
        TextView restaurantName;
        TextView restaurantAddress;
        TextView foodType;
        RelativeLayout relativeLayout;
        public ViewHolder(View itemView) {
            super(itemView);
            foodImage = itemView.findViewById(R.id.rv_foodImage);
            restaurantName = itemView.findViewById(R.id.rv_restaurantName);
            restaurantAddress = itemView.findViewById(R.id.rv_restaurantAddress);
            foodType = itemView.findViewById(R.id.rv_foodType);
            relativeLayout = itemView.findViewById(R.id.rv_RelativeLayout);
        }
    }

    public void setFilter(ArrayList<Place> newList) {
        mPlaceArrayList = new ArrayList<>();
        mPlaceArrayList.addAll(newList);
        notifyDataSetChanged();
    }


}
