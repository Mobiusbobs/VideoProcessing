package com.mobiusbobs.videoprocessing.sample.VideoProcessingTest;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.mobiusbobs.videoprocessing.core.ProcessorRunner;
import com.mobiusbobs.videoprocessing.core.codec.Extractor;
import com.mobiusbobs.videoprocessing.core.codec.MediaCodecChecker;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.gldrawer.GifDrawer;
//import com.mobiusbobs.videoprocessing.core.gldrawer.TextDrawer;
import com.mobiusbobs.videoprocessing.core.VideoProcessor;
import com.mobiusbobs.videoprocessing.core.gif.GifDecoder;
import com.mobiusbobs.videoprocessing.sample.R;
import com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing.CoordConverter;
import com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing.Duration;
import com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing.DurationGLDrawable;
import com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing.DurationGifDrawable;
import com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing.MediaMetaHelper;
import com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing.StickerDrawer;
import com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing.TextDrawer;
import com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing.WatermarkDrawer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Option:
 *
 * - Print Device MediaCodec Info
 * - Print MediaCodec Info, MediaFormat Info, based on default video file
 * - Print MediaCodec Info, MediaFormat Info, based on imported video file
 *
 * - Test Simply Decode -> Encode (with default video file)
 * - Test Simply Decode -> Encode (with chosen file)
 * - Test Simply Decode -> Encode (with recorded file)
 *
 * - Test Processing with add Image Sticker
 * - Test Processing with add Gif Sticker
 * - Test Processing with add Text Sticker
 * - Test Processing with add WaterMark(Blur)
 * - Test Processing with all of above
 *
 *
 */
/*
 * Sample:
 * https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
 *
 */
public class VideoProcessingTestActivity extends AppCompatActivity {
    private static final String TAG = "TEST";
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 10;
    private static final int REQUEST_IMPORT_FILE_FOR_CONFIG = 11;
    private static final int REQUEST_IMPORT_FILE_FOR_ENCODE = 12;

    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    // File Path
    public static final String FILE_OUTPUT_MP4 = "/sdcard/Download/TestCodec4.mp4";

