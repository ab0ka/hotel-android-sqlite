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
    private static final int DB_VERSION = 2;

    public static final String T_ROOMS = "rooms";
    public static final String T_BOOKINGS = "bookings";
    public static final String T_USERS = "users";

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

    public static final String B_USER = "user_id";
    public static final String B_CHECKIN = "checkin";
    public static final String B_ARRIVAL = "arrival";
    public static final String B_BREAKFAST = "breakfast";
    public static final String B_TRANSFER = "transfer";
    public static final String B_PAY = "pay";

    private static final String[] BOOKING_COLUMNS = {
            B_ID, B_ROOM, B_GUEST, B_NIGHTS, B_PERSONS, B_COST, B_DATE,
            B_USER, B_CHECKIN, B_ARRIVAL, B_BREAKFAST, B_TRANSFER, B_PAY
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

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
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
                + B_USER + " INTEGER NOT NULL DEFAULT 0, "
                + B_CHECKIN + " TEXT NOT NULL DEFAULT '', "
                + B_ARRIVAL + " TEXT NOT NULL DEFAULT '', "
                + B_BREAKFAST + " INTEGER NOT NULL DEFAULT 0, "
                + B_TRANSFER + " INTEGER NOT NULL DEFAULT 0, "
                + B_PAY + " TEXT NOT NULL DEFAULT '', "
                + "FOREIGN KEY(" + B_ROOM + ") REFERENCES "
                + T_ROOMS + "(" + R_ID + "))");

        createUsers(db);

        Log.i(TAG, "Үш кесте құрылды: " + T_ROOMS + ", " + T_BOOKINGS + ", " + T_USERS);
        fillRooms(db);
        fillBookings(db);
    }

    /** users кестесі және екі демо-аккаунт: әкімші мен клиент. */
    private void createUsers(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USERS + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL UNIQUE, "
                + "pass TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "full_name TEXT NOT NULL)");
        insertUser(db, "admin", "admin123", User.ADMIN, "Әкімші");
        insertUser(db, "client", "client123", User.CLIENT, "Қонақ Клиентов");
        Log.i(TAG, "users кестесі құрылды: admin және client демо-аккаунттары");
    }

    private void insertUser(SQLiteDatabase db, String username, String password,
                            String role, String fullName) {
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("pass", hash(username, password));
        v.put("role", role);
        v.put("full_name", fullName);
        db.insert(T_USERS, null, v);
    }

    /** Құпиясөз ашық күйде сақталмайды: SHA-256(логин + ":" + құпиясөз). */
    static String hash(String username, String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((username + ":" + password).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format(Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 1 -> 2: мәліметтер сақталады, тек жаңа баған мен кесте қосылады
            Log.w(TAG, "onUpgrade(): 1 -> 2, bookings кестесіне бағандар және users қосылады");
            db.execSQL("ALTER TABLE " + T_BOOKINGS + " ADD COLUMN " + B_USER + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + T_BOOKINGS + " ADD COLUMN " + B_CHECKIN + " TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE " + T_BOOKINGS + " ADD COLUMN " + B_ARRIVAL + " TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE " + T_BOOKINGS + " ADD COLUMN " + B_BREAKFAST + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + T_BOOKINGS + " ADD COLUMN " + B_TRANSFER + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + T_BOOKINGS + " ADD COLUMN " + B_PAY + " TEXT NOT NULL DEFAULT ''");
            createUsers(db);
            return;
        }
        Log.w(TAG, "onUpgrade(): " + oldVersion + " -> " + newVersion
                + ", кестелер жойылып қайта құрылады");
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
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

    /** Ағымдағы ORDER BY бөлігі (экранда SQL көрсету үшін). */
    public String sortSql() {
        return SORT_SQL[sort];
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

    /** Шахматка үшін: барлық нөмір, тұрақты тәртіппен (сұрыптауға тәуелсіз). */
    public List<Room> allUnsorted() {
        List<Room> list = new ArrayList<>();
        Cursor c = null;
        try {
            c = getReadableDatabase().query(T_ROOMS, ROOM_COLUMNS, null, null, null, null,
                    R_NUMBER + " ASC");
            while (c.moveToNext()) {
                list.add(new Room(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3),
                        c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7)));
            }
        } catch (Exception e) {
            Log.e(TAG, "allUnsorted() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return list;
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

    /** Брондау диалогының барлық өрістерімен жаңа брондау қосады. */
    public long addBooking(int roomId, String guest, int nights, int persons, int userId,
                           String checkin, String arrival, boolean breakfast,
                           boolean transfer, String pay) {
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
        v.put(B_USER, userId);
        v.put(B_CHECKIN, checkin);
        v.put(B_ARRIVAL, arrival);
        v.put(B_BREAKFAST, breakfast ? 1 : 0);
        v.put(B_TRANSFER, transfer ? 1 : 0);
        v.put(B_PAY, pay);

        long id = getWritableDatabase().insert(T_BOOKINGS, null, v);
        Log.i(TAG, "Жаңа брондау _id = " + id + ", нөмір = " + room.getNumber()
                + ", " + nights + " тәулік x " + room.getPrice() + " тг = " + cost
                + " тг, келу " + checkin + " " + arrival + ", пайдаланушы _id = " + userId);
        return id;
    }

    private Booking readBooking(Cursor c) {
        return new Booking(c.getInt(0), c.getInt(1), c.getString(2),
                c.getInt(3), c.getInt(4), c.getInt(5), c.getString(6))
                .details(c.getInt(7), c.getString(8), c.getString(9),
                        c.getInt(10) == 1, c.getInt(11) == 1, c.getString(12));
    }

    /** Бір клиенттің барлық брондаулары, нөмір нөмірімен (JOIN). */
    public List<Booking> bookingsOfUser(int userId) {
        List<Booking> list = new ArrayList<>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery("SELECT b._id, b.room_id, b.guest, b.nights, "
                    + "b.persons, b.cost, b.date, b.user_id, b.checkin, b.arrival, b.breakfast, "
                    + "b.transfer, b.pay, r.number FROM bookings b JOIN rooms r "
                    + "ON r._id = b.room_id WHERE b.user_id = ? "
                    + "ORDER BY b.date DESC, b._id DESC", new String[]{String.valueOf(userId)});
            while (c.moveToNext()) {
                list.add(readBooking(c).withRoomNumber(c.getString(13)));
            }
            Log.d(TAG, "bookingsOfUser(" + userId + "): " + list.size() + " брондау");
        } catch (Exception e) {
            Log.e(TAG, "bookingsOfUser() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Пайдаланушылар: кіру және тіркелу
    // ------------------------------------------------------------------

    /** Логин мен құпиясөз бойынша кіру. Сәтсіз болса null. */
    public User login(String username, String password) {
        Cursor c = null;
        try {
            c = getReadableDatabase().query(T_USERS,
                    new String[]{"_id", "username", "role", "full_name"},
                    "username = ? AND pass = ?",
                    new String[]{username, hash(username, password)}, null, null, null);
            if (c.moveToFirst()) {
                Log.i(TAG, "login(): кірді " + username + " (" + c.getString(2) + ")");
                return new User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3));
            }
            Log.w(TAG, "login(): қате логин немесе құпиясөз: " + username);
        } catch (Exception e) {
            Log.e(TAG, "login() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    public boolean usernameExists(String username) {
        Cursor c = null;
        try {
            c = getReadableDatabase().query(T_USERS, new String[]{"_id"},
                    "username = ?", new String[]{username}, null, null, null);
            return c.getCount() > 0;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /** Жаңа клиентті тіркейді (рөлі әрқашан client). Қате болса -1. */
    public long register(String username, String password, String fullName) {
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("pass", hash(username, password));
        v.put("role", User.CLIENT);
        v.put("full_name", fullName);
        long id = getWritableDatabase().insert(T_USERS, null, v);
        Log.i(TAG, "register(): " + username + ", _id = " + id);
        return id;
    }

    /** Есепті файлға жазуға арналған CSV мәтіні: нөмірлер және брондаулар. */
    public String reportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Қонақүй: есеп\n");
        sb.append("# нөмір саны;").append(count()).append('\n');
        sb.append("# брондау саны;").append(bookingCount()).append('\n');
        sb.append("# орташа баға;").append(averagePrice()).append('\n');
        sb.append("# жалпы табыс;").append(totalRevenue()).append('\n');
        sb.append("number;category;floor;beds;price;area;year_repair;bookings;revenue\n");
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery("SELECT r.number, r.category, r.floor, r.beds, "
                    + "r.price, r.area, r.year_repair, COUNT(b._id), IFNULL(SUM(b.cost), 0) "
                    + "FROM rooms r LEFT JOIN bookings b ON b.room_id = r._id "
                    + "GROUP BY r._id ORDER BY r.number", null);
            while (c.moveToNext()) {
                for (int i = 0; i < 9; i++) {
                    sb.append(c.getString(i)).append(i < 8 ? ';' : '\n');
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "reportCsv() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return sb.toString();
    }

    /** Бір нөмірдің брондаулары (соңғысы жоғарыда). */
    public List<Booking> bookingsOf(int roomId) {
        List<Booking> list = new ArrayList<>();
        Cursor c = null;
        try {
            c = getReadableDatabase().query(T_BOOKINGS, BOOKING_COLUMNS,
                    B_ROOM + " = ?", new String[]{String.valueOf(roomId)},
                    null, null, B_DATE + " DESC, " + B_ID + " DESC");
            while (c.moveToNext()) {
                list.add(readBooking(c));
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

    /** Кемінде бір брондауы бар нөмірлердің _id жиыны (тізімдегі «Брондалған» белгісі). */
    public java.util.Set<Integer> bookedRoomIds() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery("SELECT DISTINCT " + B_ROOM
                    + " FROM " + T_BOOKINGS, null);
            while (c.moveToNext()) {
                ids.add(c.getInt(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "bookedRoomIds() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return ids;
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

    /** Статистика жолы: диаграмма сызықтары үшін құрылымдалған нәтиже. */
    public static class StatRow {
        public final String label;
        public final String sub;
        public final long value;

        StatRow(String label, String sub, long value) {
            this.label = label;
            this.sub = sub;
            this.value = value;
        }
    }

    /** Санаттар кесімі (GROUP BY ... HAVING), value = нөмір саны. */
    public List<StatRow> categoryRows() {
        List<StatRow> rows = new ArrayList<>();
        String sql = "SELECT " + R_CATEGORY + ", COUNT(*), AVG(" + R_PRICE + "), "
                + "AVG(" + R_AREA + "), MIN(" + R_PRICE + ") "
                + "FROM " + T_ROOMS + " GROUP BY " + R_CATEGORY
                + " HAVING COUNT(*) > 0 ORDER BY COUNT(*) DESC, " + R_CATEGORY + " ASC";
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(sql, null);
            while (c.moveToNext()) {
                rows.add(new StatRow(c.getString(0),
                        "орташа " + RoomAdapter.money((int) Math.round(c.getDouble(2)))
                                + " тг, аудан " + Math.round(c.getDouble(3))
                                + " м², арзаны " + RoomAdapter.money(c.getInt(4)) + " тг",
                        c.getInt(1)));
            }
        } catch (Exception e) {
            Log.e(TAG, "categoryRows() қатесі: " + e.getMessage(), e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return rows;
    }

    /** Нөмірлердің табысы (LEFT JOIN + GROUP BY), value = түскен сома. */
    public List<StatRow> revenueRows() {
        List<StatRow> rows = new ArrayList<>();
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
                rows.add(new StatRow("№" + c.getString(0),
                        c.getString(1) + ", " + c.getInt(2) + " брондау, "
                                + c.getInt(3) + " тәулік",
                        c.getLong(4)));
            }
        } catch (Exception e) {
            Log.e(TAG, "revenueRows() қатесі: " + e.getMessage(), e);
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
