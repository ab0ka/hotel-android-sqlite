package com.example.hotel;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

/** Статистика: санаттар бойынша (GROUP BY ... HAVING), полоска-үлгі диаграммасымен. */
public class StatsCategoriesFragment extends Fragment {

    private static final String TAG = "HOTEL_FRAG";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Categories: onCreate()");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Categories: onCreateView()");
        return inflater.inflate(R.layout.fragment_stats_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DbHelper db = new DbHelper(requireContext());
        List<DbHelper.StatRow> cats = db.categoryRows();

        ((TextView) view.findViewById(R.id.sqlCategories)).setText(
                "SELECT category, COUNT(*), AVG(price), AVG(area), MIN(price)\n"
                        + "FROM rooms GROUP BY category\n"
                        + "HAVING COUNT(*) > 0");

        String[] labels = new String[cats.size()];
        int[] values = new int[cats.size()];
        int[] stains = new int[cats.size()];
        for (int i = 0; i < cats.size(); i++) {
            labels[i] = cats.get(i).label;
            values[i] = (int) cats.get(i).value;
            stains[i] = RoomAdapter.categoryColor(requireContext(), cats.get(i).label);
        }
        ((StripChartView) view.findViewById(R.id.stripCategories))
                .setData(labels, values, stains);
        StatsViews.fillBars(requireContext(),
                (LinearLayout) view.findViewById(R.id.containerCategories), cats, true);
        Log.i(TAG, "Categories: " + cats.size() + " топ");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Categories: onDestroyView()");
    }
}
