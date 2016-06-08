package com.mobiusbobs.videoprocessing.core;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.mobiusbobs.videoprocessing.core.codec.Extractor;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.gles.surface.InputSurface;
import com.mobiusbobs.videoprocessing.core.gles.surface.OutputSurface;
import com.mobiusbobs.videoprocessing.core.program.BlurHShaderProgram;
import com.mobiusbobs.videoprocessing.core.program.BlurVShaderProgram;
import com.mobiusbobs.videoprocessing.core.program.TextureOpacityShaderProgram;
import com.mobiusbobs.videoprocessing.core.program.TextureShaderProgram;
import com.mobiusbobs.videoprocessing.core.tmp.BlurTable;
import com.mobiusbobs.videoprocessing.core.tmp.Watermark;
import com.mobiusbobs.videoprocessing.core.util.CoordConverter;
import com.mobiusbobs.videoprocessing.core.util.FrameBufferHelper;
import com.mobiusbobs.videoprocessing.core.util.TextureHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.translateM;
import static com.mobiusbobs.videoprocessing.core.util.CoordConverter.rectCoordToGLCoord;

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

    private OnProgressListener onProgressListener;

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

    // for audio hotfix
    long lastAudioPresentationTime = 0;

    // FBO
    // should be power of 2 (POT)
    private int fboWidth = 1024;
    private int fboHeight = 1024;
    // first FBO that stores all the render and apply first blur
    private int fboId;
    private int fboTextureId;
    // second FBO that take the first FBO and apply second blur
    private int fboBlurId;
    private int fboBlurTextureId;

    // shader program
    private TextureShaderProgram textureProgram;
    private BlurHShaderProgram blurHorizontalProgram;
    private BlurVShaderProgram blurVerticalProgram;
    private TextureOpacityShaderProgram textureOpacityProgram;

    // data object
    private BlurTable blurTable;
    private Watermark watermark;

    // matrix
    protected float[] mMVPMatrix = new float[16];
    protected float[] mModelMatrix = new float[16];
    protected float[] mViewMatrix = new float[16];
    protected float[] mProjectionMatrix = new float[16];

    // watermark texture
    private int textureWatermark;




    private Context context;

    // Constructor
    private VideoProcessor(Context context) {
        this.context = context;
    }

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

        // --- setup extract ---
        int videoTrackIndex = Extractor.getAndSelectVideoTrackIndex(videoExtractor);
        int audioTrackIndex = Extractor.getAndSelectAudioTrackIndex(audioExtractor);

        // input video format
        MediaFormat inputVideoFormat = videoExtractor.getTrackFormat(videoTrackIndex);
        long videoDuration = inputVideoFormat.getLong(MediaFormat.KEY_DURATION);
        // fix config problem: http://stackoverflow.com/questions/15105843/mediacodec-jelly-bean
        inputVideoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);

        // input audio format
        MediaFormat inputAudioFormat = audioExtractor.getTrackFormat(audioTrackIndex);
        long audioDuration = inputAudioFormat.getLong(MediaFormat.KEY_DURATION);

        // --- video encoder ---
        // Create a MediaCodec for the desired codec, then configure it as an encoder with
        // our desired properties. Request a Surface to use for input.
        MediaFormat outputVideoFormat = createOutputVideoFormat(inputVideoFormat);
        AtomicReference<Surface> inputSurfaceReference = new AtomicReference<>();
        outputVideoFormat.setLong(MediaFormat.KEY_DURATION, 0); // videoDuration + 5 * 1000 * 1000); // TODO
        videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
        inputSurface = new InputSurface(inputSurfaceReference.get());
        inputSurface.makeCurrent();

        // --- video decoder ---
        // to create OutputSurface object, it must be after inputSurface.makeCurrent()
        outputSurface = new OutputSurface(getOutputSurfaceRenderVerticesData(inputVideoFormat));
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



        // object
        blurTable = new BlurTable();
        watermark = new Watermark();

        // shader program
        textureProgram = new TextureShaderProgram(context);
        textureOpacityProgram = new TextureOpacityShaderProgram(context);
        blurHorizontalProgram = new BlurHShaderProgram(context);
        blurVerticalProgram = new BlurVShaderProgram(context);

        // create fbo
        int[] tmp = FrameBufferHelper.createFrameBuffer(fboWidth, fboHeight);
        fboId = tmp[0];
        fboTextureId = tmp[1];

        // create fboBlur
        tmp = FrameBufferHelper.createFrameBuffer(fboWidth, fboHeight);
        fboBlurId = tmp[0];
        fboBlurTextureId = tmp[1];

        // load texture
        textureWatermark = TextureHelper.loadTexture(context, R.drawable.logo_watermark);




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

        long animationTime = 5 * 1000 * 1000;    // TODO
        long videoTotalDuration = videoDuration + animationTime; // TODO
        long lastPTimeUs = 0;
        boolean hasVideoEncoderEndSignalSent = false;

        long audioMaxTime = videoTotalDuration;

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
                        audioMaxTime
                );

                long sampleTime = audioExtractor.getSampleTime();
                if (sampleTime == -1) {
                    Log.d("WATER", "audioExtractor: sampleTime reached! Loop!!!");
                    audioMaxTime -= audioDuration;
                    audioPTimeOffset += audioDuration;
                    audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    audioDecoder.flush();
                }
            }

            // --- pull output frames from video decoder and feed it to encoder ---
            if (encoderOutputVideoFormat == null || muxing) {
                if (videoDecoderDone) {
                    if (lastPTimeUs >= videoTotalDuration) {
                        if (!hasVideoEncoderEndSignalSent) {
                            Log.d(TAG, "-----signal end of input stream");
                            videoEncoder.signalEndOfInputStream();
                            hasVideoEncoderEndSignalSent = true;
                        }
                    } else {
                        long timeUs = lastPTimeUs + 20 * 1000;
                        Log.d(TAG, "-----render extended video timeUs: " + timeUs);
                        render(outputSurface, inputSurface, timeUs, videoDuration);
                        lastPTimeUs = timeUs;
                    }

                    Log.d("WATER", "Video(extended) timeStamp=" + lastPTimeUs + ", duration=" + videoTotalDuration);

                } else {
                    boolean frameAvailable = checkVideoDecodeState(videoDecoder, videoDecoderOutputBufferInfo);

                    if (frameAvailable) {
                        videoDecodedFrameCount++;
                        long timeUs = videoDecoderOutputBufferInfo.presentationTimeUs;

                        outputSurface.awaitNewImage();
                        render(outputSurface, inputSurface, timeUs, videoDuration);
                        lastPTimeUs = timeUs;
                    }

                    if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "FLAG: videoDecoderDone!!! video decoder: EOS");
                        videoDecoderDone = true;
                    }
                }
            }

            // --- poll output frames from audio decoder ----
            if (!audioDecoderDone && pendingAudioDecoderOutputBufferIndex == -1) {
                updateAudioDecodeState(audioDecoder, audioDecoderOutputBufferInfo);
            }

            // Feed the pending decoded audio buffer to the audio encoder.
            // TODO check to see if I can implement this into audio Decoder...
            if (pendingAudioDecoderOutputBufferIndex != -1) {
                audioDecoderDone = pipeAudioStream(audioDecoder, audioEncoder, audioPTimeOffset, videoDuration);
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

            if (onProgressListener != null) {
                long currentVideoSampleTime = videoExtractor.getSampleTime();
                float percentage = (float) currentVideoSampleTime / videoDuration;
                if (percentage < 0 || percentage > 1) {
                    percentage = 1.0f;
                }
                onProgressListener.onProgress(percentage);
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


    // ----- test function -------
    protected void setupProjectionMatrix() {
        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float left = -1.0f;   //-ratio;
        final float right = 1.0f;   //ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = -1.0f;   //1.0f;
        final float far = 1.0f;     //10.0f;

        Matrix.orthoM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    protected void setupViewMatrix() {
        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.0f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = 0.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
    }

    protected void calculateMVPMatrix()   {
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    }

    private void setupViewport(int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);

        setupProjectionMatrix();
        setupViewMatrix();
        calculateMVPMatrix();

        /*
        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width / (float) height, 1f, 10f);

        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, 0f, 0f, -2.5f);
        rotateM(modelMatrix, 0, 0f, 1f, 0f, 0f);

        final float[] temp = new float[16];
        multiplyMM(temp, 0, projectionMatrix, 0, modelMatrix, 0);
        System.arraycopy(temp, 0, projectionMatrix, 0, temp.length);
        */
    }


    private void renderWatermarkAnimation(
      OutputSurface outputSurface,
      long timeUs,
      long videoDuration
    ) {
        float blur;
        float opacity;
        long animationTime = (timeUs - videoDuration) / 1000L;

        // use 2 sec to animate blur and fade in
        if (animationTime < 2000) {
            blur = (float) (1 - (2000 - animationTime) / 2000.0);
            opacity = (float) (1 - (2000 - animationTime) / 2000.0);
        } else {
            blur = 1.0f;
            opacity = 1.0f;
        }
        Log.d("TEST", "Render: animationTime=" + animationTime + ", blur=" + blur + ", opacity=" + opacity);

        // -------- render to fbo ---------
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        setupViewport(fboWidth, fboWidth);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // draw to texture
        outputSurface.drawImage();
        for (GLDrawable drawer : drawerList) {
            drawer.draw(videoDuration);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        // -------------------------------

        // -------- render to fbo 2 ---------
        glBindFramebuffer(GL_FRAMEBUFFER, fboBlurId);
        setupViewport(fboWidth, fboWidth);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        //draw to texture
        blurHorizontalProgram.useProgram();
        blurHorizontalProgram.setUniforms(mMVPMatrix, fboTextureId, blur);
        blurTable.bindData(blurHorizontalProgram);
        blurTable.draw();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        // -------------------------------

        // -------- render to the screen ---------
        setupViewport(720, 1280);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        blurVerticalProgram.useProgram();
        blurVerticalProgram.setUniforms(mMVPMatrix, fboBlurTextureId, blur); //fboTextureId //texture
        blurTable.bindData(blurVerticalProgram);
        blurTable.draw();

        textureOpacityProgram.useProgram();
        textureOpacityProgram.setUniforms(mMVPMatrix, textureWatermark, opacity);
        watermark.bindData(textureOpacityProgram);
        watermark.draw();
        // ----------------------------------------
    }

    // ----- video draw function -----
    private void render(
            // surface from decoder
            OutputSurface outputSurface,

            // surface to encoder
            InputSurface inputSurface,

            // presentation time in microsecond (10^-6s)
            long timeUs,

            long videoDuration
    ) {
        // render video
        if (timeUs < videoDuration) {
            long timeMs = timeUs / 1000;
            Log.d(TAG, "render video... timeMs = " + timeMs);
            outputSurface.drawImage();
            for (GLDrawable drawer : drawerList) {
                drawer.draw(timeMs);
            }
        }
        // render watermark animation
        else {
            renderWatermarkAnimation(outputSurface, timeUs, videoDuration);
        }

        inputSurface.setPresentationTime(timeUs * 1000);
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
    private boolean pipeAudioStream(MediaCodec audioDecoder, MediaCodec audioEncoder, long pTimeOffset, long thresholdTime) {
        int encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (encoderInputBufferIndex < 0) {
            Log.e(TAG, "audioEncoder.dequeueInputBuffer: no audio encoder input buffer");
            return false;
        }

        ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
        int size = audioDecoderOutputBufferInfo.size;
        long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs + pTimeOffset;
        if (presentationTime < lastAudioPresentationTime) {
            presentationTime = lastAudioPresentationTime + 1;
        }
        lastAudioPresentationTime = presentationTime;

        if (size >= 0) {
            ByteBuffer decoderOutputBuffer =
                    audioDecoderOutputBuffers[pendingAudioDecoderOutputBufferIndex].duplicate();
            decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset);
            decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size);

            encoderInputBuffer.position(0);

            // handle extra frame
            if (presentationTime >= thresholdTime) {
                Log.d("WATER", "MediaCodec,BufferInfo: encoderInputBufferIndex = " + encoderInputBufferIndex);
                Log.d("WATER", "MediaCodec,BufferInfo: size = " + audioDecoderOutputBufferInfo.size);
                Log.d("WATER", "MediaCodec,BufferInfo: presentationTimeUs with offset... = " + presentationTime);
                Log.d("WATER", "MediaCodec,BufferInfo: flags = " + audioDecoderOutputBufferInfo.flags);


                // empty
                byte[] bytes = new byte[size];
                ByteBuffer emptyBuffer = ByteBuffer.wrap(bytes);
                emptyBuffer.position(0);

                // fade
                byte[] fadeArray = new byte[size];
                for(int i=0; i < fadeArray.length/2; i++) {
                    int index = i*2;
                    ByteBuffer bb = ByteBuffer.allocate(2);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.put(decoderOutputBuffer.get(index));
                    bb.put(decoderOutputBuffer.get(index + 1));
                    short tmp = (short)(bb.getShort(0) / 8);    //cut the volume in half

                    bb.putShort(0, tmp);
                    fadeArray[index] = bb.get(0);
                    fadeArray[index + 1] = bb.get(1);
                }
                ByteBuffer fadeBuffer = ByteBuffer.wrap(fadeArray);
                fadeBuffer.position(0);


                emptyBuffer.position(0);
                encoderInputBuffer.put(fadeBuffer);

            // normal case
            } else {
                encoderInputBuffer.put(decoderOutputBuffer);
            }

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

        ByteBuffer encoderOutputBuffer = videoEncoderOutputBuffers[encoderOutputBufferIndex];

        if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "video encoder: codec config buffer");
            // Simply ignore codec config buffers.
            videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
            return false;
        }

        if (videoEncoderOutputBufferInfo.size != 0) {
            muxer.writeSampleData(
                    outputVideoTrack,
                    encoderOutputBuffer,
                    videoEncoderOutputBufferInfo
            );
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

        Log.d("WATER", "audioChannelCount = " + audioChannelCount);
        outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);
        outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);
        outputAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioMaxInputSize);

        return outputAudioFormat;
    }

    private MediaFormat createOutputVideoFormat(MediaFormat inputVideoFormat) {
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

        Log.d(TAG, "createOutputVideoFormat: width = " + OUTPUT_VIDEO_WIDTH);
        Log.d(TAG, "createOutputVideoFormat: height = " + OUTPUT_VIDEO_HEIGHT);

        MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(
                OUTPUT_VIDEO_MIME_TYPE,
                OUTPUT_VIDEO_WIDTH,
                OUTPUT_VIDEO_HEIGHT
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

    private float[] getOutputSurfaceRenderVerticesData(MediaFormat inputVideoFormat) {
        float outputRatio = (float)OUTPUT_VIDEO_HEIGHT / OUTPUT_VIDEO_WIDTH;

        int inputVideoWidth = getMediaDataOrDefault(
                inputVideoFormat, MediaFormat.KEY_WIDTH, OUTPUT_VIDEO_WIDTH);
        int inputVideoHeight = getMediaDataOrDefault(
                inputVideoFormat, MediaFormat.KEY_HEIGHT, OUTPUT_VIDEO_HEIGHT);

        float width = inputVideoWidth;
        float height = inputVideoHeight;
        float ratio = height / width;

        if (ratio > outputRatio) {
            width = OUTPUT_VIDEO_HEIGHT / ratio;
            height = OUTPUT_VIDEO_HEIGHT;
        } else {
            width = OUTPUT_VIDEO_WIDTH;
            height = OUTPUT_VIDEO_WIDTH * ratio;
        }

        // horizontal
        float hDiff = OUTPUT_VIDEO_WIDTH - width;
        int hOffset = (int)hDiff / 2;

        // vertical
        float vDiff = OUTPUT_VIDEO_HEIGHT - height;
        int vOffset = (int)vDiff / 2;

        float x1 = rectCoordToGLCoord(hOffset, OUTPUT_VIDEO_WIDTH);
        float x2 = rectCoordToGLCoord(OUTPUT_VIDEO_WIDTH - hOffset, OUTPUT_VIDEO_WIDTH);
        float y1 = rectCoordToGLCoord(vOffset, OUTPUT_VIDEO_HEIGHT);
        float y2 = rectCoordToGLCoord(OUTPUT_VIDEO_HEIGHT - vOffset, OUTPUT_VIDEO_HEIGHT);
        return CoordConverter.getTriangleVerticesData(x1, y1, x2, y2);
    }

    // ----- listener -----
    public interface OnProgressListener {
        void onProgress(float percentage);
    }

    // ----- builder -----
    public static class Builder {
        private List<GLDrawable> drawableList = new ArrayList<>();
        private int inputResId = -1;
        private int musicResId = -1;
        private String musicFilePath = null;
        private String inputFilePath = null;
        private String outputFilePath = null;
        private OnProgressListener onProgressListener = null;

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

        public Builder setBackgroundMusic(String musicFilePath) {
            this.musicFilePath = musicFilePath;
            return this;
        }

        public Builder setOnProcessListener(OnProgressListener listener) {
            this.onProgressListener = listener;
            return this;
        }

        private MediaExtractor createAudioExtractor(Context context) throws IOException{
            if (musicFilePath != null)
                return Extractor.createExtractor(musicFilePath);
            else if (musicResId > 0)
                return Extractor.createExtractor(context, musicResId);
            else if (inputResId != -1)
                return Extractor.createExtractor(context, inputResId);
            else if (inputFilePath != null)
                return Extractor.createExtractor(inputFilePath);
            else
                throw new IllegalStateException("No input specified");
        }

        public VideoProcessor build(Context context) throws IOException {
            VideoProcessor processor = new VideoProcessor(context);
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

            processor.onProgressListener = this.onProgressListener;

            return processor;
        }
    }
}
