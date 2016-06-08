package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.content.Context;
import android.graphics.Bitmap;

import com.mobiusbobs.videoprocessing.core.program.TextureShaderProgram;
import com.mobiusbobs.videoprocessing.core.util.VertexArray;
import com.mobiusbobs.videoprocessing.core.util.BitmapHelper;
import com.mobiusbobs.videoprocessing.core.util.CoordConverter;

import java.io.IOException;

import static com.mobiusbobs.videoprocessing.core.tmp.Constants.BYTES_PER_FLOAT;

/**
 * VideoProcessing
 * <p/>
 * Created by rayshih on 4/13/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class WatermarkDrawer implements GLDrawable {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT
      + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private Context context;
    private BaseDrawer drawer;
    private int resourceId;
    private float[] vertexData;
    private final VertexArray vertexArray;

    public WatermarkDrawer(Context context, CoordConverter coordConverter, int resourceId) {
        this.context = context;
        this.resourceId = resourceId;
        vertexData = coordConverter.getAlignCenterVertices(resourceId);
        vertexArray = new VertexArray(vertexData);

        drawer = new BaseDrawer(
          vertexData
        );
    }

    // position / texture coordinate
    public void bindData(TextureShaderProgram textureProgram) {
        vertexArray.setVertexAttribPointer(
          0,
          textureProgram.getPositionAttributeLocation(),
          POSITION_COMPONENT_COUNT,
          STRIDE
        );

        vertexArray.setVertexAttribPointer(
          POSITION_COMPONENT_COUNT,
          textureProgram.getTextureCoordinatesAttributeLocation(),
          TEXTURE_COORDINATES_COMPONENT_COUNT,
          STRIDE
        );
    }

    @Override
    public void init() throws IOException {
        Bitmap bitmap = BitmapHelper.generateBitmap(context, resourceId);
        drawer.init();
        bitmap.recycle();
    }

    @Override
    public void draw(long timeMs) {
        drawer.draw(timeMs);
    }
}
