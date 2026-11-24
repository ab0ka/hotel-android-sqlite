package com.example.hotel;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

/**
 * Статистика экраны: агрегат функциялар (COUNT, AVG, MIN, MAX, SUM),
 * GROUP BY бойынша санаттар кесімі және JOIN арқылы нөмірлердің табысы.
 * Санаттар полоска-үлгілер түрінде, қалғаны сызылған кесте түрінде көрсетіледі.
 */
public class StatsActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_STATS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.appbar));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Статистика");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        DbHelper db = new DbHelper(this);

        int rooms = db.count();
        int bookings = db.bookingCount();
        int busy = db.busyRooms();
        int load = rooms == 0 ? 0 : Math.round(busy * 100f / rooms);
        int revenue = db.totalRevenue();

        Ui.countUp(findViewById(R.id.kpiRevenue), revenue, v -> RoomAdapter.money(v) + " тг");
        ((TextView) findViewById(R.id.kpiSub)).setText("жалпы табыс: " + bookings
                + " брондау, " + db.totalNights() + " тәулік");

        LinearLayout figures = findViewById(R.id.containerFigures);
        addPair(figures, "Нөмір саны", String.valueOf(rooms));
        addPair(figures, "Брондалған нөмір", busy + " / " + rooms + ", " + load + " %");
        addPair(figures, "Ең арзан, тг", RoomAdapter.money(db.minPrice()));
        addPair(figures, "Орташа баға, тг", RoomAdapter.money(db.averagePrice()));
        addPair(figures, "Ең қымбат, тг", RoomAdapter.money(db.maxPrice()));

        List<DbHelper.StatRow> cats = db.categoryRows();
        ((TextView) findViewById(R.id.sqlCategories)).setText(
                "SELECT category, COUNT(*), AVG(price), AVG(area), MIN(price)\n"
                        + "FROM rooms GROUP BY category\n"
                        + "HAVING COUNT(*) > 0");
        fillStrips(cats);
        fillBars((LinearLayout) findViewById(R.id.containerCategories), cats, true);

        ((TextView) findViewById(R.id.sqlRevenue)).setText(
                "SELECT r.number, COUNT(b._id), SUM(b.nights), SUM(b.cost)\n"
                        + "FROM rooms r LEFT JOIN bookings b\n"
                        + "  ON b.room_id = r._id\n"
                        + "GROUP BY r._id ORDER BY SUM(b.cost) DESC");
        fillBars((LinearLayout) findViewById(R.id.containerRevenue), db.revenueRows(), false);

        Log.i(TAG, "Статистика: " + rooms + " нөмір, " + bookings + " брондау, табыс "
                + revenue + " тг, ең табысты: " + db.topRoom());
    }

    private void addPair(LinearLayout container, String key, String value) {
        View v = LayoutInflater.from(this).inflate(R.layout.row_pair, container, false);
        ((TextView) v.findViewById(R.id.txtKey)).setText(key);
        ((TextView) v.findViewById(R.id.txtVal)).setText(value);
        container.addView(v);
    }

    /** Санаттар: полоска-үлгілер, ұзындығы нөмір санына тәуелді. */
    private void fillStrips(List<DbHelper.StatRow> rows) {
        String[] labels = new String[rows.size()];
        int[] values = new int[rows.size()];
        int[] stains = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            labels[i] = rows.get(i).label;
            values[i] = (int) rows.get(i).value;
            stains[i] = RoomAdapter.categoryColor(this, rows.get(i).label);
        }
        ((StripChartView) findViewById(R.id.stripCategories)).setData(labels, values, stains);
    }

    /** Жолдар тізімінен сызылған кесте жолдарын құрады. */
    private void fillBars(LinearLayout container, List<DbHelper.StatRow> rows,
                          boolean byCategory) {
        container.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Мәлімет жоқ");
            container.addView(empty);
            return;
        }

        long max = 1;
        for (DbHelper.StatRow row : rows) {
            max = Math.max(max, row.value);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < rows.size(); i++) {
            DbHelper.StatRow row = rows.get(i);
            View v = inflater.inflate(R.layout.row_bar, container, false);
            ((TextView) v.findViewById(R.id.txtLabel)).setText(row.label);
            ((TextView) v.findViewById(R.id.txtValue)).setText(byCategory
                    ? row.value + " нөмір"
                    : RoomAdapter.money((int) row.value) + " тг");
            ((TextView) v.findViewById(R.id.txtSub)).setText(row.sub);

            LinearProgressIndicator bar = v.findViewById(R.id.bar);
            bar.setProgressCompat((int) (row.value * 1000 / max), Ui.animationsOn(this));
            container.addView(v, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            if (i < rows.size() - 1) {
                View rule = new View(this);
                rule.setBackgroundResource(R.drawable.bg_rule);
                container.addView(rule, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
            }
        }
    }
}
