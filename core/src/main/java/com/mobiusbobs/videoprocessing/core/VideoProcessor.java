package com.mobiusbobs.videoprocessing.core;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.surface.InputSurface;
import com.mobiusbobs.videoprocessing.core.surface.OutputSurface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * VideoProcessor
 * <p>
 *     Test the different usage of MediaCodec(encoder/decoder)/MediaExtractor/MediaMuxer
 *     This is based on example from
 *     https://android.googlesource.com/platform/cts/
 *     +/jb-mr2-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
 * </p>
 * Created by wangalbert on 3/21/16.
 * Refactored by rayshih on 4/8/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
@SuppressWarnings("deprecation")
public class VideoProcessor {
    public static final String TAG = "VideoProcessor";

    /** How long to wait for the next buffer to become available. */
    private static final int TIMEOUT_USEC = 10000;

    // ----- input & output -----
    private MediaExtractor videoExtractor;
    private MediaExtractor audioExtractor;

    public String outputPath;

    // ----- format parameters -----
    // parameters for the video encoder
    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int OUTPUT_VIDEO_BIT_RATE = 6000000; // 2 Mbps
    private static final int OUTPUT_VIDEO_FRAME_RATE = 30;        // 15fps
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10; // 10 seconds between I-frames

    private static final int OUTPUT_VIDEO_COLOR_FORMAT =
      MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
    private static final int OUTPUT_VIDEO_WIDTH = 720;
    private static final int OUTPUT_VIDEO_HEIGHT = 1280;
    private static final int OUTPUT_VIDEO_MAX_INPUT_SIZE = 0;

    // parameters for the audio encoder
    private static final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm"; // Advanced Audio Coding
    private static final int OUTPUT_AUDIO_CHANNEL_COUNT = 1; // Must match the input stream.
    private static final int OUTPUT_AUDIO_BIT_RATE = 128 * 1024;
    private static final int OUTPUT_AUDIO_AAC_PROFILE =
      MediaCodecInfo.CodecProfileLevel.AACObjectLC;     // AACObjectHE
    private static final int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 44100; // Must match the input stream.
    private static final int OUTPUT_AUDIO_MAX_INPUT_SIZE = 4096 * 2;

    // ----- other parameters -----

    // drawers
    private List<GLDrawable> drawerList;

    // ----- process states -----
    private int videoExtractedFrameCount = 0;
    private int videoDecodedFrameCount = 0;
    private int videoEncodedFrameCount = 0;
    private int audioExtractedFrameCount = 0;
    private int audioDecodedFrameCount = 0;
    private int audioEncodedFrameCount = 0;

    // We will get these from the decoders when notified of a format change.
    // decoder
    MediaFormat decoderOutputAudioFormat;

    // We will get these from the encoders when notified of a format change.
    // encoder
    MediaFormat encoderOutputVideoFormat = null;
    MediaFormat encoderOutputAudioFormat = null;

    // The audio decoder output buffer to process, -1 if none.
    int pendingAudioDecoderOutputBufferIndex = -1;

    // buffers
    // video buffer
    ByteBuffer[] videoDecoderInputBuffers;
    MediaCodec.BufferInfo videoDecoderOutputBufferInfo;

    ByteBuffer[] videoEncoderOutputBuffers;
    MediaCodec.BufferInfo videoEncoderOutputBufferInfo;

    // audio buffer
    ByteBuffer[] audioDecoderInputBuffers;
    MediaCodec.BufferInfo audioDecoderOutputBufferInfo;

    ByteBuffer[] audioDecoderOutputBuffers;

    ByteBuffer[] audioEncoderInputBuffers;
    MediaCodec.BufferInfo audioEncoderOutputBufferInfo;

    ByteBuffer[] audioEncoderOutputBuffers;

    // track data
    // We will determine these once we have the output format.
    int outputVideoTrack = -1;
    int outputAudioTrack = -1;

    // Constructor
    private VideoProcessor() {}

