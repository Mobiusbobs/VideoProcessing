package com.mobiusbobs.videoprocessing.core;

import android.content.Context;
import android.graphics.Point;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.mobiusbobs.videoprocessing.core.Surface.InputSurface;
import com.mobiusbobs.videoprocessing.core.Surface.OutputSurface;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * android
 * <p>
 *   Test the different usage of MediaCodec(encoder/decoder)/MediaExtractor/MediaMuxer
 *   This is based on example from
 *   https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
 * </p>
 * Created by wangalbert on 3/21/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class VideoProcessor {
  public static final String TAG = "CodecManager";

  /** How long to wait for the next buffer to become available. */
  private static final int TIMEOUT_USEC = 10000;

  // parameters for the video encoder
  private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
  private static final int OUTPUT_VIDEO_BIT_RATE = 6000000; // 2Mbps
  private static final int OUTPUT_VIDEO_FRAME_RATE = 30;    // 15fps
  private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10; // 10 seconds between I-frames    // TODO what is this for
  private static final int OUTPUT_VIDEO_COLOR_FORMAT =
    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
  private static final int OUTPUT_VIDEO_WIDTH = 480;
  private static final int OUTPUT_VIDEO_HEIGHT = 720;
  private static final int OUTPUT_VIDEO_MAX_INPUT_SIZE = 0;

  // parameters for the audio encoder
  private static final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm"; // Advanced Audio Coding
  private static final int OUTPUT_AUDIO_CHANNEL_COUNT = 2; // Must match the input stream.
  private static final int OUTPUT_AUDIO_BIT_RATE = 128 * 1024;
  private static final int OUTPUT_AUDIO_AAC_PROFILE =
    MediaCodecInfo.CodecProfileLevel.AACObjectLC;   //AACObjectHE
  private static final int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 44100; // Must match the input stream.
  private static final int OUTPUT_AUDIO_MAX_INPUT_SIZE = 0;

  private Context context;

  private StickerDrawer stickerDrawer;
  private StickerDrawer logoDrawer;

  private int screenWidth;
  private int screenHeight;

  public interface OnMuxerDone {
    void onDone();
  }

  private OnMuxerDone onMuxerDone;

  private boolean withWatermark;

  // Constructor
  public VideoProcessor(Context context, boolean withWatermark) {
    this.context = context;
    this.withWatermark = withWatermark;

    // get screen size...
    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    screenWidth = size.x;
    screenHeight = size.y;
    Log.d(TAG, "screen size width=" + screenWidth + ", height=" + screenHeight);
  }

  public void setOnMuxerDone(OnMuxerDone onMuxerDone) {
    this.onMuxerDone = onMuxerDone;
  }

  private int getMediaDataOrDefault(MediaFormat inputVideoFormat, String key, int defaultValue) {
    if (inputVideoFormat.containsKey(key)) return inputVideoFormat.getInteger(key);
    else return defaultValue;
  }

  private MediaFormat createOutputVideoFormat(MediaFormat inputVideoFormat) {
    int videoWidth = getMediaDataOrDefault(inputVideoFormat, MediaFormat.KEY_WIDTH, OUTPUT_VIDEO_WIDTH); //inputVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
    int videoHeight = getMediaDataOrDefault(inputVideoFormat, MediaFormat.KEY_HEIGHT, OUTPUT_VIDEO_HEIGHT); //inputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
    int videoFrameRate = getMediaDataOrDefault(inputVideoFormat, MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE); //inputVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
    int videoMaxInputSize = getMediaDataOrDefault(inputVideoFormat, MediaFormat.KEY_MAX_INPUT_SIZE, OUTPUT_VIDEO_MAX_INPUT_SIZE); //inputVideoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);

    Log.d(TAG, "createOutputVideoFormat: width = " + videoWidth);
    Log.d(TAG, "createOutputVideoFormat: height = " + videoHeight);
    // TODO this is a hot fix before the new video recorder takes place.
    int width = Math.min(videoWidth, videoHeight);
    int height = Math.max(videoWidth, videoHeight);

    MediaFormat outputVideoFormat =
      MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, width, height);
    outputVideoFormat.setInteger(
      MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
    outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
    outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);
    outputVideoFormat.setInteger(
      MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);
    outputVideoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, videoMaxInputSize);

    return outputVideoFormat;
  }

  private MediaFormat createOutputAudioFormat(MediaFormat inputAudioFormat) {
    int audioSamplingRate = getMediaDataOrDefault(inputAudioFormat, MediaFormat.KEY_SAMPLE_RATE, OUTPUT_AUDIO_SAMPLE_RATE_HZ); //inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    int audioChannelCount = getMediaDataOrDefault(inputAudioFormat, MediaFormat.KEY_CHANNEL_COUNT, OUTPUT_AUDIO_CHANNEL_COUNT); //inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
    int audioMaxInputSize = getMediaDataOrDefault(inputAudioFormat, MediaFormat.KEY_MAX_INPUT_SIZE, OUTPUT_AUDIO_MAX_INPUT_SIZE); //inputAudioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);

    MediaFormat outputAudioFormat =
      MediaFormat.createAudioFormat(OUTPUT_AUDIO_MIME_TYPE, audioSamplingRate, audioChannelCount);
    outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);
    outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);
    outputAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioMaxInputSize);

    return outputAudioFormat;
  }

  public void extractDecodeEditEncodeMux(String outputPath, String inputFileUrl, String stickerUrl, int watermarkId) throws Exception {
    MediaExtractor videoExtractor;
    MediaExtractor audioExtractor;

    MediaCodec videoDecoder;
    MediaCodec audioDecoder;
    MediaCodec videoEncoder;
    MediaCodec audioEncoder;
    MediaMuxer muxer;

    OutputSurface outputSurface;  // output for decoder
    InputSurface inputSurface;    // input for encoder

    MediaCodecInfo videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE);  // for video encoder
    MediaCodecInfo audioCodecInfo = selectCodec(OUTPUT_AUDIO_MIME_TYPE);  // for audio encoder

    // --- extractor ---
    //videoExtractor = Extractor.createExtractor(context, inputRawFileId);
    videoExtractor = Extractor.createExtractor(inputFileUrl);
    int videoTrackIndex = Extractor.getAndSelectVideoTrackIndex(videoExtractor);

    //audioExtractor = Extractor.createExtractor(context, inputRawFileId);
    audioExtractor = Extractor.createExtractor(inputFileUrl);
    int audioTrackIndex = Extractor.getAndSelectAudioTrackIndex(audioExtractor);

    MediaFormat inputVideoFormat = videoExtractor.getTrackFormat(videoTrackIndex);
    // fix config problem: http://stackoverflow.com/questions/15105843/mediacodec-jelly-bean
    inputVideoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
    MediaFormat inputAudioFormat = audioExtractor.getTrackFormat(audioTrackIndex);

    // ----- mediacodec -----
    // --- video encoder ---
    // Create a MediaCodec for the desired codec, then configure it as an encoder with
    // our desired properties. Request a Surface to use for input.
    MediaFormat outputVideoFormat = createOutputVideoFormat(inputVideoFormat);
    AtomicReference<Surface> inputSurfaceReference = new AtomicReference<Surface>();
    videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
    inputSurface = new InputSurface(inputSurfaceReference.get());
    inputSurface.makeCurrent();

    // --- video decoder ---
    outputSurface = new OutputSurface();
    videoDecoder = createVideoDecoder(inputVideoFormat, outputSurface.getSurface());

    // --- audio encoder / decoder ---
    // Create a MediaCodec for the desired codec, then configure it as an encoder with
    // our desired properties. Request a Surface to use for input.
    MediaFormat outputAudioFormat = createOutputAudioFormat(inputAudioFormat);
    audioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat);

    // Create a MediaCodec for the decoder, based on the extractor's format.
    audioDecoder = createAudioDecoder(inputAudioFormat);

    // --- muxer ---
    muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

    // --- setup watermark ---
    VerticesHelper verticesHelper = new VerticesHelper(context, screenWidth, screenHeight);
    logoDrawer = new StickerDrawer(context, watermarkId, verticesHelper.getWatermarkVertices(watermarkId, 30));

    // --- setup sticker ----
    if(stickerUrl!= null)
      stickerDrawer = new StickerDrawer(context, stickerUrl, verticesHelper.getAlignCenterVertices(stickerUrl));

    // --- do the actual extract decode edit encode mux ---
    doExtractDecodeEncodeMux(
      videoExtractor,
      audioExtractor,
      videoDecoder,
      videoEncoder,
      audioDecoder,
      audioEncoder,
      muxer,
      inputSurface,
      outputSurface);

  }

  private void doExtractDecodeEncodeMux(
    MediaExtractor videoExtractor,
    MediaExtractor audioExtractor,
    MediaCodec videoDecoder,
    MediaCodec videoEncoder,
    MediaCodec audioDecoder,
    MediaCodec audioEncoder,
    MediaMuxer muxer,
    InputSurface inputSurface,
    OutputSurface outputSurface
  ) {
    // video buffer
    ByteBuffer[] videoDecoderInputBuffers = videoDecoder.getInputBuffers();
    ByteBuffer[] videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();;
    ByteBuffer[] videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();;
    MediaCodec.BufferInfo videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
    MediaCodec.BufferInfo videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();

    // audio buffer
    ByteBuffer[] audioDecoderInputBuffers = audioDecoder.getInputBuffers();
    ByteBuffer[] audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
    ByteBuffer[] audioEncoderInputBuffers = audioEncoder.getInputBuffers();
    ByteBuffer[] audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
    MediaCodec.BufferInfo audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
    MediaCodec.BufferInfo audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();

    // We will get these from the decoders when notified of a format change.
    MediaFormat decoderOutputVideoFormat = null;
    MediaFormat decoderOutputAudioFormat = null;

    // We will get these from the encoders when notified of a format change.
    MediaFormat encoderOutputVideoFormat = null;
    MediaFormat encoderOutputAudioFormat = null;

    // We will determine these once we have the output format.
    int outputVideoTrack = -1;
    int outputAudioTrack = -1;

    // Whether things are done on the video side.
    boolean videoExtractorDone = false;
    boolean videoDecoderDone = false;
    boolean videoEncoderDone = false;

    // Whether things are done on the audio side.
    boolean audioExtractorDone = false;
    boolean audioDecoderDone = false;
    boolean audioEncoderDone = false;
    // The audio decoder output buffer to process, -1 if none.
    int pendingAudioDecoderOutputBufferIndex = -1;

    boolean muxing = false;

    int videoExtractedFrameCount = 0;
    int videoDecodedFrameCount = 0;
    int videoEncodedFrameCount = 0;
    int audioExtractedFrameCount = 0;
    int audioDecodedFrameCount = 0;
    int audioEncodedFrameCount = 0;

    while(!videoEncoderDone || !audioEncoderDone) {

      // --- extract video from extractor ---
      while(!videoExtractorDone && (encoderOutputVideoFormat == null || muxing)) {
        // 1.) get the index of next buffer to be filled
        int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (decoderInputBufferIndex < 0) {
          Log.e(TAG, "videoDecoder.dequeueInputBuffer: no video decoder input buffer");
          break;
        }
        // 2.) get the byte buffer from index
        ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
        // 3.) extract the data and store it in the buffer
        int size = videoExtractor.readSampleData(decoderInputBuffer, 0);
        long presentationTime = videoExtractor.getSampleTime();
        // 4.) queue the buffer to codec to process
        if (size >= 0) {
          videoDecoder.queueInputBuffer(
            decoderInputBufferIndex,
            0,
            size,
            presentationTime,
            videoExtractor.getSampleFlags());
        }
        videoExtractorDone = !videoExtractor.advance();
        if (videoExtractorDone) {
          Log.d(TAG, "FLAG: videoExtractorDone!!!");
          videoDecoder.queueInputBuffer(
            decoderInputBufferIndex,
            0,
            0,
            0,
            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }
        videoExtractedFrameCount++;
        break;
      }

      // --- extract audio from extractor ---
      while(!audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
        // 1.) get the index of next buffer to be filled
        int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
          Log.e(TAG, "audioDecoder.dequeueInputBuffer: no audio decoder input buffer");
          break;
        }
        // 2.) get the byte buffer from index
        ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
        // 3.) extract the data and store it in the buffer
        int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
        long presentationTime = audioExtractor.getSampleTime();
        // 4.) queue the buffer to codec to process
        if (size >= 0) {
          audioDecoder.queueInputBuffer(
            decoderInputBufferIndex,
            0,
            size,
            presentationTime,
            audioExtractor.getSampleFlags());
        }
        audioExtractorDone = !audioExtractor.advance();
        if (audioExtractorDone) {
          Log.d(TAG, "FLAG: audioExtractorDone!!!");
          audioDecoder.queueInputBuffer(
            decoderInputBufferIndex,
            0,
            0,
            0,
            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }
        audioExtractedFrameCount++;
        break;
      }

      // pull output frames from video decoder and feed it to encoder
      while(!videoDecoderDone && (encoderOutputVideoFormat == null || muxing)) {
        int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(
          videoDecoderOutputBufferInfo, TIMEOUT_USEC);
        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
          Log.d(TAG, "no video decoder output buffer");
          break;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
          Log.e(TAG, "video decoder: output buffers changed");
          videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
          break;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          decoderOutputVideoFormat = videoDecoder.getOutputFormat();
          Log.d(TAG, "video decoder: output format changed: " + decoderOutputVideoFormat);
          break;
        }

        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
          Log.e(TAG, "video decoder: codec config buffer");
          videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
          break;
        }

        boolean render = videoDecoderOutputBufferInfo.size != 0;
        // send the output buffer to the render surface first
        videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, render);

        if (render) {
          outputSurface.awaitNewImage();
          // Edit the frame and send it to the encoder.

          outputSurface.drawImage();
          if (stickerDrawer!=null)
            stickerDrawer.drawSticker();
          if (withWatermark)
            logoDrawer.drawSticker();
          //stickerDrawer.drawBox(videoDecodedFrameCount);

          inputSurface.setPresentationTime(videoDecoderOutputBufferInfo.presentationTimeUs * 1000);
          Log.d(TAG, "input surface: swap buffers");

          inputSurface.swapBuffers();
          Log.d(TAG, "video encoder: notified of new frame");
        }
        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          Log.d(TAG, "FLAG: videoDecoderDone!!! video decoder: EOS");
          videoDecoderDone = true;
          videoEncoder.signalEndOfInputStream();
        }
        videoDecodedFrameCount++;
        break;
      }

      // poll output frames from audio decoder
      while (!audioDecoderDone && pendingAudioDecoderOutputBufferIndex == -1) {
        int decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(
          audioDecoderOutputBufferInfo, TIMEOUT_USEC);

        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
          Log.d(TAG, "no audio decoder output buffer");
          break;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
          Log.d(TAG, "audio decoder: output buffers changed");
          audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
          break;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          decoderOutputAudioFormat = audioDecoder.getOutputFormat();
          Log.d(TAG, "audio decoder: output format changed: " + decoderOutputAudioFormat);
          break;
        }

        if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
          Log.d(TAG, "audio decoder: codec config buffer");
          audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
          break;
        }

        pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex;
        audioDecodedFrameCount++;
        // We extracted a pending frame, let's try something else next.
        break;
      }

      // Feed the pending decoded audio buffer to the audio encoder.
      // TODO check to see if I can implement this into audio Decoder...
      while (pendingAudioDecoderOutputBufferIndex != -1) {
        int encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (encoderInputBufferIndex < 0) {
          Log.e(TAG, "audioEncoder.dequeueInputBuffer: no audio encoder input buffer");
          break;
        }

        ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
        int size = audioDecoderOutputBufferInfo.size;
        long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs;

        if (size >= 0) {
          ByteBuffer decoderOutputBuffer =
            audioDecoderOutputBuffers[pendingAudioDecoderOutputBufferIndex].duplicate();
          decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset);
          decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size);
          encoderInputBuffer.position(0);
          encoderInputBuffer.put(decoderOutputBuffer);
          audioEncoder.queueInputBuffer(
            encoderInputBufferIndex,
            0,
            size,
            presentationTime,
            audioDecoderOutputBufferInfo.flags);
        }
        audioDecoder.releaseOutputBuffer(pendingAudioDecoderOutputBufferIndex, false);
        pendingAudioDecoderOutputBufferIndex = -1;
        if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          Log.d(TAG, "FLAG: audioDecoderDone!!! audio decoder: EOS");
          audioDecoderDone = true;
        }
        break;
      }

      // Poll frames from the video encoder and send them to the muxer.
      while (!videoEncoderDone && (encoderOutputVideoFormat == null || muxing)) {
        int encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(
          videoEncoderOutputBufferInfo, TIMEOUT_USEC);
        if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
          Log.d(TAG, "no video encoder output buffer");
          break;
        }
        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
          Log.d(TAG, "video encoder: output buffers changed");
          videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
          break;
        }
        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          Log.e(TAG, "video encoder: output format changed");
          if (outputVideoTrack >= 0) {
            Log.e(TAG, "SHOULD NOT ENTER HERE!");
          }
          encoderOutputVideoFormat = videoEncoder.getOutputFormat();
          break;
        }

        ByteBuffer encoderOutputBuffer =
          videoEncoderOutputBuffers[encoderOutputBufferIndex];
        if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
          Log.d(TAG, "video encoder: codec config buffer");
          // Simply ignore codec config buffers.
          videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
          break;
        }

        if (videoEncoderOutputBufferInfo.size != 0) {
          muxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
        }
        if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          Log.d(TAG, "FLAG: videoEncoderDone!!! video encoder: EOS");
          videoEncoderDone = true;
        }
        videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
        videoEncodedFrameCount++;
        break;
      }

      // Poll frames from the audio encoder and send them to the muxer.
      while (!audioEncoderDone && (encoderOutputAudioFormat == null || muxing)) {
        int encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(
          audioEncoderOutputBufferInfo, TIMEOUT_USEC);

        if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
          Log.d(TAG, "no audio encoder output buffer");
          break;
        }
        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
          Log.d(TAG, "audio encoder: output buffers changed");
          audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
          break;
        }
        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          Log.d(TAG, "audio encoder: output format changed");
          if (outputAudioTrack >= 0) {
            Log.e(TAG, "SHOULD NOT ENTER HERE! audio encoder changed its output format again?");
          }
          encoderOutputAudioFormat = audioEncoder.getOutputFormat();
          break;
        }

        ByteBuffer encoderOutputBuffer = audioEncoderOutputBuffers[encoderOutputBufferIndex];
        if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
          Log.d(TAG, "audio encoder: codec config buffer");
          // Simply ignore codec config buffers.
          audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
          break;
        }

        if (audioEncoderOutputBufferInfo.size != 0) {
          Log.d(TAG, "write Audio sample to muxer");
          muxer.writeSampleData(outputAudioTrack, encoderOutputBuffer, audioEncoderOutputBufferInfo);
        }
        if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          Log.d(TAG, "FLAG: audioEncoderDone!!! audio encoder: EOS");
          audioEncoderDone = true;
        }
        audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
        audioEncodedFrameCount++;
        break;
      }

      if (!muxing &&
          (encoderOutputAudioFormat != null) &&
          (encoderOutputVideoFormat != null)
      ) {

        Log.d(TAG, "muxer: adding video track.");
        outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);

        Log.d(TAG, "muxer: adding audio track.");
        outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat);

        Log.d(TAG, "muxer: starting");
        muxer.start();
        muxing = true;
      }
    }

    muxer.stop();
    muxer.release();
    onMuxerDone.onDone();

    Log.d(TAG, "doExtractDecodeEncodeMux Done: videoExtractedFrameCount = " + videoExtractedFrameCount);
    Log.d(TAG, "doExtractDecodeEncodeMux Done: videoDecodedFrameCount = " + videoDecodedFrameCount);
    Log.d(TAG, "doExtractDecodeEncodeMux Done: videoEncodedFrameCount = " + videoEncodedFrameCount);
    Log.d(TAG, "doExtractDecodeEncodeMux Done: audioExtractedFrameCount = " + audioExtractedFrameCount);
    Log.d(TAG, "doExtractDecodeEncodeMux Done: audioDecodedFrameCount = " + audioDecodedFrameCount);
    Log.d(TAG, "doExtractDecodeEncodeMux Done: audioEncodedFrameCount = " + audioEncodedFrameCount);
  }

  /**
   * Returns the first codec capable of encoding the specified MIME type, or null if no match was
   * found.
   */
  private static MediaCodecInfo selectCodec(String mimeType) {
    int numCodecs = MediaCodecList.getCodecCount();
    for (int i = 0; i < numCodecs; i++) {
      MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
      if (!codecInfo.isEncoder()) {
        continue;
      }
      String[] types = codecInfo.getSupportedTypes();
      for (int j = 0; j < types.length; j++) {
        if (types[j].equalsIgnoreCase(mimeType)) {
          return codecInfo;
        }
      }
    }
    return null;
  }

  /**
   * Creates a decoder for the given format, which outputs to the given surface.
   *
   * @param inputFormat the format of the stream to decode
   * @param surface into which to decode the frames
   */
  private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws Exception {
    MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
    decoder.configure(inputFormat, surface, null, 0);
    decoder.start();
    return decoder;
  }

  /**
   * Creates an encoder for the given format using the specified codec, taking input from a
   * surface.
   *
   * <p>The surface to use as input is stored in the given reference.
   *
   * @param codecInfo of the codec to use
   * @param format of the stream to be produced
   * @param surfaceReference to store the surface to use as input
   */
  private MediaCodec createVideoEncoder(
      MediaCodecInfo codecInfo,
      MediaFormat format,
      AtomicReference<Surface> surfaceReference
  ) throws Exception {
    MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    // Must be called before start() is.
    surfaceReference.set(encoder.createInputSurface());
    encoder.start();
    return encoder;
  }

  /**
   * Creates a decoder for the given format.
   *
   * @param inputFormat the format of the stream to decode
   */
  private MediaCodec createAudioDecoder(MediaFormat inputFormat) throws Exception {
    MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
    decoder.configure(inputFormat, null, null, 0);
    decoder.start();
    return decoder;
  }

  /**
   * Creates an encoder for the given format using the specified codec.
   *
   * @param codecInfo of the codec to use
   * @param format of the stream to be produced
   */
  private MediaCodec createAudioEncoder(MediaCodecInfo codecInfo, MediaFormat format)
    throws Exception
  {
    MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    encoder.start();
    return encoder;
  }

  public static String getMimeTypeFor(MediaFormat format) {
    return format.getString(MediaFormat.KEY_MIME);
  }

  /** Wraps testExtractDecodeEditEncodeMux() */
  public static class ExtractDecodeEditEncodeMuxWrapper implements Runnable {
    private VideoProcessor videoProcessor;
    private String outputPath;
    private String inputFilePath;
    private String stickerFilePath;
      private int watermarkId;

    private ExtractDecodeEditEncodeMuxWrapper(
      VideoProcessor videoProcessor,
      String outputPath,
      String inputFilePath,
      String stickerFilePath,
      int watermarkId)
    {
      this.videoProcessor = videoProcessor;
      this.outputPath = outputPath;
      this.inputFilePath = inputFilePath;
      this.stickerFilePath = stickerFilePath;
        this.watermarkId = watermarkId;
    }

    @Override
    public void run() {
      try {
        videoProcessor.extractDecodeEditEncodeMux(outputPath, inputFilePath, stickerFilePath, watermarkId);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * Entry point.
     */
    public static void run(
      VideoProcessor videoProcessor,
      String outputPath,
      String inputFilePath,
      String stickerFilePath,
      int watermarkId) throws Throwable
    {
      ExtractDecodeEditEncodeMuxWrapper wrapper =
        new ExtractDecodeEditEncodeMuxWrapper(videoProcessor, outputPath, inputFilePath, stickerFilePath, watermarkId);
      Thread thread = new Thread(wrapper, "codec test");
      thread.start();
    }
  }
}
