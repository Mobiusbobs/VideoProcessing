package com.mobiusbobs.videoprocessing.core.codec;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.util.Range;
import android.widget.Toast;

import com.mobiusbobs.videoprocessing.core.VideoProcessor;

/**
 * android
 * <p>
 * Created by wangalbert on 8/8/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class MediaCodecChecker {

  public static void dumpDeviceInfo() {
    String TAG = "DeviceInfo";

    String osArch = System.getProperty("os.arch");
    String osName = System.getProperty("os.name");
    String osVersion = System.getProperty("os.version");

    int apiLevel = Build.VERSION.SDK_INT;     // API Level
    String device = Build.DEVICE;             // Device
    String display = Build.DISPLAY;           // Displau
    String model = Build.MODEL;               // Model
    String manufacturer = Build.MANUFACTURER; // Manufacturer
    String product = Build.PRODUCT;           // Product
    String hardware = Build.HARDWARE;         // Hardware

    Log.d(TAG, "OS architecture = " + osArch);
    Log.d(TAG, "OS (kernel) name = " + osName);
    Log.d(TAG, "OS (kernel) version = " + osVersion);

    Log.d(TAG, "apiLevel = " + apiLevel);
    Log.d(TAG, "device = " + device);
    Log.d(TAG, "display = " + display);
    Log.d(TAG, "model = " + model);
    Log.d(TAG, "manufacturer = " + manufacturer);
    Log.d(TAG, "product = " + product);
    Log.d(TAG, "hardware = " + hardware);
  }

  public static boolean checkVideoCapabilitySupported(MediaCodecInfo videoCodecInfo, MediaFormat inputVideoFormat) {
    Log.d("VIDEO_CAPABILITIES", "checkVideoCapabilitySupported... called...");

    if(android.os.Build.VERSION.SDK_INT >= 21) {
      int width = VideoProcessor.getMediaDataOrDefault(inputVideoFormat, MediaFormat.KEY_WIDTH, -1);
      int height = VideoProcessor.getMediaDataOrDefault(inputVideoFormat, MediaFormat.KEY_HEIGHT, -1); //inputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
      double frameRate = VideoProcessor.getMediaDataOrDefault(inputVideoFormat, MediaFormat.KEY_FRAME_RATE, -1); //inputVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);

      if (width == -1 || height == -1 || frameRate == -1) {
        Log.e("VIDEO_CAPABILITIES", "inputMediaFormat missing variable: " +
          "width=" + width + ", height=" + height + ", frameRate=" + frameRate);
        return false;
      }

      MediaCodecInfo.VideoCapabilities videoCapabilities =
        videoCodecInfo.getCapabilitiesForType(VideoProcessor.getMimeTypeFor(inputVideoFormat)).getVideoCapabilities();
      Log.d("VIDEO_CAPABILITIES", "width = " + width + ", height = " + height + ", frameRate = " + frameRate);
      Log.d("VIDEO_CAPABILITIES", "videoCapabilities = " + videoCapabilities);

      Range<Integer> bitrateRange = videoCapabilities.getBitrateRange();
      Range<Integer> frameRateRange = videoCapabilities.getSupportedFrameRates();
      Range<Integer> heightRange = videoCapabilities.getSupportedHeights();
      Range<Integer> widthRange = videoCapabilities.getSupportedWidths();
      boolean isSizeSupport = videoCapabilities.isSizeSupported(width, height);
      boolean areSizeAndRateSupport = videoCapabilities.areSizeAndRateSupported(width, height, frameRate);

      Log.d("VIDEO_CAPABILITIES", "bitrateRange = " + bitrateRange.toString());
      Log.d("VIDEO_CAPABILITIES", "frameRateRange = " + frameRateRange.toString());
      Log.d("VIDEO_CAPABILITIES", "heightRange = " + heightRange.toString());
      Log.d("VIDEO_CAPABILITIES", "widthRange = " + widthRange.toString());
      Log.d("VIDEO_CAPABILITIES", "isSizeSupport = " + isSizeSupport);
      Log.d("VIDEO_CAPABILITIES", "areSizeAndRateSupport = " + areSizeAndRateSupport);

      if (isSizeSupport && areSizeAndRateSupport) return true;
      return false;
    }

    else {
      Log.d("VIDEO_CAPABILITIES", "android.os.Build.VERSION.SDK_INT is " + android.os.Build.VERSION.SDK_INT + ", checkVideoCapabilitySupported not supported");
      return false;
    }
  }

  public static void checkAudioCapability() {


  }


  // --- dump media format information
  public static void dumpMediaFormat(MediaFormat mediaFormat) {
    Log.d("DUMP_MEDIA_FORMAT", "Start");
    printMediaFormat(mediaFormat, "KEY_AAC_PROFILE", MediaFormat.KEY_AAC_PROFILE, "Integer");
    printMediaFormat(mediaFormat, "KEY_BIT_RATE", MediaFormat.KEY_BIT_RATE, "Integer");
    printMediaFormat(mediaFormat, "KEY_CHANNEL_COUNT", MediaFormat.KEY_CHANNEL_COUNT, "Integer");
    printMediaFormat(mediaFormat, "KEY_CHANNEL_MASK", MediaFormat.KEY_CHANNEL_MASK, "Integer");
    printMediaFormat(mediaFormat, "KEY_COLOR_FORMAT", MediaFormat.KEY_COLOR_FORMAT, "Integer");
    printMediaFormat(mediaFormat, "KEY_DURATION", MediaFormat.KEY_DURATION, "Long");
    printMediaFormat(mediaFormat, "KEY_FLAC_COMPRESSION_LEVEL", MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, "Integer");
    printMediaFormat(mediaFormat, "KEY_CAPTURE_RATE", MediaFormat.KEY_CAPTURE_RATE, "Integer");
    printMediaFormat(mediaFormat, "KEY_FRAME_RATE", MediaFormat.KEY_FRAME_RATE, "Integer");
    printMediaFormat(mediaFormat, "KEY_HEIGHT", MediaFormat.KEY_HEIGHT, "Integer");
    printMediaFormat(mediaFormat, "KEY_IS_ADTS", MediaFormat.KEY_IS_ADTS, "Integer");
    printMediaFormat(mediaFormat, "KEY_I_FRAME_INTERVAL", MediaFormat.KEY_I_FRAME_INTERVAL, "Integer");
    printMediaFormat(mediaFormat, "KEY_MAX_INPUT_SIZE", MediaFormat.KEY_MAX_INPUT_SIZE, "Integer");
    printMediaFormat(mediaFormat, "KEY_MIME", MediaFormat.KEY_MIME, "String");
    printMediaFormat(mediaFormat, "KEY_SAMPLE_RATE", MediaFormat.KEY_SAMPLE_RATE, "Integer");
    printMediaFormat(mediaFormat, "KEY_WIDTH", MediaFormat.KEY_WIDTH, "Integer");

    Log.d("DUMP_MEDIA_FORMAT", "End");
  }

  public static void printMediaFormat(MediaFormat mediaFormat, String tag, String key, String type) {
    if (mediaFormat.containsKey(key)) {
      if (type.equals("Integer")) {
        Log.d("DUMP_MEDIA_FORMAT", tag + ": " + mediaFormat.getInteger(key));
      } else if (type.equals("Long")) {
        Log.d("DUMP_MEDIA_FORMAT", tag + ": " + mediaFormat.getLong(key));
      } else if (type.equals("String")) {
        Log.d("DUMP_MEDIA_FORMAT", tag + ": " + mediaFormat.getString(key));
      }
      return;
    }
    Log.d("DUMP_MEDIA_FORMAT", tag + ": Not Found");
  }
}
