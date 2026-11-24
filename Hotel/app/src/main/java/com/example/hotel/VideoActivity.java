package com.example.hotel;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

/**
 * Мультимедиа: res/raw ішіндегі бейнені VideoView арқылы ойнату.
 * Бейне жобаның өз экрандарының анимациясы (15 секунд).
 */
public class VideoActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_VIDEO";

    private VideoView videoView;
    private MaterialButton btnPlay;
    private TextView txtPos;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            int pos = videoView.getCurrentPosition() / 1000;
            int dur = videoView.getDuration() / 1000;
            txtPos.setText(pos + " / " + dur + " с");
            handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Session.current(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_video);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.appbar));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Қонақүй");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        videoView = findViewById(R.id.videoView);
        btnPlay = findViewById(R.id.btnPlay);
        txtPos = findViewById(R.id.txtVideoPos);

        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName()
                + "/" + R.raw.hotel_intro));
        videoView.setOnPreparedListener(mp -> {
            Log.i(TAG, "Бейне дайын, ұзақтығы " + mp.getDuration() + " мс");
            videoView.start();
        });
        videoView.setOnCompletionListener(mp -> {
            btnPlay.setText("Қайта ойнату");
            Log.i(TAG, "Бейне аяқталды");
        });

        btnPlay.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPlay.setText("Жалғастыру");
            } else {
                videoView.start();
                btnPlay.setText("Тоқтату");
            }
        });
        findViewById(R.id.btnRestart).setOnClickListener(v -> {
            videoView.seekTo(0);
            videoView.start();
            btnPlay.setText("Тоқтату");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null) handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
            btnPlay.setText("Жалғастыру");
        }
    }
}
