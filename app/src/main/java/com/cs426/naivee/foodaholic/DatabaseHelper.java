package com.cs426.naivee.foodaholic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "place.db";
    private static final String DATABASE_LOCATION = "/data/data/com.cs426.naivee.foodaholic/databases/";
    private static final String TABLE_NAME = "restaurant_table";
    private static final String COL1 = "ID";
    private static final String COL2 = "name";
    private static final String COL3 = "lat";
    private static final String COL4 = "lng";
    private static final String COL5 = "website";
    private static final String COL6 = "address";
    private static final String COL7 = "tel";
    private static final String COL8 = "image";
    private static final String COL9 = "foodtype";
    private Context mContext;
    private SQLiteDatabase mDatabase;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null,1);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, lat DECIMAL(10,8) NOT NULL, lng DECIMAL(11,8), website TEXT, address TEXT, tel TEXT, image BLOB, foodtype TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP IF TABLE EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addData(String name, LatLng latLng, String website, String address, String tel, byte[] image, String foodtype) {
        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(COL2,name);
//        contentValues.put(COL3,latLng.latitude);
//        contentValues.put(COL4,latLng.longitude);
//        contentValues.put(COL5,website);
//        contentValues.put(COL6,address);
//        contentValues.put(COL7,tel);
//        contentValues.put(COL8,image);
//        contentValues.put(COL9,foodtype);
//
//        long result = db.insert(TABLE_NAME,null,contentValues);
//
//        if (result == -1)
//            return false;
//        else
//            return true;
        String sql = "INSERT INTO " + TABLE_NAME + " VALUES (null,?,?,?,?,?,?,?,?)";

        SQLiteStatement statement = db.compileStatement(sql);
        statement.clearBindings();
        statement.bindString(1,name);
        statement.bindDouble(2,latLng.latitude);
        statement.bindDouble(3,latLng.longitude);
        statement.bindString(4,website);
        statement.bindString(5,address);
        statement.bindString(6,tel);
        statement.bindBlob(7,image);
        statement.bindString(8,foodtype);
        statement.executeInsert();
    }
    public Cursor showData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM " + TABLE_NAME,null);
        return data;
    }
    public Cursor showDataByID(String ID) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE ID = '" + ID + "'",null);
        return data;
    }
    public boolean updateData(String id, String name, LatLng latLng, String website, String address, String tel, byte[] image, String foodtype) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1,id);
        contentValues.put(COL2,name);
        contentValues.put(COL3,latLng.latitude);
        contentValues.put(COL4,latLng.longitude);
        contentValues.put(COL5,website);
        contentValues.put(COL6,address);
        contentValues.put(COL7,tel);
        contentValues.put(COL8,image);
        contentValues.put(COL9,foodtype);

        db.update(TABLE_NAME,contentValues,"ID = ?", new String[] {id});
        return true;
    }

    public Integer deleteData(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME,"ID = ?", new String[] {id});
    }

    public void openDatabase() {
        String dbPath = mContext.getDatabasePath(DATABASE_NAME).getPath();
        if (mDatabase != null && mDatabase.isOpen())
            return;
        mDatabase = SQLiteDatabase.openDatabase(dbPath,null,SQLiteDatabase.OPEN_READWRITE);
    }

    public void closeDatatbase() {
        if (mDatabase !=  null)
            mDatabase.close();
    }

    public ArrayList<Place> getPlaceArrayList() {
        ArrayList<Place> placeArrayList = new ArrayList<>();
        openDatabase();
        Cursor cursor = showData();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            placeArrayList.add(new Place(
                    cursor.getInt(0),
                    cursor.getString(1),
                    new LatLng(cursor.getDouble(2),cursor.getDouble(3)),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    cursor.getBlob(7),
                    cursor.getString(8)
                    ));
            cursor.moveToNext();
        }
        cursor.close();
        closeDatatbase();
        return placeArrayList;
    }
    public Place getPlace(int ID) {
        Place place;
        openDatabase();
        Cursor cursor = showDataByID(String.valueOf(ID));
        cursor.moveToFirst();
        place = new Place(
                cursor.getInt(0),
                cursor.getString(1),
                new LatLng(cursor.getDouble(2),cursor.getDouble(3)),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(6),
                cursor.getBlob(7),
                cursor.getString(8)
        );
        cursor.close();
        closeDatatbase();
        return place;
    }
}
