package com.cs426.naivee.foodaholic;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

import com.cs426.naivee.foodaholic.R;
import com.google.android.gms.maps.model.LatLng;

class Place {
    public int Id = 0;
    public String Name;
    public LatLng LatLng;
    public String Website;
    public String Address;
    public String Tel;
    public byte[] Image;
    public String foodType;

    public Place(String name, com.google.android.gms.maps.model.LatLng latLng, String website, String address, String tel, byte[] image, String foodType) {
        this.Name = name;
        this.LatLng = latLng;
        this.Website = website;
        this.Address = address;
        this.Tel = tel;
        this.Image = image;
        this.foodType = foodType;
    }

    public Place(int Id, String name, com.google.android.gms.maps.model.LatLng latLng, String website, String address, String tel, byte[] image, String foodType) {
        this.Id = Id;
        this.Name = name;
        this.LatLng = latLng;
        this.Website = website;
        this.Address = address;
        this.Tel = tel;
        this.Image = image;
        this.foodType = foodType;
    }
}
