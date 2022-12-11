/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The Item.java helps store all the necessary information
 * about an item such as itemId, startingBid, currentBid, etc
 */
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

    /**
     * Item's constructor
     * @param itemName
     * @param description
     * @param auctionHouse
     */
    public Item(String itemName, String description, AuctionHouse auctionHouse) {
        this.itemName = itemName;
        this.description = description;
        this.auctionHouse = auctionHouse;
        currentBid = 0;
        minimumBid = 1;
    }

    /**
     * Get the itemID
     * @return itemID
     */
    public int getItemID() {
        return itemID;
    }

    /**
     * Set the itemID
     * @param itemID
     */
    public void setItemID(int itemID) {
        this.itemID = itemID;
    }
    /**
     * Get the itemName
     * @return itemName
     */
    public String getItemName() {
        return itemName;
    }
    /**
     * Get the description
     * @return description
     */
    public String getDescription() {
        return description;
    }
    /**
     * Get the currentBid
     * @return currentBid
     */
    public double getCurrentBid() {
        return currentBid;
    }
    /**
     * Get the timeLeft
     * @return timeLeft
     */
    public int getTimeLeft() {
        return timeLeft;
    }
    /**
     * Get the currentWinner
     * @return currentWinner
     */
    public AuctionHouseProxy getCurrentWinner() {
        return currentWinner;
    }
    /**
     * Get the minimumBid
     * @return minimumBid
     */
    public double getMinimumBid() {
        return minimumBid;
    }
    /**
     * Get the defaultPrice
     * @return defaultPrice
     */
    public double getDefaultPrice() {
        return defaultPrice;
    }
    /**
     * Set the defaultPrice
     */
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
    /**
     * Set the currentWinner
     * @param currentWinner
     */
    public void setCurrentWinner(AuctionHouseProxy currentWinner){
        this.currentWinner = currentWinner;
    }
    /**
     * Check if bid started
     */
    public boolean isBidStarted() {
        return bidStarted;
    }
    /**
     * Check if item is sold
     */
    public boolean isSold(){return itemSold;}

    /**
     * Resetting the bid timer
     */
    public void resetTimer(){this.resetTimer = true;}
    /**
     * Set the itemSold
     * @param itemSold
     */
    public void setItemSold(boolean itemSold) {
        this.itemSold = itemSold;
    }
    /**
     * Get the currentWinnerAccount
     * @return currentWinnerAccount
     */
    public String getCurrentWinnerAccount() {
        return currentWinnerAccount;
    }

    public void setCurrentWinnerAccount(String currentWinnerAccount) {
        this.currentWinnerAccount = currentWinnerAccount;
    }
    /**
     * Set the newBidPrice
     * @param newBidPrice
     */
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

    }
}
