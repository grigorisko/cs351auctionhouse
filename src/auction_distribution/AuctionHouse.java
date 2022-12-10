package auction_distribution;
/** TODO
 *  Create the hashmap for easier access with the Host and the port information
 *  Maybe a function that give out the port and host information through the name of the company (auction house)
 *  Communicate to the bank?????????
 */

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

    public AuctionHouse() throws IOException {
        auctionHouse = this;

//        // Gets company name
//        System.out.println("Company Name:");
//        Scanner scanner = new Scanner(System.in);
//        companyName = scanner.nextLine();
        System.out.println("Enter Bank Address");
        Scanner scanner = new Scanner(System.in);
        String bankAddress = scanner.nextLine();
        // Create Bank-AuctionHouse Connection
        bankAHSocket = new Socket(bankAddress, 4999);
        bufferedReader = new BufferedReader(new InputStreamReader(bankAHSocket.getInputStream()));
        printWriter = new PrintWriter(bankAHSocket.getOutputStream());

        // Create AuctionHouse Server
        int random = new Random().nextInt(1000,5000);
        serverSocket = new ServerSocket(random);  // TODO: change this to be actual address & port

        //Send Server Data to bank. Infinite Loop until Connection Successful :)
        String bankConnectionMsg = "";
        do{
            // Gets company name
            System.out.println("Company Name:");
            scanner = new Scanner(System.in);
            companyName = scanner.nextLine();

            // Get Server Address & Port
            int serverPort = serverSocket.getLocalPort();
            sendBankMsg(companyName + ";server;localhost;"+serverPort);

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
                while(true){
                    try {
                        while(bankAHSocket.isConnected()){
                            String message = bufferedReader.readLine();
                            //bank confirmed bid
                            if(message.equals("Bid accepted")) {
                                balanceChecked = true;
                                bidAccepted = true;
                            }
                            //bank declined bid
                            else if(message.equals("Insufficient funds")) {
                                balanceChecked = true;
                                bidAccepted = false;
                            }
                            //bank sending us our account number
                            else if(message.contains("accountNumber:"))
                            {
                                bankAccount = message.split(":")[1];
                            }
                            System.out.println(message);
                        }
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
                            printWriter.println(companyName + " exiting");
                            printWriter.flush();
                            try {
                                //close bank socket
                                bankAHSocket.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            //notify agents that we are exiting
                            for(AuctionHouseProxy auctionHouseProxy:connectedClients) {
                                    auctionHouseProxy.sendAgentMsg(companyName+" exiting");
                            }
                            System.exit(0);
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

    private Item sellNewItem() {
        Random rand = new Random();
        Item itemToSell = null; 
        while (true) {
            Item tempItem = inventoryList.get(rand.nextInt(inventoryList.size()));
            if (!itemsOnSale.contains(tempItem)) {
                itemToSell = tempItem;
                break;
            }
        }
        itemToSell.setItemID(itemID);
        itemID++;
        return itemToSell;
    }

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

                itemsOnSale.add(sellNewItem());

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        AuctionHouse auctionHouse = new AuctionHouse();
        auctionHouse.listen();
        auctionHouse.sendConsoleInput();
        auctionHouse.startServer();
    }
    public List<Item> getItemsOnSale() {
        return itemsOnSale;
    }

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

            System.out.println("Item: " + item.getItemName() + " is it in the list: " + itemsOnSale.contains(item));
            itemsOnSale.remove(item);  // Removes from menu of top 3 items.
            System.out.println("Item: " + item.getItemName() + " is it in the list: " + itemsOnSale.contains(item));

            inventoryList.remove(item);  // Removes from inventory.
            System.out.print("Updated Inventory List:");
            for(Item thingy : inventoryList){
                System.out.print(" " + thingy.getItemName());
            }
            System.out.println();

            System.out.print("Updated itemOnSale List:");
            for(Item thingy : itemsOnSale){
                System.out.print(" " + thingy.getItemName());
            }
            System.out.println();
            if(inventoryList.size() > 0){
                itemsOnSale.add(sellNewItem());
            }
        }
    }

    //add a client to the list of connected clients
    public void addClient(AuctionHouseProxy auctionHouseProxy) {
        connectedClients.add(auctionHouseProxy);
    }


}