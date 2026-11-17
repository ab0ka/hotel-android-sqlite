package com.example.hotel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * SQLite деректер базасымен жұмыс істейтін негізгі класс.
 *
 * Базада екі кесте бар:
 *   rooms    — қонақүй нөмірлері;
 *   bookings — нөмірлерге жасалған брондаулар (rooms кестесіне сілтеме жасайды).
 *
 * Барлық SQL сұраныстары осы класта жинақталған, сондықтан Activity кластары
 * SQL тілімен тікелей жұмыс істемейді — тек осы кластың әдістерін шақырады.
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = "HOTEL_DB";

    private static final String DB_NAME = "hotel.db";
    private static final int DB_VERSION = 1;

    public static final String T_ROOMS = "rooms";
    public static final String T_BOOKINGS = "bookings";

    public static final String R_ID = "_id";
    public static final String R_NUMBER = "number";
    public static final String R_CATEGORY = "category";
    public static final String R_FLOOR = "floor";
    public static final String R_BEDS = "beds";
    public static final String R_PRICE = "price";
    public static final String R_AREA = "area";
    public static final String R_YEAR = "year_repair";

    public static final String B_ID = "_id";
    public static final String B_ROOM = "room_id";
    public static final String B_GUEST = "guest";
    public static final String B_NIGHTS = "nights";
    public static final String B_PERSONS = "persons";
    public static final String B_COST = "cost";
    public static final String B_DATE = "date";

    private static final String[] ROOM_COLUMNS = {
            R_ID, R_NUMBER, R_CATEGORY, R_FLOOR, R_BEDS, R_PRICE, R_AREA, R_YEAR
    };

    private static final String[] BOOKING_COLUMNS = {
            B_ID, B_ROOM, B_GUEST, B_NIGHTS, B_PERSONS, B_COST, B_DATE
    };

    /** Қонақүйде қолданылатын нөмір санаттары. */
    public static final String[] CATEGORIES = {
            "Стандарт", "Жартылай люкс", "Люкс", "Отбасылық", "Бизнес"
    };

    /** Сұрыптау нұсқаларының экранда көрінетін атаулары. */
    public static final String[] SORT_NAMES = {
            "Нөмірі бойынша",
            "Бағасы (өсуі бойынша)",
            "Бағасы (кемуі бойынша)",
            "Санаты бойынша"
    };

    /** SORT_NAMES-тегі нұсқаларға сәйкес ORDER BY бөліктері. */
    private static final String[] SORT_SQL = {
            R_NUMBER + " ASC",
            R_PRICE + " ASC",
            R_PRICE + " DESC",
            R_CATEGORY + " ASC, " + R_NUMBER + " ASC"
    };

    private int sort = 0;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ------------------------------------------------------------------
    // Базаны құру және бастапқы мәліметтер
    // ------------------------------------------------------------------

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "onCreate(): " + DB_NAME + " базасы құрылады");

        db.execSQL("CREATE TABLE " + T_ROOMS + " ("
                + R_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + R_NUMBER + " TEXT NOT NULL UNIQUE, "
                + R_CATEGORY + " TEXT NOT NULL, "
                + R_FLOOR + " INTEGER NOT NULL, "
                + R_BEDS + " INTEGER NOT NULL, "
                + R_PRICE + " INTEGER NOT NULL, "
                + R_AREA + " INTEGER NOT NULL, "
                + R_YEAR + " INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + T_BOOKINGS + " ("
                + B_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + B_ROOM + " INTEGER NOT NULL, "
                + B_GUEST + " TEXT NOT NULL, "
                + B_NIGHTS + " INTEGER NOT NULL, "
                + B_PERSONS + " INTEGER NOT NULL, "
                + B_COST + " INTEGER NOT NULL, "
                + B_DATE + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + B_ROOM + ") REFERENCES "
                + T_ROOMS + "(" + R_ID + "))");

        Log.i(TAG, "Екі кесте құрылды: " + T_ROOMS + ", " + T_BOOKINGS);
        fillRooms(db);
        fillBookings(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "onUpgrade(): " + oldVersion + " -> " + newVersion
                + ", кестелер жойылып қайта құрылады");
        db.execSQL("DROP TABLE IF EXISTS " + T_BOOKINGS);
        db.execSQL("DROP TABLE IF EXISTS " + T_ROOMS);
        onCreate(db);
    }

    private void fillRooms(SQLiteDatabase db) {
        addRoom(db, "101", "Стандарт", 1, 1, 18000, 18, 2021);
        addRoom(db, "102", "Стандарт", 1, 2, 24000, 22, 2021);
        addRoom(db, "103", "Стандарт", 1, 2, 24000, 22, 2018);
        addRoom(db, "104", "Бизнес", 1, 1, 32000, 26, 2023);
        addRoom(db, "201", "Стандарт", 2, 2, 25000, 23, 2022);
        addRoom(db, "202", "Жартылай люкс", 2, 2, 38000, 32, 2023);
        addRoom(db, "203", "Жартылай люкс", 2, 2, 38000, 30, 2019);
        addRoom(db, "204", "Отбасылық", 2, 4, 52000, 44, 2024);
        addRoom(db, "301", "Бизнес", 3, 2, 41000, 34, 2024);
        addRoom(db, "302", "Люкс", 3, 2, 65000, 48, 2025);
        addRoom(db, "303", "Люкс", 3, 3, 78000, 56, 2025);
        addRoom(db, "304", "Отбасылық", 3, 4, 56000, 46, 2020);
        addRoom(db, "401", "Люкс", 4, 2, 72000, 52, 2024);
        addRoom(db, "402", "Бизнес", 4, 1, 35000, 28, 2017);
        addRoom(db, "403", "Стандарт", 4, 2, 23000, 21, 2016);
        addRoom(db, "404", "Отбасылық", 4, 5, 64000, 58, 2022);

        Log.i(TAG, "Бастапқы 16 нөмір қосылды");
    }

    private void addRoom(SQLiteDatabase db, String number, String category, int floor,
                         int beds, int price, int area, int year) {
        ContentValues v = new ContentValues();
        v.put(R_NUMBER, number);
        v.put(R_CATEGORY, category);
        v.put(R_FLOOR, floor);
        v.put(R_BEDS, beds);
        v.put(R_PRICE, price);
        v.put(R_AREA, area);
        v.put(R_YEAR, year);
        db.insert(T_ROOMS, null, v);
    }

    private void fillBookings(SQLiteDatabase db) {
        addBooking(db, 1, "Ахметов Д.", 2, 1, 36000, "2026-11-02");
        addBooking(db, 2, "Сәрсенова А.", 3, 2, 72000, "2026-11-03");
        addBooking(db, 4, "Жұмабек Н.", 1, 1, 32000, "2026-11-03");
        addBooking(db, 6, "Кім С.", 4, 2, 152000, "2026-11-04");
        addBooking(db, 8, "Оспанов Е.", 5, 4, 260000, "2026-11-05");
        addBooking(db, 10, "Нұрлан Б.", 2, 2, 130000, "2026-11-05");
        addBooking(db, 11, "Иванов П.", 3, 3, 234000, "2026-11-06");
        addBooking(db, 12, "Сәкен Ж.", 2, 4, 112000, "2026-11-07");
        addBooking(db, 13, "Әлиев Р.", 1, 2, 72000, "2026-11-08");
        addBooking(db, 5, "Тоқтар М.", 2, 2, 50000, "2026-11-09");
        addBooking(db, 9, "Бейсенова Г.", 3, 2, 123000, "2026-11-10");
        addBooking(db, 16, "Serikov A.", 4, 5, 256000, "2026-11-11");

        Log.i(TAG, "Бастапқы 12 брондау қосылды");
    }

    private void addBooking(SQLiteDatabase db, int roomId, String guest, int nights,
                            int persons, int cost, String date) {
        ContentValues v = new ContentValues();
        v.put(B_ROOM, roomId);
        v.put(B_GUEST, guest);
        v.put(B_NIGHTS, nights);
        v.put(B_PERSONS, persons);
        v.put(B_COST, cost);
        v.put(B_DATE, date);
        db.insert(T_BOOKINGS, null, v);
    }

    // ------------------------------------------------------------------
    // Сұрыптау
    // ------------------------------------------------------------------

    public void setSort(int index) {
        if (index >= 0 && index < SORT_SQL.length) {
            sort = index;
            Log.d(TAG, "Сұрыптау өзгерді: ORDER BY " + SORT_SQL[sort]);
        }
    }

    public String sortName() {
        return SORT_NAMES[sort];
    }

    private String order() {
        return SORT_SQL[sort];
    }

    // ------------------------------------------------------------------
    // Негізгі SELECT сұраныстары (тапсырманың а, ә, б тармақтары)
    // ------------------------------------------------------------------

    /** Барлық нөмірлер. */
    public List<Room> all() {
        Log.i(TAG, "Барлық нөмірлер сұралды");
        return readRooms(null, null);
    }

    /** а) Берілген санаттағы барлық нөмірлер. */
    public List<Room> byCategory(String category) {
        Log.i(TAG, "а) санат = " + category);
        return readRooms(R_CATEGORY + " = ?", new String[]{category});
    }

    /**
     * ә) Берілген қабаттағы, бағасы көрсетілген шектен аспайтын нөмірлер.
     * Ескерту: selectionArgs мәндері TEXT түрінде берілгенімен, floor және price
     * бағандары INTEGER болғандықтан SQLite салыстыруды санға келтіріп орындайды.
     */
    public List<Room> byFloorAndPrice(int floor, int maxPrice) {
        Log.i(TAG, "ә) қабат = " + floor + ", бағасы <= " + maxPrice + " тг");
        return readRooms(R_FLOOR + " = ? AND " + R_PRICE + " <= ?",
                new String[]{String.valueOf(floor), String.valueOf(maxPrice)});
    }

    /** б) Бағасы қонақүй бойынша орташа бағадан арзан нөмірлер. */
    public List<Room> cheaperThanAverage() {
        int avg = averagePrice();
        Log.i(TAG, "б) орташа баға = " + avg + " тг, одан арзан нөмірлер сұралды");
        return readRooms(R_PRICE + " < ?", new String[]{String.valueOf(avg)});
    }

    /** Нөмірі немесе санаты бойынша іздеу (LIKE операторы). */
    public List<Room> search(String text) {
        Log.i(TAG, "Іздеу мәтіні: " + text);
        String mask = "%" + text + "%";
        return readRooms(R_NUMBER + " LIKE ? OR " + R_CATEGORY + " LIKE ?",
                new String[]{mask, mask});
    }

    /** Нөмірлерді оқудың жалпы әдісі: query() шақырылады, Cursor міндетті жабылады. */
    private List<Room> readRooms(String where, String[] args) {
        List<Room> list = new ArrayList<>();
        Cursor c = null;
        try {
            c = getReadableDatabase().query(T_ROOMS, ROOM_COLUMNS,
                    where, args, null, null, order());
            while (c.moveToNext()) {
                list.add(new Room(
                        c.getInt(0), c.getString(1), c.getString(2), c.getInt(3),
                        c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7)));
            }
            Log.d(TAG, "readRooms(): " + list.size() + " жол оқылды, where = " + where);
        } catch (Exception e) {
            Log.e(TAG, "readRooms() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return list;
    }

    public Room roomById(int id) {
        List<Room> list = readRooms(R_ID + " = ?", new String[]{String.valueOf(id)});
        return list.isEmpty() ? null : list.get(0);
    }

    // ------------------------------------------------------------------
    // INSERT / UPDATE / DELETE — нөмірлер
    // ------------------------------------------------------------------

    public long insertRoom(String number, String category, int floor,
                           int beds, int price, int area, int year) {
        ContentValues v = new ContentValues();
        v.put(R_NUMBER, number);
        v.put(R_CATEGORY, category);
        v.put(R_FLOOR, floor);
        v.put(R_BEDS, beds);
        v.put(R_PRICE, price);
        v.put(R_AREA, area);
        v.put(R_YEAR, year);

        long id = getWritableDatabase().insert(T_ROOMS, null, v);
        if (id == -1) {
            Log.e(TAG, "INSERT сәтсіз: нөмір " + number);
        } else {
            Log.i(TAG, "INSERT: жаңа нөмір _id = " + id + ", нөмірі = " + number);
        }
        return id;
    }

    /** Тек бағаны мен төсек санын жаңартады (UPDATE ... WHERE _id = ?). */
    public int updateRoom(int id, int price, int beds) {
        ContentValues v = new ContentValues();
        v.put(R_PRICE, price);
        v.put(R_BEDS, beds);

        int rows = getWritableDatabase().update(T_ROOMS, v,
                R_ID + " = ?", new String[]{String.valueOf(id)});
        Log.i(TAG, "UPDATE: _id = " + id + ", баға = " + price
                + ", төсек = " + beds + ", өзгерген жол = " + rows);
        return rows;
    }

    /** Нөмірді және оған байланысты барлық брондауларды жояды. */
    public int deleteRoom(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int bookings = db.delete(T_BOOKINGS, B_ROOM + " = ?",
                new String[]{String.valueOf(id)});
        int rows = db.delete(T_ROOMS, R_ID + " = ?",
                new String[]{String.valueOf(id)});
        Log.i(TAG, "DELETE: нөмір _id = " + id + " (жол = " + rows
                + "), онымен бірге " + bookings + " брондау жойылды");
        return rows;
    }

    /** Жөндеуден өткен жылы берілген жылдан ескі нөмірлерді топтап жою. */
    public int deleteOlderThan(int year) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + T_BOOKINGS + " WHERE " + B_ROOM
                + " IN (SELECT " + R_ID + " FROM " + T_ROOMS
                + " WHERE " + R_YEAR + " < " + year + ")");
        int rows = db.delete(T_ROOMS, R_YEAR + " < ?",
                new String[]{String.valueOf(year)});
        Log.w(TAG, "DELETE: жөндеуі " + year + " жылдан ескі "
                + rows + " нөмір жойылды");
        return rows;
    }

    /** Базаны бастапқы қалпына келтіру. */
    public void resetData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_BOOKINGS, null, null);
        db.delete(T_ROOMS, null, null);
        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '" + T_ROOMS + "'");
        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '" + T_BOOKINGS + "'");
        fillRooms(db);
        fillBookings(db);
        Log.w(TAG, "resetData(): база бастапқы қалпына келтірілді");
    }

    // ------------------------------------------------------------------
    // Брондаулар
    // ------------------------------------------------------------------

    /**
     * Жаңа брондау қосады. Құны нөмірдің бағасы бойынша есептеледі:
     * cost = nights * price.
     */
    public long addBooking(int roomId, String guest, int nights, int persons) {
        Room room = roomById(roomId);
        if (room == null) {
            Log.e(TAG, "addBooking(): нөмір табылмады, _id = " + roomId);
            return -1;
        }
        int cost = nights * room.getPrice();

        ContentValues v = new ContentValues();
        v.put(B_ROOM, roomId);
        v.put(B_GUEST, guest);
        v.put(B_NIGHTS, nights);
        v.put(B_PERSONS, persons);
        v.put(B_COST, cost);
        v.put(B_DATE, today());

        long id = getWritableDatabase().insert(T_BOOKINGS, null, v);
        Log.i(TAG, "Жаңа брондау _id = " + id + ", нөмір = " + room.getNumber()
                + ", " + nights + " тәулік x " + room.getPrice() + " тг = " + cost + " тг");
        return id;
    }

    /** Бір нөмірдің брондаулары (соңғысы жоғарыда). */
    public List<Booking> bookingsOf(int roomId) {
        List<Booking> list = new ArrayList<>();
        Cursor c = null;
        try {
            c = getReadableDatabase().query(T_BOOKINGS, BOOKING_COLUMNS,
                    B_ROOM + " = ?", new String[]{String.valueOf(roomId)},
                    null, null, B_DATE + " DESC");
            while (c.moveToNext()) {
                list.add(new Booking(c.getInt(0), c.getInt(1), c.getString(2),
                        c.getInt(3), c.getInt(4), c.getInt(5), c.getString(6)));
            }
            Log.d(TAG, "bookingsOf(" + roomId + "): " + list.size() + " брондау");
        } catch (Exception e) {
            Log.e(TAG, "bookingsOf() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return list;
    }

    public int deleteBooking(int id) {
        int rows = getWritableDatabase().delete(T_BOOKINGS, B_ID + " = ?",
                new String[]{String.valueOf(id)});
        Log.i(TAG, "DELETE брондау _id = " + id + ", жол = " + rows);
        return rows;
    }

    // ------------------------------------------------------------------
    // Статистика (GROUP BY, JOIN, агрегат функциялар)
    // ------------------------------------------------------------------

    public int count() {
        return (int) singleNumber("SELECT COUNT(*) FROM " + T_ROOMS);
    }

    public int bookingCount() {
        return (int) singleNumber("SELECT COUNT(*) FROM " + T_BOOKINGS);
    }

    public int averagePrice() {
        return (int) singleNumber("SELECT IFNULL(AVG(" + R_PRICE + "), 0) FROM " + T_ROOMS);
    }

    public int minPrice() {
        return (int) singleNumber("SELECT IFNULL(MIN(" + R_PRICE + "), 0) FROM " + T_ROOMS);
    }

    public int maxPrice() {
        return (int) singleNumber("SELECT IFNULL(MAX(" + R_PRICE + "), 0) FROM " + T_ROOMS);
    }

    public int totalRevenue() {
        return (int) singleNumber("SELECT IFNULL(SUM(" + B_COST + "), 0) FROM " + T_BOOKINGS);
    }

    public int totalNights() {
        return (int) singleNumber("SELECT IFNULL(SUM(" + B_NIGHTS + "), 0) FROM " + T_BOOKINGS);
    }

    /** Кемінде бір брондауы бар нөмірлердің саны (бос емес нөмірлер). */
    public int busyRooms() {
        return (int) singleNumber("SELECT COUNT(DISTINCT " + B_ROOM + ") FROM " + T_BOOKINGS);
    }

    private long singleNumber(String sql) {
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(sql, null);
            if (c.moveToFirst()) {
                return (long) c.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "singleNumber() қатесі: " + sql, e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return 0;
    }

    /** DISTINCT арқылы санаттар тізімі (сүзгі үшін). */
    public List<String> categoryList() {
        return distinct(R_CATEGORY);
    }

    private List<String> distinct(String column) {
        List<String> list = new ArrayList<>();
        Cursor c = null;
        try {
            c = getReadableDatabase().query(true, T_ROOMS, new String[]{column},
                    null, null, null, null, column + " ASC", null);
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "distinct(" + column + ") қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        Log.d(TAG, "DISTINCT " + column + ": " + list.size() + " мән");
        return list;
    }

    /**
     * Санаттар бойынша статистика: нөмір саны, орташа баға, орташа ауданы.
     * GROUP BY ... HAVING қолданылады.
     */
    public List<String> statsByCategory() {
        List<String> rows = new ArrayList<>();
        String sql = "SELECT " + R_CATEGORY + ", COUNT(*), AVG(" + R_PRICE + "), "
                + "AVG(" + R_AREA + "), MIN(" + R_PRICE + ") "
                + "FROM " + T_ROOMS + " GROUP BY " + R_CATEGORY
                + " HAVING COUNT(*) > 0 ORDER BY COUNT(*) DESC, " + R_CATEGORY + " ASC";
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(sql, null);
            while (c.moveToNext()) {
                rows.add(String.format(Locale.getDefault(),
                        "%s — %d нөмір, орташа баға %.0f тг, орташа аудан %.0f м², "
                                + "ең арзаны %d тг",
                        c.getString(0), c.getInt(1), c.getDouble(2),
                        c.getDouble(3), c.getInt(4)));
            }
            Log.i(TAG, "statsByCategory(): " + rows.size() + " топ");
        } catch (Exception e) {
            Log.e(TAG, "statsByCategory() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return rows;
    }

    /**
     * Нөмірлердің табысы: bookings кестесі rooms кестесімен LEFT JOIN арқылы
     * біріктіріліп, әр нөмір бойынша брондау саны мен түскен сома есептеледі.
     */
    public List<String> revenueByRoom() {
        List<String> rows = new ArrayList<>();
        String sql = "SELECT r." + R_NUMBER + ", r." + R_CATEGORY + ", "
                + "COUNT(b." + B_ID + "), IFNULL(SUM(b." + B_NIGHTS + "), 0), "
                + "IFNULL(SUM(b." + B_COST + "), 0) "
                + "FROM " + T_ROOMS + " r "
                + "LEFT JOIN " + T_BOOKINGS + " b ON b." + B_ROOM + " = r." + R_ID + " "
                + "GROUP BY r." + R_ID + " "
                + "ORDER BY SUM(b." + B_COST + ") DESC, r." + R_NUMBER + " ASC";
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(sql, null);
            while (c.moveToNext()) {
                rows.add(String.format(Locale.getDefault(),
                        "№%s (%s) — %d брондау, %d тәулік, %d тг",
                        c.getString(0), c.getString(1), c.getInt(2),
                        c.getInt(3), c.getInt(4)));
            }
            Log.i(TAG, "revenueByRoom(): " + rows.size() + " жол (JOIN + GROUP BY)");
        } catch (Exception e) {
            Log.e(TAG, "revenueByRoom() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return rows;
    }

    /** Ең көп табыс түсірген нөмір туралы жол. */
    public String topRoom() {
        List<String> rows = revenueByRoom();
        return rows.isEmpty() ? "—" : rows.get(0);
    }

    // ------------------------------------------------------------------
    // Көмекші әдістер
    // ------------------------------------------------------------------

    public static int currentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    private String today() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    /** Нөмірдің базада бар-жоғын тексереді (қайталануды болдырмау үшін). */
    public boolean numberExists(String number) {
        Cursor c = null;
        try {
            c = getReadableDatabase().query(T_ROOMS, new String[]{R_ID},
                    R_NUMBER + " = ?", new String[]{number}, null, null, null);
            boolean exists = c.getCount() > 0;
            Log.d(TAG, "numberExists(" + number + ") = " + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "numberExists() қатесі: " + e.getMessage(), e);
            return false;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
}
