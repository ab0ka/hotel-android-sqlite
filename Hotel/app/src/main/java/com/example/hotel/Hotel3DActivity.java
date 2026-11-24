package com.example.hotel;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 3D шахматка: қонақүй ғимараты OpenGL ES 2.0 арқылы салынады. Қабаттар бірінің
 * үстіне бірі, әр қабатта нөмірлер 2 бағанмен. Барлық рөлге қолжетімді.
 */
public class Hotel3DActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_3D";

    private GLSurfaceView glView;
    private Hotel3DRenderer renderer;
    private float lastX, lastY;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Session.current(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_hotel3d);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.appbar));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Қонақүй");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        DbHelper db = new DbHelper(this);
        List<Room> rooms = db.allUnsorted();
        Set<Integer> booked = db.bookedRoomIds();

        float[] blue = {0.04f, 0.52f, 1f, 1f};
        float[] grey = {0.40f, 0.46f, 0.58f, 1f};

        // Әр қабатта нөмірлер 2 бағанмен: x = баған, z = қатар
        List<Hotel3DRenderer.Box> boxes = new ArrayList<>();
        int[] perFloor = new int[32];
        int maxFloor = 1;
        int booking = 0;
        for (Room r : rooms) {
            int f = Math.max(1, Math.min(31, r.getFloor()));
            int i = perFloor[f]++;
            float x = (i % 2) * 1.1f - 0.55f;
            float z = (i / 2) * 1.1f - 0.55f;
            boolean isBooked = booked.contains(r.getId());
            if (isBooked) {
                booking++;
            }
            boxes.add(new Hotel3DRenderer.Box(x, (f - 1) * 0.95f, z, isBooked ? blue : grey));
            maxFloor = Math.max(maxFloor, f);
        }

        renderer = new Hotel3DRenderer();
        renderer.setBoxes(boxes, (maxFloor - 1) * 0.95f / 2f);

        ((TextView) findViewById(R.id.txt3dInfo)).setText(rooms.size() + " нөмір, "
                + booking + " брондалған, " + maxFloor + " қабат.");

        glView = findViewById(R.id.glView);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        GestureDetector gestures = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        renderer.auto = !renderer.auto;
                        Log.i(TAG, "Автоайналу: " + renderer.auto);
                        return true;
                    }
                });

        glView.setOnTouchListener((v, e) -> {
            gestures.onTouchEvent(e);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = e.getX();
                    lastY = e.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    renderer.angleY += (e.getX() - lastX) * 0.5f;
                    renderer.angleX = Math.max(-40f, Math.min(70f,
                            renderer.angleX + (e.getY() - lastY) * 0.3f));
                    lastX = e.getX();
                    lastY = e.getY();
                    break;
                default:
                    break;
            }
            return true;
        });
        Log.i(TAG, "3D сахна: " + boxes.size() + " куб");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glView != null) {
            glView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glView != null) {
            glView.onPause();
        }
    }
}
