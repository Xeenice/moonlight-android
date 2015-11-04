package com.limelight;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by grigorev on 04.11.2015.
 */
public class FlatSbsRenderer implements GLSurfaceView.Renderer {

    public interface Callback {

        void glSurfaceCreated(SurfaceTexture surfaceTexture);

    }

    private static final String VERTEX_SHADER =
            "attribute vec2 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "  vec4 color = texture2D(sTexture, vTexCoord);\n" +
            "  gl_FragColor = color;\n" +
            "}\n";

    private static final float VERTICES[] = {
            // X, Y, U, V
            -1.0f,  1.0f, 0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 1.0f,
             0.0f,  1.0f, 1.0f, 0.0f,
             0.0f, -1.0f, 1.0f, 1.0f,
             0.0f,  1.0f, 0.0f, 0.0f,
             0.0f, -1.0f, 0.0f, 1.0f,
             1.0f,  1.0f, 1.0f, 0.0f,
             1.0f, -1.0f, 1.0f, 1.0f };
    private static final short INDICES[] = {
            0, 1, 2,
            2, 1, 3,
            4, 5, 6,
            6, 5, 7 };
    private static final int VERTEX_POSITION_OFFSET = 0;
    private static final int VERTEX_TEXCOORD_OFFSET = 2;
    private static final int VERTEX_POSITION_SIZE = 2;
    private static final int VERTEX_TEXCOORD_SIZE = 2;
    private static final int COORDS_PER_VERTEX = VERTEX_POSITION_SIZE + VERTEX_TEXCOORD_SIZE;
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private GLSurfaceView glSurfaceView;
    private Callback callback;
    private SurfaceTexture surfaceTexture;
    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;
    private int program;
    private int textureId;
    private int positionHandle;
    private int textureHandle;

    public FlatSbsRenderer(GLSurfaceView glSurfaceView) {
        this.glSurfaceView = glSurfaceView;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);

        indexBuffer = ByteBuffer.allocateDirect(INDICES.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        indexBuffer.put(INDICES);
        indexBuffer.position(0);

        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, VERTEX_SHADER);
        GLES20.glCompileShader(vertexShader);

        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, FRAGMENT_SHADER);
        GLES20.glCompileShader(fragmentShader);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        textureHandle = GLES20.glGetAttribLocation(program, "aTexCoord");

        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                glSurfaceView.requestRender();
            }
        });

        callback.glSurfaceCreated(surfaceTexture);
    }

    @Override
    public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);

        GLES20.glEnableVertexAttribArray(positionHandle);
        vertexBuffer.position(VERTEX_POSITION_OFFSET);
        GLES20.glVertexAttribPointer(positionHandle,
                VERTEX_POSITION_SIZE, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, vertexBuffer);

        GLES20.glEnableVertexAttribArray(textureHandle);
        vertexBuffer.position(VERTEX_TEXCOORD_OFFSET);
        GLES20.glVertexAttribPointer(textureHandle,
                VERTEX_TEXCOORD_SIZE, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, vertexBuffer);

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, INDICES.length,
                GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureHandle);
    }

}
