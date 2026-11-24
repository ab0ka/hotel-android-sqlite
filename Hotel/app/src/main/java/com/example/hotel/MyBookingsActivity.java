package com.example.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Клиенттің өз брондаулары (JOIN: bookings + rooms). Нөмірді басу ашады,
 * ұзақ басу брондауды болдырмайды.
 */
public class MyBookingsActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_MY";

    private DbHelper db;
    private User me;
    private BookingAdapter adapter;
    private final List<Booking> data = new ArrayList<>();
    private TextView txtSummary, txtEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.appbar));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Қонақүй");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        db = new DbHelper(this);
        me = Session.current(this);
        if (me == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        txtSummary = findViewById(R.id.txtSummary);
        txtEmpty = findViewById(R.id.txtEmptyMy);
        ListView list = findViewById(R.id.listMy);
        adapter = new BookingAdapter(this, data);
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, RoomActivity.class);
            intent.putExtra(RoomActivity.EXTRA_ID, data.get(position).getRoomId());
            startActivity(intent);
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            cancel(data.get(position));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (me != null) {
            load();
        }
    }

    private void load() {
        data.clear();
        data.addAll(db.bookingsOfUser(me.getId()));
        adapter.notifyDataSetChanged();
        int sum = 0;
        int nights = 0;
        for (Booking b : data) {
            sum += b.getCost();
            nights += b.getNights();
        }
        txtSummary.setText(data.size() + " брондау, " + nights + " тәулік, барлығы "
                + RoomAdapter.money(sum) + " тг");
        txtEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        Log.i(TAG, me.getUsername() + ": " + data.size() + " брондау, " + sum + " тг");
    }

    private void cancel(Booking b) {
        new AlertDialog.Builder(this)
                .setTitle("Брондауды болдырмау")
                .setMessage("№" + b.getRoomNumber() + " нөміріндегі брондау жойылады. "
                        + "Жалғастырамыз ба?")
                .setPositiveButton("Болдырмау", (dialog, which) -> {
                    db.deleteBooking(b.getId());
                    Toast.makeText(this, "Брондау жойылды", Toast.LENGTH_SHORT).show();
                    load();
                })
                .setNegativeButton("Жабу", null)
                .show();
    }
}
