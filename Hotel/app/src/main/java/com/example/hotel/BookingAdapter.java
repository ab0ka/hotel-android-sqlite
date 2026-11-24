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
        String when = b.getCheckin().isEmpty() ? b.getDate()
                : b.getCheckin() + (b.getArrival().isEmpty() ? "" : " " + b.getArrival());
        String line = (b.getRoomNumber().isEmpty() ? "" : "№" + b.getRoomNumber() + ", ")
                + when + ", " + b.getNights() + " тәулік, " + b.getPersons() + " адам";
        String extras = b.extrasText();
        ((TextView) row.findViewById(R.id.txtInfo)).setText(
                extras.isEmpty() ? line : line + "\n" + extras);
        ((TextView) row.findViewById(R.id.txtCost)).setText(
                RoomAdapter.money(b.getCost()) + " тг");

        return row;
    }
}
