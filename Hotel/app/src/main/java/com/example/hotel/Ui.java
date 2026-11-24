package com.example.hotel;

import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Общие помощники интерфейса: отступы под системные панели и анимации. */
public final class Ui {

    private Ui() {
    }

    /**
     * Верхний отступ отдаётся шапке (чтобы цвет шапки уходил под статус-бар),
     * нижний и боковые — корневому контейнеру.
     */
    public static void applyInsets(View root, View appbar) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            appbar.setPadding(0, bars.top, 0, 0);
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Уважаем системную настройку «убрать анимации» (reduced motion). */
    public static boolean animationsOn(Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (Exception e) {
            return true;
        }
    }

    public interface IntFormat {
        String format(int value);
    }

    /** Плавный «счётчик»: число набегает от нуля до значения (смысловая анимация). */
    public static void countUp(TextView view, int target, IntFormat format) {
        if (!animationsOn(view.getContext()) || target <= 0) {
            view.setText(format.format(target));
            return;
        }
        ValueAnimator a = ValueAnimator.ofInt(0, target);
        a.setDuration(750);
        a.setInterpolator(new DecelerateInterpolator(1.6f));
        a.addUpdateListener(v -> view.setText(format.format((int) v.getAnimatedValue())));
        a.start();
    }
}
