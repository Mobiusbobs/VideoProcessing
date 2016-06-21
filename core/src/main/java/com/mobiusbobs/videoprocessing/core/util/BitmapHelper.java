package com.mobiusbobs.videoprocessing.core.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * VideoProcessing
 * <p/>
 * Created by rayshih on 4/13/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class BitmapHelper {

    public static Bitmap generateBitmap(Context context, int resId) {
        return BitmapFactory.decodeResource(context.getResources(), resId);
    }

    public static Bitmap generateBitmap(String fileUrl) throws IOException {
      return generateBitmap(fileUrl, 1);
    }

    public static Bitmap generateBitmap(String fileUrl, int sampleSize) throws IOException {
        // get bitmap
        File imageFile = new File(fileUrl);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);

        if (bitmap != null) return bitmap;

        InputStream is = new URL(fileUrl).openStream();
        bitmap = BitmapFactory.decodeStream(is, null, bmOptions);

        return bitmap;
    }
}
