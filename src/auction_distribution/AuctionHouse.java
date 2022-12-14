/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The AuctionHouse.java responsible for
 */
package auction_distribution;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Is a Client (to Bank) and Server (to Agent)Bid
 */
public class AuctionHouse {
    private ServerSocket serverSocket;
    private Socket bankAHSocket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    // Our Company Name
    private String companyName;
    // Holds list of all items in our inventory.txt file
    private List<Item> inventoryList = new ArrayList<Item>();
    // Holds top 3 items that are up for auction.
    private static List<Item> itemsOnSale = new ArrayList<Item>();
    private int itemID = 0;
    private AuctionHouse auctionHouse;
    //checks for bank communication
    private boolean balanceChecked = false;
    private boolean bidAccepted = false;
    //check for finalizing item sale
    private boolean processingBid = false;
    //account number
    private String bankAccount;
    //static list of all connected agents
    private static List<AuctionHouseProxy> connectedClients = new ArrayList<>();

    /**
     * The AuctionHouse constructor
     * @throws IOException
     */
    public AuctionHouse() throws IOException {
        auctionHouse = this;
        System.out.println("Enter Bank Address/Hostname");
        Scanner scanner = new Scanner(System.in);
        String bankAddress = scanner.nextLine();
        System.out.println("Enter Bank port");
        String portString = scanner.nextLine();
        int port = Integer.parseInt(portString);
        // Create Bank-AuctionHouse Connection
        bankAHSocket = new Socket(bankAddress, port);
        bufferedReader = new BufferedReader(new InputStreamReader(bankAHSocket.getInputStream()));
        printWriter = new PrintWriter(bankAHSocket.getOutputStream());

        // Create AuctionHouse Server
        serverSocket = new ServerSocket(0);

        //Send Server Data to bank. Infinite Loop until Connection Successful :)
        String bankConnectionMsg = "";
        do{
            // Gets company name
            System.out.println("Company Name:");
            scanner = new Scanner(System.in);
            companyName = scanner.nextLine();

            // Get Server Address & Port
            int serverPort = serverSocket.getLocalPort();
            sendBankMsg(companyName + ";server;address;"+serverPort);

            // Check to see if Bank Accepted our Name, Address, & Port
            bankConnectionMsg = bufferedReader.readLine();
            System.out.println(bankConnectionMsg);
        }while (!bankConnectionMsg.equals("Bank Connection Successful."));

        initializeInventory();
        System.out.println(itemsOnSale.toString());
    }

    /**
     * Sends a message to Bank
     * @param message
     */
    private void sendBankMsg(String message){
        printWriter.println(message);
        printWriter.flush();
    }

