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

/** Статистика: жалпы көрсеткіштер (COUNT, SUM, AVG, MIN, MAX). Fragment өмірлік циклі логқа жазылады. */
public class StatsOverviewFragment extends Fragment {

    private static final String TAG = "HOTEL_FRAG";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Overview: onCreate()");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Overview: onCreateView()");
        return inflater.inflate(R.layout.fragment_stats_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DbHelper db = new DbHelper(requireContext());

        int rooms = db.count();
        int bookings = db.bookingCount();
        int busy = db.busyRooms();
        int load = rooms == 0 ? 0 : Math.round(busy * 100f / rooms);
        int revenue = db.totalRevenue();

        Ui.countUp((TextView) view.findViewById(R.id.kpiRevenue), revenue,
                v -> RoomAdapter.money(v) + " тг");
        ((TextView) view.findViewById(R.id.kpiSub)).setText("жалпы табыс: " + bookings
                + " брондау, " + db.totalNights() + " тәулік");

        LinearLayout figures = view.findViewById(R.id.containerFigures);
        StatsViews.addPair(requireContext(), figures, "Нөмір саны", String.valueOf(rooms));
        StatsViews.addPair(requireContext(), figures, "Брондалған нөмір",
                busy + " / " + rooms + ", " + load + " %");
        StatsViews.addPair(requireContext(), figures, "Ең арзан, тг",
                RoomAdapter.money(db.minPrice()));
        StatsViews.addPair(requireContext(), figures, "Орташа баға, тг",
                RoomAdapter.money(db.averagePrice()));
        StatsViews.addPair(requireContext(), figures, "Ең қымбат, тг",
                RoomAdapter.money(db.maxPrice()));
        Log.i(TAG, "Overview: " + rooms + " нөмір, " + bookings + " брондау, табыс " + revenue);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "Overview: onStart()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Overview: onDestroyView()");
    }
}
