package com.example.hotel;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Бір нөмір туралы толық мәлімет: сипаттамасы, брондаулар тізімі
 * және жаңа брондау қосу мүмкіндігі.
 */
public class RoomActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_ROOM";

    public static final String EXTRA_ID = "room_id";

    private DbHelper db;
    private int roomId;
    private Room room;

    private TextView txtTitle, txtDetails, txtTotal;
    private ListView listBookings;
    private BookingAdapter adapter;
    private final List<Booking> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Нөмір туралы");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        db = new DbHelper(this);
        roomId = getIntent().getIntExtra(EXTRA_ID, -1);
        Log.i(TAG, "Ашылған нөмірдің _id = " + roomId);

        txtTitle = findViewById(R.id.txtTitle);
        txtDetails = findViewById(R.id.txtDetails);
        txtTotal = findViewById(R.id.txtTotal);
        listBookings = findViewById(R.id.listBookings);
        Button btnBook = findViewById(R.id.btnBook);

        adapter = new BookingAdapter(this, data);
        listBookings.setAdapter(adapter);

        listBookings.setOnItemLongClickListener((parent, view, position, id) -> {
            Booking b = data.get(position);
            confirmDeleteBooking(b);
            return true;
        });

        btnBook.setOnClickListener(v -> showBookingDialog());

        load();
    }

    /** Нөмір мен оның брондауларын базадан қайта оқиды. */
    private void load() {
        room = db.roomById(roomId);
        if (room == null) {
            Toast.makeText(this, "Нөмір табылмады", Toast.LENGTH_LONG).show();
            Log.e(TAG, "roomById(" + roomId + ") = null");
            finish();
            return;
        }

        txtTitle.setText("№" + room.getNumber() + " · " + room.getCategory());
        txtDetails.setText("Қабаты: " + room.getFloor()
                + "\nТөсек саны: " + room.getBeds()
                + "\nАуданы: " + room.getArea() + " м²"
                + "\nБағасы: " + RoomAdapter.money(room.getPrice()) + " тг/тәулік"
                + "\nБір төсекке: " + RoomAdapter.money(room.pricePerBed()) + " тг"
                + "\nСоңғы жөндеу: " + room.getYearRepair() + " жыл ("
                + room.repairAge(DbHelper.currentYear()) + " жыл бұрын)");

        data.clear();
        data.addAll(db.bookingsOf(roomId));
        adapter.notifyDataSetChanged();

        int nights = 0;
        int sum = 0;
        for (Booking b : data) {
            nights += b.getNights();
            sum += b.getCost();
        }
        txtTotal.setText("Брондау: " + data.size() + " · барлығы " + nights
                + " тәулік · " + RoomAdapter.money(sum) + " тг");
        Log.i(TAG, "№" + room.getNumber() + ": " + data.size() + " брондау, "
                + nights + " тәулік, " + sum + " тг");
    }

    /** Жаңа брондау қосу диалогы. */
    private void showBookingDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_booking, null);
        EditText editGuest = view.findViewById(R.id.editGuest);
        EditText editNights = view.findViewById(R.id.editNights);
        EditText editPersons = view.findViewById(R.id.editPersons);
        TextView txtPrice = view.findViewById(R.id.txtPriceInfo);
        txtPrice.setText("Нөмірдің бағасы: " + RoomAdapter.money(room.getPrice())
                + " тг/тәулік. Құны тәулік санына көбейтіледі.");

        new AlertDialog.Builder(this)
                .setTitle("Брондау қосу")
                .setView(view)
                .setPositiveButton("Қосу", (dialog, which) -> {
                    String guest = editGuest.getText().toString().trim();
                    if (guest.isEmpty()) {
                        Toast.makeText(this, "Қонақтың аты-жөнін жазыңыз",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int nights;
                    int persons;
                    try {
                        nights = Integer.parseInt(editNights.getText().toString().trim());
                        persons = Integer.parseInt(editPersons.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Брондау сандары дұрыс емес", e);
                        Toast.makeText(this, "Тәулік пен адам санын сан түрінде жазыңыз",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (nights <= 0 || nights > 30) {
                        Toast.makeText(this, "Тәулік саны 1-30 аралығында болуы керек",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (persons <= 0 || persons > room.getBeds()) {
                        Toast.makeText(this, "Адам саны төсек санынан аспауы керек ("
                                + room.getBeds() + ")", Toast.LENGTH_LONG).show();
                        Log.w(TAG, "Адам саны артық: " + persons
                                + " > төсек " + room.getBeds());
                        return;
                    }
                    long id = db.addBooking(roomId, guest, nights, persons);
                    if (id == -1) {
                        Toast.makeText(this, "Брондау қосылмады", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Брондау қосылды, құны "
                                + RoomAdapter.money(nights * room.getPrice()) + " тг",
                                Toast.LENGTH_LONG).show();
                        load();
                    }
                })
                .setNegativeButton("Болдырмау", null)
                .show();
    }

    private void confirmDeleteBooking(Booking b) {
        new AlertDialog.Builder(this)
                .setTitle("Брондауды жою")
                .setMessage("«" + b.getGuest() + "» брондауы жойылады. Жалғастырамыз ба?")
                .setPositiveButton("Жою", (dialog, which) -> {
                    db.deleteBooking(b.getId());
                    Toast.makeText(this, "Брондау жойылды", Toast.LENGTH_SHORT).show();
                    load();
                })
                .setNegativeButton("Болдырмау", null)
                .show();
    }
}
