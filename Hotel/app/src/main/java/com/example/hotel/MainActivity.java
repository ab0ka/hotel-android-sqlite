package com.example.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Негізгі экран: қонақүй нөмірлерінің тізімі, іздеу режимдері және сұрыптау.
 *
 * Іздеудің үш режимі тапсырманың а, ә, б тармақтарына сәйкес келеді:
 *   а) санат бойынша;
 *   ә) қабат және ең жоғары баға бойынша;
 *   б) бағасы орташадан арзан нөмірлер.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_UI";

    private DbHelper db;
    private ListView listView;
    private RoomAdapter adapter;
    private final List<Room> data = new ArrayList<>();

    private RadioGroup radioMode;
    private EditText editFirst, editSecond;
    private TextView txtHint, txtResult;
    private Spinner spinnerSort, spinnerCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Қонақүй");
        toolbar.setSubtitle("Нөмірлер мен брондаулар");
        setSupportActionBar(toolbar);

        db = new DbHelper(this);

        listView = findViewById(R.id.listRooms);
        View header = getLayoutInflater().inflate(R.layout.header_filter, listView, false);
        listView.addHeaderView(header, null, false);

        radioMode = header.findViewById(R.id.radioMode);
        editFirst = header.findViewById(R.id.editFirst);
        editSecond = header.findViewById(R.id.editSecond);
        txtHint = header.findViewById(R.id.txtHint);
        txtResult = header.findViewById(R.id.txtResult);
        spinnerSort = header.findViewById(R.id.spinnerSort);
        spinnerCategory = header.findViewById(R.id.spinnerCategory);
        Button btnFind = header.findViewById(R.id.btnFind);
        Button btnAll = header.findViewById(R.id.btnAll);

        adapter = new RoomAdapter(this, data);
        listView.setAdapter(adapter);

        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, DbHelper.CATEGORIES));

        spinnerSort.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, DbHelper.SORT_NAMES));
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                db.setSort(position);
                showAll();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        radioMode.setOnCheckedChangeListener((group, checkedId) -> updateHint());
        btnFind.setOnClickListener(v -> find());
        btnAll.setOnClickListener(v -> showAll());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Room r = (Room) parent.getItemAtPosition(position);
            if (r != null) {
                openRoom(r);
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Room r = (Room) parent.getItemAtPosition(position);
            if (r != null) {
                editOrDelete(r);
                return true;
            }
            return false;
        });

        updateHint();
        showAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showAll();
    }

    /** Таңдалған режимге қарай енгізу өрістерінің түсіндірмесін ауыстырады. */
    private void updateHint() {
        int id = radioMode.getCheckedRadioButtonId();
        if (id == R.id.radioCategory) {
            txtHint.setText("а) Тізімнен нөмір санатын таңдаңыз");
            spinnerCategory.setVisibility(View.VISIBLE);
            editFirst.setVisibility(View.GONE);
            editSecond.setVisibility(View.GONE);
        } else if (id == R.id.radioFloor) {
            txtHint.setText("ә) Қабатты және ең жоғары бағаны жазыңыз");
            spinnerCategory.setVisibility(View.GONE);
            editFirst.setVisibility(View.VISIBLE);
            editSecond.setVisibility(View.VISIBLE);
        } else {
            txtHint.setText("б) Бағасы қонақүй бойынша орташадан арзан нөмірлер");
            spinnerCategory.setVisibility(View.GONE);
            editFirst.setVisibility(View.GONE);
            editSecond.setVisibility(View.GONE);
        }
    }

    /** Барлық нөмірлерді көрсету. */
    private void showAll() {
        List<Room> list = db.all();
        apply(list, "Барлығы: " + list.size() + " нөмір · " + db.sortName());
    }

    /** Таңдалған режим бойынша іздеу. */
    private void find() {
        int id = radioMode.getCheckedRadioButtonId();
        String first = editFirst.getText().toString().trim();
        String second = editSecond.getText().toString().trim();

        if (id == R.id.radioCategory) {
            String category = (String) spinnerCategory.getSelectedItem();
            List<Room> list = db.byCategory(category);
            apply(list, "а) «" + category + "» санаты: " + list.size() + " нөмір");

        } else if (id == R.id.radioFloor) {
            if (first.isEmpty() || second.isEmpty()) {
                Toast.makeText(this, "Қабат пен бағаны жазыңыз", Toast.LENGTH_SHORT).show();
                return;
            }
            int floor;
            int price;
            try {
                floor = Integer.parseInt(first);
                price = Integer.parseInt(second);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Сан дұрыс енгізілмеді: " + first + " / " + second);
                Toast.makeText(this, "Қабат пен бағаны сан түрінде жазыңыз",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            List<Room> list = db.byFloorAndPrice(floor, price);
            apply(list, "ә) " + floor + "-қабат, бағасы <= "
                    + RoomAdapter.money(price) + " тг: " + list.size() + " нөмір");

        } else {
            int avg = db.averagePrice();
            List<Room> list = db.cheaperThanAverage();
            apply(list, "б) орташа баға " + RoomAdapter.money(avg)
                    + " тг, одан арзан: " + list.size() + " нөмір");
        }
    }

    /** Тізімді жаңартып, қорытынды жазуды шығарады. */
    private void apply(List<Room> list, String result) {
        data.clear();
        data.addAll(list);
        adapter.notifyDataSetChanged();
        txtResult.setText(result);
        Log.i(TAG, "Нәтиже: " + result);
        if (list.isEmpty()) {
            Toast.makeText(this, "Сұранысқа сәйкес нөмір табылмады",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Нөмір туралы толық экранға өту. */
    private void openRoom(Room r) {
        Intent intent = new Intent(this, RoomActivity.class);
        intent.putExtra(RoomActivity.EXTRA_ID, r.getId());
        startActivity(intent);
    }

    /** Ұзақ басқанда: өзгерту немесе жою. */
    private void editOrDelete(Room r) {
        String[] actions = {"Өзгерту (UPDATE)", "Жою (DELETE)"};
        new AlertDialog.Builder(this)
                .setTitle("№" + r.getNumber())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(r);
                    } else {
                        confirmDelete(r);
                    }
                })
                .setNegativeButton("Болдырмау", null)
                .show();
    }

    private void showEditDialog(Room r) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_room, null);
        EditText editPrice = view.findViewById(R.id.editPrice);
        EditText editBeds = view.findViewById(R.id.editBeds);
        editPrice.setText(String.valueOf(r.getPrice()));
        editBeds.setText(String.valueOf(r.getBeds()));

        new AlertDialog.Builder(this)
                .setTitle("Өзгерту: №" + r.getNumber())
                .setView(view)
                .setPositiveButton("Сақтау", (dialog, which) -> {
                    try {
                        int price = Integer.parseInt(editPrice.getText().toString().trim());
                        int beds = Integer.parseInt(editBeds.getText().toString().trim());
                        if (price <= 0 || beds <= 0) {
                            Toast.makeText(this, "Мәндер нөлден үлкен болуы керек",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int rows = db.updateRoom(r.getId(), price, beds);
                        Toast.makeText(this, rows + " жол жаңартылды",
                                Toast.LENGTH_SHORT).show();
                        showAll();
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Өзгерту кезінде сан дұрыс емес", e);
                        Toast.makeText(this, "Сандарды дұрыс жазыңыз",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Болдырмау", null)
                .show();
    }

    private void confirmDelete(Room r) {
        new AlertDialog.Builder(this)
                .setTitle("Жою")
                .setMessage("№" + r.getNumber() + " нөмірі және оның барлық "
                        + "брондаулары жойылады. Жалғастырамыз ба?")
                .setPositiveButton("Жою", (dialog, which) -> {
                    int rows = db.deleteRoom(r.getId());
                    Toast.makeText(this, rows + " нөмір жойылды",
                            Toast.LENGTH_SHORT).show();
                    showAll();
                })
                .setNegativeButton("Болдырмау", null)
                .show();
    }

    // ------------------------------------------------------------------
    // Мәзір
    // ------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add) {
            startActivity(new Intent(this, AddRoomActivity.class));
            return true;

        } else if (id == R.id.action_stats) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;

        } else if (id == R.id.action_delete_old) {
            deleteOldDialog();
            return true;

        } else if (id == R.id.action_reset) {
            new AlertDialog.Builder(this)
                    .setTitle("Бастапқы мәліметтер")
                    .setMessage("База бастапқы 16 нөмір және 12 брондау күйіне "
                            + "келтіріледі. Жалғастырамыз ба?")
                    .setPositiveButton("Келтіру", (dialog, which) -> {
                        db.resetData();
                        Toast.makeText(this, "База қалпына келтірілді",
                                Toast.LENGTH_SHORT).show();
                        showAll();
                    })
                    .setNegativeButton("Болдырмау", null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteOldDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_delete_old, null);
        EditText editYear = view.findViewById(R.id.editYear);

        new AlertDialog.Builder(this)
                .setTitle("Ескі нөмірлерді жою")
                .setView(view)
                .setPositiveButton("Жою", (dialog, which) -> {
                    try {
                        int year = Integer.parseInt(editYear.getText().toString().trim());
                        int rows = db.deleteOlderThan(year);
                        Toast.makeText(this, rows + " нөмір жойылды",
                                Toast.LENGTH_LONG).show();
                        showAll();
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Жыл дұрыс енгізілмеді", e);
                        Toast.makeText(this, "Жылды сан түрінде жазыңыз",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Болдырмау", null)
                .show();
    }
}
