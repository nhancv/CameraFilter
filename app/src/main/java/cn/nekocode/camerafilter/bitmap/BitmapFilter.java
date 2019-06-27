/*
 * MIT License
 *
 * Copyright (c) 2019 Nhan Cao
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package cn.nekocode.camerafilter.bitmap;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.support.annotation.CallSuper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.nekocode.camerafilter.MyGLUtils;
import cn.nekocode.camerafilter.R;
import cn.nekocode.camerafilter.RenderBuffer;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public abstract class BitmapFilter {
    static final float SQUARE_COORDS[] = {
//            1.0f, -1.0f,
//            -1.0f, -1.0f,
//            1.0f, 1.0f,
//            -1.0f, 1.0f,
            -1.0f, -1.0f,       // V1 - bottom left
            -1.0f,  1.0f,       // V2 - top left
            1.0f, -1.0f,        // V3 - bottom right
            1.0f,  1.0f,        // V4 - top right
    };
    static final float TEXTURE_COORDS[] = {
            // Mapping coordinates for the vertices
//            1.0f, 0.0f,
//            0.0f, 0.0f,
//            1.0f, 1.0f,
//            0.0f, 1.0f,
            0.0f, 1.0f,		// top left		(V2)
            0.0f, 0.0f,		// bottom left	(V1)
            1.0f, 1.0f,		// top right	(V4)
            1.0f, 0.0f		// bottom right	(V3)
    };
    static FloatBuffer VERTEX_BUF, TEXTURE_COORD_BUF; // buffer holding the texture coordinates
    static int PROGRAM = 0;

    private static final float ROATED_TEXTURE_COORDS[] = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
    };
    private static FloatBuffer ROATED_TEXTURE_COORD_BUF;

    final long START_TIME = System.currentTimeMillis();
    int iFrame = 0;

    public BitmapFilter(Context context) {
        // Setup default Buffers
        if (VERTEX_BUF == null) {
            VERTEX_BUF = ByteBuffer.allocateDirect(SQUARE_COORDS.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            VERTEX_BUF.put(SQUARE_COORDS);
            VERTEX_BUF.position(0);
        }

        if (TEXTURE_COORD_BUF == null) {
            TEXTURE_COORD_BUF = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            TEXTURE_COORD_BUF.put(TEXTURE_COORDS);
            TEXTURE_COORD_BUF.position(0);
        }

        if (ROATED_TEXTURE_COORD_BUF == null) {
            ROATED_TEXTURE_COORD_BUF = ByteBuffer.allocateDirect(ROATED_TEXTURE_COORDS.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            ROATED_TEXTURE_COORD_BUF.put(ROATED_TEXTURE_COORDS);
            ROATED_TEXTURE_COORD_BUF.position(0);
        }

        if (PROGRAM == 0) {
            PROGRAM = MyGLUtils.buildProgram(context, R.raw.vertext, R.raw.original_rtt);
        }
    }

    @CallSuper
    public void onAttach() {
        iFrame = 0;
    }

    final public void draw(int bitmapTexId, int canvasWidth, int canvasHeight) {
        onDraw(bitmapTexId, canvasWidth, canvasHeight);
        iFrame++;
    }

    abstract void onDraw(int cameraTexId, int canvasWidth, int canvasHeight);

    void setupShaderInputs(int program, int[] iResolution, int[] iChannels, int[][] iChannelResolutions) {
        setupShaderInputs(program, VERTEX_BUF, TEXTURE_COORD_BUF, iResolution, iChannels, iChannelResolutions);
    }

    void setupShaderInputs(int program, FloatBuffer vertex, FloatBuffer textureCoord, int[] iResolution, int[] iChannels, int[][] iChannelResolutions) {
        GLES20.glUseProgram(program);

        int iResolutionLocation = GLES20.glGetUniformLocation(program, "iResolution");
        GLES20.glUniform3fv(iResolutionLocation, 1,
                FloatBuffer.wrap(new float[]{(float) iResolution[0], (float) iResolution[1], 1.0f}));

        float time = ((float) (System.currentTimeMillis() - START_TIME)) / 1000.0f;
        int iGlobalTimeLocation = GLES20.glGetUniformLocation(program, "iGlobalTime");
        GLES20.glUniform1f(iGlobalTimeLocation, time);

        int iFrameLocation = GLES20.glGetUniformLocation(program, "iFrame");
        GLES20.glUniform1i(iFrameLocation, iFrame);

        int vPositionLocation = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(vPositionLocation);
        GLES20.glVertexAttribPointer(vPositionLocation, 2, GLES20.GL_FLOAT, false, 4 * 2, vertex);

        int vTexCoordLocation = GLES20.glGetAttribLocation(program, "vTexCoord");
        GLES20.glEnableVertexAttribArray(vTexCoordLocation);
        GLES20.glVertexAttribPointer(vTexCoordLocation, 2, GLES20.GL_FLOAT, false, 4 * 2, textureCoord);

        for (int i = 0; i < iChannels.length; i++) {
            int sTextureLocation = GLES20.glGetUniformLocation(program, "iChannel" + i);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, iChannels[i]);
            GLES20.glUniform1i(sTextureLocation, i);
        }

        float _iChannelResolutions[] = new float[iChannelResolutions.length * 3];
        for (int i = 0; i < iChannelResolutions.length; i++) {
            _iChannelResolutions[i * 3] = iChannelResolutions[i][0];
            _iChannelResolutions[i * 3 + 1] = iChannelResolutions[i][1];
            _iChannelResolutions[i * 3 + 2] = 1.0f;
        }

        int iChannelResolutionLocation = GLES20.glGetUniformLocation(program, "iChannelResolution");
        GLES20.glUniform3fv(iChannelResolutionLocation,
                _iChannelResolutions.length, FloatBuffer.wrap(_iChannelResolutions));
    }

    public static void release() {
        PROGRAM = 0;
    }
}
