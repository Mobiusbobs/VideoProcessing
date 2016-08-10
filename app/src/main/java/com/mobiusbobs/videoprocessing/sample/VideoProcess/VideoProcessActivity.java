package com.mobiusbobs.videoprocessing.sample.VideoProcess;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

//import com.mobiusbobs.videoprocessing.core.gldrawer.TextDrawer;
import com.mobiusbobs.videoprocessing.sample.R;
import com.mobiusbobs.videoprocessing.sample.Util.MediaFileHelper;

import java.io.File;

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
public class VideoProcessActivity extends AppCompatActivity {
    private static final String TAG = "TEST";
    private static final int REQUEST_IMPORT_FILE_FOR_CONFIG = 11;
    private static final int REQUEST_IMPORT_FILE_FOR_ENCODE = 12;

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
        "Test Processing with add Pet Tag",
        "Test Processing with all of above",
    };

    private VideoProcessTask task;
    private File defaultFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actiivty_video_processing_test);
        initView();

        task = new VideoProcessTask(this, this);
        defaultFile = MediaFileHelper.copyRawFileToInternalStorage(this, R.raw.test1);
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
                    task.openMovieDirectory(VideoProcessActivity.this);
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
                task.runPrintDeviceInfo();
                break;
            case 1: // "Print MediaCodec / MediaFormat Info(default video file)"
                task.runPrintMediaCondecInfo(defaultFile.getAbsolutePath());
                break;
            case 2: // "Print MediaCodec / MediaFormat Info(imported video file)"
                task.requestImportFile(REQUEST_IMPORT_FILE_FOR_CONFIG);
                break;

            // simple encoder / decoder check
            case 3: //"Test Simply Decode -> Encode(default video file)"
                task.runVideoProcess(defaultFile.getAbsolutePath());
                break;
            case 4: //"Test Simply Decode -> Encode(chosen file)"
                task.requestImportFile(REQUEST_IMPORT_FILE_FOR_ENCODE);
                break;
            case 5: //"Test Simply Decode -> Encode(recorded file)"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                break;

            // image processing check
            case 6: //"Test Processing with add Image Sticker"
                task.runVideoProcessWithImageSticker(defaultFile.getAbsolutePath());
                break;
            case 7: //"Test Processing with add Gif Sticker"
                task.runVideoProcessWithGifSticker(defaultFile.getAbsolutePath());
                break;
            case 8: //"Test Processing with add Text Sticker"
                task.runVideoProcessWithTextSticker(defaultFile.getAbsolutePath());
                break;
            case 9: //"Test Processing with add WaterMark(Blur)"
                task.runVideoProcessWithWatermark(defaultFile.getAbsolutePath());
                //Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                break;
            case 10: //"Test Processing with add Pet Tag"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                break;
            case 11: //"Test Processing with all of above"
                Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show();
                //runVideoProcess();
                break;
        }
    }

    // --- onActivity result --- //
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMPORT_FILE_FOR_CONFIG) {
                Uri selectedVideoUri = data.getData();
                String filePath = MediaFileHelper.getRealPathFromUri(this, selectedVideoUri);
                task.runPrintMediaCondecInfo(filePath);
            } else if(requestCode == REQUEST_IMPORT_FILE_FOR_ENCODE) {
                Uri selectedVideoUri = data.getData();
                String filePath = MediaFileHelper.getRealPathFromUri(this, selectedVideoUri);
                Log.d(TAG, "file = " + filePath);
                task.runVideoProcess(filePath);
            }
        }
    }


}
