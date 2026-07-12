package models;

public class Item {
    private String id;
    private String name;
    private double startPrice;

    public Item(String id, String name, double startPrice) {
        this.id = id;
        this.name = name;
        this.startPrice = startPrice;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getStartPrice() { return startPrice; }
}
