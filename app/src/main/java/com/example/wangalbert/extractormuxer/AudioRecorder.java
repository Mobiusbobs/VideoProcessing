package com.example.wangalbert.extractormuxer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * android
 * <p/>
 * Created by wangalbert on 3/18/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class AudioRecorder {
  public static final String TAG = "AudioRecorder";


  private AudioRecord recorder;

  private static final String MIME_TYPE = "audio/mp4a-latm";
  private static final int SAMPLE_RATE = 44100;  // 44.1[KHz] is only setting guaranteed to be available on all devices.
  private static final int BIT_RATE = 64000;
  public static final int SAMPLES_PER_FRAME = 1024;  // AAC, bytes/frame/channel
  public static final int FRAMES_PER_BUFFER = 25;  // AAC, frame/buffer/sec

  private int buffSize = 0;
  private int frequncy = 44100;
  private boolean isRecording = false;

  private Thread audioRecordThread = null;
  private String AudioThreadName = "AudioRecorder Thread";

  private String outputFilePath;

  public AudioRecorder(String outputFilePath) {
    init();
    this.outputFilePath = outputFilePath;
  }

  private static int[] mSampleRates = new int[]{44100, 22050, 11025, 8000};

  public AudioRecord findAudioRecord() {
    for (int rate : mSampleRates) {
      for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT}) {
        for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
          try {
            Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
              + channelConfig);
            int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

            if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
              // check if we can instantiate and have a success
              AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

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

  private void init() {
    buffSize = AudioRecord
      .getMinBufferSize(frequncy, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    // http://stackoverflow.com/questions/4843739/audiorecord-object-not-initializing
    recorder = findAudioRecord();

    /*
    recorder = new AudioRecord(
      MediaRecorder.AudioSource.MIC,
      frequncy,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT,
      buffSize
    );
    */
  }

  public boolean isRecording() {
    return isRecording;
  }

  public void startRecroding() {
    Log.d(TAG, "startRecroding...");
    isRecording = true;
    recorder.startRecording();

    // create thread for audio record
    audioRecordThread = new Thread(new Runnable() {
      public void run() {
        writeAudioDataToFile();
      }
    }, AudioThreadName);
    audioRecordThread.start();
  }

  public void stopRecroding() {
    Log.d(TAG, "stopRecroding...");
    isRecording = false;
    recorder.stop();
  }

  // TODO: Problem... audio record playback speed is increased!!!
  private void writeAudioDataToFile() {
    // Write the output audio in byte
    short sData[] = new short[buffSize / 2];

    FileOutputStream os = null;
    try {
      os = new FileOutputStream(outputFilePath);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    while (isRecording) {
      // gets the voice output from microphone to byte format

      // TODO this might be what i want to use...
      //recorder.read(ByteBuffer, int);
      recorder.read(sData, 0, buffSize / 2);

      Log.d(TAG, "Short: write to file: " + sData.toString());
      try {
        // // writes the data to file from buffer
        // // stores the voice buffer
        byte bData[] = short2byte(sData);
        os.write(bData, 0, buffSize);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    try {
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private byte[] short2byte(short[] sData) {
    int shortArrsize = sData.length;
    byte[] bytes = new byte[shortArrsize * 2];
    for (int i = 0; i < shortArrsize; i++) {
      bytes[i * 2] = (byte) (sData[i] & 0x00FF);
      bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
      sData[i] = 0;
    }
    return bytes;

  }


  int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we
  // use only 1024
  int BytesPerElement = 2; // 2 bytes in 16bit format

  private void writeAudioDataToFile2() {
    // Write the output audio in byte

    String filePath = "/sdcard/voice8K16bitmono.pcm";
    short sData[] = new short[BufferElements2Rec];

    FileOutputStream os = null;
    try {
      os = new FileOutputStream(filePath);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    while (isRecording) {
      // gets the voice output from microphone to byte format

      recorder.read(sData, 0, BufferElements2Rec);
      Log.d(TAG, "Short wirting to file" + sData.toString());
      try {
        // // writes the data to file from buffer
        // // stores the voice buffer

        byte bData[] = short2byte(sData);

        os.write(bData, 0, BufferElements2Rec * BytesPerElement);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}