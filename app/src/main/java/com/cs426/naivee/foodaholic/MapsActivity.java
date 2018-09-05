package com.cs426.naivee.foodaholic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cs426.naivee.foodaholic.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnPolylineClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static boolean isDataChanged = false;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int SPEECH_TO_TEXT = 10;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(9.17682,105.15242), new LatLng(22.82333,104.98357));

    private BitmapDrawable mBitmapDrawable;
    private GoogleMap mMap;
    private Place mInitialLocation = null;
    private ArrayList<Place> mPlaceArrayList = new ArrayList<>();
    private View mMapView;
    private AutoCompleteTextView mSearchText;
    private Marker mSearchMarker = null;
    private LatLng mSearchLatLng;
    private Polyline mMyDirection;
    private DatabaseHelper placeDB;
    private Place mMarkerClicked;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private LocationManager mLocationManager;
    private ImageView mListMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestLocationPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMapView = mapFragment.getView();

        placeDB = new DatabaseHelper(this);
        mBitmapDrawable = (BitmapDrawable)getResources().getDrawable(R.drawable.add_image);

        mSearchText = (AutoCompleteTextView) findViewById(R.id.maps_input_search);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this,Places.getGeoDataClient(this,null),LAT_LNG_BOUNDS,null);
        mSearchText.setAdapter(mPlaceAutocompleteAdapter);
        mSearchText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hideKeyboard(MapsActivity.this);
                geoLocate();
            }
        });
        onSearch();
