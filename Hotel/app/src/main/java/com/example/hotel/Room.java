package com.example.hotel;

/**
 * Қонақүй нөмірі туралы мәліметтерді сақтайтын модель класы.
 * Бір объект rooms кестесіндегі бір жолға сәйкес келеді.
 */
public class Room implements java.io.Serializable {

    private final int id;
    private final String number;
    private final String category;
    private final int floor;
    private final int beds;
    private final int price;
    private final int area;
    private final int yearRepair;

    public Room(int id, String number, String category, int floor,
                int beds, int price, int area, int yearRepair) {
        this.id = id;
        this.number = number;
        this.category = category;
        this.floor = floor;
        this.beds = beds;
        this.price = price;
        this.area = area;
        this.yearRepair = yearRepair;
    }

    public int getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getCategory() {
        return category;
    }

    public int getFloor() {
        return floor;
    }

    public int getBeds() {
        return beds;
    }

    public int getPrice() {
        return price;
    }

    public int getArea() {
        return area;
    }

    public int getYearRepair() {
        return yearRepair;
    }

    /** Нөмір үлкен (отбасылық) болып саналады ма. */
    public boolean isFamily() {
        return beds >= 3;
    }

    /** Соңғы жөндеуден кейін өткен жыл саны. */
    public int repairAge(int currentYear) {
        return currentYear - yearRepair;
    }

    /** Бір төсекке шаққандағы баға. */
    public int pricePerBed() {
        if (beds <= 0) {
            return price;
        }
        return price / beds;
    }

    /** Тізімде көрсетілетін қысқа атау. */
    public String title() {
        return "№" + number + " · " + category;
    }

    @Override
    public String toString() {
        return title();
    }
}
