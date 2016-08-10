package com.mobiusbobs.videoprocessing.sample.Util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * android
 * <p/>
 * Created by wangalbert on 8/5/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class MediaFileHelper {
    public static final String TAG = "MediaFileHelper";

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public static final String DIRECTORY_TEST = "TEST";

    /**
     * Creates a media file in the {@code Environment.DIRECTORY_PICTURES} directory. The directory
     * is persistent and available to other applications like gallery.
     *
     * @param type Media type. Can be video or image.
     * @return A file object pointing to the newly created file.
     */
    @SuppressLint("SimpleDateFormat")
    public static File getOutputMediaFile(int type){
        File mediaStorageDir = getOutputDirectoryPath();

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static File getOutputDirectoryPath() {
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return  null;
        }

        return new File(Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_MOVIES), DIRECTORY_TEST);
    }

    // TODO update file name
    public static File copyRawFileToInternalStorage(Context context, int testRawId) {
        File file = new File(context.getFilesDir() + File.separator + "input.mp4");
        try {
            InputStream inputStream = context.getResources().openRawResource(testRawId);
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

    // http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver()
              .query(contentUri,  proj, null, null, null);

            int columnIndex = 0;
            if (cursor == null) {
                throw new IllegalArgumentException(
                  "Cannot get cursor with contentUri: " + contentUri
                );
            }

            columnIndex = cursor
              .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }



}
