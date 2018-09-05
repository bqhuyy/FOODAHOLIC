package com.cs426.naivee.foodaholic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cs426.naivee.foodaholic.R;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private LinearLayout mLayoutDots;
    private TextView[] mDotsTextView;
    private int[] mLayouts;
    private Button mBtnSkip;
    private Button mBtnNext;
    private MyPagerAdapter mMyPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isFirstTimeStartApp()) {
            startMapsActivity();
            finish();
        }

        setStatusBarTransparent();

        setContentView(R.layout.activity_welcome);

        mViewPager = findViewById(R.id.viewPaper);
        mLayoutDots = findViewById(R.id.welcome_dotLayout);
        mBtnNext = findViewById(R.id.welcome_buttonNext);
        mBtnSkip = findViewById(R.id.welcome_buttonSkip);

        mBtnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMapsActivity();
            }
        });

        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPage = mViewPager.getCurrentItem()+1;
                if (currentPage < mLayouts.length)
                    mViewPager.setCurrentItem(currentPage);
                else startMapsActivity();
            }
        });
        mLayouts = new int[] {R.layout.slider_1,R.layout.slider_2,R.layout.slider_3};
        mMyPagerAdapter = new MyPagerAdapter(mLayouts,getApplicationContext());
        mViewPager.setAdapter(mMyPagerAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == mLayouts.length-1) {
                    mBtnNext.setText("START");
                    mBtnSkip.setVisibility(View.GONE);
                }
                else {
                    mBtnNext.setText("NEXT");
                    mBtnSkip.setVisibility(View.VISIBLE);
                }
                setDotsStatus(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        setDotsStatus(0);
    }

    private boolean isFirstTimeStartApp() {
        SharedPreferences ref = getApplicationContext().getSharedPreferences("IntroSliderApp", Context.MODE_PRIVATE);
        return ref.getBoolean("FirstTimeStartFlag",true);
    }

    private void setFirstTimeStartStatus(boolean status) {
        SharedPreferences ref = getApplicationContext().getSharedPreferences("IntroSliderApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = ref.edit();
        editor.putBoolean("FirstTimeStartFlag",status);
        editor.commit();
    }

    private void setDotsStatus(int page) {
        mLayoutDots.removeAllViews();
        mDotsTextView = new TextView[mLayouts.length];
        for (int i = 0; i < mDotsTextView.length; i++) {
            mDotsTextView[i] = new TextView(this);
            mDotsTextView[i].setText(Html.fromHtml("&#8226"));
            mDotsTextView[i].setTextSize(30);
            //Inactive color dotview
            mDotsTextView[i].setTextColor(Color.parseColor("#a9b4bb"));
            mLayoutDots.addView(mDotsTextView[i]);
        }
        //Set current dot active
        if (mDotsTextView.length > 0)
            mDotsTextView[page].setTextColor(Color.parseColor("#ffffff"));
    }

    private void startMapsActivity() {
        setFirstTimeStartStatus(false);
        startActivity(new Intent(WelcomeActivity.this,MapsActivity.class));
    }

    private void setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
