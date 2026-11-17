package com.example.hotel;

/**
 * Бір брондау (тұрақ) туралы мәлімет — bookings кестесінің бір жолы.
 */
public class Booking {

    private final int id;
    private final int roomId;
    private final String guest;
    private final int nights;
    private final int persons;
    private final int cost;
    private final String date;

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

    /** Бір тәулікке түскен орташа сома. */
    public int costPerNight() {
        if (nights <= 0) {
            return cost;
        }
        return cost / nights;
    }
}
