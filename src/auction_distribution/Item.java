package auction_distribution;

import java.util.Timer;
import java.util.TimerTask;

public class Item{
    private String itemName;
    private String description;
    private int itemID;
    private double currentBid;
    private int timeLeft;
    private AuctionHouseProxy currentWinner;
    private String currentWinnerAccount;
    private double minimumBid;
    private double defaultPrice;
    private boolean bidStarted;
    private boolean itemSold = false;
    private boolean resetTimer = false;
    private final int BIDDING_DURATION = 30; // how long bidding should last for (seconds)
    private int timer = BIDDING_DURATION;
    private Timer t = new Timer();
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

    public void setDefaults() {
        this.currentBid = 0;
        this.minimumBid = 1;
        this.bidStarted = false;
        this.itemSold = false;
        t = new Timer();
//        this.timer = BIDDING_DURATION;
//        this.resetTimer = true;
        this.currentWinner = null;
    }

    public void setCurrentWinner(AuctionHouseProxy currentWinner){
        this.currentWinner = currentWinner;
    }

    public boolean isBidStarted() {
        return bidStarted;
    }

    public boolean isSold(){return itemSold;}

    public void resetTimer(){this.resetTimer = true;}

    public void setItemSold(boolean itemSold) {
        this.itemSold = itemSold;
    }

    public String getCurrentWinnerAccount() {
        return currentWinnerAccount;
    }

    public void setCurrentWinnerAccount(String currentWinnerAccount) {
        this.currentWinnerAccount = currentWinnerAccount;
    }

    public void setNewBidPrice(double newBidPrice){
        Item thisItem = this;
        if(!itemSold){
            currentBid = newBidPrice;
            minimumBid = currentBid + 1;
            //start 30 second timer and reset if needed, updates every 1 second
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    //this print prevented me from typing exit in console
//                    System.out.println("timer countdown: " + timer);
                    timer = timer-1;
                    if(resetTimer) {
                        timer = BIDDING_DURATION;
                        resetTimer=false;
                    }
//                if (timer<=0) {
//                    timer = 0;
//                }
                    timeLeft = timer;
                    if (timeLeft<=0) {  // Removed: "&& !itemSold"
                        try {
                            t.cancel();  // Stops bid timer
                            itemSold=true;
                            auctionHouse.finalizeBid(thisItem);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
            t.schedule(tt,0,1000);
        }
//        new Thread(() -> {
//            Item thisItem = this;
//            new Timer().schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    timer = timer-1;
//                    System.out.println("Timer Running " + timer);
//                    if(resetTimer) {
//                        timer = BIDDING_DURATION;
//                        resetTimer=false;
//                    }
//                    if (timer<0) {
//                        timer = 0;
//                    }
//                    timeLeft = timer;
//                    if (timeLeft==0 && !itemSold) {
//                        try {
//                            auctionHouse.finalizeBid(thisItem);
//                            itemSold=true;
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }
//            },0,1000);
//        }).start();
    }
}
