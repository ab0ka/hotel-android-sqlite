package com.example.hotel;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Категории номеров как полоски-образцы: хлопковые блоттеры с острым кончиком,
 * подвешены к рейке; длина зависит от числа номеров, на кончике пятно цвета категории.
 * Название напечатано вдоль полоски, число под кончиком. Полоски раскрываются один раз.
 */
public class StripChartView extends View {

    private final Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stain = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint name = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint count = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private String[] labels = new String[0];
    private int[] values = new int[0];
    private int[] stains = new int[0];
    private float progress = 1f;
    private final float d;

    public StripChartView(Context context) {
        this(context, null);
    }

    public StripChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        d = getResources().getDisplayMetrics().density;
        card.setColor(getContext().getColor(R.color.ink));
        card.setShadowLayer(8 * d, 0, 3 * d, 0x66000000);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(d);
        edge.setColor(getContext().getColor(R.color.muted));
        rail.setColor(getContext().getColor(R.color.muted));
        rail.setStrokeWidth(1.5f * d);
        name.setColor(getContext().getColor(R.color.paper));
        name.setTextSize(13 * d);
        name.setTextAlign(Paint.Align.LEFT);
        count.setColor(getContext().getColor(R.color.accent_2));
        count.setTextSize(20 * d);
        count.setTextAlign(Paint.Align.CENTER);
        count.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    }

    public void setData(String[] labels, int[] values, int[] stains) {
        this.labels = labels;
        this.values = values;
        this.stains = stains;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            sb.append(labels[i]).append(": ").append(values[i]).append(". ");
        }
        setContentDescription(sb.toString());
        requestLayout();
        if (Ui.animationsOn(getContext())) {
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(900);
            a.setInterpolator(new DecelerateInterpolator(1.6f));
            a.addUpdateListener(v -> {
                progress = (float) v.getAnimatedValue();
                invalidate();
            });
            a.start();
        } else {
            progress = 1f;
            invalidate();
        }
    }

    private float lengthFor(int v) {
        return (104 + 24 * v) * d;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int max = 1;
        for (int v : values) {
            max = Math.max(max, v);
        }
        int h = (int) (lengthFor(max) + 16 * d + 14 * d + 20 * d + 40 * d);
        setMeasuredDimension(resolveSize(0, widthSpec), h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = values.length;
        if (n == 0) {
            return;
        }
        float railY = 16 * d;
        float pad = 16 * d;
        canvas.drawLine(pad, railY, getWidth() - pad, railY, rail);

        float stripW = 46 * d;
        float tip = 16 * d;
        float usable = getWidth() - 2 * pad;
        float gap = n > 1 ? Math.min(30 * d, (usable - n * stripW) / (n - 1)) : 0;
        float total = n * stripW + (n - 1) * gap;
        float x = pad + (usable - total) / 2f;

        for (int i = 0; i < n; i++) {
            float len = lengthFor(values[i]) * progress;
            float top = railY;
            float bottom = top + len;
            float l = x;
            float r = x + stripW;
            float cx = (l + r) / 2f;

            path.reset();
            path.moveTo(l, top);
            path.lineTo(r, top);
            path.lineTo(r, bottom - tip);
            path.lineTo(cx, bottom);
            path.lineTo(l, bottom - tip);
            path.close();
            canvas.drawPath(path, card);
            canvas.drawPath(path, edge);

            // пятно подтекает снизу вверх от кончика
            canvas.save();
            canvas.clipPath(path);
            stain.setShader(new LinearGradient(0, bottom, 0, bottom - Math.max(1f, len * 0.45f),
                    (stains[i] & 0x00FFFFFF) | 0xDD000000, stains[i] & 0x00FFFFFF,
                    Shader.TileMode.CLAMP));
            canvas.drawRect(l, top, r, bottom, stain);
            canvas.restore();

            // название вдоль полоски
            if (progress > 0.6f) {
                canvas.save();
                canvas.translate(cx + name.getTextSize() * 0.35f, top + 12 * d);
                canvas.rotate(90);
                name.setAlpha((int) (255 * Math.min(1f, (progress - 0.6f) / 0.4f)));
                canvas.drawText(labels[i], 0, 0, name);
                canvas.restore();
            }

            // число под кончиком
            count.setAlpha((int) (255 * progress));
            canvas.drawText(String.valueOf(values[i]), cx, bottom + 24 * d, count);

            x += stripW + gap;
        }
    }
}
