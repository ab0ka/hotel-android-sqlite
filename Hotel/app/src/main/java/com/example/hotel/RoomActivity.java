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

    private TextView txtTitle, txtCategory, txtPrice, txtPerBed, txtFloor, txtBeds,
            txtArea, txtRepair, txtRepairAge, txtTotal, txtEmptyBookings;
    private ListView listBookings;
    private BookingAdapter adapter;
    private final List<Booking> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.appbar));

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
        txtCategory = findViewById(R.id.txtCategory);
        txtPrice = findViewById(R.id.txtPrice);
        txtPerBed = findViewById(R.id.txtPerBed);
        txtFloor = findViewById(R.id.txtFloor);
        txtBeds = findViewById(R.id.txtBeds);
        txtArea = findViewById(R.id.txtArea);
        txtRepair = findViewById(R.id.txtRepair);
        txtRepairAge = findViewById(R.id.txtRepairAge);
        txtEmptyBookings = findViewById(R.id.txtEmptyBookings);
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

        txtTitle.setText("№" + room.getNumber());
        txtCategory.setText(room.getCategory());
        Ui.countUp(txtPrice, room.getPrice(), v -> RoomAdapter.money(v) + " тг");
        txtPerBed.setText("тәулігіне, бір төсекке " + RoomAdapter.money(room.pricePerBed()) + " тг");
        txtFloor.setText(String.valueOf(room.getFloor()));
        txtBeds.setText(String.valueOf(room.getBeds()));
        txtArea.setText(room.getArea() + " м²");
        txtRepair.setText(String.valueOf(room.getYearRepair()));
        txtRepairAge.setText("жөндеу, " + room.repairAge(DbHelper.currentYear()) + " жыл");

        data.clear();
        data.addAll(db.bookingsOf(roomId));
        adapter.notifyDataSetChanged();
        txtEmptyBookings.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);

        int nights = 0;
        int sum = 0;
        for (Booking b : data) {
            nights += b.getNights();
            sum += b.getCost();
        }
        txtTotal.setText(data.size() + " брондау, " + nights
                + " тәулік, барлығы " + RoomAdapter.money(sum) + " тг");
        Log.i(TAG, "№" + room.getNumber() + ": " + data.size() + " брондау, "
                + nights + " тәулік, " + sum + " тг");
    }

    /** Жаңа брондау қосу диалогы. */
    private void showBookingDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_booking, null);
        EditText editGuest = view.findViewById(R.id.editGuest);
        EditText editNights = view.findViewById(R.id.editNights);
        EditText editPersons = view.findViewById(R.id.editPersons);
        TextView txtPriceInfo = view.findViewById(R.id.txtPriceInfo);
        txtPriceInfo.setText("Нөмір " + room.getNumber() + ", "
                + RoomAdapter.money(room.getPrice()) + " тг тәулігіне, төсек: "
                + room.getBeds());
        TextView txtPreview = view.findViewById(R.id.txtCostPreview);
        txtPreview.setText("Құны: 0 тг");
        editNights.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                int n = 0;
                try {
                    n = Integer.parseInt(s.toString().trim());
                } catch (NumberFormatException ignored) {
                    // бос немесе дұрыс емес мән — құны 0
                }
                txtPreview.setText("Құны: " + n + " × " + RoomAdapter.money(room.getPrice())
                        + " = " + RoomAdapter.money(n * room.getPrice()) + " тг");
            }
        });

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
