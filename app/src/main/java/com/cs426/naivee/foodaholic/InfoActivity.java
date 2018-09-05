package com.cs426.naivee.foodaholic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cs426.naivee.foodaholic.R;

public class InfoActivity extends AppCompatActivity {
    private static boolean isDataChanged = false;
    private Place mPlace;
    private DatabaseHelper placeDB;
    private int mID;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_info);
        placeDB = new DatabaseHelper(InfoActivity.this);
        getIncomingIntent();
        createToolbar();
    }

    private void createToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.res_info_toolbar);
        toolbar.setTitle(mPlace.Name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.info_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void getIncomingIntent() {
        if (getIntent().hasExtra("position")) {
            mID = getIntent().getIntExtra("position",0);
//            mPlace = getIntent().getParcelableExtra("info");
            mPlace = placeDB.getPlace(mID);
            setInfo();
        }
    }

    private void setInfo() {
        TextView address = findViewById(R.id.res_info_address);
        TextView tel = findViewById(R.id.res_info_telephone);
        TextView foodType = findViewById(R.id.res_info_foodtype);
        TextView website = findViewById(R.id.res_info_website);
        address.setText(mPlace.Address);
        tel.setText(mPlace.Tel);
        foodType.setText(mPlace.foodType);
        website.setText(mPlace.Website);
        ImageView imageView = findViewById(R.id.res_info_image);
        Glide.with(this)
                .asBitmap()
                .load(mPlace.Image)
                .into(imageView);
    }

    public void callActivity() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        TextView tel = findViewById(R.id.res_info_telephone);
        String tmp = tel.getText().toString();
        tmp = tmp.replaceAll("[^0-9,+]","");
        callIntent.setData(Uri.parse("tel:"+tmp));
        InfoActivity.this.startActivity(callIntent);
    }

    public void websiteActivity() {
        TextView website = findViewById(R.id.res_info_website);
        FrameLayout frameLayout = new FrameLayout(InfoActivity.this);
        final WebView webView = new WebView(InfoActivity.this);
        webView.setVisibility(View.INVISIBLE);
        String tmp = website.getText().toString();
        webView.loadUrl(tmp);
        final ProgressBar loadingWheel = new ProgressBar(InfoActivity.this);
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
        new AlertDialog.Builder(InfoActivity.this).setView(frameLayout)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    public void startEditActivity() {
        Intent intent = new Intent(InfoActivity.this,EditInfoActivity.class);
        intent.putExtra("position",mPlace.Id);
        InfoActivity.this.startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.res_info_item_edit:
                startEditActivity();
                return true;
            case R.id.res_info_item_call:
                callActivity();
                return true;
            case R.id.res_info_item_website:
                websiteActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
}
