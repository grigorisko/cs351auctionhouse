/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The AgentProxy.java responsible for listening and sending messages
 * from it clients counterpart, and execute different tasks upon requested. This AgentProxy mainly ]
 * focus on the interaction with the bank
 */
package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class AgentProxy implements Runnable{
    private Socket agentToBankSocket;
    private BufferedReader in;
    private PrintWriter out;
    private int bankBalance;
    private int trueBalance;

    private String balanceMessage = "";

    private String bankAccountNumber;
    private String clientName;
    private List<Item> chosenList;
    private HashMap<String, AgentAHProxy> connectedAHs;
    private AgentAHProxy selectedAH;
    private String selectedAH_Name; // Used for emergency disconnect.
    private boolean messageParsed = true; //boolean to wait for message parsing before displaying in console
    private Agent agent;
    private int activeBids = 0;
    private String bidStatus;
    private boolean ahMessageParsed = false;
    private String ahMessage;

    /**
     * AgentProxy's constructor
     * @param socket
     * @param agent
     * @throws IOException
     */
    public AgentProxy(Socket socket, Agent agent) throws IOException {
        this.agent = agent;
        // Gets username
        System.out.println("Bidding Username:");
        Scanner scanner = new Scanner(System.in);
        clientName = scanner.nextLine();
        System.out.println("Initial Balance:");
        double initialBalance = scanner.nextDouble();
        String initialBalanceMsg = "initialbalance:" + initialBalance;

        // Create Agent-Bank Connection
        this.agentToBankSocket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        this.bankBalance = 0;
        this.trueBalance = 0;
        System.out.println("Connected to Bank.");


        // Send Initial Client Information to Bank
        sendBankMsg("agent;"+clientName);

        sendBankMsg(initialBalanceMsg);


        // Get List of Active Auction Houses from Bank
        connectedAHs = new HashMap<>();
        // Get bank account number
        bankAccountNumber = in.readLine().split(":")[1];
        String[] activeAHs = in.readLine().strip().split("/");
        //System.out.println(activeAHs.toString());
        System.out.println("Active Auction Houses = " + Arrays.toString(activeAHs));
        // Connect to ALL Active Auction Houses
        // check that there are auction houses
        if(activeAHs.length > 0 && !activeAHs[0].equals("")) {
            for (String ahServer : activeAHs) {
                String serverName = ahServer.split(";")[0];
                String address = ahServer.split(";")[1];
                int port = Integer.parseInt(ahServer.split(";")[2]);
                System.out.println("Trying to Connect to " + address + " w/ port " + port);
                Socket newConnectedServer = new Socket(address, port);
                //AgentAHProxy ahProxy = new AgentAHProxy(newConnectedServer, this);
                Thread ahProxyThread = new Thread(new AgentAHProxy(newConnectedServer, this, serverName));
                ahProxyThread.start();
            }
        }
        // Start Independent Threads
        consoleInput();
    }

    /**
     * Get the list of all available AuctionHouses
     * @return
     */
    private synchronized String[] getListOfAHs(){
        String[] list = new String[connectedAHs.size()];

        int i = 0;
        for(String serverName: connectedAHs.keySet()){
            list[i] = serverName;
            i++;
        }

        return list;
    }

    /**
     * Send message to the
     * @param message
     */
    public void sendBankMsg(String message) {
        out.println(message);
        out.flush();
    }

    /**
     * Menu enum
     */
    enum Menu {
        FIRST,
        SECOND,
        THIRD,
        EXIT
    }
    /**
     * Gets Console Input
     */
    private void consoleInput(){
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            Menu menu = Menu.FIRST;
            String items = "";
            try {
                while(agentToBankSocket.isConnected()){
                    //print correct menu depending on state
                    if(menu.equals(Menu.FIRST)) {
                        System.out.println("1. Check Balance");
                        System.out.println("2. View Available Auction Houses");
                        System.out.println("3. Exit");
                        String menuInput = scanner.nextLine();
                        if(menuInput.equals("1")) {
                            //send message to bank to retrieve balance
                            sendBankMsg("balance");
                            messageParsed = false;
                            //wait for message to be parsed by listener
                            while(!messageParsed) {
                                Thread.sleep(50);
                            }
                        }
                        else if(menuInput.equals("2")) {
                            menu = Menu.SECOND;
                        }
                        else if(menuInput.equals("3")) {
                            //exitlogic
                            //check if active bids
                            //if not, exit
                            if(activeBids==0) {
                                out.println("Agent " + clientName + " exiting");
                                for (AgentAHProxy ahProxy : connectedAHs.values()) {
                                    //String temp = ahProxy.sendAHMsg("Agent "+clientName+" exiting");
                                    ahProxy.sendAHMsg("Agent " + clientName + " exiting");
                                    ahProxy.getAgentToAHSocket().close();
                                }
                                menu = Menu.EXIT;
                            }
                            else {
                                System.out.println("Cannot exit while bids are active");
                            }
                        }

                    }
                    //Auction house display menu
                    //Get all auction houses and display them in a menu list
                    else if(menu.equals(Menu.SECOND)) {
                        if (connectedAHs.size()>0) {
                            int i = 1;
                            for(String ahName : getListOfAHs()){
                                System.out.println(i + ". " + ahName);
                                i++;
                            }
                        }
                        else System.out.println("No available Auction Houses");
                        int previousEntry = connectedAHs.size()+1;
                        System.out.println(previousEntry + ". Previous Menu");
                        String menuInput = scanner.nextLine();
                        //go back to previous menu
                        if(menuInput.equals("" + previousEntry)) {
                            menu = Menu.FIRST;
                        }
                        else {
                            // Grabs & stores all AH items for sale.
                            int i = 0;
                            for(AgentAHProxy ahProxy : connectedAHs.values()){
                                int menuEntry = i + 1;
                                String auctionHouseOption = "" + menuEntry;
                                if(menuInput.equals(auctionHouseOption)){
                                    selectedAH = ahProxy;
                                    ahProxy.sendAHMsg("name");
                                    selectedAH.setAhMessageParsed(false);
                                    while(!selectedAH.isAhMessageParsed()){
                                        Thread.sleep(50);
                                    }
                                    selectedAH_Name = selectedAH.getReturnMessage();

                                    selectedAH.setAhMessageParsed(false);
                                    selectedAH.sendAHMsg("items");
                                    while(!selectedAH.isAhMessageParsed()){
                                        Thread.sleep(50);
                                    }
                                    items = selectedAH.getReturnMessage();
                                    //set auction house that user chose
                                    menu = Menu.THIRD;
                                }
                                i++;
                            }
                        }
                    }
                    //display third page menu, which is the item menu of a specific AH
                    else if(menu.equals(Menu.THIRD)) {
                        //get items on sale from selected auction house
                        //items = selectedAH.sendAHMsg("items");
                        try{
                            selectedAH.setAhMessageParsed(false);
                            selectedAH.sendAHMsg("items");
                            while(!selectedAH.isAhMessageParsed()){
                                Thread.sleep(50);
                            }
                            items = selectedAH.getReturnMessage();
                            String[] itemsOnSale = items.split(";");
                            List<String> itemStrings = new ArrayList<>();
                            for(String s:itemsOnSale) {
                                String itemInfo = "";
                                String[] splitItemInfo = s.split(":");
                                itemInfo = "Item Name: " + splitItemInfo[0] + ", Item ID: " + splitItemInfo[1] + ", Item Description: " + splitItemInfo[2]
                                        + ", Current Bid: " + splitItemInfo[3] + ", Minimum Bid: " + splitItemInfo[4]
                                        + ", Time Left: " + splitItemInfo[5];
                                itemStrings.add(itemInfo);
                            }
                            // Print prompt after our request in case we need to abort, console doesn't look funky.
                            System.out.println("Bid on an item using format: Row BidAmount");
                            for(int i=0;i<itemStrings.size();i++){
                                int menuEntry = i+1;
                                System.out.println(menuEntry + ". " +itemStrings.get(i));
                            }
                            int previousEntry = itemStrings.size()+1;
                            System.out.println(previousEntry + ". Previous Menu");
                            System.out.println(previousEntry+1 + ". Refresh Menu");
                            String menuInput = scanner.nextLine();
                            //go back to previous menu
                            if(menuInput.equals("" + previousEntry)) {
                                menu = Menu.SECOND;
                            }else if(menuInput.equals("" + previousEntry+1)){
                                continue;  // Refreshes Menu items.
                            }
                            else {
                                //prevent bid message console lock if auction house
                                //disconnected when we tried to place a bid
                                if(!selectedAH.getAgentToAHSocket().isClosed()) {
                                    // Displays all items in an AH to bid
                                    if (menuInput.split(" ").length == 2) {
                                        String desiredRow = menuInput.split(" ")[0];
                                        String bidPrice = menuInput.split(" ")[1];
                                        for (int i = 0; i < itemStrings.size(); i++) {
                                            int menuEntry = i + 1;
                                            String itemSelected = "" + menuEntry;
                                            if (desiredRow.equals(itemSelected)) {
                                                //set item to bid on
                                                String[] words = menuInput.split(" ");
                                                try {
                                                    // This giant line of code just grabs itemID from our
                                                    // item description "Item Name: ~, Item ID: ~,...."
                                                    int itemID = Integer.parseInt(itemStrings.get(Integer.parseInt(desiredRow) - 1).split(", ")[1].split("Item ID: ")[1]);
                                                    double bid = Double.parseDouble(bidPrice);
                                                    //send bid to auction house
                                                    System.out.println("Bid Sent.");
                                                    //String bidStatus = selectedAH.sendAHMsg("trybid:"+itemID+":"+bid+":"+bankAccountNumber);
                                                    selectedAH.setAhMessageParsed(false);
                                                    selectedAH.sendAHMsg("trybid:" + itemID + ":" + bid + ":" + bankAccountNumber);
                                                    while (!selectedAH.isAhMessageParsed()) {
                                                        Thread.sleep(50);
                                                    }
                                                    String bidStatus = selectedAH.getReturnMessage();
                                                    System.out.println(bidStatus);
                                                    if (bidStatus.equals("ACCEPTED")) {
                                                        increaseActiveBids();
                                                    }
                                                } catch (NumberFormatException e) {
                                                    System.out.println("Incorrect bid amount input");
                                                }
                                            }
                                        }

                                    }
                                }else if(selectedAH.getAgentToAHSocket().isClosed()){
                                    //Inform the user that the auction house
                                    //disconnected and go back to the second menu
                                    System.out.println("Auction House Disconnected");
                                    menu = Menu.SECOND;
                                }else {
                                    System.out.println("Incorrect input");
                                }
                            }
                        }catch (Exception e){
                            // Activates when an error happens
                            System.out.println("Auction House Empty...removing from list.");
                            removeAuctionHouse(selectedAH_Name);
                            menu = Menu.SECOND;
                        }

                    }

                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                out.close();
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }


    /**
     * Listen for Incoming Messages From Bank ONLY
     */
    @Override
    public void run() {
        String clientMessage = "";
        while(true){
            try {
                while(agentToBankSocket.isConnected() && !agentToBankSocket.isClosed()){
                    // Read income message
                    String message = in.readLine();
                    //new auction house connected
                    if(message.contains("newAH/")) {
                        String ahs = message.split("/")[1];
                        String ahName = ahs.split(";")[0];
                        String address = ahs.split(";")[1];
                        int port = Integer.parseInt(ahs.split(";")[2]);
                        System.out.println("Trying to Connect to " + address + " w/ port " + port);
                        Socket newConnectedServer = new Socket(address, port);
                        Thread ahProxyThread = new Thread(new AgentAHProxy(newConnectedServer, this, ahName));
                        ahProxyThread.start();
                    }
                    if(message.contains("Available Balance:")) {
                        balanceMessage = message;
                        //print out balance
                        System.out.println(balanceMessage);
                        messageParsed = true;
                    }
                    if(message.contains("accountnumber:")) {
                        bankAccountNumber = message.split(":")[1];
                    }
                    if(message.contains("exit acknowledged")) {
                        agentToBankSocket.close();
                        agent.exit();
                    }
                    // Figure out who it's from

                    // Do logic, based on who it's from

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    /**
     * Increase the active bid
     */
    public synchronized void increaseActiveBids() {
        activeBids++;
    }

    /**
     * Decrease the active bid
     */
    public synchronized void decreaseActiveBids() {
        activeBids--;
    }

    /**
     * Add a new AuctionHouse into the list of AuctionHouse
     * @param agentAHProxy
     * @param name
     */
    public void addAuctionHouse(AgentAHProxy agentAHProxy, String name) {
        connectedAHs.put(name,agentAHProxy);
    }

    /**
     * Remove a AuctionHouse into the list of AuctionHouse
     * @param auctionHouseName
     */
    public void removeAuctionHouse(String auctionHouseName) {
        connectedAHs.remove(auctionHouseName);
    }
}