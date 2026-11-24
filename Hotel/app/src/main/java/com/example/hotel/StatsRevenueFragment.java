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

/** Статистика: нөмірлердің табысы (LEFT JOIN + GROUP BY). */
public class StatsRevenueFragment extends Fragment {

    private static final String TAG = "HOTEL_FRAG";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Revenue: onCreate()");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Revenue: onCreateView()");
        return inflater.inflate(R.layout.fragment_stats_revenue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DbHelper db = new DbHelper(requireContext());
        ((TextView) view.findViewById(R.id.sqlRevenue)).setText(
                "SELECT r.number, COUNT(b._id), SUM(b.nights), SUM(b.cost)\n"
                        + "FROM rooms r LEFT JOIN bookings b\n"
                        + "  ON b.room_id = r._id\n"
                        + "GROUP BY r._id ORDER BY SUM(b.cost) DESC");
        StatsViews.fillBars(requireContext(),
                (LinearLayout) view.findViewById(R.id.containerRevenue), db.revenueRows(), false);
        Log.i(TAG, "Revenue: ең табысты " + db.topRoom());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Revenue: onDestroyView()");
    }
}
