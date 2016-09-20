package com.mobiusbobs.videoprocessing.sample;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;

import com.mobiusbobs.videoprocessing.core.util.CoordConverter;
import com.mobiusbobs.videoprocessing.core.gldrawer.GifDrawer;
import com.mobiusbobs.videoprocessing.core.ProcessorRunner;
import com.mobiusbobs.videoprocessing.core.util.Size;
import com.mobiusbobs.videoprocessing.core.util.Timer;
import com.mobiusbobs.videoprocessing.core.VideoProcessor;
import com.mobiusbobs.videoprocessing.core.gif.GifDecoder;

import java.io.IOException;

/*
 * Sample:
 * https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
 *
 */
public class VideoProcessingTestActivity extends AppCompatActivity {
    private static final String TAG = "TEST";

    // File Path
    public static final String FILE_OUTPUT_MP4 = "/sdcard/Download/TestCodec4.mp4";

    private Size outputVideoSize = new Size(720, 1280);

    // MainComponent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actiivty_video_processing_test);
        initView();
    }

    private void initView() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    runVideoProcess();
                    Snackbar.make(view, "Run test of adding gif, watermark and text", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
        }
    }

    // TODO two output from shared middle production
    // private int resultCounter = 0;
    private void runVideoProcess() {

        final Timer timer = new Timer();
        final Timer timerW = new Timer();
        timer.startTimer();
        timerW.startTimer();

        // resultCounter = 0;

//        CodecManager codec = new CodecManager(this, false, Util.getScreenDimen(this));
//        codec.setOnMuxerDone(new CodecManager.OnMuxerDone() {
//          @Override
//          public void onDone() {
//            timer.endTimer("mux(no watermark) is done");
//            resultCounter++;
//            if(resultCounter==2) Log.d(TAG, "result done! call callback!!!");
//          }
//        });
//        CodecManager.ExtractDecodeEditEncodeMuxWrapper.run(codec, FILE_OUTPUT_MP4, R.raw.test_21);

        Context context = getApplicationContext();
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int screenWidth = point.x;
        int screenHeight = point.y;
        CoordConverter coordConverter = new CoordConverter(context, screenWidth, screenHeight);

        // --- setup gif drawer ---
        int gifId = R.raw.gif_funny;
        GifDecoder gifDecoder = GifDrawer.createGifDecoder(context, gifId);
        float[] gifVertices = coordConverter.getAlignCenterVertices(gifId);
        GifDrawer gifDrawer = new GifDrawer(context, outputVideoSize, gifDecoder, gifVertices);

        // --- setup watermark ---
        WatermarkDrawer watermarkDrawer = new WatermarkDrawer(context, coordConverter);

        // --- setup text drawer ---
//        int pawId = R.drawable.icon_paw;
//        TextDrawer textDrawer = new TextDrawer(
//                context,
//                coordConverter,
//                screenWidth,
//                "Tiger",
//                pawId
//        );

        try {
            VideoProcessor videoProcessor = new VideoProcessor.Builder(outputVideoSize)
                    .setInputResId(R.raw.test_21)
                    .setBackgroundMusic(R.raw.short1)
                    .addDrawer(gifDrawer)
                    .addDrawer(watermarkDrawer)
//                    .addDrawer(textDrawer)
                    .setOutputPath(FILE_OUTPUT_MP4)
                    .build(context);

            ProcessorRunner.run(videoProcessor, "Add Gif And Logo", new ProcessorRunner.ProcessorRunnerCallback() {
                @Override
                public void onCompleted() {
                    timerW.endTimer("mux(with watermark) is done");
//                    resultCounter++;
//                    if (resultCounter==2) Log.d(TAG, "result done! call callback!!!");
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "Video Processing failed");
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- setup sticker ----
        // TODO create another example for this
//        int stickerDrawableId = R.drawable.frames_hungry;
//        stickerDrawer = new BaseDrawer(
//                context,
//                stickerDrawableId,
//                coordConverter.getAlignCenterVertices(stickerDrawableId)
//        );
    }
}
