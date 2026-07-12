package models;

public class Auction {
    private Item item;
    private double highestBid;
    private User leader; // Pode ser null se não houver lances
    private boolean isOpen;

    public Auction(Item item) {
        this.item = item;
        this.highestBid = item.getStartPrice();
        this.leader = null;
        this.isOpen = true; // Todo leilão nasce aberto
    }

    public Item getItem() { return item; }
    public double getHighestBid() { return highestBid; }
    public User getLeader() { return leader; }
    public boolean isOpen() { return isOpen; }

    public void setHighestBid(double bid) { this.highestBid = bid; }
    public void setLeader(User leader) { this.leader = leader; }
    public void closeAuction() { this.isOpen = false; }
}