    public void process() throws Exception {

        MediaCodec videoDecoder;
        MediaCodec audioDecoder;
        MediaCodec videoEncoder;
        MediaCodec audioEncoder;
        MediaMuxer muxer;

        OutputSurface outputSurface;    // output for decoder
        InputSurface inputSurface;        // input for encoder

        MediaCodecInfo videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE);    // for video encoder
        MediaCodecInfo audioCodecInfo = selectCodec(OUTPUT_AUDIO_MIME_TYPE);    // for audio encoder

        // --- setup extractors ---
        int videoTrackIndex = Extractor.getAndSelectVideoTrackIndex(videoExtractor);
        int audioTrackIndex = Extractor.getAndSelectAudioTrackIndex(audioExtractor);

        MediaFormat inputVideoFormat = videoExtractor.getTrackFormat(videoTrackIndex);
        long videoDuration = inputVideoFormat.getLong(MediaFormat.KEY_DURATION);
        // fix config problem: http://stackoverflow.com/questions/15105843/mediacodec-jelly-bean
        inputVideoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);

        MediaFormat inputAudioFormat = audioExtractor.getTrackFormat(audioTrackIndex);
        long audioDuration = inputAudioFormat.getLong(MediaFormat.KEY_DURATION);

        // ----- mediacodec -----
        // --- video encoder ---
        // Create a MediaCodec for the desired codec, then configure it as an encoder with
        // our desired properties. Request a Surface to use for input.
        MediaFormat outputVideoFormat = createOutputVideoFormat(inputVideoFormat);
        AtomicReference<Surface> inputSurfaceReference = new AtomicReference<>();
        videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
        inputSurface = new InputSurface(inputSurfaceReference.get());
        inputSurface.makeCurrent();

        // --- video decoder ---
        outputSurface = new OutputSurface();
        videoDecoder = createVideoDecoder(inputVideoFormat, outputSurface.getSurface());

        // setup drawers
        for (GLDrawable drawer : drawerList) {
            drawer.init();
        }

        // --- audio encoder / decoder ---
        // Create a MediaCodec for the desired codec, then configure it as an encoder with
        // our desired properties. Request a Surface to use for input.
        MediaFormat outputAudioFormat = createOutputAudioFormat(inputAudioFormat);
        audioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat);

        // Create a MediaCodec for the decoder, based on the extractor's format.
        audioDecoder = createAudioDecoder(inputAudioFormat);

        // --- muxer ---
        muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        // --- do the actual extract decode edit encode mux ---
        doProcess(
            videoExtractor,
            audioExtractor,
            videoDecoder,
            videoEncoder,
            audioDecoder,
            audioEncoder,
            muxer,
            inputSurface,
            outputSurface,
            videoDuration,
            audioDuration);

    }

    private void doProcess(
        MediaExtractor videoExtractor,
        MediaExtractor audioExtractor,
        MediaCodec videoDecoder,
        MediaCodec videoEncoder,
        MediaCodec audioDecoder,
        MediaCodec audioEncoder,
        MediaMuxer muxer,
        InputSurface inputSurface,
        OutputSurface outputSurface,
        long videoDuration,
        long audioDuration
    ) {
        prepareBuffers(
                videoDecoder, audioDecoder,
                videoEncoder, audioEncoder
        );

        // Whether things are done on the video side.
        boolean videoExtractorDone = false;
        boolean videoDecoderDone = false;
        boolean videoEncoderDone = false;

        // Whether things are done on the audio side.
        boolean audioExtractorDone = false;
        boolean audioDecoderDone = false;
        boolean audioEncoderDone = false;

        boolean muxing = false;

        long audioPTimeOffset = 0;

        while (!videoEncoderDone || !audioEncoderDone) {
            // --- extract video from extractor ---
            if (!videoExtractorDone && (encoderOutputVideoFormat == null || muxing)) {
                videoExtractorDone = extractVideoData(
                        videoExtractor,
                        videoDecoder,
                        videoDecoderInputBuffers
                );
            }

            // --- extract audio from extractor ---
            if (!audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
                audioExtractorDone = extractAudioData(
                        audioExtractor,
                        audioDecoder,
                        audioDecoderInputBuffers,
                        videoDuration
                );

                long sampleTime = audioExtractor.getSampleTime();
                if (sampleTime == -1) {
                    videoDuration -= audioDuration;
                    audioPTimeOffset += audioDuration;
                    audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    audioDecoder.flush();
                }
            }

            // --- pull output frames from video decoder and feed it to encoder ---
            if (!videoDecoderDone && (encoderOutputVideoFormat == null || muxing)) {

                boolean frameAvailable = checkVideoDecodeState(videoDecoder, videoDecoderOutputBufferInfo);

                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "FLAG: videoDecoderDone!!! video decoder: EOS");
                    videoDecoderDone = true;
                }

                if (frameAvailable) {
                    videoDecodedFrameCount++;
                    render(outputSurface, videoDecoderOutputBufferInfo, inputSurface);
                }

                if (videoDecoderDone) {
                    videoEncoder.signalEndOfInputStream();
                }
            }

            // --- poll output frames from audio decoder ----
            if (!audioDecoderDone && pendingAudioDecoderOutputBufferIndex == -1) {
                updateAudioDecodeState(audioDecoder, audioDecoderOutputBufferInfo);
            }

            // Feed the pending decoded audio buffer to the audio encoder.
            // TODO check to see if I can implement this into audio Decoder...
            if (pendingAudioDecoderOutputBufferIndex != -1) {
                audioDecoderDone = pipeAudioStream(audioDecoder, audioEncoder, audioPTimeOffset);
            }

            // --- mux video ---
            if (!videoEncoderDone && (encoderOutputVideoFormat == null || muxing)) {
                videoEncoderDone = muxVideo(videoEncoder, muxer);
            }

            // --- mux audio ---
            if (!audioEncoderDone && (encoderOutputAudioFormat == null || muxing)) {
                audioEncoderDone = muxAudio(audioEncoder, muxer);
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

        Log.d(TAG, "doExtractDecodeEncodeMux Done: videoExtractedFrameCount = " + videoExtractedFrameCount);
        Log.d(TAG, "doExtractDecodeEncodeMux Done: videoDecodedFrameCount = " + videoDecodedFrameCount);
        Log.d(TAG, "doExtractDecodeEncodeMux Done: videoEncodedFrameCount = " + videoEncodedFrameCount);
        Log.d(TAG, "doExtractDecodeEncodeMux Done: audioExtractedFrameCount = " + audioExtractedFrameCount);
        Log.d(TAG, "doExtractDecodeEncodeMux Done: audioDecodedFrameCount = " + audioDecodedFrameCount);
        Log.d(TAG, "doExtractDecodeEncodeMux Done: audioEncodedFrameCount = " + audioEncodedFrameCount);
    }

    private void prepareBuffers(
            MediaCodec videoDecoder,
            MediaCodec audioDecoder,
            MediaCodec videoEncoder,
            MediaCodec audioEncoder
    ) {
        // video buffer
        videoDecoderInputBuffers = videoDecoder.getInputBuffers();
        videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
        videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
        videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();

        // audio buffer
        audioDecoderInputBuffers = audioDecoder.getInputBuffers();
        audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
        audioEncoderInputBuffers = audioEncoder.getInputBuffers();
        audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
        audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
        audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
    }

    private boolean extractVideoData(
            MediaExtractor videoExtractor,
            MediaCodec videoDecoder,
            ByteBuffer[] videoDecoderInputBuffers
    ) {
        // 1.) get the index of next buffer to be filled
        int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (decoderInputBufferIndex < 0) {
            Log.e(TAG, "videoDecoder.dequeueInputBuffer: no video decoder input buffer");
            return false;
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
        boolean videoExtractorDone = !videoExtractor.advance();
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
        return videoExtractorDone;
    }

    private boolean extractAudioData(
            MediaExtractor audioExtractor,
            MediaCodec audioDecoder,
            ByteBuffer[] audioDecoderInputBuffers,
            Long maxTime
    ) {
        // 1.) get the index of next buffer to be filled
        int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.e(TAG, "audioDecoder.dequeueInputBuffer: no audio decoder input buffer");
            return false;
        }

        // 1.5) check if audio sampleTime exceed duration
        long sampleTime = audioExtractor.getSampleTime();
        if (sampleTime > maxTime) {
            Log.d(TAG, "FLAG: reach videoDuration: duration=" + maxTime + ", sampleTime=" + sampleTime);
            audioDecoder.queueInputBuffer(
              decoderInputBufferIndex,
              0,
              0,
              0,
              MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return true;
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

        boolean audioExtractorDone = !audioExtractor.advance();
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
        return audioExtractorDone;
    }

    /**
     * Check video decode state
     *
     * We don't really care about the output buffer and format,
     * since we bind the decoder to surface directly
     *
     * @return frameAvailable
     */
    private boolean checkVideoDecodeState(
            MediaCodec videoDecoder,
            MediaCodec.BufferInfo videoDecoderOutputBufferInfo
    ) {
        int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(
                videoDecoderOutputBufferInfo, TIMEOUT_USEC);

        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.d(TAG, "no video decoder output buffer");
            return false;
        }

        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            Log.e(TAG, "video decoder: output buffers changed");
            // videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
            return false;
        }

        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat decoderOutputVideoFormat = videoDecoder.getOutputFormat();
            Log.d(TAG, "video decoder: output format changed: " + decoderOutputVideoFormat);
            return false;
        }

        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.e(TAG, "video decoder: codec config buffer");
            // TODO figure out why
            videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
            return false;
        }

        boolean frameAvailable = videoDecoderOutputBufferInfo.size != 0;
        // send the output buffer to the render surface first
        videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, frameAvailable);

        return frameAvailable;
    }


    // ----- video draw function -----
    private void render(
            // surface from decoder
            OutputSurface outputSurface,
            MediaCodec.BufferInfo videoDecoderOutputBufferInfo,

            // surface to encoder
            InputSurface inputSurface
    ) {
        // fetch frame
        outputSurface.awaitNewImage();
        outputSurface.drawImage();

        long timeMs = videoDecoderOutputBufferInfo.presentationTimeUs / 1000;
        for (GLDrawable drawer : drawerList) {
            drawer.draw(timeMs);
        }

        inputSurface.setPresentationTime(videoDecoderOutputBufferInfo.presentationTimeUs * 1000);

        Log.d(TAG, "input surface: swap buffers");
        inputSurface.swapBuffers();

        // TODO check this
        Log.d(TAG, "video encoder: notified of new frame");
    }

    // ----- audio handle function -----
    private void updateAudioDecodeState(
            MediaCodec audioDecoder,
            MediaCodec.BufferInfo audioDecoderOutputBufferInfo
    ) {
        int decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(
                audioDecoderOutputBufferInfo, TIMEOUT_USEC);

        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.d(TAG, "no audio decoder output buffer");
            return;
        }

        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            Log.d(TAG, "audio decoder: output buffers changed");
            audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
            return;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            decoderOutputAudioFormat = audioDecoder.getOutputFormat();
            Log.d(TAG, "audio decoder: output format changed: " + decoderOutputAudioFormat);
            return;
        }

        if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "audio decoder: codec config buffer");
            audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
            return;
        }

        pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex;
        audioDecodedFrameCount++;
        // We extracted a pending frame, let's try something else next.
    }

    /**
     *
     * @param audioDecoder The audio decoder from
     * @param audioEncoder The audio encoder to
     * @return audioDecoderDone
     */
    private boolean pipeAudioStream(MediaCodec audioDecoder, MediaCodec audioEncoder, long pTimeOffset) {
        int encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (encoderInputBufferIndex < 0) {
            Log.e(TAG, "audioEncoder.dequeueInputBuffer: no audio encoder input buffer");
            return false;
        }

        ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
        int size = audioDecoderOutputBufferInfo.size;
        long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs + pTimeOffset;

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
            return true;
        }

        return false;
    }

    // muxing methods
    /**
     * @param videoEncoder Where the video from
     * @return videoEncoderDone
     */
    private boolean muxVideo(MediaCodec videoEncoder, MediaMuxer muxer) {
        int encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(
                videoEncoderOutputBufferInfo, TIMEOUT_USEC);

        if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.d(TAG, "no video encoder output buffer");
            return false;
        }
        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            Log.d(TAG, "video encoder: output buffers changed");
            videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
            return false;
        }
        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            Log.e(TAG, "video encoder: output format changed");
            if (outputVideoTrack >= 0) {
                Log.e(TAG, "SHOULD NOT ENTER HERE!");
            }
            encoderOutputVideoFormat = videoEncoder.getOutputFormat();
            return false;
        }

        ByteBuffer encoderOutputBuffer =
                videoEncoderOutputBuffers[encoderOutputBufferIndex];
        if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "video encoder: codec config buffer");
            // Simply ignore codec config buffers.
            videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
            return false;
        }

        if (videoEncoderOutputBufferInfo.size != 0) {
            muxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
        }
        if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d(TAG, "FLAG: videoEncoderDone!!! video encoder: EOS");
            return true;
        }
        videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
        videoEncodedFrameCount++;
        return false;
    }

    /**
     * @param audioEncoder Where the audio from
     * @return audioEncoderDone
     */
    private boolean muxAudio(MediaCodec audioEncoder, MediaMuxer muxer) {
        int encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(
                audioEncoderOutputBufferInfo, TIMEOUT_USEC);

        if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.d(TAG, "no audio encoder output buffer");
            return false;
        }
        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            Log.d(TAG, "audio encoder: output buffers changed");
            audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
            return false;
        }
        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            Log.d(TAG, "audio encoder: output format changed");
            if (outputAudioTrack >= 0) {
                Log.e(TAG, "SHOULD NOT ENTER HERE! audio encoder changed its output format again?");
            }
            encoderOutputAudioFormat = audioEncoder.getOutputFormat();
            return false;
        }

        ByteBuffer encoderOutputBuffer = audioEncoderOutputBuffers[encoderOutputBufferIndex];
        if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "audio encoder: codec config buffer");
            // Simply ignore codec config buffers.
            audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
            return false;
        }

        if (audioEncoderOutputBufferInfo.size != 0) {
            Log.d(TAG, "write Audio sample to muxer");
            muxer.writeSampleData(outputAudioTrack, encoderOutputBuffer, audioEncoderOutputBufferInfo);
        }
        if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d(TAG, "FLAG: audioEncoderDone!!! audio encoder: EOS");
            return true;
        }
        audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
        audioEncodedFrameCount++;
        return false;
    }

    // ----- codec creator -----
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

    // ----- format helper -----
    public static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    private MediaFormat createOutputAudioFormat(MediaFormat inputAudioFormat) {
        int audioSamplingRate = getMediaDataOrDefault(
                inputAudioFormat,
                MediaFormat.KEY_SAMPLE_RATE,
                OUTPUT_AUDIO_SAMPLE_RATE_HZ
        );
        int audioChannelCount = getMediaDataOrDefault(
                inputAudioFormat,
                MediaFormat.KEY_CHANNEL_COUNT,
                OUTPUT_AUDIO_CHANNEL_COUNT
        );
        int audioMaxInputSize = getMediaDataOrDefault(
                inputAudioFormat,
                MediaFormat.KEY_MAX_INPUT_SIZE,
                OUTPUT_AUDIO_MAX_INPUT_SIZE
        );

        MediaFormat outputAudioFormat = MediaFormat.createAudioFormat(
                OUTPUT_AUDIO_MIME_TYPE,
                audioSamplingRate,
                audioChannelCount
        );

        outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);
        outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);
        outputAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioMaxInputSize);

        return outputAudioFormat;
    }

    private MediaFormat createOutputVideoFormat(MediaFormat inputVideoFormat) {
        int videoWidth = getMediaDataOrDefault(
                inputVideoFormat,
                MediaFormat.KEY_WIDTH,
                OUTPUT_VIDEO_WIDTH
        );
        int videoHeight = getMediaDataOrDefault(
                inputVideoFormat,
                MediaFormat.KEY_HEIGHT,
                OUTPUT_VIDEO_HEIGHT
        );
        int videoFrameRate = getMediaDataOrDefault(
                inputVideoFormat,
                MediaFormat.KEY_FRAME_RATE,
                OUTPUT_VIDEO_FRAME_RATE
        );
        int videoMaxInputSize = getMediaDataOrDefault(
                inputVideoFormat,
                MediaFormat.KEY_MAX_INPUT_SIZE,
                OUTPUT_VIDEO_MAX_INPUT_SIZE
        );

        Log.d(TAG, "createOutputVideoFormat: width = " + videoWidth);
        Log.d(TAG, "createOutputVideoFormat: height = " + videoHeight);

        MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(
                OUTPUT_VIDEO_MIME_TYPE,
                videoWidth,
                videoHeight
        );
        outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
        outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
        outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);
        outputVideoFormat.setInteger(
                MediaFormat.KEY_I_FRAME_INTERVAL,
                OUTPUT_VIDEO_IFRAME_INTERVAL
        );
        outputVideoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, videoMaxInputSize);

        return outputVideoFormat;
    }

    // ----- utils -----
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
            for (String type: types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private int getMediaDataOrDefault(MediaFormat inputVideoFormat, String key, int defaultValue) {
        if (inputVideoFormat.containsKey(key)) return inputVideoFormat.getInteger(key);
        else return defaultValue;
    }

    // ----- builder -----
    public static class Builder {
        private List<GLDrawable> drawableList = new ArrayList<>();
        private int inputResId = -1;
        private int musicResId = -1;
        private String inputFilePath = null;
        private String outputFilePath = null;

        public Builder addDrawer(GLDrawable drawer) {
            drawableList.add(drawer);
            return this;
        }

        public Builder setInputResId(int inputResId) {
            this.inputResId = inputResId;
            this.inputFilePath = null;
            return this;
        }

        public Builder setInputFilePath(String inputFilePath) {
            this.inputResId = -1;
            this.inputFilePath = inputFilePath;
            return this;
        }

        public Builder setOutputPath(String outputFilePath) {
            this.outputFilePath = outputFilePath;
            return this;
        }

        public Builder setBackgroundMusic(int musicResId) {
            this.musicResId = musicResId;
            return this;
        }

        private MediaExtractor createAudioExtractor(Context context) throws IOException{
            if (musicResId != -1)
                return Extractor.createExtractor(context, musicResId);
            else if (inputResId != -1)
                return Extractor.createExtractor(context, inputResId);
            else if (inputFilePath != null)
                return Extractor.createExtractor(inputFilePath);
            else
                throw new IllegalStateException("No input specified");
        }

        public VideoProcessor build(Context context) throws IOException {
            VideoProcessor processor = new VideoProcessor();
            processor.drawerList = drawableList;

            if (inputResId != -1) {
                processor.videoExtractor = Extractor.createExtractor(context, inputResId);
            } else if (inputFilePath != null) {
                processor.videoExtractor = Extractor.createExtractor(inputFilePath);
            } else {
                throw new IllegalStateException("No input specified");
            }

            processor.audioExtractor = createAudioExtractor(context);

            if (outputFilePath != null) {
                processor.outputPath = outputFilePath;
            } else {
                throw new IllegalStateException("No output specified");
            }

            return processor;
        }
    }
}