//        trackingLocation();
        //setStatusBarTransparent();
    }

    private void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void onSearch() {
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        || event.getAction() == KeyEvent.KEYCODE_ENTER) {
                    hideKeyboard(MapsActivity.this);
                    geoLocate();
                }
                return false;
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void trackingLocation() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        mMap.setMyLocationEnabled(true);
                        if (mMyDirection != null && mMarkerClicked != null) {
                            Location myLocation = getMyLocation();
                            LatLng endLatLng = mMarkerClicked.LatLng;
                            String url = getRequestUrl(new LatLng(myLocation.getLatitude(),myLocation.getLongitude()),endLatLng);
                            TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                            taskRequestDirections.execute(url);
                        }
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

    private void geoLocate() {
        String searchString = mSearchText.getText().toString();
        List<Address> addressList = null;
        Address address;
        if (searchString != null && !searchString.equals("")) {
            Geocoder geocoder = new Geocoder(MapsActivity.this);
            try {
                addressList = geocoder.getFromLocationName(searchString,1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                address = addressList.get(0);
            } catch (Exception e) {
                Toast.makeText(MapsActivity.this, "Not found!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mSearchMarker != null)
                mSearchMarker.remove();
            LatLng latLng = new LatLng(address.getLatitude(),address.getLongitude());
            mSearchMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            mSearchMarker.setTag(-1);
            mSearchLatLng = latLng;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    trackingLocation();
                }
                break;
        }
        return;
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            trackingLocation();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setStyleForMap();
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            mMap.setMyLocationEnabled(true);
        }
        changePositionOfButtons();

        if (isFirstTimeStartApp()) {
            addDefaultData();
            setFirstTimeStartStatus(false);
        }
        else mPlaceArrayList = placeDB.getPlaceArrayList();

        addDefaultMarker();
//        BitmapDrawable bitmapDrawable = (BitmapDrawable)getResources().getDrawable(R.drawable.home_marker_icon);
//        Bitmap smallerBitmap = bitmapDrawable.getBitmap();
//        smallerBitmap = Bitmap.createScaledBitmap(smallerBitmap,100,100, false);
//        mMap.addMarker(new MarkerOptions()
//                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.home_marker_icon))
//                //.anchor(0.5f,0.5f)
//                .icon(BitmapDescriptorFactory.fromBitmap(smallerBitmap))
//                .position(mPlaceArrayList.get(0).LatLng)
//                .title(mPlaceArrayList.get(0).Name)).setTag(mPlaceArrayList.get(0));
//        mMap.addMarker(new MarkerOptions()
//                .position(mPlaceArrayList.get(1).LatLng)
//                .title(mPlaceArrayList.get(1).Name)).setTag(mPlaceArrayList.get(1));

//        updateAndDisplayCurrentLocation();

        Location myMoveLocation = getMyLocation();
        if (myMoveLocation == null)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.762622,106.660172), 13)); //Initial location, ZOOM
        else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myMoveLocation.getLatitude(),myMoveLocation.getLongitude()), 13));
        }
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnPolylineClickListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isDataChanged) {
            finish();
            startActivity(getIntent());
            isDataChanged = false;
        }
    }

    public static void setIsDataChanged() {
        isDataChanged = true;
    }

    private void addDefaultData() {
        placeDB.addData(
                "Pho Le",
                new LatLng(10.7553102,106.6713732),
                "http://www.phole.vn/",
                "413-415 Nguyen Trai, District 5, HCM City, Vietnam",
                "+842839234008",
                BitmapUtility.getBytes(((BitmapDrawable)getResources().getDrawable(R.drawable.dt_pho_le)).getBitmap()),
                "Pho");
        placeDB.addData(
                "Gong Cha Nguyen Dinh Chieu",
                new LatLng(10.7725344,106.6958551),
                "http://gongcha.com.vn/",
                "70 Nguyen Dinh Chieu, District 1, HCM City, Vietnam",
                "+842839101970",
                BitmapUtility.getBytes(((BitmapDrawable)getResources().getDrawable(R.drawable.dt_gong_cha)).getBitmap()),
                "Milk tea");
        placeDB.addData(
                "Hai Nam Chicken Rice",
                new LatLng(10.7550326,106.6640645),
                "http://comgahainam.vn/",
                "107H Ngo Quyen, District 5, HCM City, Vietnam",
                "0903802040",
                BitmapUtility.getBytes(((BitmapDrawable)getResources().getDrawable(R.drawable.dt_hai_nam)).getBitmap()),
                "Chicken Rice");
        placeDB.addData(
                "Ca Can Dumplings - Noodle",
                new LatLng(10.7572874,106.6661475),
                "https://www.google.com.vn/search?q=Hu+tieu+ca+can&rlz=1C1CHZL_viVN768VN768&oq=Hu+tieu+ca+can&aqs=chrome..69i57j0j69i60l3j0.5606j1j7&sourceid=chrome&ie=UTF-8",
                "110 Hung Vuong, District 5, HCM City, Vietnam",
                "",
                BitmapUtility.getBytes(((BitmapDrawable)getResources().getDrawable(R.drawable.dt_ca_can)).getBitmap()),
                "Dumpling, Noodle");
        placeDB.addData(
                "Marukamen Udon",
                new LatLng(10.7727466,106.6934676),
                "https://www.facebook.com/MarukameUdonVN/",
                "215 Ly Tu Trong, District 1, HCM City, Vietnam",
                "(028) 6270 2999",
                BitmapUtility.getBytes(((BitmapDrawable)getResources().getDrawable(R.drawable.dt_marukamen_udon)).getBitmap()),
                "Udon, Mixed Rice");
        placeDB.addData(
                "Hanuri Korean Fast Food",
                new LatLng(10.7701853,106.6683029),
                "https://www.facebook.com/Hanurikorean/",
                "736 Su Van Hanh, District 10, HCM City, Vietnam",
                "028 3880 0060",
                BitmapUtility.getBytes(((BitmapDrawable)getResources().getDrawable(R.drawable.dt_hanuri)).getBitmap()),
                "Mixed Rice");
        placeDB.addData(
                "R&B Tea",
                new LatLng(10.7721833,106.7018249),
                "https://www.facebook.com/RBteavietnam/",
                "86 Ngo Duc Ke, District 1, HCM City, Vietnam",
                "0868 983 53",
                BitmapUtility.getBytes(((BitmapDrawable)getResources().getDrawable(R.drawable.dt_r_b)).getBitmap()),
                "Milk Tea");
        mPlaceArrayList = placeDB.getPlaceArrayList();
    }



    private void addDefaultMarker() {
        Place place;
        for (int position = 0; position < mPlaceArrayList.size(); position++) {
            place = mPlaceArrayList.get(position);
            mMap.addMarker(new MarkerOptions()
                    .position(place.LatLng)
                    .title(place.Name)).setTag(mPlaceArrayList.get(position).Id);
        }
    }

    private void changePositionOfButtons() {
        if (mMapView != null && mMapView.findViewById(Integer.parseInt("1")) != null) {
            View locationButton = ((View) mMapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams buttonLayoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
//            buttonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
//            buttonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
//            buttonLayoutParams.setMargins(0, 0, 30, 0);
            buttonLayoutParams.setMargins(0, 290, 30, 0);

            View compassButton = ((View) mMapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("5"));
            RelativeLayout.LayoutParams compassButtonLayoutParams = (RelativeLayout.LayoutParams)
                    compassButton.getLayoutParams();

//            compassButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
//            compassButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
//            compassButtonLayoutParams.setMargins(30, 0, 0, 80);
            compassButtonLayoutParams.setMargins(30, 290, 0, 0);
        }
    }

    private void setStyleForMap() {
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success)
                Log.e("MapsActivityRaw", "Style parsing failed.");
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivityRaw", "Can't find style.", e);
        }
    }

    private double calculateDistanceBetween2PointsOnMap(LatLng location1, LatLng location2) {
        double
                radiusOfTheEarth = 6371.0,
                distanceLongitude = Math.toRadians(location2.longitude - location1.longitude),
                distanceLatitude = Math.toRadians(location2.latitude - location1.latitude);
        double a = Math.sin(distanceLatitude/2.0) * Math.sin(distanceLatitude/2.0)
                + Math.cos(Math.toRadians(location1.latitude)) * Math.cos(Math.toRadians(location2.latitude))
                * Math.sin(distanceLongitude/2) * Math.sin(distanceLongitude/2.0);
        a = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0-a));
        return Math.round((radiusOfTheEarth * a) * 100.0) / 100.0;
    }

    private String getDirectionName(LatLng location1, LatLng location2) {
        String[] directionName = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        double directionValue = Math.toDegrees(Math.atan2(location2.longitude-location1.longitude,location2.latitude-location1.latitude));
        int directionIndex = (int) Math.round(directionValue/45);
        if (directionIndex < 0)
            directionIndex += 8;
        return directionName[directionIndex];
    }


    @SuppressLint("MissingPermission")
    @Override
    public boolean onMarkerClick(final Marker marker) {
        hideKeyboard(MapsActivity.this);
        if ((int) marker.getTag() == -1) {
            CharSequence options[] = new CharSequence[] {
                    "Delete this search marker",
                    "Find direction from current location",
                    "Find direction from initial location",
                    "Save this marker"
            };
            mMarkerClicked = (new Place(
                    -1,
                    "",
                    mSearchLatLng,
                    "",
                    "",
                    "",
                    null,
                    ""
            ));
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pick an activity");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Location myLocation = getMyLocation();
                    switch (which)
                    {
                        case 0:
                            Toast.makeText(MapsActivity.this, "Deleted search marker", Toast.LENGTH_SHORT).show();
                            mSearchMarker.remove();
                            break;
                        case 1: case 2:
                            if (mInitialLocation != null && mInitialLocation.Name.compareTo("") == 0)
                                mInitialLocation = null;
                            if (which == 1 && myLocation == null) {
                                Toast.makeText(MapsActivity.this, "My location not found!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (which == 2 && mInitialLocation == null){
                                Toast.makeText(MapsActivity.this, "Initial location not found!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            LatLng beginLatLng;
                            if (which == 1) {
                                beginLatLng = new LatLng(myLocation.getLatitude(),
                                        myLocation.getLongitude());
                                trackingLocation();
                            }
                            else beginLatLng = mInitialLocation.LatLng;
                            String url = getRequestUrl(beginLatLng,mSearchLatLng);
                            TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                            taskRequestDirections.execute(url);
                            break;
                        case 3:
                            Intent intent = new Intent(MapsActivity.this,AddInfoActivity.class);
                            intent.putExtra("lat",mSearchLatLng.latitude);
                            intent.putExtra("lng",mSearchLatLng.longitude);
                            intent.putExtra("address",mSearchText.getText().toString());
                            startActivity(intent);
                            break;
                    }
                }
            });
            builder.show();
            return false;
        }
        CharSequence options[] = new CharSequence[] {"Go to website",
                "Mark as initial location",
                "Delete this marker",
                "Find direction from current location",
                "Find direction from initial location",
                "Information"
        };
        final int position = (int)marker.getTag();
        mMarkerClicked = placeDB.getPlace(position);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (mMarkerClicked.Name.compareTo("") != 0)
            builder.setTitle(mMarkerClicked.Name);
        else builder.setTitle("Pick an activity");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Location myLocation = getMyLocation();
                switch (which)
                {
                    case 0:
                        /*Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(place.Website));
                        startActivity(webIntent);*/
                        /*WebView webView = new WebView(MapsActivity.this);
                        webView.loadUrl(place.Website);
                        webView.setWebViewClient(new WebViewClient());
                        new AlertDialog.Builder(MapsActivity.this).setView(webView)
                                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).show();*/
                        FrameLayout frameLayout = new FrameLayout(MapsActivity.this);
                        final WebView webView = new WebView(MapsActivity.this);
                        webView.setVisibility(View.INVISIBLE);
                        webView.loadUrl(mMarkerClicked.Website);
                        final ProgressBar loadingWheel = new ProgressBar(MapsActivity.this);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100,100);
                        loadingWheel.setLayoutParams(params);
                        frameLayout.addView(webView);
                        frameLayout.addView(loadingWheel);
                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public void onPageFinished(WebView view, String url) {
                                super.onPageFinished(view, url);
                                webView.setVisibility(View.VISIBLE);
                                loadingWheel.setVisibility(View.GONE);
                            }
                        });
                        new AlertDialog.Builder(MapsActivity.this).setView(frameLayout)
                                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).show();
                        break;
                    case 1:
                        mInitialLocation = mMarkerClicked;
                        Toast.makeText(MapsActivity.this, "Marked " + mMarkerClicked.Name + " as initial location", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(MapsActivity.this, "Deleted " + mMarkerClicked.Name + " marker", Toast.LENGTH_SHORT).show();
                        if (mMarkerClicked == mInitialLocation)
                            mInitialLocation = null;
                        mPlaceArrayList.remove(mMarkerClicked);
                        placeDB.deleteData(String.valueOf(position));
                        marker.remove();
                        break;
                    /*case 3:
                        Toast toast = Toast.makeText(MapsActivity.this, "Distance from " + mInitialLocation.Name + ": "
                                + Double.toString(calculateDistanceBetween2PointsOnMap(mInitialLocation.LatLng, ((Place) marker.getTag()).LatLng))
                                + "km\nDirection: " + getDirectionName(mInitialLocation.LatLng,((Place) marker.getTag()).LatLng), Toast.LENGTH_SHORT);
                        toast.show();
                        break;*/
                    case 3: case 4:
                        if (mInitialLocation != null && mInitialLocation.Name.compareTo("") == 0)
                            mInitialLocation = null;
                        if (which == 3 && myLocation == null) {
                            Toast.makeText(MapsActivity.this, "My location not found!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (which == 4 && mInitialLocation == null){
                            Toast.makeText(MapsActivity.this, "Initial location not found!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        LatLng beginLatLng;
                        if (which == 3) {
                            beginLatLng = new LatLng(myLocation.getLatitude(),
                                    myLocation.getLongitude());
                            trackingLocation();
                        }
                        else beginLatLng = mInitialLocation.LatLng;
                        LatLng endLatLng = mMarkerClicked.LatLng;
                        String url = getRequestUrl(beginLatLng,endLatLng);
                        TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                        taskRequestDirections.execute(url);
                        break;
                    case 5:
                        Intent intent = new Intent(MapsActivity.this,InfoActivity.class);
                        intent.putExtra("position",position);
                        MapsActivity.this.startActivity(intent);
                        break;
                }
            }
        });
        builder.show();
        return false;
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

    private String getRequestUrl(LatLng origin, LatLng dest) {
        String str_org = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String params = str_org + "&" + str_dest + "&" + sensor + "&" + mode;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + params;
        return url;
    }

    private String requestDirection(String requestUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(requestUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null)
                inputStream.close();
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    public void listViewOnClick(View view) {
        Intent intent = new Intent(MapsActivity.this,RecyclerViewActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        polyline.remove();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void goToCameraBearing(View view) {
        Intent intent = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            intent = new Intent(MapsActivity.this, CameraBearingActivity.class);
            startActivity(intent);
        }
    }

    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>> > {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            if (mMyDirection != null)
                mMyDirection.remove();
            ArrayList points = null;
            PolylineOptions polylineOptions = null;
            for (List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();
                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat,lon));
                }
                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.RED);
                polylineOptions.geodesic(true);
            }

            if (polylineOptions != null) {
                mMyDirection = mMap.addPolyline(polylineOptions);
                mMyDirection.setClickable(true);
            }
            else
                Toast.makeText(getApplicationContext(), "Direction not found!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideKeyboard(MapsActivity.this);
        return super.onTouchEvent(event);
    }

    @Override
    public void onMapLongClick(final LatLng latLng) {
        hideKeyboard(MapsActivity.this);
        CharSequence options[] = new CharSequence[] {"Add a marker"/*, "Calculate distance and direction"*/};
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick an activity");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which)
                {
                    case 0:
                        //createInputAlertDialog();
                        Intent intent = new Intent(MapsActivity.this,AddInfoActivity.class);
                        intent.putExtra("lat",latLng.latitude);
                        intent.putExtra("lng",latLng.longitude);
                        startActivity(intent);
                        break;
                    /*case 1:
                        Toast toast = Toast.makeText(MapsActivity.this, "Distance from " + mInitialLocation.Name + ": "
                                + Double.toString(calculateDistanceBetween2PointsOnMap(mInitialLocation.LatLng, latLng))
                                + "km\nDirection: " + getDirectionName(mInitialLocation.LatLng,latLng), Toast.LENGTH_SHORT);
                        toast.show();
                        break;*/
                }
            }

            private void createInputAlertDialog() {
                final EditText name = new EditText(MapsActivity.this);
                name.setTextColor(Color.rgb(0,0,0));
                name.setHint("Compulsory");
                name.setSingleLine(true);
                name.setTextSize(15);
                final EditText website = new EditText(MapsActivity.this);
                website.setTextColor(Color.rgb(0,0,0));
                website.setHint("Optional");
                website.setSingleLine(true);
                website.setTextSize(15);
                final EditText address = new EditText(MapsActivity.this);
                address.setTextColor(Color.rgb(0,0,0));
                address.setHint("Optional");
                address.setSingleLine(true);
                address.setTextSize(15);
                final EditText foodType = new EditText(MapsActivity.this);
                foodType.setTextColor(Color.rgb(0,0,0));
                foodType.setHint("Optional");
                foodType.setSingleLine(true);
                foodType.setTextSize(15);
                final EditText tel = new EditText(MapsActivity.this);
                tel.setTextColor(Color.rgb(0,0,0));
                tel.setHint("Optional");
                tel.setSingleLine(true);
                tel.setTextSize(15);
                ScrollView scrollView = new ScrollView(MapsActivity.this);
                LinearLayout linearLayout = new LinearLayout(MapsActivity.this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams inputLayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                LinearLayout.LayoutParams nameLayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                inputLayoutParams.setMargins(100,10,100,0);
                nameLayoutParams.setMargins(100,100,100,0);
                TextView nameLabel = new TextView(MapsActivity.this);
                nameLabel.setText("Name");
                nameLabel.setTypeface(null, Typeface.BOLD);
                nameLabel.setTextSize(18);
                TextView websiteLabel = new TextView(MapsActivity.this);
                websiteLabel.setText("Website");
                websiteLabel.setTypeface(null, Typeface.BOLD);
                websiteLabel.setTextSize(18);
                TextView addressLabel = new TextView(MapsActivity.this);
                addressLabel.setText("Address");
                addressLabel.setTypeface(null, Typeface.BOLD);
                addressLabel.setTextSize(18);
                TextView foodTypeLabel = new TextView(MapsActivity.this);
                foodTypeLabel.setText("Food Type");
                foodTypeLabel.setTypeface(null, Typeface.BOLD);
                foodTypeLabel.setTextSize(18);
                TextView telLabel = new TextView(MapsActivity.this);
                telLabel.setText("Telephone");
                telLabel.setTypeface(null, Typeface.BOLD);
                telLabel.setTextSize(18);
                linearLayout.addView(nameLabel, nameLayoutParams);
                linearLayout.addView(name, inputLayoutParams);
                linearLayout.addView(addressLabel, inputLayoutParams);
                linearLayout.addView(address, inputLayoutParams);
                linearLayout.addView(foodTypeLabel, inputLayoutParams);
                linearLayout.addView(foodType, inputLayoutParams);
                linearLayout.addView(websiteLabel,inputLayoutParams);
                linearLayout.addView(website,inputLayoutParams);
                linearLayout.addView(telLabel, inputLayoutParams);
                linearLayout.addView(tel, inputLayoutParams);
                scrollView.addView(linearLayout);
                new AlertDialog.Builder(MapsActivity.this).setView(scrollView)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (name.getText().toString().compareTo("") != 0) {
                                    int n = mPlaceArrayList.size()+1;
                                    mPlaceArrayList.add(new Place(
                                            n,
                                            name.getText().toString(),
                                            latLng,
                                            website.getText().toString(),
                                            address.getText().toString(),
                                            tel.getText().toString(),
                                            BitmapUtility.getBytes(mBitmapDrawable.getBitmap()),
                                            foodType.getText().toString()
                                            ));
                                    mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title(name.getText().toString())).setTag(n);
                                    //ADD DATA TO DATABASE
                                    placeDB.addData(name.getText().toString(),
                                            latLng,
                                            website.getText().toString(),
                                            address.getText().toString(),
                                            tel.getText().toString(),
                                            BitmapUtility.getBytes(mBitmapDrawable.getBitmap()),
                                            foodType.getText().toString());
                                }
                            }
                        }).show();
            }
        });
        builder.show();

    }

    private void setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private boolean isFirstTimeStartApp() {
        SharedPreferences ref = getApplicationContext().getSharedPreferences("MapApp", Context.MODE_PRIVATE);
        return ref.getBoolean("FirstTimeStartFlag",true);
    }

    private void setFirstTimeStartStatus(boolean status) {
        SharedPreferences ref = getApplicationContext().getSharedPreferences("MapApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = ref.edit();
        editor.putBoolean("FirstTimeStartFlag",status);
        editor.commit();
    }

    public void getSpeechInput(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(intent, SPEECH_TO_TEXT);
        }
        catch (ActivityNotFoundException a) {
            Toast.makeText(this,"Your device do not support speech input",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SPEECH_TO_TEXT:
                ArrayList<String> result;
                if (resultCode == RESULT_OK && data != null) {
                    result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mSearchText.setText(result.get(0));
                    hideKeyboard(MapsActivity.this);
                    geoLocate();
                }
                break;
        }
    }
}

