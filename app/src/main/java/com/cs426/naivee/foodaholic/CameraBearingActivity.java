package com.cs426.naivee.foodaholic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;

public class CameraBearingActivity extends AppCompatActivity implements SensorEventListener{

    private static final int PERMISSION_REQUEST_ACCESS_LOCATION = 123;
    SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    TextView mTextView;

    long currentTime;

    private static final String TAG = "CameraBearingActivity";
    SurfaceView cameraView;
    Camera camera = null;
    float angleOfViewVertical;
    float angleOfViewHorizontal;
    float fence1, fence2, fence3, fence4, fence5, fence6;

    DatabaseHelper placeDB;
    ArrayList<Place> mPlaceArrayList = new ArrayList<>();
    Location currentLocation;
    LocationManager mLocationManager;


    ImageView mImageView1, mImageView2, mImageView3, mImageView4, mImageView5;
    TextView mTextView1, mTextView2, mTextView3, mTextView4, mTextView5;
    LinearLayout mLinearLayout1, mLinearLayout2, mLinearLayout3, mLinearLayout4, mLinearLayout5;
    Place place1, place2, place3, place4, place5;
    float dist1, dist2, dist3, dist4, dist5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_bearing);
        currentLocation = getMyLocation();
        //camera
        cameraView = findViewById(R.id.surface_view_camera_bearing);
        try{
            camera = Camera.open();
            camera.setDisplayOrientation(90);
        }catch (Exception e){
            //can not use camera
        }
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                    angleOfViewVertical = camera.getParameters().getVerticalViewAngle();
                    angleOfViewHorizontal = camera.getParameters().getHorizontalViewAngle();
                    float fence = angleOfViewHorizontal/5;
                    fence1 = -angleOfViewHorizontal/2;
                    fence2 = fence1+fence;
                    fence3 = fence2+fence;
                    fence4 = fence3+fence;
                    fence5 = fence4+fence;
                    fence6 = fence5+fence;
                } catch (IOException e) {
                    Log.d(TAG, "Error setting camera preview: " + e.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        //compass
        mTextView = findViewById(R.id.textViewCameraBearing);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        currentTime = System.currentTimeMillis();

        mImageView1 = findViewById(R.id.sticker1);
        mImageView2 = findViewById(R.id.sticker2);
        mImageView3 = findViewById(R.id.sticker3);
        mImageView4 = findViewById(R.id.sticker4);
        mImageView5 = findViewById(R.id.sticker5);
        mTextView1 = findViewById(R.id.nameSticker1);
        mTextView2 = findViewById(R.id.nameSticker2);
        mTextView3 = findViewById(R.id.nameSticker3);
        mTextView4 = findViewById(R.id.nameSticker4);
        mTextView5 = findViewById(R.id.nameSticker5);
        mLinearLayout1 = findViewById(R.id.li1);
        mLinearLayout2 = findViewById(R.id.li2);
        mLinearLayout3 = findViewById(R.id.li3);
        mLinearLayout4 = findViewById(R.id.li4);
        mLinearLayout5 = findViewById(R.id.li5);
        dist1 = dist2 =dist3=dist4=dist5 = Float.MAX_VALUE;

        placeDB = new DatabaseHelper(CameraBearingActivity.this);
        mPlaceArrayList = placeDB.getPlaceArrayList();
        RequestLocationPermission();
        String display;
        if(currentLocation != null)
            display = String.valueOf(currentLocation.getLongitude())+" "+String.valueOf(currentLocation.getLatitude());
        else
            display = "no location";
        mTextView.setText(display);
        azimuth = 260;

        if(currentLocation != null){
            for(Place place:mPlaceArrayList){
                //determine section which this place belongs
                Location locationPlace = new Location("dummy string");
                locationPlace.setLatitude(place.LatLng.latitude);
                locationPlace.setLongitude(place.LatLng.longitude);
                float bearing = currentLocation.bearingTo(locationPlace);
                float dd = bearing - azimuth;
                if(dd<0)
                    dd+=360;
                if(dd>180)
                    dd-=360;
                //calculate the distance between this place and current location
                float distance = calculateDistanceBetween2PointsOnMap(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),new LatLng(locationPlace.getLatitude(),locationPlace.getLongitude()));
                if(place1 == place && (dd<fence1 || dd>fence2)){
                    place1=null;
                    mLinearLayout1.setVisibility(View.INVISIBLE);
                    dist1=Float.MAX_VALUE;
                }
                if(place2 == place && (dd<fence2 || dd>fence3)){
                    place2=null;
                    mLinearLayout2.setVisibility(View.INVISIBLE);
                    dist2=Float.MAX_VALUE;
                }
                if(place3 == place && (dd<fence3 || dd>fence4)){
                    place3=null;
                    mLinearLayout3.setVisibility(View.INVISIBLE);
                    dist3=Float.MAX_VALUE;
                }
                if(place4 == place && (dd<fence4 || dd>fence5)){
                    place4=null;
                    mLinearLayout4.setVisibility(View.INVISIBLE);
                    dist4=Float.MAX_VALUE;
                }
                if(place5 == place && (dd<fence5 || dd>fence6)){
                    place5=null;
                    mLinearLayout5.setVisibility(View.INVISIBLE);
                    dist5=Float.MAX_VALUE;
                }
                if(dd>=fence1&&dd<fence2){
                    if(distance<dist1){
                        mTextView1.setText(place.Name + ": "+ Float.toString(distance) + "km");
                        dist1 = distance;
                        mLinearLayout1.setVisibility(View.VISIBLE);
                        place1=place;
                    }
                }
                else if(dd<fence3){
                    if(distance<dist2){
                        mTextView2.setText(place.Name + ": "+ Float.toString(distance) + "km");
                        dist2 = distance;
                        mLinearLayout2.setVisibility(View.VISIBLE);
                        place2=place;
                    }
                }
                else if(dd<fence4){
                    if(distance<dist3){
                        mTextView3.setText(place.Name + ": "+ Float.toString(distance) + "km");
                        dist3 = distance;
                        mLinearLayout3.setVisibility(View.VISIBLE);
                        place3=place;
                    }
                }
                else if(dd<fence5){
                    if(distance<dist4){
                        mTextView4.setText(place.Name + ": "+ Float.toString(distance) + "km");
                        dist4 = distance;
                        mLinearLayout4.setVisibility(View.VISIBLE);
                        place4=place;
                    }
                }
                else if(dd<fence6){
                    if(distance<dist5){
                        mTextView5.setText(place.Name + ": "+ Float.toString(distance) + "km");
                        dist5 = distance;
                        mLinearLayout5.setVisibility(View.VISIBLE);
                        place5=place;
                    }
                }
            }
        }
    }

    private void RequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST_ACCESS_LOCATION
            );
        }
        else {
            trackingLocation();
            currentLocation = getMyLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void trackingLocation() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION){
            Log.d("Location change", "Got result of permission");
            if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                trackingLocation();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private Location getMyLocation() {
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (myLocation == null) {
            myLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        return myLocation;
    }


    @Override
    public void onBackPressed() {
        if (camera != null){
            camera.release();
            camera = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer,1000);
        mSensorManager.registerListener(this, magnetometer,1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    float[] mGravity = new float[3];
    float[] mGeomagnetic = new float[3];
    float azimuth;
    @Override
    public void onSensorChanged(SensorEvent event) {
        currentLocation = getMyLocation();
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            System.arraycopy(event.values,0,mGravity,0,mGravity.length);
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            System.arraycopy(event.values,0,mGeomagnetic,0,mGeomagnetic.length);
        if(mGravity!=null && mGeomagnetic!=null){
            float R[] = new float[9];
            if(SensorManager.getRotationMatrix(R,null,mGravity,mGeomagnetic)){
                float orientation[] = new float[3];
                SensorManager.getOrientation(R,orientation);
                azimuth = orientation[0];
                azimuth = (float) Math.toDegrees((double)azimuth);
                if(azimuth<0)
                    azimuth+=360;
                long now = System.currentTimeMillis();
                if(now - currentTime >= 1000)
                {
                    mTextView.setText(String.valueOf(azimuth));
                    currentTime = now;
                    for(Place place:mPlaceArrayList){
                        //determine section which this place belongs
                        Location locationPlace = new Location("dummy string");
                        locationPlace.setLatitude(place.LatLng.latitude);
                        locationPlace.setLongitude(place.LatLng.longitude);
                        float bearing = currentLocation.bearingTo(locationPlace);
                        if (bearing<0)
                            bearing+=360;
                        float dd = bearing - azimuth;
                        if(dd<0)
                            dd+=360;
                        else if(dd>180)
                            dd-=360;
                        //calculate the distance between this place and current location
                        float distance = calculateDistanceBetween2PointsOnMap(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),new LatLng(locationPlace.getLatitude(),locationPlace.getLongitude()));
                        if(place1 == place && (dd<fence1 || dd>fence2)){
                            place1=null;
                            mLinearLayout1.setVisibility(View.INVISIBLE);
                            mTextView1.setVisibility(View.INVISIBLE);
                            mImageView1.setVisibility(View.INVISIBLE);
                            dist1=Float.MAX_VALUE;
                        }
                        if(place2 == place && (dd<fence2 || dd>fence3)){
                            place2=null;
                            mLinearLayout2.setVisibility(View.INVISIBLE);
                            mTextView2.setVisibility(View.INVISIBLE);
                            mImageView2.setVisibility(View.INVISIBLE);
                            dist2=Float.MAX_VALUE;
                        }
                        if(place3 == place && (dd<fence3 || dd>fence4)){
                            place3=null;
                            mLinearLayout3.setVisibility(View.INVISIBLE);
                            mTextView3.setVisibility(View.INVISIBLE);
                            mImageView3.setVisibility(View.INVISIBLE);
                            dist3=Float.MAX_VALUE;
                        }
                        if(place4 == place && (dd<fence4 || dd>fence5)){
                            place4=null;
                            mLinearLayout4.setVisibility(View.INVISIBLE);
                            mTextView4.setVisibility(View.INVISIBLE);
                            mImageView4.setVisibility(View.INVISIBLE);
                            dist4=Float.MAX_VALUE;
                        }
                        if(place5 == place && (dd<fence5 || dd>fence6)){
                            place5=null;
                            mLinearLayout5.setVisibility(View.INVISIBLE);
                            mTextView5.setVisibility(View.INVISIBLE);
                            mImageView5.setVisibility(View.INVISIBLE);
                            dist5=Float.MAX_VALUE;
                        }
                        if(dd>=fence1&&dd<fence2){
                            if(distance<dist1){
                                mTextView1.setText(place.Name + ": "+ Float.toString(distance) + "km");
                                dist1 = distance;
                                mLinearLayout1.setVisibility(View.VISIBLE);
                                mTextView1.setVisibility(View.VISIBLE);
                                mImageView1.setVisibility(View.VISIBLE);
                                place1 = place;
                            }
                        }
                        else if(dd<fence3){
                            if(distance<dist2){
                                mTextView2.setText(place.Name + ": "+ Float.toString(distance) + "km");
                                dist2 = distance;
                                mLinearLayout2.setVisibility(View.VISIBLE);
                                mTextView2.setVisibility(View.VISIBLE);
                                mImageView2.setVisibility(View.VISIBLE);
                                place2 = place;
                            }
                        }
                        else if(dd<fence4){
                            if(distance<dist3){
                                mTextView3.setText(place.Name + ": "+ Float.toString(distance) + "km");
                                dist3 = distance;
                                mLinearLayout3.setVisibility(View.VISIBLE);
                                mTextView3.setVisibility(View.VISIBLE);
                                mImageView3.setVisibility(View.VISIBLE);
                                place3 = place;
                            }
                        }
                        else if(dd<fence5){
                            if(distance<dist4){
                                mTextView4.setText(place.Name + ": "+ Float.toString(distance) + "km");
                                dist4 = distance;
                                mLinearLayout4.setVisibility(View.VISIBLE);
                                mTextView4.setVisibility(View.VISIBLE);
                                mImageView4.setVisibility(View.VISIBLE);
                                place4 = place;
                            }
                        }
                        else if(dd<fence6){
                            if(distance<dist5){
                                mTextView5.setText(place.Name + ": "+ Float.toString(distance) + "km");
                                dist5 = distance;
                                mLinearLayout5.setVisibility(View.VISIBLE);
                                mTextView5.setVisibility(View.VISIBLE);
                                mImageView5.setVisibility(View.VISIBLE);
                                place5 = place;
                            }
                        }
                    }
                }
                Log.d("orientation","run");
            }
        }
    }

    private float calculateDistanceBetween2PointsOnMap(LatLng location1, LatLng location2) {
        double
                radiusOfTheEarth = 6371.0,
                distanceLongitude = Math.toRadians(location2.longitude - location1.longitude),
                distanceLatitude = Math.toRadians(location2.latitude - location1.latitude);
        double a = Math.sin(distanceLatitude/2.0) * Math.sin(distanceLatitude/2.0)
                + Math.cos(Math.toRadians(location1.latitude)) * Math.cos(Math.toRadians(location2.latitude))
                * Math.sin(distanceLongitude/2) * Math.sin(distanceLongitude/2.0);
        a = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0-a));
        return (float)(Math.round((radiusOfTheEarth * a) * 100.0) / 100.0);
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
