package com.mobiusbobs.videoprocessing.core.codec;

import android.util.Log;

/**
 * android
 * PT => Presentation Time (us)
 * Created by wangalbert on 3/31/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class PTHelper {
  public static final String TAG = "Util";

  public static final int RECORD_STATE_ON_PAUSE = 1;
  public static final int RECORD_STATE_ON_RESUME = 2;

  private long timeOffsetNs = 0;
  private long pauseTime = 0;
  private long resumeTime = 0;

  public static long getCurrentPTSUs() {
    return System.nanoTime();
  }

  public long getPTSUsWithOffset() {
    return (getCurrentPTSUs() - timeOffsetNs) / 1000L ;
  }

  public long getTimeOffsetNs() {
    return timeOffsetNs;
  }

  // calculate presentation time offset based on record state
  public void adjustPTSUsTime(int state) {
    switch(state) {
      case RECORD_STATE_ON_PAUSE:
        pauseTime = System.nanoTime();
        break;
      case RECORD_STATE_ON_RESUME:
        resumeTime = System.nanoTime();
        if (pauseTime != 0 && resumeTime > pauseTime)
          timeOffsetNs += (resumeTime - pauseTime);
        break;
    }
    Log.d(TAG, "state " + state + ": timeOffset=" + timeOffsetNs);
  }
}
