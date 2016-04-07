package com.example.wangalbert.extractormuxersample;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * android
 * <p/>
 * Created by wangalbert on 3/18/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class Extractor {
  private static final String TAG = "Extractor";

  public Extractor() {

  }

  public MediaExtractor createExtractor(String filePath) {
    MediaExtractor extractor = new MediaExtractor();

    // set dataSource for extractor
    File file = new File(filePath);
    try {
      FileDescriptor fd = new FileInputStream(file).getFD();
      extractor.setDataSource(fd);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return extractor;
  }

  public static MediaExtractor createExtractor(Context context, int rawResId) throws IOException {
    AssetFileDescriptor srcFd = context.getResources().openRawResourceFd(rawResId);
    MediaExtractor extractor = new MediaExtractor();
    extractor.setDataSource(srcFd.getFileDescriptor(), srcFd.getStartOffset(),
      srcFd.getLength());
    return extractor;
  }


  // MediaExtractor
  // TODO update this..?
  public void extractVideoFile(String filePath) {
    MediaExtractor extractor = createExtractor(filePath);

    int videoTrack = -1;
    int audioTrack = -1;

    int numTracks = extractor.getTrackCount();
    Log.d(TAG, "numTracks = " + numTracks);
    for (int i = 0; i < numTracks; ++i) {
      MediaFormat format = extractor.getTrackFormat(i);
      if (isVideoFormat(format)) {
        videoTrack = i;
        extractor.selectTrack(videoTrack);
      }
      else if (isAudioFormat(format)) {
        audioTrack = i;
        extractor.selectTrack(audioTrack);
      }

      // Log information
      String mime = format.getString(MediaFormat.KEY_MIME);
      Log.d(TAG, "extractVideoFile: format.toString = " + format.toString());
      Log.d(TAG, "extractVideoFile: mime = " + mime);
    }

    // TODO: usually we will get the inputBuffer from MediaCodec, but for now,
    // TODO  i'll create my own byteBuffer and write the data to a file to test
    ByteBuffer inputBuffer = ByteBuffer.allocate(1024 * 1024);  //tmp allocation
    int offset = 100;

    // TEST: Extracted file
    FileChannel tmpOutVideo = null;
    FileChannel tmpOutAudio = null;
    File outputAVC = new File(MainActivity.FILE_OUTPUT_AVC);
    File outputAAC = new File(MainActivity.FILE_OUTPUT_AAC);
    try {
      boolean append = true;
      tmpOutVideo = new FileOutputStream(outputAVC, append).getChannel();
      tmpOutAudio = new FileOutputStream(outputAAC, append).getChannel();
    } catch(IOException e) {
      e.printStackTrace();
    }

    while (true) { //extractor.readSampleData(inputBuffer, ...) >= 0) {
      int sampleDataSize = extractor.readSampleData(inputBuffer, offset);
      int trackIndex = extractor.getSampleTrackIndex();

      Log.d(TAG, "sample Data size = " + sampleDataSize + ", trackIndex = " + trackIndex);
      if(sampleDataSize == -1) {
        Log.d(TAG, "Read Sample Data: no more data are available");
        break;
      }

      //int trackIndex = extractor.getSampleTrackIndex();
      //long presentationTimeUs = extractor.getSampleTime();

      // TODO: test: write to file
      try {
        if (trackIndex == videoTrack)
          tmpOutVideo.write(inputBuffer);
        else if (trackIndex == audioTrack)
          tmpOutAudio.write(inputBuffer);
        else
          Log.e(TAG, "UNWANTED TRACK!!")
            ;
        inputBuffer.clear();
      } catch(IOException e) {
        e.printStackTrace();
      }

      extractor.advance();
    }

    try {
      tmpOutVideo.close();
      tmpOutAudio.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.d(TAG, "File Size: Original: MP4 = " + new File(filePath).length());
    Log.d(TAG, "File Size: Video: MP4 = " + outputAVC.length());
    Log.d(TAG, "File Size: Audio: AAC = " + outputAAC.length());

    extractor.release();
    extractor = null;
  }

  private static boolean isVideoFormat(MediaFormat format) {
    return getMimeTypeFor(format).startsWith("video/");
  }
  private static boolean isAudioFormat(MediaFormat format) {
    return getMimeTypeFor(format).startsWith("audio/");
  }
  public static String getMimeTypeFor(MediaFormat format) {
    return format.getString(MediaFormat.KEY_MIME);
  }


  public static int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
    for (int index = 0; index < extractor.getTrackCount(); ++index) {
      if (isVideoFormat(extractor.getTrackFormat(index))) {
        extractor.selectTrack(index);
        Log.d(TAG, "Select Video Track index: " + index);
        return index;
      }
    }
    return -1;
  }

  public static int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
    for (int index = 0; index < extractor.getTrackCount(); ++index) {
      if (isAudioFormat(extractor.getTrackFormat(index))) {
        extractor.selectTrack(index);
        Log.d(TAG, "Select Audio Track index: " + index);
        return index;
      }
    }
    return -1;
  }

  // ----- FOR TESTING -----


}