    /**
     * Starts AuctionHouse Server
     * @throws IOException
     */
    private void startServer() throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!serverSocket.isClosed()){
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Thread thread = new Thread(new AuctionHouseProxy(clientSocket,auctionHouse ));
                        thread.start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    /**
     * Listens for incoming messages
     */
    private void listen(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (bankAHSocket.isConnected() && !bankAHSocket.isClosed()) {

                    try {
                        String message = bufferedReader.readLine();
                        //bank confirmed bid
                        if (message.equals("Bid accepted")) {
                            balanceChecked = true;
                            bidAccepted = true;
                        }
                        //bank declined bid
                        else if (message.equals("Insufficient funds")) {
                            balanceChecked = true;
                            bidAccepted = false;
                        }
                        //bank sending us our account number
                        else if (message.contains("accountNumber:")) {
                            bankAccount = message.split(":")[1];
                        }
                        else if(message.contains("exit acknowledged")) {
                            //close socket
                            bankAHSocket.close();
                            //exit
                            System.exit(0);
                        }
                        System.out.println(message);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    /**
     * Sends a message to bank, when we type something into Console.
     */
    private void sendConsoleInput(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);

                while(bankAHSocket.isConnected()){
                    String message = scanner.nextLine();
                    //exit logic
                    if(message.equalsIgnoreCase("exit")) {
                        boolean bidIsActive = false;
                        //if an item has a timer running prevent exit
                        for(Item item: getItemsOnSale()) {
                            if (item.getTimeLeft()>0) {
                                bidIsActive = true;
                            }
                        }
                        if(bidIsActive) {
                            System.out.println("Cannot exit while bid is active");
                        }
                        //else cleanup
                        else {
                            //message bank that we are exiting
                            printWriter.println(companyName + "/exiting");
                            printWriter.flush();
                            //notify agents that we are exiting
                            for(AuctionHouseProxy auctionHouseProxy:connectedClients) {
                                    auctionHouseProxy.sendAgentMsg("exiting/"+companyName);
                            }
                        }
                    }
                    else {
                        printWriter.println(message);
                        printWriter.flush();
                    }
                }
            }
        }).start();
    }

    /**
     * Selling the item, remove it from the inventory
     * @return
     */
    private synchronized Item sellNewItem() {
        Random rand = new Random();
        Item itemToSell = inventoryList.remove(rand.nextInt(inventoryList.size()));
        itemToSell.setItemID(itemID);
        itemID++;
        return itemToSell;
    }

    /**
     * Read in the inventory file, store it inside of inventoryList
     */
    private void initializeInventory() {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader
                             (getClass().getClassLoader().getResourceAsStream("inventory")))) {
            String line;
            String[] words;
            while ((line = reader.readLine()) != null) {
                words = line.split(" ");
                String itemName = words[0];
                String description="";
                for (int i=1;i<words.length;i++) {
                    if(i==words.length-1) {
                        description += words[i];
                    }
                    else {
                        description += words[i] + " ";
                    }
                }
                Item newItem = new Item(itemName,description,this);
                inventoryList.add(newItem);
            }
            for (int i=0;i<3;i++) {

//                itemsOnSale.add(sellNewItem());
                getItemsOnSale().add(sellNewItem());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starting the server
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        AuctionHouse auctionHouse = new AuctionHouse();
        auctionHouse.listen();
        auctionHouse.sendConsoleInput();
        auctionHouse.startServer();
    }

    /**
     * Get the itemsOnSale list
     * @return itemsOnSale
     */
    public synchronized List<Item> getItemsOnSale() {
        return itemsOnSale;
    }
    /**
     * Get the inventoryList list
     * @return inventoryList
     */
    public List<Item> getInventoryList(){
        return inventoryList;
    }

    /**
     * Processes a new Bid Amount for an Item. Synchronized, so only 1 bid
     *  quantity can be checked at a time. At the end, method tells bidding
     *  agent if they were accepted or rejected.
     */
    public synchronized void processBid(AuctionHouseProxy agent, double bidOffer, String bankAccount, int itemID) throws InterruptedException {
        // Check if item exists. Grab it if it does.
        processingBid = true;
        Item item = null;
        for(Item product : getItemsOnSale()){
            if(product.getItemID() == itemID){
                item = product;
                break;
            }
        }

        // Check if new bid offer is greater than current bid price
        if(item != null && bidOffer >= item.getMinimumBid() && (item.getTimeLeft()>0 || !item.isBidStarted())){
            //ask bank if bidder has enough balance
            sendBankMsg("balance;"+bankAccount+";"+bidOffer);
            balanceChecked = false;
            bidAccepted = false;
            while(!balanceChecked) {
                Thread.sleep(50);
            }
            if(bidAccepted) {
                //if no previous bid, accept
                if(item.getCurrentWinner()==null) {
                    System.out.println("New Bid Winner: $" + bidOffer);
                    item.setNewBidPrice(bidOffer);
                    item.setCurrentWinner(agent);
                    item.setCurrentWinnerAccount(bankAccount);
                    item.setItemSold(false);
                    System.out.println("Item " + item.getItemName() + " with bid " + item.getCurrentBid());
                    agent.sendAgentMsg("" + Status.ACCEPTED);
                }
                //if previous bid, check that bidder is different
                else if (!item.getCurrentWinner().equals(agent) && !item.isSold()) {
                    item.resetTimer();  // Reset Timer before time runs out
                    System.out.println("New Bid Winner: $" + bidOffer);
                    item.getCurrentWinner().sendAgentMsg(""+ Status.OUTBID + " on item: "+
                                                        item.getItemName()+ " itemID: "+
                                                        item.getItemID()+ " from Auction House: "+
                                                        companyName + " New Bid: " + bidOffer);
                    //unblock old winners balance
                    sendBankMsg("unblock;"+item.getCurrentWinnerAccount()+";"+item.getCurrentBid());
                    item.setNewBidPrice(bidOffer);
                    item.setCurrentWinner(agent);
                    item.setCurrentWinnerAccount(bankAccount);
                    item.setItemSold(false);
                    System.out.println("Item " + item.getItemName() + " with bid " + item.getCurrentBid());
                    agent.sendAgentMsg("" + Status.ACCEPTED);
                }
                //reject
                else {
                    agent.sendAgentMsg("" + Status.REJECTED);
                }
            }
            else {
                agent.sendAgentMsg("" + Status.REJECTED);
            }
        }else{
            agent.sendAgentMsg("" + Status.REJECTED);
        }
        processingBid = false;
    }

    /**
     * Finalizing the bid after the 30 sec ended. Notify the winner
     * @param item the item that is being bid
     * @throws InterruptedException
     */
    public void finalizeBid(Item item) throws InterruptedException {
        while(processingBid) {
            Thread.sleep(50);
        }
        if(item.getTimeLeft()<=0) {
            System.out.println("Item Sold.");
            item.getCurrentWinner().sendAgentMsg(""+Status.WINNER+", Won Item: "+item.getItemName()+
                                                 ", ItemID: "+item.getItemID()+", From Auction House: "+
                                                companyName);
            item.getCurrentWinner().sendAgentMsg("finalize;"+this.bankAccount+";"+item.getCurrentBid());
            item.setDefaults(); // Sometimes reappears in menu, this removes previous bid prices.

            System.out.println("Item: " + item.getItemName() + " is it in the list: " + getItemsOnSale().contains(item));
            getItemsOnSale().remove(item);
            System.out.println("Item: " + item.getItemName() + " is it in the list: " + getItemsOnSale().contains(item));

            inventoryList.remove(item);  // Removes from inventory.
            if(inventoryList.size() > 0){
                getItemsOnSale().add(sellNewItem());
            }
        }
    }



    /**
     * Adding a new client to the list of connected clients
     * @param auctionHouseProxy
     */
    public void addClient(AuctionHouseProxy auctionHouseProxy) {
        connectedClients.add(auctionHouseProxy);
    }

    /**
     * Get the company name
     * @return companyName
     */
    public String getCompanyName() {
        return companyName;
    }
}