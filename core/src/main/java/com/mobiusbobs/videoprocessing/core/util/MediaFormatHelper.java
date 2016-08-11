package com.mobiusbobs.videoprocessing.core.util;

import android.media.MediaFormat;

/**
 * VideoProcessing
 * <p/>
 * Created by rayshih on 8/11/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class MediaFormatHelper {
  public static int getInteger(MediaFormat inputVideoFormat, String key, int defaultValue) {
    if (inputVideoFormat.containsKey(key)) {
      return inputVideoFormat.getInteger(key);
    }

    return defaultValue;
  }
}