    // Test List
    private String[] testList = {
        "Print Device Info",
        "Print MediaCodec / MediaFormat Info(default video file)",
        "Print MediaCodec / MediaFormat Info(imported video file)",

        "Test Simply Decode -> Encode(default video file)",
        "Test Simply Decode -> Encode(chosen file)",
        "Test Simply Decode -> Encode(recorded file)",

        "Test Processing with add Image Sticker",
        "Test Processing with add Gif Sticker",
        "Test Processing with add Text Sticker",
        "Test Processing with add WaterMark(Blur)",
        "Test Processing with all of above",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actiivty_video_processing_test);
        initView();
    }

    private void initView() {

        setupListView();

        setupFab();

    }

    // --- UI Component --- //
    // create test list
    private void setupListView() {
        ListView listView = (ListView)findViewById(R.id.list_view);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, testList);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                performTest(position);
            }
        });
    }

    // create fab to toggle log
    private void setupFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                    runVideoProcess();
                    Snackbar.make(view, "Run test of adding gif, watermark and text", Snackbar.LENGTH_LONG)
                      .setAction("Action", null).show();
                    */
                }
            });
        }
    }

    // --- Action --- //
    public void performTest(int testId) {
        Log.d(TAG, "running test [" + testList[testId] + "]... ... ...");
        switch(testId)  {
            // print info
            case 0: //"Print Device Info"
                runPrintDeviceInfo();
                break;
            case 1: // "Print MediaCodec / MediaFormat Info(default video file)"
                File file = copyRawFileToInternalStorage(R.raw.test1);
                Log.d(TAG, "file = " + file.getAbsolutePath());
                runPrintMediaCondecInfo(file.getAbsolutePath());
                break;
            case 2: // "Print MediaCodec / MediaFormat Info(imported video file)"
                requestImportFile(REQUEST_IMPORT_FILE_FOR_CONFIG);
                break;


            // simple encoder / decoder check
            case 3: //"Test Simply Decode -> Encode(default video file)"
                //Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                File file2 = copyRawFileToInternalStorage(R.raw.test1);
                String[] stickers = {};
                Log.d(TAG, "file = " + file2.getAbsolutePath());
                runVideoProcess(file2.getAbsolutePath(), stickers, "");

                break;
            case 4: //"Test Simply Decode -> Encode(chosen file)"
                requestImportFile(REQUEST_IMPORT_FILE_FOR_ENCODE);

                break;
            case 5: //"Test Simply Decode -> Encode(recorded file)"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                break;

            // image processing check
            case 6: //"Test Processing with add Image Sticker"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                //runVideoProcess();
                break;
            case 7: //"Test Processing with add Gif Sticker"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                //runVideoProcess();
                break;
            case 8: //"Test Processing with add Text Sticker"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                //runVideoProcess();
                break;
            case 9: //"Test Processing with add WaterMark(Blur)"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                //runVideoProcess();
                break;
            case 10: //"Test Processing with all of above"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                //runVideoProcess();
                break;
        }
    }

    // --- printing test --- //
    private void runPrintDeviceInfo() {
        Log.d(TAG, "runPrintDeviceInfo... ");
        MediaCodecChecker.dumpDeviceInfo();
    }

    private void runPrintMediaCondecInfo(String filePath) {
        Log.d(TAG, "runPrintMediaCodecInfo... filePath = " + filePath);

        // Extract file
        MediaExtractor videoExtractor = Extractor.createExtractor(filePath);
        int videoTrackIndex = Extractor.getAndSelectVideoTrackIndex(videoExtractor);
        MediaFormat videoFormat = videoExtractor.getTrackFormat(videoTrackIndex);

        // get mediaCodecInfo
        MediaCodecInfo videoCodecInfo = VideoProcessor.selectCodec(OUTPUT_VIDEO_MIME_TYPE);

        // check Media Codec for video capability with the given video format
        boolean pass = MediaCodecChecker.checkVideoCapabilitySupported(videoCodecInfo, videoFormat);
        if (pass)   {
            Toast.makeText(this, "PASS: \n" + filePath + " is supported", Toast.LENGTH_LONG).show();
        }   else    {
            String msg = "FAIL: \n" + filePath + " is not supported\nPlease check log for more detail";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void requestImportFile(int requestCode) {
        //http://stackoverflow.com/questions/31044591/how-to-select-a-video-from-the-gallery-and-get-its-real-path
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), requestCode);
    }

    // --- encoder / decoder test --- //


    // --- video processing test --- //
    private void runVideoProcess(String filePath, String[] stickerList, String petname) {
        //String inputVideoFilePath = "";
        //String[] stickerList = {"Text Sticker", "ABCDE"};
        //String petName = "myPetName";
        Log.d(TAG, "runVideoProcess... filePath = ");

        // file output
        File fileOutput = MediaFileHelper.getOutputMediaFile(MediaFileHelper.MEDIA_TYPE_VIDEO, true);
        if (fileOutput == null) {
            Log.e(TAG, "Can not get output media file path");
            return;
        }
        Log.d(TAG, "fileOutput = " + fileOutput);

        // video info
        final String fileOutputPath = fileOutput.toString();
        final long videoDuration = MediaMetaHelper.getMediaDuration(this, Uri.parse(filePath));

        // coordinate converter
        CoordConverter coordConverter = new CoordConverter(this);

        // --- gl drawable (sticker / gif / text / watermark / etc.)
        // watermark
        /*
        int watermarkWidth = 223 * 2;
        int watermarkHeight = 52 * 2;
        float[] watermarkVertices = coordConverter.getVertices(
          (VideoProcessor.OUTPUT_VIDEO_WIDTH - watermarkWidth) / 2,
          (VideoProcessor.OUTPUT_VIDEO_HEIGHT - watermarkHeight) / 2,
          watermarkWidth,
          watermarkHeight
        );
        WatermarkDrawer watermarkDrawer = new WatermarkDrawer(this, watermarkVertices);
        watermarkDrawer.setDuration(new Duration(videoDuration - 100, videoDuration + 1000));
        */

        List<GLDrawable> drawerList = new ArrayList<>();
        //drawerList.addAll(getStickerDrawerList(stickerList, coordConverter));
        //drawerList.add(topRightTextDrawer);
        //drawerList.add(watermarkDrawer);

        try {
            VideoProcessor.Builder builder = new VideoProcessor.Builder()
              .setInputFilePath(filePath)
              .setOutputPath(fileOutputPath);

            for (GLDrawable drawer: drawerList) {
                //builder.addDrawer(drawer);
            }

            VideoProcessor videoProcessor = builder.build(this);

            ProcessorRunner.run(videoProcessor, "Process video", new ProcessorRunner.ProcessorRunnerCallback() {
                @Override
                public void onCompleted() {
                    Log.d(TAG, "VideoProcess... complete... success...");
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "VideoProcess... fail... OnError = " + e);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private List<GLDrawable> getStickerDrawerList(
      String[] stickers,
      CoordConverter coordConverter
    ) {
        List<GLDrawable> drawers = new ArrayList<>();
        int size = stickers.length;
        for (int i = 0; i < size; i++) {
            DurationGLDrawable d = getStickerDrawer(true, stickers[i], coordConverter);
            if (d != null) {
                d.setDuration(new Duration());
                d.setRotate(0);
                drawers.add(d);
            }
        }
        return drawers;
    }

    private DurationGLDrawable getStickerDrawer(boolean isTextSticker, String sticker, CoordConverter coordConverter) {
        if (sticker == null) return null;

        if (isTextSticker) {
            String text = sticker;
            String textColor = "purple";
            int x = 50;
            int y = 200;
            double fontSize = 30;

            return new TextDrawer(
              this, coordConverter,
              text,
              fontSize, textColor,
              x, y
            );
        }   else {
            // image sticker: gif/sticker drawer
            String stickerFilePath = sticker;
            String stickerExt = stickerFilePath.substring(stickerFilePath.length() - 3);
            Boolean isGif = stickerExt.equalsIgnoreCase("gif");

            float[] vertices = coordConverter.getVertices(100, 100, 200, 200);
            int sampleSize = 1;
            if (isGif) {
                GifDecoder gifDecoder = GifDrawer.createGifDecoder(stickerFilePath, sampleSize);
                return new DurationGifDrawable(this, gifDecoder, vertices);
            } else {
                return new StickerDrawer(this, stickerFilePath, vertices, sampleSize);
            }
        }
    }

    // --- helper method --- //
    public File copyRawFileToInternalStorage(int testRawId) {
        File file = new File(this.getFilesDir() + File.separator + "input.mp4");
        try {
            InputStream inputStream = getResources().openRawResource(testRawId);
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            byte buf[]=new byte[1024];
            int len;
            while((len=inputStream.read(buf))>0) {
                fileOutputStream.write(buf,0,len);
            }

            fileOutputStream.close();
            inputStream.close();
        } catch (IOException e1) {

        }
        return file;
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
              .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    // --- onActivity result --- //
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Log.d(TAG, "onActivity result... requestCode = " + requestCode);
            if (requestCode == REQUEST_IMPORT_FILE_FOR_CONFIG) {
                Uri selectedVideoUri = data.getData();
                String filePath = getPath(selectedVideoUri);
                Log.d(TAG, "onActivity result... filePath = " + filePath);
                runPrintMediaCondecInfo(filePath);
            } else if(requestCode == REQUEST_IMPORT_FILE_FOR_ENCODE) {
                Uri selectedVideoUri = data.getData();
                String filePath = getPath(selectedVideoUri);
                String[] stickers = {};

                Log.d(TAG, "file = " + filePath);
                Log.d(TAG, "process imported file...");
                runVideoProcess(filePath, stickers, "");
            }
        }
    }


}
