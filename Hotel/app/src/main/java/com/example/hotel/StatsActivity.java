package com.example.hotel;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;
import java.util.Locale;

/**
 * Статистика экраны: агрегат функциялар (COUNT, AVG, MIN, MAX, SUM),
 * GROUP BY бойынша санаттар кесімі және JOIN арқылы нөмірлердің табысы.
 */
public class StatsActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_STATS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Статистика");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        DbHelper db = new DbHelper(this);

        TextView txtGeneral = findViewById(R.id.txtGeneral);
        TextView txtCategories = findViewById(R.id.txtCategories);
        TextView txtRevenue = findViewById(R.id.txtRevenue);

        int rooms = db.count();
        int bookings = db.bookingCount();
        int busy = db.busyRooms();
        double load = rooms == 0 ? 0 : busy * 100.0 / rooms;

        StringBuilder general = new StringBuilder();
        general.append("Нөмір саны: ").append(rooms).append('\n');
        general.append("Брондау саны: ").append(bookings).append('\n');
        general.append("Брондалған нөмір: ").append(busy)
                .append(" (").append(String.format(Locale.getDefault(), "%.0f", load))
                .append("%)").append('\n');
        general.append("Орташа баға: ").append(RoomAdapter.money(db.averagePrice()))
                .append(" тг").append('\n');
        general.append("Ең арзан нөмір: ").append(RoomAdapter.money(db.minPrice()))
                .append(" тг").append('\n');
        general.append("Ең қымбат нөмір: ").append(RoomAdapter.money(db.maxPrice()))
                .append(" тг").append('\n');
        general.append("Барлық тәулік: ").append(db.totalNights()).append('\n');
        general.append("Жалпы табыс: ").append(RoomAdapter.money(db.totalRevenue()))
                .append(" тг");
        txtGeneral.setText(general.toString());

        txtCategories.setText(join(db.statsByCategory()));
        txtRevenue.setText(join(db.revenueByRoom()));

        Log.i(TAG, "Статистика: " + rooms + " нөмір, " + bookings + " брондау, табыс "
                + db.totalRevenue() + " тг, ең табысты: " + db.topRoom());
    }

    /** Жолдар тізімін бір мәтінге біріктіреді. */
    private String join(List<String> rows) {
        if (rows.isEmpty()) {
            return "Мәлімет жоқ";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            sb.append(i + 1).append(". ").append(rows.get(i));
            if (i < rows.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
