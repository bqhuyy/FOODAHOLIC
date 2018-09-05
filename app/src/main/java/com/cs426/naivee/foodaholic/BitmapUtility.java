package com.cs426.naivee.foodaholic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BitmapUtility {
    public static byte[] getBytes(Bitmap bitmap) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
            byte[] array = stream.toByteArray();
            stream.close();
            return array;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image,0,image.length);
    }
}
