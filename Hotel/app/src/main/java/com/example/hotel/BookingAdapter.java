package com.example.hotel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Бір нөмірдің брондауларын көрсететін адаптер.
 */
public class BookingAdapter extends ArrayAdapter<Booking> {

    public BookingAdapter(Context context, List<Booking> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_booking, parent, false);
        }

        Booking b = getItem(position);
        if (b == null) {
            return row;
        }

        ((TextView) row.findViewById(R.id.txtGuest)).setText(b.getGuest());
        ((TextView) row.findViewById(R.id.txtInfo)).setText(b.getDate() + ", "
                + b.getNights() + " тәулік, " + b.getPersons() + " адам");
        ((TextView) row.findViewById(R.id.txtCost)).setText(
                RoomAdapter.money(b.getCost()) + " тг");

        return row;
    }
}
