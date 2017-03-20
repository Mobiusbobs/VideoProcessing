/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiusbobs.videoprocessing.core.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

/**

 */
public class EncoderCore {
    private static final String TAG = "EncoderCore";
    private static final boolean VERBOSE = false;

    protected MuxerWrapper muxer;
    protected MediaCodec encoder;

    protected MediaCodec.BufferInfo bufferInfo;

    protected int trackIndex;
    protected boolean muxerStarted;

    protected static final int TIMEOUT_USEC = 10000;	// 10 ms

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public EncoderCore(MuxerWrapper muxer) {
        bufferInfo = new MediaCodec.BufferInfo();
        trackIndex = -1;
        muxerStarted = false;
        this.muxer = muxer;
    }

    /**
     * Releases encoder resources.
     */
    public synchronized void release() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if (encoder != null) {
            encoder.stop();
            encoder.release();
            encoder = null;
        }
        if (muxer != null) {
            // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
            //       of frames submitted, and don't call stop() if we haven't written anything.
            muxer.stopAndRelease();
            muxer = null;
        }
    }

    protected void encode(
      final ByteBuffer buffer,
      final int length,
      final long presentationTimeUs,
      boolean endOfStream
    ) {
        final ByteBuffer[] inputBuffers = encoder.getInputBuffers();

        while (true) {
            final int inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);

            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }

                if (endOfStream) {
                    Log.d(TAG, "Read end of stream");
                    encoder.queueInputBuffer(inputBufferIndex, 0, 0,
                      presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    encoder.queueInputBuffer(inputBufferIndex, 0, length,
                      presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            }
        }
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean isSurfaceInput, boolean endOfStream) {
        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if (isSurfaceInput && endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            encoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        while (true) {
            int encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = encoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (muxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = encoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                trackIndex = muxer.addTrack(newFormat);
                muxerStarted = true;
                if (!muxer.start()) {
                    // we should wait until muxer is ready
                    synchronized (muxer) {
                        while (!muxer.isStarted())
                            try {
                                muxer.wait(100);
                            } catch (final InterruptedException e) {
                                break;
                            }
                    }
                }
            } else if (encoderStatus < 0) {
                Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);

                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                    if (VERBOSE) {
                        Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer, ts=" +
                                bufferInfo.presentationTimeUs);
                    }
                }

                encoder.releaseOutputBuffer(encoderStatus, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }

}
