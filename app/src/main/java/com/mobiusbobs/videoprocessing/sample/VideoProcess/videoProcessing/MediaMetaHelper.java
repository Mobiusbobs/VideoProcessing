package com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

/**
 * android
 * <p/>
 * Created by rayshih on 6/10/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class MediaMetaHelper {

    public static MediaMetadataRetriever createRetriever(Context context, Uri uri) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(context, uri);
        return mediaMetadataRetriever;
    }

    // in ms
    public static long getMediaDuration(MediaMetadataRetriever mediaMetadataRetriever) {
        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (time == null || time.equalsIgnoreCase("0")) {
            return -1;
        }

        return Long.parseLong(time);
    }

    // in ms
    public static long getMediaDuration(Context context, Uri uri) {
        MediaMetadataRetriever retriever = createRetriever(context, uri);
        return getMediaDuration(retriever);
    }
}
