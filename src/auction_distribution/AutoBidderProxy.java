/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The AutoBidderProxy.java is a class that handles the automated bidding.
 * It gets all available items from all connected auction houses and chooses
 * a random item to bid on.
 */
package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class AutoBidderProxy implements Runnable{
    private Socket autoBidderToBankSocket;
    private BufferedReader in;
    private PrintWriter out;
    private int bankBalance;
    private double trueBalance;

    private String balanceMessage = "";

    private String bankAccountNumber;
    private String clientName;
    private HashMap<String, AutoBidderAHProxy> connectedAHs;
    private boolean messageParsed = true; //boolean to wait for message parsing before displaying in console
    private AutoBidder autoBidder;
    private int activeBids = 0;
    private boolean biddingActive = false;
    private int timer = 0;
    private int biddingInterval = 5;
    private Timer t = new Timer();
    /**
     * AutoBidderProxy's constructor
     * @param socket
     * @param autoBidder
     * @throws IOException
     */
    public AutoBidderProxy(Socket socket, AutoBidder autoBidder) throws IOException {
        this.autoBidder = autoBidder;
        // Gets username
        System.out.println("Bidding Username:");
        Scanner scanner = new Scanner(System.in);
        clientName = scanner.nextLine();
        System.out.println("Initial Balance:");
        double initialBalance = scanner.nextDouble();
        String initialBalanceMsg = "initialbalance:" + initialBalance;
        System.out.println("Enter bidding interval in seconds");
        biddingInterval = scanner.nextInt();

        // Create AutoBidder-Bank Connection
        this.autoBidderToBankSocket = socket;
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
                Thread ahProxyThread = new Thread(new AutoBidderAHProxy(newConnectedServer, this, serverName));
                ahProxyThread.start();
            }
        }
        System.out.println("Type \"start\" to start bidding, \"pause\" to stop bidding, \"exit\" to exit");
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
     * Stating the bid
     */
    private void startBidding() {
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                timer = timer-1;
                //try to bid every timer seconds
                if (timer<=0 && biddingActive) {
                    if (biddingActive) {
                        boolean bidSuccessful = false;
                        while(!bidSuccessful) {
                            //request balance from bank
                            sendBankMsg("AutoBidderAvailableBalance");
                            messageParsed = false;
                            //wait for message to be parsed by listener
                            while (!messageParsed) {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            trueBalance = Double.parseDouble(balanceMessage);
                            List<String> itemList = new ArrayList<>();
                            //get all items from all connected auction houses
                            for (AutoBidderAHProxy ahProxy : connectedAHs.values()) {
                                ahProxy.setAhMessageParsed(false);
                                try {
                                    ahProxy.sendAHMsg("autoBidderItemRequest");
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                while (!ahProxy.isAhMessageParsed()) {
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                String items = ahProxy.getReturnMessage();
                                if(!items.equals("null")) {
                                    for (String s : items.split(";")) {
                                        double minBid = Double.parseDouble(s.split(":")[2]);
                                        String currentWinner = s.split(":")[3];
                                        //only add to available item list if we can actually bid on them
                                        if (trueBalance >= minBid && !bankAccountNumber.equals(currentWinner)) {
                                            itemList.add(s);
                                        }
                                    }
                                }
                            }
                            //if there are available items, try to bid
                            if (itemList.size() > 0) {
                                //choose a random item from our list
                                Random random = new Random();
                                int itemIndex = random.nextInt(itemList.size());
                                String itemToBid = itemList.get(itemIndex);
                                String itemID = itemToBid.split(":")[1];
                                String bid = itemToBid.split(":")[2];
                                AutoBidderAHProxy selectedAH = connectedAHs.get(itemToBid.split(":")[0]);
                                selectedAH.setAhMessageParsed(false);
                                //try to bid on chosen item and wait for response
                                if (!selectedAH.getAutoBidderToAHSocket().isClosed()) {
                                    try {
                                        selectedAH.sendAHMsg("trybid:" + itemID + ":" + bid + ":" + bankAccountNumber);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    while (!selectedAH.isAhMessageParsed()) {
                                        try {
                                            Thread.sleep(50);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    String ahMessage = selectedAH.getReturnMessage();
                                    //if bid was accepted, break out of while and restart timer
                                    //else keep trying to bid
                                    if (ahMessage.contains("ACCEPTED")) {
                                        System.out.println("Placed bid on itemID " + itemID + " from auction house: "+itemToBid.split(":")[0]);
                                        bidSuccessful = true;
                                        increaseActiveBids();
                                    }
                                }

                            } else {
                                System.out.println("No available items to bid on with current balance");
                                break;
                            }
                        }
                    }
                    timer = biddingInterval;
                }
            }
        };
        t.schedule(tt,0,1000);
    }
    /**
     * Gets Console Input
     */
    private void consoleInput(){
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            String items = "";
            try {
                while(autoBidderToBankSocket.isConnected() && !autoBidderToBankSocket.isClosed()){
                    String consoleInput = scanner.nextLine();
                    if (consoleInput.equalsIgnoreCase("start")) {
                        startBidding();
                        biddingActive = true;
                    }
                    else if (consoleInput.equalsIgnoreCase("pause")) {
                        biddingActive = false;
                    }
                    else if (consoleInput.equalsIgnoreCase("exit")) {
                        if(activeBids==0) {
                            out.println("Auto Bidder " + clientName + " exiting");
                            for (AutoBidderAHProxy ahProxy : connectedAHs.values()) {
                                ahProxy.sendAHMsg("Auto Bidder " + clientName + " exiting");
                                ahProxy.getAutoBidderToAHSocket().close();
                            }
                        }
                        else {
                            System.out.println("Cannot exit while bids are active");
                        }
                    }
                }
            } catch (IOException e) {
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
                while(autoBidderToBankSocket.isConnected() && !autoBidderToBankSocket.isClosed()){
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
                        Thread ahProxyThread = new Thread(new AutoBidderAHProxy(newConnectedServer, this, ahName));
                        ahProxyThread.start();
                    }
                    if(message.contains("Available Balance:")) {
                        balanceMessage = message.split(":")[1];
                        //print out balance
                        messageParsed = true;
                    }
                    if(message.contains("accountnumber:")) {
                        bankAccountNumber = message.split(":")[1];
                    }
                    if(message.contains("exit acknowledged")) {
                        autoBidderToBankSocket.close();
                        autoBidder.exit();
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
     * @param autoBidderAHProxy
     * @param name
     */
    public void addAuctionHouse(AutoBidderAHProxy autoBidderAHProxy, String name) {
        connectedAHs.put(name,autoBidderAHProxy);
    }
    /**
     * Remove a AuctionHouse into the list of AuctionHouse
     * @param auctionHouseName
     */
    public void removeAuctionHouse(String auctionHouseName) {
        connectedAHs.remove(auctionHouseName);
    }
}