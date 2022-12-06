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
    private String clientName;
    private List<Item> chosenList;
    private ArrayList<Socket> connectedAHs;  //List of Available Auction Houses

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

        // Create Agent-Bank Connection
        this.agentToBankSocket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        this.bankBalance = 0;
        this.trueBalance = 0;
        System.out.println("Connected to a new Server.");

        // Send Initial Client Information to Bank
        sendBankMsg("agent;");

        // Get List of Active Auction Houses from Bank
        connectedAHs = new ArrayList<>();
        String[] activeAHs = in.readLine().strip().split(" ");
        System.out.println(activeAHs.toString());
        System.out.println("Active Auction Houses = " + Arrays.toString(activeAHs));

        // Connect to ALL Active Auction Houses
        for(String ahServer : activeAHs){
            String address = ahServer.split(";")[0];
            int port = Integer.parseInt(ahServer.split(";")[1]);
            System.out.println("Trying to Connect to " + address + " w/ port " + port);
            Socket newConnectedServer = new Socket(address, port);
            connectedAHs.add(newConnectedServer);

        }


        // Start Independent Threads
        consoleInput();
    }


    private void sendBankMsg(String message){
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
                            String balance = in.readLine();
                            System.out.println(balance);
                            menu = Menu.FIRST;
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
                    else if(menu.equals(Menu.SECOND)) {
                        for(int i=0;i<connectedAHs.size();i++) {
                            int menuEntry = i+1;
                            System.out.println(menuEntry +". Auction House " + menuEntry);
                        }
                        int previousEntry = connectedAHs.size()+1;
                        System.out.println(previousEntry + ". Previous Menu");
                        String menuInput = scanner.nextLine();
                        if(menuInput.equals("" + previousEntry)) {
                            menu = Menu.FIRST;
                        }
                        else {
                            for(int i=0;i<connectedAHs.size();i++) {
                                int menuEntry = i + 1;
                                String auctionHouseOption = "" + menuEntry;
                                if (menuInput.equals(auctionHouseOption)) {
                                    //set auction house
                                    menu = Menu.THIRD;
                                }
                            }
                        }
                    }
                    else if(menu.equals(Menu.THIRD)) {
                        System.out.println("Bid on an item using format: itemID bidAmount");
                        //get items on sale from selected auction house
                        for(int i=0;i<3;i++){//for(int i=0;i<items.size();i++) {
                            int menuEntry = i+1;
                            //System.out.println(menuEntry + ". " +items[1].Info);
                        }
                        int previousEntry = 3+1;//items.size() + 1;
                        System.out.println(previousEntry + ". Previous Menu");
                        String menuInput = scanner.nextLine();
                        if(menuInput.equals("" + previousEntry)) {
                            menu = Menu.SECOND;
                        }
                        else {
                            for(int i=0;i<3;i++){//for(int i=0;i<items.size();i++) {
                                int menuEntry = i + 1;
                                String itemSelected = "" + menuEntry;
                                if (menuInput.equals(itemSelected)) {
                                    //set item to bid on
                                    String[] words = menuInput.split(" ");
                                    try{
                                        int bid = Integer.parseInt(words[1]);
                                        //send bid to auction house
                                    } catch (NumberFormatException e) {
                                        System.out.println("Incorrect bid amount input");
                                    }
                                }
                            }
                        }
                    }


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
//                    }
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

                    // Figure out who it's from

                    // Do logic, based on who it's from


                    System.out.println(message);
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