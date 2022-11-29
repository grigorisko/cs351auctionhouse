package auction_distribution;

public class Item {
    private String itemName;
    private String description;
    private int itemID;
    private double currentBid;
    private int timeLeft;
    private int currentWinner;
    private double minimumBid;

    public Item(String itemName, String description) {
        this.itemName = itemName;
        this.description = description;
        currentBid = 0;
        minimumBid = 1;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public int getCurrentWinner() {
        return currentWinner;
    }

    public double getMinimumBid() {
        return minimumBid;
    }
}
