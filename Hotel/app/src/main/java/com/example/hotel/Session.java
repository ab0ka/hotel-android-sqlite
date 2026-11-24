package com.example.hotel;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Кіру сессиясы: SharedPreferences ішінде сақталады, сондықтан қосымшаны
 * жапқаннан кейін де пайдаланушы есте қалады, «Шығу» басылғанша.
 */
public final class Session {

    private static final String PREFS = "hotel_session";

    private Session() {
    }

    public static void save(Context context, User u) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("id", u.getId())
                .putString("username", u.getUsername())
                .putString("role", u.getRole())
                .putString("name", u.getFullName())
                .apply();
    }

    /** Ағымдағы пайдаланушы немесе null, егер ешкім кірмеген болса. */
    public static User current(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int id = p.getInt("id", -1);
        if (id < 0) {
            return null;
        }
        return new User(id, p.getString("username", ""), p.getString("role", User.CLIENT),
                p.getString("name", ""));
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
