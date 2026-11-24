package com.example.hotel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

/** Статистика бөлімдері үшін ортақ көрініс құрастырушылар. */
final class StatsViews {

    private StatsViews() {
    }

    /** «Атауы ... мәні» жолы. */
    static void addPair(Context ctx, LinearLayout container, String key, String value) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.row_pair, container, false);
        ((TextView) v.findViewById(R.id.txtKey)).setText(key);
        ((TextView) v.findViewById(R.id.txtVal)).setText(value);
        container.addView(v);
    }

    /** Жолдар тізімінен сызылған кесте жолдарын құрады (шкала = үлес). */
    static void fillBars(Context ctx, LinearLayout container, List<DbHelper.StatRow> rows,
                         boolean byCategory) {
        container.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("Мәлімет жоқ");
            container.addView(empty);
            return;
        }

        long max = 1;
        for (DbHelper.StatRow row : rows) {
            max = Math.max(max, row.value);
        }

        LayoutInflater inflater = LayoutInflater.from(ctx);
        for (int i = 0; i < rows.size(); i++) {
            DbHelper.StatRow row = rows.get(i);
            View v = inflater.inflate(R.layout.row_bar, container, false);
            ((TextView) v.findViewById(R.id.txtLabel)).setText(row.label);
            ((TextView) v.findViewById(R.id.txtValue)).setText(byCategory
                    ? row.value + " нөмір"
                    : RoomAdapter.money((int) row.value) + " тг");
            ((TextView) v.findViewById(R.id.txtSub)).setText(row.sub);

            LinearProgressIndicator bar = v.findViewById(R.id.bar);
            bar.setProgressCompat((int) (row.value * 1000 / max), Ui.animationsOn(ctx));
            container.addView(v, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            if (i < rows.size() - 1) {
                View rule = new View(ctx);
                rule.setBackgroundResource(R.drawable.bg_rule);
                container.addView(rule, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
            }
        }
    }
}
