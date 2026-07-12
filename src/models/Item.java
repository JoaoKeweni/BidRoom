package models;

public class Item {
    private String id;
    private String name;
    private double startPrice;
    private String imageUrl;

    public Item(String id, String name, double startPrice, String imageUrl) {
        this.id = id;
        this.name = name;
        this.startPrice = startPrice;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getStartPrice() {
        return startPrice;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
}
