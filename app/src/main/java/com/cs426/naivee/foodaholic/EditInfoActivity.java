package com.cs426.naivee.foodaholic;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cs426.naivee.foodaholic.R;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.zip.Inflater;

public class EditInfoActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_TAKE_TEXT_FOR_NAME = 401;
    private static final int REQUEST_CODE_TAKE_TEXT_FOR_ADDRESS = 402;
    private static final int REQUEST_CODE_TAKE_TEXT_FOR_FOODTYPE = 598;
    private static final int REQUEST_CODE_TAKE_TEXT_FOR_TEL = 985;
    private static final int REQUEST_CODE_TAKE_TEXT_FOR_WEBSITE = 188;
    private Place mPlace;
    private EditText mName;
    private EditText mAddress;
    private EditText mTel;
    private EditText mFoodType;
    private EditText mWebsite;
    private ImageView mImage;
    private int mID;
    private DatabaseHelper placeDB;
    private static final int PERMISSIONS_REQUEST_ACCESS_CAMERA = 2;
    private static final int PERMISSIONS_REQUEST_READ_LIBRARY = 3;
    private static final int REQUEST_CODE_READ_LIBRARY = 20;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 21;

    private void createToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.edit_info_toolbar);
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_restaurant_info);

        placeDB = new DatabaseHelper(EditInfoActivity.this);

        mName = findViewById(R.id.edit_info_name);
        mAddress = findViewById(R.id.edit_info_address);
        mTel = findViewById(R.id.edit_info_tel);
        mFoodType = findViewById(R.id.edit_info_foodtype);
        mWebsite = findViewById(R.id.edit_info_website);
        mImage = findViewById(R.id.edit_info_image);
        ScrollView scrollView = findViewById(R.id.edit_info_scrollview);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard(EditInfoActivity.this);
                return true;
            }
        });
        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(EditInfoActivity.this);
                CharSequence options[] = new CharSequence[] {
                        "From library",
                        "From camera"
                };
                final AlertDialog.Builder builder = new AlertDialog.Builder(EditInfoActivity.this);
                builder.setTitle("Choose image");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (which)
                        {
                            case 0:
                                requestReadLibraryPermission();
                                break;
                            case 1:
                                requestCameraPermission();
                                break;
                        }
                    }
                });
                builder.show();
            }
        });
        getIncomingIntent();
        createToolbar();
    }

    private void getIncomingIntent() {
        if (getIntent().hasExtra("position")) {
//            mPlace = getIntent().getParcelableExtra("info");
            mID = getIntent().getIntExtra("position",0);
            mPlace = placeDB.getPlace(mID);
            setInfo();
        }
    }

    public void saveInfo() {
        if (mName.getText().toString().compareTo("") != 0) {
            placeDB.updateData(String.valueOf(mID),
                    mName.getText().toString(),
                    mPlace.LatLng,
                    mWebsite.getText().toString(),
                    mAddress.getText().toString(),
                    mTel.getText().toString(),
                    BitmapUtility.getBytes(((BitmapDrawable)mImage.getDrawable()).getBitmap()),
                    mFoodType.getText().toString()
            );
            MapsActivity.setIsDataChanged();
            InfoActivity.setIsDataChanged();
            RecyclerViewActivity.setIsDataChanged();
            Toast.makeText(EditInfoActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(EditInfoActivity.this, "Name should not be empty!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.edit_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setInfo() {
        mName.setText(mPlace.Name);
        mAddress.setText(mPlace.Address);
        mTel.setText(mPlace.Tel);
        mFoodType.setText(mPlace.foodType);
        mWebsite.setText(mPlace.Website);
        Glide.with(this)
                .asBitmap()
                .load(mPlace.Image)
                .into(mImage);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_info_save_item:
                saveInfo();
                setResult(Activity.RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideKeyboard(EditInfoActivity.this);
        return super.onTouchEvent(event);
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_ACCESS_CAMERA);
        }
        else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent,REQUEST_CODE_CAPTURE_IMAGE);
        }
    }

    private void requestReadLibraryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_LIBRARY);
        }
        else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent,REQUEST_CODE_READ_LIBRARY);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_LIBRARY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent,REQUEST_CODE_READ_LIBRARY);
                }
                else
                    Toast.makeText(EditInfoActivity.this, "You don't have permission to access file location!", Toast.LENGTH_SHORT).show();
                break;
            case PERMISSIONS_REQUEST_ACCESS_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent,REQUEST_CODE_CAPTURE_IMAGE);
                }
                else
                    Toast.makeText(EditInfoActivity.this, "You don't have permission to use camera!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_TAKE_TEXT_FOR_NAME:
                if(resultCode == Activity.RESULT_OK){
                    EditText editText = findViewById(R.id.edit_info_name);
                    String s = data.getStringExtra("text");
                    editText.setText(s);
                }
                break;
            case REQUEST_CODE_TAKE_TEXT_FOR_ADDRESS:
                if(resultCode == Activity.RESULT_OK){
                    EditText editText = findViewById(R.id.edit_info_address);
                    String s = data.getStringExtra("text");
                    editText.setText(s);
                }
                break;
            case REQUEST_CODE_TAKE_TEXT_FOR_FOODTYPE:
                if(resultCode == Activity.RESULT_OK){
                    EditText editText = findViewById(R.id.edit_info_foodtype);
                    String s = data.getStringExtra("text");
                    editText.setText(s);
                }
                break;
            case REQUEST_CODE_TAKE_TEXT_FOR_TEL:
                if(resultCode == Activity.RESULT_OK){
                    EditText editText = findViewById(R.id.edit_info_tel);
                    String s = data.getStringExtra("text");
                    editText.setText(s);
                }
                break;
            case REQUEST_CODE_TAKE_TEXT_FOR_WEBSITE:
                if(resultCode == Activity.RESULT_OK){
                    EditText editText = findViewById(R.id.edit_info_website);
                    String s = data.getStringExtra("text");
                    editText.setText(s);
                }
                break;
            case REQUEST_CODE_READ_LIBRARY:
                if (resultCode == RESULT_OK && data != null) {
                    Uri uri = data.getData();

                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        mImage.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case REQUEST_CODE_CAPTURE_IMAGE:
                if (resultCode == RESULT_OK && data != null) {
                    Bitmap bitmap = (Bitmap)data.getExtras().get("data");
                    mImage.setImageBitmap(bitmap);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void editNameByImage(View view) {
        Intent intent = new Intent(this,OCRActivity.class);
        startActivityForResult(intent,REQUEST_CODE_TAKE_TEXT_FOR_NAME);
    }

    public void editAddressByImage(View view) {
        Intent intent = new Intent(this,OCRActivity.class);
        startActivityForResult(intent,REQUEST_CODE_TAKE_TEXT_FOR_ADDRESS);
    }

    public void editFoodTypeByImage(View view) {
        Intent intent = new Intent(this,OCRActivity.class);
        startActivityForResult(intent,REQUEST_CODE_TAKE_TEXT_FOR_FOODTYPE);
    }

    public void editTelephoneNumberByImage(View view) {
        Intent intent = new Intent(this,OCRActivity.class);
        startActivityForResult(intent,REQUEST_CODE_TAKE_TEXT_FOR_TEL);
    }

    public void editWebsiteByImage(View view) {
        Intent intent = new Intent(this,OCRActivity.class);
        startActivityForResult(intent,REQUEST_CODE_TAKE_TEXT_FOR_WEBSITE);
    }
}
