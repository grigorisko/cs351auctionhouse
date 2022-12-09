package auction_distribution;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class Item{
    private String itemName;
    private String description;
    private int itemID;
    private double currentBid;
    private int timeLeft;
    private AuctionHouseProxy currentWinner;
    private double minimumBid;
    private double defaultPrice;
    private boolean bidStarted;
    private boolean itemSold = false;
    private boolean resetTimer = false;
    private int timer = 30;
    private AuctionHouse auctionHouse;

    public Item(String itemName, String description, double defaultPrice, int itemId, AuctionHouse auctionHouse) {
        this.itemName = itemName;
        this.description = description;
        this.defaultPrice = defaultPrice;
        this.itemID = itemId;
        this.auctionHouse = auctionHouse;
        currentBid = 0;
        minimumBid = 1;
    }
    public Item(String itemName, String description, AuctionHouse auctionHouse) {
        this.itemName = itemName;
        this.description = description;
        this.auctionHouse = auctionHouse;
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

    public AuctionHouseProxy getCurrentWinner() {
        return currentWinner;
    }

    public double getMinimumBid() {
        return minimumBid;
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(double defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public void setCurrentWinner(AuctionHouseProxy currentWinner){
        this.currentWinner = currentWinner;
    }

    public boolean isBidStarted() {
        return bidStarted;
    }

    public void setItemSold(boolean itemSold) {
        this.itemSold = itemSold;
    }

    public void setNewBidPrice(double newBidPrice){
        if(currentBid==defaultPrice) {
            bidStarted = true;
        }
        else {
            resetTimer = true;
        }
        currentBid = newBidPrice;
        minimumBid = currentBid + 1;
        //start 30 second timer and reset if needed, updates every 1 second
        new Thread(() -> {
            Item thisItem = this;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    timer = timer-1;
                    if(resetTimer) {
                        timer=30;
                        resetTimer=false;
                    }
                    if (timer<0) {
                        timer = 0;
                    }
                    timeLeft = timer;
                    if (timeLeft==0 && !itemSold) {
                        try {
                            auctionHouse.finalizeBid(thisItem);
                            itemSold=true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            },0,1000);
        }).start();
    }
}
