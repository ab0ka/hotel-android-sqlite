package com.example.hotel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ListView үшін нөмірлер тізімін көрсететін адаптер.
 * Әр жол item_room.xml макеті бойынша құрылады: журнал жолы, карточка емес.
 */
public class RoomAdapter extends ArrayAdapter<Room> {

    private Set<Integer> booked = new HashSet<>();

    public RoomAdapter(Context context, List<Room> items) {
        super(context, 0, items);
    }

    /** Брондауы бар нөмірлердің _id жиыны: жолдағы «брондалған» жазуы үшін. */
    public void setBooked(Set<Integer> ids) {
        booked = ids;
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

        ((TextView) row.findViewById(R.id.txtNumber)).setText(r.getNumber());
        ((TextView) row.findViewById(R.id.txtCategory)).setText(r.getCategory());
        ((TextView) row.findViewById(R.id.txtInfo)).setText(r.getFloor() + "-қабат, "
                + r.getBeds() + " төсек, " + r.getArea() + " м²");
        ((TextView) row.findViewById(R.id.txtPrice)).setText(money(r.getPrice()) + " тг");

        // Күй сөзбен ғана: «брондалған» сия түсімен, «бос» бозғылт
        TextView txtStatus = row.findViewById(R.id.txtStatus);
        boolean isBooked = booked.contains(r.getId());
        txtStatus.setText(isBooked ? "брондалған" : "бос");
        txtStatus.setTextColor(ContextCompat.getColor(getContext(),
                isBooked ? R.color.accent_2 : R.color.muted));

        return row;
    }

    /** Категорияға тән түс: полоска-үлгідегі дақ түсі. */
    public static int categoryColor(Context context, String category) {
        int res;
        if ("Жартылай люкс".equals(category)) {
            res = R.color.stain_4;
        } else if ("Люкс".equals(category)) {
            res = R.color.stain_3;
        } else if ("Отбасылық".equals(category)) {
            res = R.color.stain_1;
        } else if ("Бизнес".equals(category)) {
            res = R.color.stain_2;
        } else {
            res = R.color.stain_neutral;
        }
        return ContextCompat.getColor(context, res);
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
