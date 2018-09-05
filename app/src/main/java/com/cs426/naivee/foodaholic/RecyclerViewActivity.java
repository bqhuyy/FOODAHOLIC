package com.cs426.naivee.foodaholic;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.cs426.naivee.foodaholic.R;

import java.util.ArrayList;

public class RecyclerViewActivity extends AppCompatActivity implements SearchView.OnQueryTextListener{
    private static boolean isDataChanged = false;
    private ArrayList<Place> mPlaceArrayList;
    private Intent mIntent;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private DatabaseHelper placeDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
//        mIntent = getIntent();
//        mPlaceArrayList = mIntent.getParcelableArrayListExtra("PlaceArrayList");
        placeDB = new DatabaseHelper(this);
        mPlaceArrayList = placeDB.getPlaceArrayList();
        RecyclerView recyclerView = findViewById(R.id.rv_recyle_view);
        mRecyclerViewAdapter = new RecyclerViewAdapter(this,mPlaceArrayList);

        recyclerView.setAdapter(mRecyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        SearchView searchView = findViewById(R.id.rv_searchBar);
        searchView.setOnQueryTextListener(this);
        //setStatusBarTransparent();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    private void setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        ArrayList<Place> filterList = new ArrayList<>();
        for (Place place : mPlaceArrayList) {
            if (place.Name.toLowerCase().contains(newText.toLowerCase())
                    || place.Address.toLowerCase().contains(newText.toLowerCase())
                    || place.foodType.toLowerCase().contains(newText.toLowerCase()))
                filterList.add(place);
        }
        mRecyclerViewAdapter.setFilter(filterList);
        return true;
    }
}
