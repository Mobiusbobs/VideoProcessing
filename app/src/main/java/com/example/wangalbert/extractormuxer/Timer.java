package com.example.wangalbert.extractormuxer;

import android.util.Log;

/**
 * android
 * <p/>
 * Created by wangalbert on 4/1/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class Timer {
  private static final String TAG = "Timer";

  private long startTime = 0;

  public void startTimer() {
    startTime = System.currentTimeMillis();
    Log.d(TAG, "startTimer");
  }

  public void endTimer(String msg) {
    long endTime = System.currentTimeMillis();
    Log.d(TAG, "endTimer: " + msg + ": time elapse = " + (endTime - startTime));
  }
}
