package com.mobiusbobs.videoprocessing.core.codec;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;

/**
 * android
 * <p>
 * Created by wangalbert on 3/30/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class AudioEncoderCore extends EncoderCore {
  private static final String TAG = "AudioEncoderCore";

  public static final String MIME_TYPE = "audio/mp4a-latm";
  public static final int BIT_RATE = 64000;

  // Constructor
  public AudioEncoderCore(MuxerWrapper muxerWrapper, AudioRecord audioRecord) throws IOException {
    super(muxerWrapper);

    // get sampleRate and channelCount from AudioRecord
    int channelCount = audioRecord.getChannelCount();
    int sampleRate = audioRecord.getSampleRate();
    Log.d(TAG, "AudioEncoderCore: channel count=" + channelCount + ", sampleRate=" + sampleRate);

    // create mediaFormat for audio
    final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, sampleRate, channelCount);
    audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
    audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);

    // create mediaCodec
    encoder = MediaCodec.createEncoderByType(MIME_TYPE);
    encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    encoder.start();
  }

  public void drainEncoder() {
    drainEncoder(false, false);
  }

}
