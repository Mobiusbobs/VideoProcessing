package com.mobiusbobs.videoprocessing.core.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * android
 * <p>
 * Created by wangalbert on 3/30/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class MuxerWrapper {
  public static final String TAG = "MuxerWrapper";

  private MediaMuxer muxer;

  // flag
  private int encoderCount = 0;
  private static int trackCount = 0;
  private boolean muxerStarted = false;

  // Constructor
  public MuxerWrapper(MediaMuxer muxer, int encoderCount) {
    this.muxer = muxer;
    this.encoderCount = encoderCount;
    trackCount = 0;
  }

  public synchronized boolean isStarted() {
    return muxerStarted;
  }

  synchronized boolean start() {
    Log.v(TAG, "start called!");
    trackCount++;
    if ((encoderCount > 0) && (trackCount == encoderCount)) {
      muxer.start();
      muxerStarted = true;
      notifyAll();
      Log.v(TAG, "MediaMuxer started:");
    }
    return muxerStarted;
  }

  /**
   * request stop recording from encoder when encoder received EOS
   */
  synchronized void stopAndRelease() {
    Log.d(TAG, "stop: trackCount=" + trackCount);
    trackCount--;
    if ((encoderCount > 0) && (trackCount <= 0)) {
      muxer.stop();
      muxer.release();
      muxerStarted = false;
      Log.d(TAG, "MediaMuxer stopped:");
    }
  }

  /**
   * assign encoder to muxer
   * @param format
   * @return minus value indicate error
   */
  synchronized int addTrack(final MediaFormat format) {
    if (muxerStarted)
      throw new IllegalStateException("muxer already started");
    final int trackIndex = muxer.addTrack(format);
    Log.d(TAG, "addTrack:trackNum=" + encoderCount + ",trackIndex=" + trackIndex + ",format=" + format);
    return trackIndex;
  }

  /**
   * write encoded data to muxer
   * @param trackIndex
   * @param byteBuf
   * @param bufferInfo
   */
  synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
    if (trackCount > 0)
      muxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
  }

}
