package com.mobiusbobs.videoprocessing.sample.Util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * android
 * <p/>
 * Created by wangalbert on 8/9/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class Logger {
  private static final String TAG = "Logger";



  public static void toastLong(final Activity activity, final String msg) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
        Log.d(TAG, msg);
      }
    });
  }

  public static void toastShort(final Activity activity, final String msg) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, msg);
      }
    });
  }
}
