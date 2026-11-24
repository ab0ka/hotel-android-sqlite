package com.example.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

/**
 * Статистика экраны: үш бөлім, әрқайсысы өз Fragment-і (жалпы, санаттар, табыс).
 * Бөлім ауыстырылғанда FragmentManager Fragment-ті ауыстырады.
 * Тек әкімшіге қолжетімді.
 */
public class StatsActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_STATS";
    private static final String KEY_TAB = "tab";

    private TabLayout tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        User me = Session.current(this);
        if (me == null || !me.isAdmin()) {
            Log.w(TAG, "Қолжетімсіз: статистика тек әкімшіге");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_stats);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.appbar));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Статистика");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tabs = findViewById(R.id.tabStats);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                show(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        int start = savedInstanceState == null ? 0 : savedInstanceState.getInt(KEY_TAB, 0);
        TabLayout.Tab t = tabs.getTabAt(start);
        if (t != null) {
            t.select();
        }
        show(start);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (tabs != null) {
            outState.putInt(KEY_TAB, tabs.getSelectedTabPosition());
        }
    }

    private void show(int position) {
        Fragment f;
        switch (position) {
            case 1:
                f = new StatsCategoriesFragment();
                break;
            case 2:
                f = new StatsRevenueFragment();
                break;
            default:
                f = new StatsOverviewFragment();
                break;
        }
        Log.i(TAG, "Fragment ауыстырылды: " + f.getClass().getSimpleName());
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.statsContainer, f)
                .commit();
    }
}
