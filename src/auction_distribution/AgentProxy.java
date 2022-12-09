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
    private boolean messageParsed = true; //boolean to wait for message parsing before displaying in console
    private Agent agent;
    private int activeBids = 0;
    private String bidStatus;
    private boolean ahMessageParsed = false;
    private String ahMessage;

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
        System.out.println("Connected to a new Server.");


        // Send Initial Client Information to Bank
        sendBankMsg("agent;");

        sendBankMsg(initialBalanceMsg);


        // Get List of Active Auction Houses from Bank
        connectedAHs = new HashMap<>();
        // Get bank account number
        bankAccountNumber = in.readLine().split(":")[1];
        String[] activeAHs = in.readLine().strip().split(" ");
        System.out.println(activeAHs.toString());
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


    private synchronized String[] getListOfAHs(){
        String[] list = new String[connectedAHs.size()];

        int i = 0;
        for(String serverName: connectedAHs.keySet()){
            list[i] = serverName;
            i++;
        }

        return list;
    }

    public void sendBankMsg(String message) {
        out.println(message);
        out.flush();
    }

    //enum for menu state
    enum Menu {
        FIRST,
        SECOND,
        THIRD,
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
                                agentToBankSocket.close();
                                agent.exit();
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
                        System.out.println("Bid on an item using format: Row BidAmount");
                        //get items on sale from selected auction house
                        //items = selectedAH.sendAHMsg("items");
                        selectedAH.setAhMessageParsed(false);
                        selectedAH.sendAHMsg("items");
                        while(!selectedAH.isAhMessageParsed()){
                            Thread.sleep(50);
                        }
                        items = selectedAH.getReturnMessage();
                        System.out.println("Return Item Message: " + items);
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
                        for(int i=0;i<itemStrings.size();i++){
                            int menuEntry = i+1;
                            System.out.println(menuEntry + ". " +itemStrings.get(i));
                        }
                        int previousEntry = itemStrings.size()+1;
                        System.out.println(previousEntry + ". Previous Menu");
                        String menuInput = scanner.nextLine();
                        //go back to previous menu
                        if(menuInput.equals("" + previousEntry)) {
                            menu = Menu.SECOND;
                        }
                        else {
                            // Displays all items in an AH to bid
                            if(menuInput.split(" ").length == 2){
                                String desiredRow = menuInput.split(" ")[0];
                                String bidPrice = menuInput.split(" ")[1];
                                for(int i=0;i<itemStrings.size();i++){
                                    int menuEntry = i + 1;
                                    String itemSelected = "" + menuEntry;
                                    if (desiredRow.equals(itemSelected)) {
                                        //set item to bid on
                                        String[] words = menuInput.split(" ");
                                        try {
                                            // This giant line of code just grabs itemID from our
                                            // item description "Item Name: ~, Item ID: ~,...."
                                            int itemID = Integer.parseInt(itemStrings.get(Integer.parseInt(desiredRow)-1).split(", ")[1].split("Item ID: ")[1]);
                                            double bid = Double.parseDouble(bidPrice);
                                            //send bid to auction house
                                            System.out.println("Bid Sent.");
                                            //String bidStatus = selectedAH.sendAHMsg("trybid:"+itemID+":"+bid+":"+bankAccountNumber);
                                            selectedAH.setAhMessageParsed(false);
                                            selectedAH.sendAHMsg("trybid:"+itemID+":"+bid+":"+bankAccountNumber);
                                            while(!selectedAH.isAhMessageParsed()){
                                                Thread.sleep(50);
                                            }
                                            String bidStatus = selectedAH.getReturnMessage();
                                            System.out.println(bidStatus);
                                            if(bidStatus.equals("ACCEPTED")) {
                                                increaseActiveBids();
                                            }
                                        } catch (NumberFormatException e) {
                                            System.out.println("Incorrect bid amount input");
                                        }
                                    }
                                }
                            }else{
                                System.out.println("Incorrect input");
                            }
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
                while(agentToBankSocket.isConnected()){
                    // Read income message
                    String message = in.readLine();
                    //new auction house connected
                    if(message.contains("newAH ")) {
                        String[] ahs = message.split(" ");
                        for (int i=1;i<ahs.length;i++) {
                            String ahName = ahs[i].split(";")[0];
                            String address = ahs[i].split(";")[1];
                            int port = Integer.parseInt(ahs[i].split(";")[2]);
                            System.out.println("Trying to Connect to " + address + " w/ port " + port);
                            Socket newConnectedServer = new Socket(address, port);
                            //AgentAHProxy ahProxy = new AgentAHProxy(newConnectedServer, this);
                            Thread ahProxyThread = new Thread(new AgentAHProxy(newConnectedServer, this, ahName));
                            ahProxyThread.start();
                        }
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
                    // Figure out who it's from

                    // Do logic, based on who it's from

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    public void setBankBalance(int bankBalance) {
        this.bankBalance = bankBalance;
    }
    public int getBankBalance() {
        return bankBalance;
    }
    public void printBalance(int balance) {
        out.println(getClientName() + " have " + balance + " dollars.");
    }
    public String getClientName() {
        return clientName;
    }
    public void printList(List<Item> itemList) {
        for (Item item : itemList) {
            System.out.println("Item:" + item.getItemName()+  " price: " + item.getDefaultPrice());
        }
    }
    public synchronized void increaseActiveBids() {
        activeBids++;
    }
    public synchronized void decreaseActiveBids() {
        activeBids--;
    }
    public void addAuctionHouse(AgentAHProxy agentAHProxy, String name) {
        connectedAHs.put(name,agentAHProxy);
    }
}