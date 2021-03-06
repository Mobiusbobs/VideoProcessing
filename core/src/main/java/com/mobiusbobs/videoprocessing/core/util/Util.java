package com.mobiusbobs.videoprocessing.core.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;


public class Util {
    private static final String TAG = "Util";

    public static void fileSize(String filePath) {
        File file = new File(filePath);
        long fileSize = file.length();
        Log.d(TAG, "fileSize=" + fileSize + ", " + fileSize / 1024 + "KB, " + fileSize / 1024 / 1024 + "MB");
    }

    public static int[] getScreenDimen(Context context)    {
        // get screen size...
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return new int[] {size.x, size.y};
    }

  // from http://stackoverflow.com/questions/4896223/how-to-get-an-uri-of-an-image-resource-in-android
  public static Uri resourceToUri(Context context, int resID) {
    return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
      context.getResources().getResourcePackageName(resID) + '/' +
      context.getResources().getResourceTypeName(resID) + '/' +
      context.getResources().getResourceEntryName(resID));
  }

}
