package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class AgentProxy implements Runnable{
    private Socket agentToBankSocket;
    private BufferedReader in;
    private PrintWriter out;
    private ArrayList<AgentProxy> clients;
    private int bankBalance;
    private int trueBalance;

    private String balanceMessage = "";

    private String bankAccountNumber;
    private String clientName;
    private List<Item> chosenList;
    private ArrayList<AgentAHProxy> connectedAHs; //proxy list of AHs
    private AgentAHProxy selectedAH;
    private boolean messageParsed = true; //boolean to wait for message parsing before displaying in console

    public AgentProxy(Socket agentSocket, ArrayList<AgentProxy> clients) throws IOException {
        this.agentToBankSocket = agentSocket;
        this.clients = clients;
        in = new BufferedReader(new InputStreamReader(agentSocket.getInputStream()));
        out = new PrintWriter(agentSocket.getOutputStream(), true);
        this.bankBalance = 0;
        this.trueBalance = 0;
        System.out.println("New Bidder Connected.");
    }

    public AgentProxy(Socket socket) throws IOException {
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
        connectedAHs = new ArrayList<>();
        // Get bank account number
        bankAccountNumber = in.readLine().split(":")[1];
        String[] activeAHs = in.readLine().strip().split(" ");
        System.out.println(activeAHs.toString());
        System.out.println("Active Auction Houses = " + Arrays.toString(activeAHs));
        // Connect to ALL Active Auction Houses
        // check that there are auction houses
        if(activeAHs.length>1) {
            for (String ahServer : activeAHs) {
                String address = ahServer.split(";")[0];
                int port = Integer.parseInt(ahServer.split(";")[1]);
                System.out.println("Trying to Connect to " + address + " w/ port " + port);
                Socket newConnectedServer = new Socket(address, port);
                AgentAHProxy ahProxy = new AgentAHProxy(newConnectedServer);
                connectedAHs.add(ahProxy);
                //connectedAHs.add(newConnectedServer);

            }
        }


        // Start Independent Threads
        consoleInput();
    }


    private void sendBankMsg(String message) {
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
                        else if(menuInput.equals("3"));{
                            //exitlogic
                            //check if active bids
                            //if not, exit
                        }

                    }
                    //Auction house display menu
                    //Get all auction houses and display them in a menu list
                    else if(menu.equals(Menu.SECOND)) {
                        if (connectedAHs.size()>0) {
                            for (int i = 0; i < connectedAHs.size(); i++) {
                                int menuEntry = i + 1;
                                System.out.println(menuEntry + ". Auction House " + menuEntry);
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
                            for(int i=0;i<connectedAHs.size();i++) {
                                int menuEntry = i + 1;
                                String auctionHouseOption = "" + menuEntry;
                                if (menuInput.equals(auctionHouseOption)) {
                                    selectedAH = connectedAHs.get(i);
                                    items = selectedAH.sendAHMsg("items");
                                    System.out.println(items);
                                    //set auction house that user chose
                                    menu = Menu.THIRD;
                                }
                            }
                        }
                    }
                    //display third page menu, which is the item menu of a specific AH
                    else if(menu.equals(Menu.THIRD)) {
                        System.out.println("Bid on an item using format: itemID bidAmount");
                        //get items on sale from selected auction house
                        String[] itemsOnSale = items.split(";");
                        List<String> itemStrings = new ArrayList<>();
                        for(String s:itemsOnSale) {
                            String itemInfo = "";
                            String[] splitItemInfo = s.split(":");
                            itemInfo = "Item Name: " + splitItemInfo[0] + ", Item Description: " + splitItemInfo[1]
                                    + ", Current Bid: " + splitItemInfo[2] + ", Minimum Bid: " + splitItemInfo[3]
                                    + ", Time Left: " + splitItemInfo[4];
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
                            for(int i=0;i<itemStrings.size();i++){
                                int menuEntry = i + 1;
                                String itemSelected = "" + menuEntry;
                                if (menuInput.equals(itemSelected)) {
                                    //set item to bid on
                                    String[] words = menuInput.split(" ");
                                    if (words.length==2) {
                                        try {
                                            double bid = Double.parseDouble(words[1]);
                                            //send bid to auction house
                                            selectedAH.sendAHMsg("trybid:"+itemSelected+":"+bid);

                                        } catch (NumberFormatException e) {
                                            System.out.println("Incorrect bid amount input");
                                        }
                                    }
                                    else System.out.println("Incorrect input");
                                }
                            }
                        }
                    }

                    /*
                    String input = scanner.nextLine();

                    // Find Keywords & Process input
                    // bid 3000 to ah1\
                    if (input.contains("Set bank balance ")) {
                        int firstSpace = input.indexOf(" ");
                        int secSpace = input.indexOf(" ", firstSpace + 1);
                        int thirdSpace = input.indexOf(" ", secSpace + 1);
                        if (firstSpace != -1) {
                            setBankBalance(Integer.parseInt(input.substring(thirdSpace + 1)));
                        }
                    } else if (input.equals("What is my bank balance?")) {
                        printBalance(getBankBalance());
                    } else if (input.contains("Bid")) { // Format "bid [item] for [x amount] at [ah?]"
                        int firstSpace = input.indexOf(" ");
                        int secondSpace = input.indexOf(" ", firstSpace + 1);
                        int thirdSpace = input.indexOf(" ", secondSpace + 1);
                        int forthSpace = input.indexOf(" ", thirdSpace + 1);
                        int fifthSpace = input.indexOf(" ", forthSpace + 1);
                        String item = input.substring(firstSpace + 1, secondSpace);
                        //System.out.println(input.substring(thirdSpace + 1, forthSpace));
                        int itemPrice = Integer.parseInt(input.substring(thirdSpace + 1, forthSpace));
                        String chosenAuctionHouse = input.substring(fifthSpace + 1);
                        System.out.println("Item is: " + item);
                        System.out.println("Item's price: " + itemPrice);
                        System.out.println("Chosen ah: " + chosenAuctionHouse);
                    } else if (input.equals("What are the available items?")) { // Does not work. Maybe try to intialize AHProxy to use it for calling
                        System.out.println(1);
                        chosenList = AuctionHouse.getItemsOnSale();
                        printList(chosenList);
                    }
//                    if(input.contains("bid")){
//                        System.out.println("You want to bid...");
//
//                    }
//                    if(input.contains("balance")){
//                        System.out.println("You want to check balance...");
//                    }
//
//                    else{
//                        System.out.println("unable to find keywords...");
//                    }*/
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
     * Listen for Incoming Messages (Bank or Auction Houses)
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
                            String address = ahs[i].split(";")[0];
                            int port = Integer.parseInt(ahs[i].split(";")[1]);
                            System.out.println("Trying to Connect to " + address + " w/ port " + port);
                            Socket newConnectedServer = new Socket(address, port);
                            AgentAHProxy ahProxy = new AgentAHProxy(newConnectedServer);
                            if(i==(connectedAHs.size()+1)) {
                                connectedAHs.add(ahProxy);
                            }
                            //connectedAHs.add(newConnectedServer);

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
    // For testing purposes
    public void outToAll(String msg) {
        for (AgentProxy aClient : clients) {
            aClient.out.println(msg);
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
}