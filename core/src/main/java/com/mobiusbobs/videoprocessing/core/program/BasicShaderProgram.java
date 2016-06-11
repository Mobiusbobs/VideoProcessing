package com.mobiusbobs.videoprocessing.core.program;

import android.content.Context;
import com.mobiusbobs.videoprocessing.core.R;

/**
 * android
 * <p/>
 * Created by rayshih on 6/11/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class BasicShaderProgram extends ShaderProgram {

    public BasicShaderProgram(Context context) {
        super(context,
                R.raw.basic_vertex_shader,
                R.raw.basic_fragment_shader);
    }

    // TODO bind
}
