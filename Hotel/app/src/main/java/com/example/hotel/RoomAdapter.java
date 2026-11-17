package com.example.hotel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * ListView үшін нөмірлер тізімін көрсететін адаптер.
 * Әр жол item_room.xml макеті бойынша құрылады.
 */
public class RoomAdapter extends ArrayAdapter<Room> {

    public RoomAdapter(Context context, List<Room> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_room, parent, false);
        }

        Room r = getItem(position);
        if (r == null) {
            return row;
        }

        TextView txtNumber = row.findViewById(R.id.txtNumber);
        TextView txtInfo = row.findViewById(R.id.txtInfo);
        TextView txtPrice = row.findViewById(R.id.txtPrice);
        TextView txtExtra = row.findViewById(R.id.txtExtra);

        txtNumber.setText("№" + r.getNumber() + " · " + r.getCategory());
        txtInfo.setText(r.getFloor() + "-қабат · " + r.getBeds() + " төсек · "
                + r.getArea() + " м² · жөндеу " + r.getYearRepair());
        txtPrice.setText(money(r.getPrice()) + " тг");
        txtExtra.setText("тәулік" + (r.isFamily() ? " · отбасылық" : ""));

        return row;
    }

    /** Санды мыңдықтарға бөліп жазады: 24000 -> 24 000. */
    public static String money(int value) {
        String digits = String.valueOf(value);
        StringBuilder sb = new StringBuilder();
        int counter = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            sb.append(digits.charAt(i));
            counter++;
            if (counter % 3 == 0 && i > 0) {
                sb.append(' ');
            }
        }
        return sb.reverse().toString();
    }

    /** Ондық бөлшекті қысқа жазу. */
    public static String decimal(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }
}
