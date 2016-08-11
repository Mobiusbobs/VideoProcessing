package com.mobiusbobs.videoprocessing.core.codec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * android
 * <p/>
 * Created by wangalbert on 3/22/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class AudioRecorder {
  public static final String TAG = "AudioRecord";

  // For calculate audio record buffer
  public static final int SAMPLES_PER_FRAME = 1024;	// AAC, bytes/frame/channel
  public static final int FRAMES_PER_BUFFER = 25; 	// AAC, frame/buffer/sec

  // Log
  private static boolean VERBOSE = false;

  // Recoder
  AudioRecord audioRecord;

  // Encoder
  AudioEncoderCore audioEncoderCore;

  // State Flag
  public boolean isEOS = false;
  public boolean isCapturing = false; // Capturing the audio input
  public boolean isRecording = false; // true: writing data to mux, false: pause state. do nothing.

  // Presentation timestamp offset
  private PTHelper ptHelper;

  // Constructor
  public AudioRecorder(MuxerWrapper muxerWrapper) throws IOException {
    audioRecord = createAudioRecord();
    audioEncoderCore = new AudioEncoderCore(muxerWrapper, audioRecord);
    ptHelper = new PTHelper();
  }

  public void startRecord() {
    Log.d(TAG, "Start Record!");
    isCapturing = true;
    isRecording = true;   // recording internal state

    AudioThread audioThread = new AudioThread();
    audioThread.start();
  }

  public void resumeRecord() {
    if (!isCapturing) {
      startRecord();
    } else {
      isRecording = true;
      ptHelper.adjustPTSUsTime(PTHelper.RECORD_STATE_ON_RESUME);
      Log.d(TAG, "Resume Record");
    }
  }

  public void pauseRecrod() {
    Log.d(TAG, "Pause Record");
    isRecording = false;
    ptHelper.adjustPTSUsTime(PTHelper.RECORD_STATE_ON_PAUSE);
  }

  public void stopRecord() {
    Log.d(TAG, "Stop Record");
    isCapturing = false;
    isRecording = false;
  }

  public void release() {
    if (audioEncoderCore != null) {
      audioEncoderCore.release();
    }
  }

  private static int[] mSampleRates = new int[]{44100, 22050, 11025, 8000};
  public AudioRecord createAudioRecord() {
    // declare bufferSize for AudioRecord
    int bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;

    for (int rate : mSampleRates) {
      for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT}) {
        for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
          try {
            Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
              + channelConfig);

            int minBufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
            if (bufferSize < minBufferSize)
              bufferSize = minBufferSize;

            if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
              // check if we can instantiate and have a success
              AudioRecord recorder =
                new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);
              if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                return recorder;
            }
          } catch (Exception e) {
            Log.e(TAG, rate + "Exception, keep trying.", e);
          }
        }
      }
    }
    return null;
  }

  private void printFlag(String msg) {
    if (VERBOSE)
      Log.d(TAG, msg + ": isCapturing=" + isCapturing + ", isEOS=" + isEOS);
  }

  /**
   * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
   * and write them to the MediaCodec encoder
   */
  private class AudioThread extends Thread {
    private long lastAudioPresentationTime = 0;
    @Override
    public void run() {
      android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
      try {
        if (audioRecord != null) {
          if (isCapturing) {
            final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
            int readBytes;

            // start recording
            audioRecord.startRecording();

            printFlag("before enter while loop");
            while(isCapturing && !isEOS) {
              buf.clear();
              // read audio data from internal mic to buffer and return amount of byte read

              if (isRecording)
                readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
              else
                readBytes = 0;

              if (readBytes > 0) {
                long presentationTime = ptHelper.getPTSUsWithOffset();
                // HOTFIX to skip bytes with timestamp issue
                if (presentationTime < lastAudioPresentationTime) continue;
                lastAudioPresentationTime = presentationTime;

                // set audio data to encoder
                buf.position(readBytes);
                buf.flip();

                audioEncoderCore.encode(buf, readBytes, presentationTime, false);
                printFlag("encode done");

                audioEncoderCore.drainEncoder();
                printFlag("drain done");
              }
            }
            printFlag("Exit while loop");
            printFlag("audioRecord.stop()");
            audioRecord.stop();
          }

          printFlag("audioRecord.release()");
          audioRecord.release();
          audioEncoderCore.release();
        } else {
          Log.e(TAG, "failed to initialize AudioRecord");
        }
      } catch (final Exception e) {
        Log.e(TAG, "AudioThread#run", e);
      }
    }
  }

}
