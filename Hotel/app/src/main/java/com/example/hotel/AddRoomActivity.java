package com.example.hotel;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Жаңа нөмір қосу экраны. Мәліметтер базаға жазылғанға дейін
 * барлық өрістер тексеріледі (валидация).
 */
public class AddRoomActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_ADD";

    private DbHelper db;

    private EditText editNumber, editFloor, editBeds, editPrice, editArea, editYear;
    private Spinner spinnerCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_room);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.appbar));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Нөмір қосу");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        db = new DbHelper(this);

        editNumber = findViewById(R.id.editNumber);
        editFloor = findViewById(R.id.editFloor);
        editBeds = findViewById(R.id.editBeds);
        editPrice = findViewById(R.id.editPrice);
        editArea = findViewById(R.id.editArea);
        editYear = findViewById(R.id.editYear);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, DbHelper.CATEGORIES));

        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);
        btnSave.setOnClickListener(v -> save());
        btnCancel.setOnClickListener(v -> finish());
    }

    /** Өрістерді тексеріп, базаға жаңа жол қосады. */
    private void save() {
        String number = editNumber.getText().toString().trim();
        String category = (String) spinnerCategory.getSelectedItem();
        String floorText = editFloor.getText().toString().trim();
        String bedsText = editBeds.getText().toString().trim();
        String priceText = editPrice.getText().toString().trim();
        String areaText = editArea.getText().toString().trim();
        String yearText = editYear.getText().toString().trim();

        if (number.isEmpty() || floorText.isEmpty() || bedsText.isEmpty()
                || priceText.isEmpty() || areaText.isEmpty() || yearText.isEmpty()) {
            fail("Барлық өрістерді толтырыңыз");
            return;
        }

        int floor;
        int beds;
        int price;
        int area;
        int year;
        try {
            floor = Integer.parseInt(floorText);
            beds = Integer.parseInt(bedsText);
            price = Integer.parseInt(priceText);
            area = Integer.parseInt(areaText);
            year = Integer.parseInt(yearText);
        } catch (NumberFormatException e) {
            fail("Сандық өрістерді сан түрінде жазыңыз");
            return;
        }

        if (floor < 1 || floor > 20) {
            fail("Қабат 1-20 аралығында болуы керек");
            return;
        }
        if (beds < 1 || beds > 8) {
            fail("Төсек саны 1-8 аралығында болуы керек");
            return;
        }
        if (price <= 0) {
            fail("Баға нөлден үлкен болуы керек");
            return;
        }
        if (area < 8 || area > 200) {
            fail("Аудан 8-200 м² аралығында болуы керек");
            return;
        }
        int current = DbHelper.currentYear();
        if (year < 1990 || year > current) {
            fail("Жөндеу жылы 1990-" + current + " аралығында болуы керек");
            return;
        }
        if (db.numberExists(number)) {
            fail("Мұндай нөмір базада бар: " + number);
            return;
        }

        long id = db.insertRoom(number, category, floor, beds, price, area, year);
        if (id == -1) {
            fail("Нөмір қосылмады, базадағы қате");
            return;
        }

        Toast.makeText(this, "Жаңа нөмір қосылды, _id = " + id,
                Toast.LENGTH_LONG).show();
        Log.i(TAG, "Қосылды: №" + number + " (" + category + "), _id = " + id);
        finish();
    }

    /** Қатені экранға да, LogCat-қа да шығарады. */
    private void fail(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Валидация қатесі: " + message);
    }
}
