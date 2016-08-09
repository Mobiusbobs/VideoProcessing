package com.mobiusbobs.videoprocessing.sample;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

/**
 * VideoProcessing
 * <p>
 * Created by rayshih on 8/9/16.
 * Copyright (c) 2015 MobiusBobs Inc. All rights reserved.
 */

public class Util {

  // http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
  public static String getRealPathFromURI(Context context, Uri contentUri) {
    Cursor cursor = null;
    try {
      String[] proj = { MediaStore.Images.Media.DATA };
      cursor = context.getContentResolver()
        .query(contentUri,  proj, null, null, null);

      int columnIndex = 0;
      if (cursor == null) {
        throw new IllegalArgumentException(
          "Cannot get cursor with contentUri: " + contentUri
        );
      }

      columnIndex = cursor
        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(columnIndex);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  public static void toastLong(Context context, String msg) {
    Toast
      .makeText(context, msg, Toast.LENGTH_LONG)
      .show();
  }

  public static void toastShort(Context context, String msg) {
    Toast
      .makeText(context, msg, Toast.LENGTH_SHORT)
      .show();
  }
}
