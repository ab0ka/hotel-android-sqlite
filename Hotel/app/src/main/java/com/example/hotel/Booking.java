package com.example.hotel;

import java.io.Serializable;

/**
 * Бір брондау (тұрақ) туралы мәлімет: bookings кестесінің бір жолы.
 * Күні, келу уақыты, таңғы ас, трансфер және төлем түрі брондау диалогынан келеді.
 */
public class Booking implements Serializable {

    private final int id;
    private final int roomId;
    private final String guest;
    private final int nights;
    private final int persons;
    private final int cost;
    private final String date;

    private int userId;
    private String checkin = "";
    private String arrival = "";
    private boolean breakfast;
    private boolean transfer;
    private String pay = "";
    private String roomNumber = "";

    public Booking(int id, int roomId, String guest, int nights,
                   int persons, int cost, String date) {
        this.id = id;
        this.roomId = roomId;
        this.guest = guest;
        this.nights = nights;
        this.persons = persons;
        this.cost = cost;
        this.date = date;
    }

    /** Қосымша өрістерді орнатады (Builder стилінде). */
    public Booking details(int userId, String checkin, String arrival,
                           boolean breakfast, boolean transfer, String pay) {
        this.userId = userId;
        this.checkin = checkin == null ? "" : checkin;
        this.arrival = arrival == null ? "" : arrival;
        this.breakfast = breakfast;
        this.transfer = transfer;
        this.pay = pay == null ? "" : pay;
        return this;
    }

    public Booking withRoomNumber(String number) {
        this.roomNumber = number == null ? "" : number;
        return this;
    }

    public int getId() {
        return id;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getGuest() {
        return guest;
    }

    public int getNights() {
        return nights;
    }

    public int getPersons() {
        return persons;
    }

    public int getCost() {
        return cost;
    }

    public String getDate() {
        return date;
    }

    public int getUserId() {
        return userId;
    }

    public String getCheckin() {
        return checkin;
    }

    public String getArrival() {
        return arrival;
    }

    public boolean hasBreakfast() {
        return breakfast;
    }

    public boolean hasTransfer() {
        return transfer;
    }

    public String getPay() {
        return pay;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    /** Бір тәулікке түскен орташа сома. */
    public int costPerNight() {
        if (nights <= 0) {
            return cost;
        }
        return cost / nights;
    }

    /** Қосымша қызметтер бір жолда: таңғы ас, трансфер, төлем. */
    public String extrasText() {
        StringBuilder sb = new StringBuilder();
        if (breakfast) {
            sb.append("таңғы ас");
        }
        if (transfer) {
            sb.append(sb.length() > 0 ? ", " : "").append("трансфер");
        }
        if (!pay.isEmpty()) {
            sb.append(sb.length() > 0 ? ", " : "").append(pay);
        }
        return sb.toString();
    }
}
