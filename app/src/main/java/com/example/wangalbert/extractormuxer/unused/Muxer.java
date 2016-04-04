package com.example.wangalbert.extractormuxer.unused;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.example.wangalbert.extractormuxer.Extractor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * android
 * <p/>
 * Created by wangalbert on 3/18/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class Muxer {
  public static final String TAG = "Muxer";
  public static final boolean VERBOSE = true;

  private static final int MAX_SAMPLE_SIZE = 256 * 1024;


  public void cloneMediaUsingMuxer(String filePath, String outputPath, int degrees)
    throws IOException
  {

    // Set up MediaExtractor to read from the source.
    MediaExtractor extractor = new Extractor().createExtractor(filePath);
    int trackCount = extractor.getTrackCount();

    // Set up MediaMuxer for the destination.
    MediaMuxer muxer;
    muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

    // Set up the tracks.
    HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
    for (int i = 0; i < trackCount; i++) {
      extractor.selectTrack(i);
      MediaFormat format = extractor.getTrackFormat(i);
      int dstIndex = muxer.addTrack(format);
      indexMap.put(i, dstIndex);
    }

    // Copy the samples from MediaExtractor to MediaMuxer.
    boolean sawEOS = false;
    int bufferSize = MAX_SAMPLE_SIZE;
    int frameCount = 0;
    int offset = 100; //TODO why is offset 100
    ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    if (degrees >= 0) {
      muxer.setOrientationHint(degrees);
    }
    muxer.start();

    while (!sawEOS) {
      bufferInfo.offset = offset;
      bufferInfo.size = extractor.readSampleData(dstBuf, offset);
      if (bufferInfo.size < 0) {
        if (VERBOSE) {
          Log.d(TAG, "saw input EOS.");
        }
        sawEOS = true;
        bufferInfo.size = 0;
      } else {
        bufferInfo.presentationTimeUs = extractor.getSampleTime();
        bufferInfo.flags = extractor.getSampleFlags();
        int trackIndex = extractor.getSampleTrackIndex();

        muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
        extractor.advance();
        frameCount++;
        if (VERBOSE) {
          Log.d(TAG, "Frame (" + frameCount + ") " +
            "PresentationTimeUs:" + bufferInfo.presentationTimeUs +
            " Flags:" + bufferInfo.flags +
            " TrackIndex:" + trackIndex +
            " Size(KB) " + bufferInfo.size / 1024);
        }
      }
    }
    muxer.stop();
    muxer.release();
    return;
  }

  public static void release(MediaMuxer muxer) {
    if (muxer != null) {
      muxer.stop();
      muxer.release();
    }
  }
}
