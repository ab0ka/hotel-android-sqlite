package com.example.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Негізгі экран: қонақүй нөмірлерінің тізімі, іздеу режимдері және сұрыптау.
 *
 * Іздеудің үш режимі тапсырманың а, ә, б тармақтарына сәйкес келеді:
 *   а) санат бойынша;
 *   ә) қабат және ең жоғары баға бойынша;
 *   б) бағасы орташадан арзан нөмірлер.
 *
 * Әр сұраныстың нақты SQL мәтіні экранда көрсетіледі.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_UI";

    private static final int MODE_ALL = 0;
    private static final int MODE_CATEGORY = 1;
    private static final int MODE_FLOOR = 2;
    private static final int MODE_AVERAGE = 3;

    private DbHelper db;
    private ListView listView;
    private RoomAdapter adapter;
    private final List<Room> data = new ArrayList<>();

    private TabLayout tabMode;
    private View rowInputs;
    private EditText editFirst, editSecond;
    private TextView txtHint, txtResult, txtSql, txtEmpty;
    private TextView txtLedger, statAvg, statRevenue, statLoad;
    private boolean statsShown;
    private LinearLayout boardFloors;
    private java.util.Set<Integer> shownIds = new java.util.HashSet<>();
    private Spinner spinnerSort, spinnerCategory;

    /** Соңғы сәтті іздеу — экранға оралғанда қайта орындалады. */
    private int lastMode = MODE_ALL;
    private String lastCategory;
    private int lastFloor;
    private int lastPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.appbar));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Қонақүй");
        setSupportActionBar(toolbar);

        db = new DbHelper(this);

        listView = findViewById(R.id.listRooms);
        View header = getLayoutInflater().inflate(R.layout.header_filter, listView, false);
        listView.addHeaderView(header, null, false);

        tabMode = header.findViewById(R.id.tabMode);
        rowInputs = header.findViewById(R.id.rowInputs);
        editFirst = header.findViewById(R.id.editFirst);
        editSecond = header.findViewById(R.id.editSecond);
        txtHint = header.findViewById(R.id.txtHint);
        txtResult = header.findViewById(R.id.txtResult);
        txtSql = header.findViewById(R.id.txtSql);
        txtEmpty = header.findViewById(R.id.txtEmpty);
        txtLedger = header.findViewById(R.id.txtLedger);
        boardFloors = header.findViewById(R.id.boardFloors);
        statAvg = header.findViewById(R.id.statAvg);
        statRevenue = header.findViewById(R.id.statRevenue);
        statLoad = header.findViewById(R.id.statLoad);
        spinnerSort = header.findViewById(R.id.spinnerSort);
        spinnerCategory = header.findViewById(R.id.spinnerCategory);

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
                refresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        tabMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateHint();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        header.findViewById(R.id.btnFind).setOnClickListener(v -> find());
        header.findViewById(R.id.btnAll).setOnClickListener(v -> showAll());
        findViewById(R.id.fabAdd).setOnClickListener(
                v -> startActivity(new Intent(this, AddRoomActivity.class)));

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
        refresh();
    }

    /** Таңдалған режимге қарай енгізу өрістерін көрсетеді немесе жасырады. */
    private void updateHint() {
        int id = tabMode.getSelectedTabPosition();
        if (id == 0) {
            txtHint.setText("а) Тізімнен нөмір санатын таңдаңыз");
            spinnerCategory.setVisibility(View.VISIBLE);
            rowInputs.setVisibility(View.GONE);
        } else if (id == 1) {
            txtHint.setText("ә) Қабатты және ең жоғары бағаны жазыңыз");
            spinnerCategory.setVisibility(View.GONE);
            rowInputs.setVisibility(View.VISIBLE);
        } else {
            txtHint.setText("б) Бағасы қонақүй бойынша орташадан арзан нөмірлер");
            spinnerCategory.setVisibility(View.GONE);
            rowInputs.setVisibility(View.GONE);
        }
    }

    /** Жоғарғы сводканы (нөмір саны, орташа баға, табыс) қайта есептейді. */
    private void updateStats() {
        int rooms = db.count();
        int busy = db.busyRooms();
        int load = rooms == 0 ? 0 : Math.round(busy * 100f / rooms);
        txtLedger.setText(rooms + " нөмір, " + db.bookingCount() + " брондау.");
        statAvg.setText(RoomAdapter.money(db.averagePrice()));
        if (!statsShown) {
            // бірінші көрсетуде сандар «жиналады», кейін бірден жаңарады
            statsShown = true;
            Ui.countUp(statRevenue, db.totalRevenue(), RoomAdapter::money);
            Ui.countUp(statLoad, busy, v -> v + " / " + rooms + ", " + load + " %");
        } else {
            statRevenue.setText(RoomAdapter.money(db.totalRevenue()));
            statLoad.setText(busy + " / " + rooms + ", " + load + " %");
        }
    }

    /** Соңғы іздеуді (немесе толық тізімді) қайта орындайды. */
    private void refresh() {
        switch (lastMode) {
            case MODE_CATEGORY:
                runCategory(lastCategory);
                break;
            case MODE_FLOOR:
                runFloor(lastFloor, lastPrice);
                break;
            case MODE_AVERAGE:
                runAverage();
                break;
            default:
                runAll();
                break;
        }
    }

    /** Барлық нөмірлерді көрсету. */
    private void showAll() {
        lastMode = MODE_ALL;
        runAll();
        animateList();
    }

    /** Результаты «въезжают» по очереди (280 мс, со сдвигом) — отклик на действие. */
    private void animateList() {
        if (!Ui.animationsOn(this)) {
            return;
        }
        android.view.animation.AnimationSet set = new android.view.animation.AnimationSet(true);
        set.addAnimation(new android.view.animation.AlphaAnimation(0f, 1f));
        set.addAnimation(new android.view.animation.TranslateAnimation(0, 0, 70, 0));
        set.setDuration(280);
        set.setInterpolator(new android.view.animation.DecelerateInterpolator());
        android.view.animation.LayoutAnimationController c =
                new android.view.animation.LayoutAnimationController(set, 0.06f);
        listView.setLayoutAnimation(c);
        listView.scheduleLayoutAnimation();
    }

    /**
     * Шахматка отеля: этаж за этажом, каждый номер плиткой. Заполненная плитка: есть
     * брондау; контур: бос. Плитки, не попавшие в текущий поиск, бозарады. Басу ашады.
     */
    private void buildBoard() {
        boardFloors.removeAllViews();
        List<Room> all = db.allUnsorted();
        java.util.Set<Integer> booked = db.bookedRoomIds();
        java.util.TreeMap<Integer, List<Room>> byFloor = new java.util.TreeMap<>(
                java.util.Collections.reverseOrder());
        for (Room r : all) {
            List<Room> l = byFloor.get(r.getFloor());
            if (l == null) {
                l = new ArrayList<>();
                byFloor.put(r.getFloor(), l);
            }
            l.add(r);
        }
        float d = getResources().getDisplayMetrics().density;
        boolean motion = Ui.animationsOn(this);
        int n = 0;
        for (java.util.Map.Entry<Integer, List<Room>> e : byFloor.entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.topMargin = Math.round(6 * d);
            boardFloors.addView(row, rp);

            TextView lab = new TextView(this);
            lab.setText(e.getKey() + "-қабат");
            lab.setTextColor(getColor(R.color.muted));
            lab.setTextSize(13);
            row.addView(lab, new LinearLayout.LayoutParams(Math.round(64 * d),
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            for (Room r : e.getValue()) {
                boolean isBooked = booked.contains(r.getId());
                boolean inResult = shownIds.contains(r.getId());
                TextView tile = new TextView(this);
                tile.setText(r.getNumber());
                tile.setGravity(android.view.Gravity.CENTER);
                tile.setTextSize(18);
                tile.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
                android.graphics.drawable.GradientDrawable g =
                        new android.graphics.drawable.GradientDrawable();
                g.setCornerRadius(Math.round(18 * d));
                if (isBooked) {
                    g.setColor(getColor(R.color.accent));
                    tile.setTextColor(getColor(R.color.accent_ink));
                } else {
                    g.setColor(getColor(R.color.paper_2));
                    g.setStroke(Math.round(1f * d), getColor(R.color.card_edge));
                    tile.setTextColor(getColor(R.color.ink));
                }
                tile.setBackground(g);
                tile.setContentDescription("Нөмір " + r.getNumber() + ", "
                        + (isBooked ? "брондалған" : "бос"));
                final Room room = r;
                tile.setOnClickListener(v -> openRoom(room));
                float target = inResult ? 1f : 0.2f;
                if (motion) {
                    tile.setAlpha(0f);
                    tile.animate().alpha(target).setStartDelay(30L * n).setDuration(260).start();
                } else {
                    tile.setAlpha(target);
                }
                n++;
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                        Math.round(58 * d), 1f);
                tp.setMargins(Math.round(3 * d), 0, Math.round(3 * d), 0);
                row.addView(tile, tp);
            }
        }
    }

    private static String lowerFirst(String t) {
        return t.isEmpty() ? t : Character.toLowerCase(t.charAt(0)) + t.substring(1);
    }

    private String orderSql() {
        return "ORDER BY " + db.sortSql();
    }

    private void runAll() {
        List<Room> list = db.all();
        apply(list, "Барлығы: " + list.size() + " нөмір, " + lowerFirst(db.sortName()),
                "SELECT * FROM rooms\n" + orderSql());
    }

    private void runCategory(String category) {
        List<Room> list = db.byCategory(category);
        apply(list, "а) «" + category + "»: " + list.size() + " нөмір",
                "SELECT * FROM rooms\nWHERE category = '" + category + "'\n" + orderSql());
    }

    private void runFloor(int floor, int price) {
        List<Room> list = db.byFloorAndPrice(floor, price);
        apply(list, "ә) " + floor + "-қабат, ≤ " + RoomAdapter.money(price)
                        + " тг: " + list.size() + " нөмір",
                "SELECT * FROM rooms\nWHERE floor = " + floor + " AND price <= " + price
                        + "\n" + orderSql());
    }

    private void runAverage() {
        int avg = db.averagePrice();
        List<Room> list = db.cheaperThanAverage();
        apply(list, "б) орташа баға " + RoomAdapter.money(avg) + " тг, одан арзан: "
                        + list.size() + " нөмір",
                "SELECT * FROM rooms\nWHERE price < (SELECT AVG(price)\n"
                        + "               FROM rooms)\n" + orderSql());
    }

    /** Таңдалған режим бойынша іздеу. */
    private void find() {
        int id = tabMode.getSelectedTabPosition();

        if (id == 0) {
            lastMode = MODE_CATEGORY;
            lastCategory = (String) spinnerCategory.getSelectedItem();
            runCategory(lastCategory);
            animateList();

        } else if (id == 1) {
            String first = editFirst.getText().toString().trim();
            String second = editSecond.getText().toString().trim();
            if (first.isEmpty() || second.isEmpty()) {
                Toast.makeText(this, "Қабат пен бағаны жазыңыз", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                lastFloor = Integer.parseInt(first);
                lastPrice = Integer.parseInt(second);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Сан дұрыс енгізілмеді: " + first + " / " + second);
                Toast.makeText(this, "Қабат пен бағаны сан түрінде жазыңыз",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            lastMode = MODE_FLOOR;
            runFloor(lastFloor, lastPrice);
            animateList();

        } else {
            lastMode = MODE_AVERAGE;
            runAverage();
            animateList();
        }
    }

    /** Тізімді жаңартып, қорытынды мен SQL мәтінін шығарады. */
    private void apply(List<Room> list, String result, String sql) {
        data.clear();
        data.addAll(list);
        adapter.setBooked(db.bookedRoomIds());
        adapter.notifyDataSetChanged();
        shownIds.clear();
        for (Room r : list) {
            shownIds.add(r.getId());
        }
        buildBoard();
        txtResult.setText(result);
        txtSql.setText(sql);
        txtEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        updateStats();
        Log.i(TAG, "Нәтиже: " + result);
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
                        refresh();
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
                    refresh();
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
                        refresh();
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
