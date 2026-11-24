package com.example.hotel;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 3D графика (OpenGL ES 2.0): қонақүй ғимараты. Әр нөмір жеке куб, қабаттар
 * бірінің үстіне бірі қойылған. Брондалған нөмір көк, бос нөмір сұр.
 * Шейдер жарықты қарапайым бағытталған жарық моделімен есептейді.
 */
public class Hotel3DRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "HOTEL_3D";

    private static final String VERTEX =
            "uniform mat4 uMVP;\n"
            + "uniform mat4 uRot;\n"
            + "attribute vec4 aPos;\n"
            + "attribute vec3 aNormal;\n"
            + "varying float vLight;\n"
            + "void main() {\n"
            + "  vec3 n = normalize((uRot * vec4(aNormal, 0.0)).xyz);\n"
            + "  vLight = 0.38 + 0.62 * max(dot(n, normalize(vec3(0.5, 0.9, 0.7))), 0.0);\n"
            + "  gl_Position = uMVP * aPos;\n"
            + "}\n";

    private static final String FRAGMENT =
            "precision mediump float;\n"
            + "uniform vec4 uColor;\n"
            + "varying float vLight;\n"
            + "void main() {\n"
            + "  gl_FragColor = vec4(uColor.rgb * vLight, uColor.a);\n"
            + "}\n";

    /** Бір нөмір: орны және түсі. */
    public static class Box {
        final float x, y, z;
        final float[] color;

        Box(float x, float y, float z, float[] color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
        }
    }

    private final List<Box> boxes = new ArrayList<>();
    private float centerY = 0f;

    /** Айналу бұрыштары (градус). UI ағыны жазады, GL ағыны оқиды. */
    public volatile float angleY = 30f;
    public volatile float angleX = 18f;
    public volatile boolean auto = true;
    public volatile float zoom = 1f;

    private FloatBuffer vertexBuffer;
    private int program;
    private int aPos, aNormal, uMVP, uRot, uColor;

    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] rot = new float[16];
    private final float[] tmp = new float[16];
    private final float[] mv = new float[16];
    private final float[] mvp = new float[16];

    private long last = System.nanoTime();

    public void setBoxes(List<Box> list, float centerY) {
        synchronized (boxes) {
            boxes.clear();
            boxes.addAll(list);
            this.centerY = centerY;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.02f, 0.03f, 0.06f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        program = link(compile(GLES20.GL_VERTEX_SHADER, VERTEX),
                compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT));
        aPos = GLES20.glGetAttribLocation(program, "aPos");
        aNormal = GLES20.glGetAttribLocation(program, "aNormal");
        uMVP = GLES20.glGetUniformLocation(program, "uMVP");
        uRot = GLES20.glGetUniformLocation(program, "uRot");
        uColor = GLES20.glGetUniformLocation(program, "uColor");

        vertexBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(CUBE).position(0);
        Log.i(TAG, "OpenGL ES: " + GLES20.glGetString(GLES20.GL_VERSION));
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / Math.max(1, height);
        Matrix.frustumM(proj, 0, -ratio * 0.6f, ratio * 0.6f, -0.6f, 0.6f, 1.5f, 30f);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        long now = System.nanoTime();
        float dt = (now - last) / 1e9f;
        last = now;
        if (auto) {
            angleY += 18f * dt;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        float dist = 9.5f / zoom;
        Matrix.setLookAtM(view, 0, 0f, 1.2f, dist, 0f, 0f, 0f, 0f, 1f, 0f);

        // Бүкіл ғимаратты бірге айналдыру
        float[] world = new float[16];
        Matrix.setIdentityM(world, 0);
        Matrix.rotateM(world, 0, angleX, 1f, 0f, 0f);
        Matrix.rotateM(world, 0, angleY, 0f, 1f, 0f);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, vertexBuffer);
        vertexBuffer.position(3);
        GLES20.glEnableVertexAttribArray(aNormal);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 24, vertexBuffer);

        synchronized (boxes) {
            for (Box b : boxes) {
                Matrix.setIdentityM(model, 0);
                Matrix.translateM(model, 0, b.x, b.y - centerY, b.z);
                Matrix.scaleM(model, 0, 0.86f, 0.74f, 0.86f);

                Matrix.multiplyMM(tmp, 0, world, 0, model, 0);
                Matrix.multiplyMM(mv, 0, view, 0, tmp, 0);
                Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0);

                System.arraycopy(world, 0, rot, 0, 16);
                GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0);
                GLES20.glUniformMatrix4fv(uRot, 1, false, rot, 0);
                GLES20.glUniform4fv(uColor, 1, b.color, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
            }
        }
    }

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            Log.e(TAG, "Шейдер құрастырылмады: " + GLES20.glGetShaderInfoLog(s));
        }
        return s;
    }

    private static int link(int vs, int fs) {
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);
        return p;
    }

    /** Бірлік куб: 36 төбе, әрқайсысы позиция (3) және нормаль (3). */
    private static final float[] CUBE = {
            // +Z
            -.5f, -.5f, .5f, 0, 0, 1, .5f, -.5f, .5f, 0, 0, 1, .5f, .5f, .5f, 0, 0, 1,
            -.5f, -.5f, .5f, 0, 0, 1, .5f, .5f, .5f, 0, 0, 1, -.5f, .5f, .5f, 0, 0, 1,
            // -Z
            .5f, -.5f, -.5f, 0, 0, -1, -.5f, -.5f, -.5f, 0, 0, -1, -.5f, .5f, -.5f, 0, 0, -1,
            .5f, -.5f, -.5f, 0, 0, -1, -.5f, .5f, -.5f, 0, 0, -1, .5f, .5f, -.5f, 0, 0, -1,
            // +X
            .5f, -.5f, .5f, 1, 0, 0, .5f, -.5f, -.5f, 1, 0, 0, .5f, .5f, -.5f, 1, 0, 0,
            .5f, -.5f, .5f, 1, 0, 0, .5f, .5f, -.5f, 1, 0, 0, .5f, .5f, .5f, 1, 0, 0,
            // -X
            -.5f, -.5f, -.5f, -1, 0, 0, -.5f, -.5f, .5f, -1, 0, 0, -.5f, .5f, .5f, -1, 0, 0,
            -.5f, -.5f, -.5f, -1, 0, 0, -.5f, .5f, .5f, -1, 0, 0, -.5f, .5f, -.5f, -1, 0, 0,
            // +Y
            -.5f, .5f, .5f, 0, 1, 0, .5f, .5f, .5f, 0, 1, 0, .5f, .5f, -.5f, 0, 1, 0,
            -.5f, .5f, .5f, 0, 1, 0, .5f, .5f, -.5f, 0, 1, 0, -.5f, .5f, -.5f, 0, 1, 0,
            // -Y
            -.5f, -.5f, -.5f, 0, -1, 0, .5f, -.5f, -.5f, 0, -1, 0, .5f, -.5f, .5f, 0, -1, 0,
            -.5f, -.5f, -.5f, 0, -1, 0, .5f, -.5f, .5f, 0, -1, 0, -.5f, -.5f, .5f, 0, -1, 0,
    };
}
