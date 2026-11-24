package com.example.hotel;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.IntentCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Бір нөмір туралы толық мәлімет: сипаттамасы, брондаулар тізімі және жаңа
 * брондау қосу. Room объектісі Intent арқылы Serializable түрінде келеді.
 * Әкімші барлық брондауды көреді, клиент тек өзінікін.
 */
public class RoomActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_ROOM";

    public static final String EXTRA_ID = "room_id";
    public static final String EXTRA_ROOM = "room";

    private DbHelper db;
    private User me;
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
        Log.d(TAG, "onCreate()");
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
        me = Session.current(this);
        if (me == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Serializable: бүкіл Room объектісі Intent арқылы берілді
        Room passed = IntentCompat.getSerializableExtra(getIntent(), EXTRA_ROOM, Room.class);
        roomId = passed != null ? passed.getId() : getIntent().getIntExtra(EXTRA_ID, -1);
        Log.i(TAG, "Ашылған нөмірдің _id = " + roomId
                + (passed != null ? " (Serializable: №" + passed.getNumber() + ")" : ""));

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
            if (!me.isAdmin() && b.getUserId() != me.getId()) {
                Toast.makeText(this, "Тек өз брондауыңызды жоюға болады",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            confirmDeleteBooking(b);
            return true;
        });

        btnBook.setOnClickListener(v -> showBookingDialog());

        load();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    /** Нөмір мен оның брондауларын базадан қайта оқиды (рөлге қарай сүзіледі). */
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
        for (Booking b : db.bookingsOf(roomId)) {
            if (me.isAdmin() || b.getUserId() == me.getId()) {
                data.add(b);
            }
        }
        adapter.notifyDataSetChanged();
        txtEmptyBookings.setText(me.isAdmin() ? "Брондаулар әлі жоқ."
                : "Бұл нөмірде сіздің брондауыңыз жоқ.");
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
                + nights + " тәулік, " + sum + " тг, рөлі " + me.getRole());
    }

    private static String fmtDate(int y, int m, int d) {
        return String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
    }

    /** Брондау диалогы: DatePicker, TimePicker, SeekBar, CheckBox, ToggleButton, RadioButton. */
    private void showBookingDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_booking, null);
        EditText editGuest = view.findViewById(R.id.editGuest);
        EditText editPersons = view.findViewById(R.id.editPersons);
        Button btnDate = view.findViewById(R.id.btnDate);
        Button btnTime = view.findViewById(R.id.btnTime);
        SeekBar seek = view.findViewById(R.id.seekNights);
        TextView txtNights = view.findViewById(R.id.txtNights);
        CheckBox chkBreakfast = view.findViewById(R.id.chkBreakfast);
        ToggleButton tglTransfer = view.findViewById(R.id.tglTransfer);
        RadioGroup radioPay = view.findViewById(R.id.radioPay);
        TextView txtPreview = view.findViewById(R.id.txtCostPreview);
        TextView txtPriceInfo = view.findViewById(R.id.txtPriceInfo);

        txtPriceInfo.setText("Нөмір " + room.getNumber() + ", "
                + RoomAdapter.money(room.getPrice()) + " тг тәулігіне, төсек: "
                + room.getBeds());
        if (!me.isAdmin()) {
            editGuest.setText(me.getFullName());
        }

        final Calendar now = Calendar.getInstance();
        final String today = fmtDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        final String[] date = {today};
        final String[] time = {"14:00"};
        btnDate.setText("Күні: " + date[0]);
        btnTime.setText("Уақыт: " + time[0]);

        btnDate.setOnClickListener(v -> new DatePickerDialog(this, (dp, y, m, d) -> {
            date[0] = fmtDate(y, m, d);
            btnDate.setText("Күні: " + date[0]);
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)).show());

        btnTime.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, mi) -> {
            time[0] = String.format(Locale.US, "%02d:%02d", h, mi);
            btnTime.setText("Уақыт: " + time[0]);
        }, 14, 0, true).show());

        Runnable updatePreview = () -> {
            int n = seek.getProgress() + 1;
            txtNights.setText("Тәулік: " + n);
            txtPreview.setText("Құны: " + n + " × " + RoomAdapter.money(room.getPrice())
                    + " = " + RoomAdapter.money(n * room.getPrice()) + " тг");
        };
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                updatePreview.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        updatePreview.run();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Брондау қосу")
                .setView(view)
                .setPositiveButton("Қосу", null)
                .setNegativeButton("Болдырмау", null)
                .create();

        // Қате болса диалог жабылмайды, енгізілген мәліметтер жоғалмайды
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String guest = editGuest.getText().toString().trim();
                    if (guest.isEmpty()) {
                        warn("Қонақтың аты-жөнін жазыңыз");
                        return;
                    }
                    int persons;
                    try {
                        persons = Integer.parseInt(editPersons.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Адам саны дұрыс емес", e);
                        warn("Адам санын сан түрінде жазыңыз");
                        return;
                    }
                    int nights = seek.getProgress() + 1;
                    if (persons <= 0 || persons > room.getBeds()) {
                        Log.w(TAG, "Адам саны артық: " + persons + " > төсек " + room.getBeds());
                        warn("Адам саны төсек санынан аспауы керек (" + room.getBeds() + ")");
                        return;
                    }
                    if (date[0].compareTo(today) < 0) {
                        warn("Келу күні өткен күн болмауы керек");
                        return;
                    }
                    String pay = radioPay.getCheckedRadioButtonId() == R.id.radioCard
                            ? "карта" : "қолма-қол";
                    long id = db.addBooking(roomId, guest, nights, persons, me.getId(),
                            date[0], time[0], chkBreakfast.isChecked(),
                            tglTransfer.isChecked(), pay);
                    if (id == -1) {
                        warn("Брондау қосылмады");
                        return;
                    }
                    dialog.dismiss();
                    confirmTone();
                    Toast.makeText(this, "Брондау қосылды, құны "
                            + RoomAdapter.money(nights * room.getPrice()) + " тг",
                            Toast.LENGTH_LONG).show();
                    load();
                }));
        dialog.show();
    }

    private void warn(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /** Мультимедиа: сәтті брондауда қысқа дыбыс сигналы. */
    private void confirmTone() {
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70);
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 150);
            listBookings.postDelayed(tone::release, 400);
        } catch (RuntimeException e) {
            Log.w(TAG, "Дыбыс қосылмады: " + e.getMessage());
        }
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
