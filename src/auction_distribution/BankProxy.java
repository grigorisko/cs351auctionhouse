package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Waits for input from Auction House to send to Bank
 */
public class BankProxy implements Runnable{
    private static ArrayList<String> activeAuctionHouses = new ArrayList<>();
    private Socket clientSocket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private int ID;
    private int balance;
    private int lockedBalance=0;
    private boolean isAuctionHouse = false;
    private static ArrayList<BankProxy> bankAccounts = new ArrayList<>();

    public BankProxy(Socket clientSocket, int id) throws IOException {
        // Socket connection w/ Client
        this.clientSocket = clientSocket;
        this.ID = id;
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        printWriter = new PrintWriter(clientSocket.getOutputStream());

        // Get Initial Client Data (format: name;clientType;address;port)
        String clientInfo = bufferedReader.readLine();

        // Parse initial client data based what client connected (AH or Agent)
        if(clientInfo.toLowerCase().contains("server")){  // Client = AH
            System.out.println("New <AuctionHouse> Connected w/ Server details: " + clientInfo);

            this.balance = 0;
            this.isAuctionHouse = true;

            // Add AuctionHouse to list of active AuctionHouses
            String auctionHouseServer = clientInfo.split("server;")[1];
            addAuctionHouse(auctionHouseServer);

            bankAccounts.add(this);

        }else{  // Client = Agent
            System.out.println("New <Agent> Connected w/ details: " + clientInfo);

            this.balance = 1000;
            bankAccounts.add(this);

            // Send client list of active AuctionHouses
            printWriter.println(getActiveAuctionHouses());
            printWriter.flush();
        }
    }

    /**
     * Adds an auction house to our list of active auction houses.
     *  Method used for synchronization purposes.
     * @param auctionHouse
     */
    private synchronized void addAuctionHouse(String auctionHouse){
        activeAuctionHouses.add(auctionHouse);
    }

    /**
     * Returns a String of all address & ports of our active auction houses.
     * @return string formatted as: address1;port address2;port
     */
    private synchronized String getActiveAuctionHouses(){
        String listAsString = "";
        for(String auctionServer: activeAuctionHouses){
            listAsString = listAsString + " " + auctionServer;
        }

        return listAsString;
    }


    /**
     * Listen for Incoming Messages
     */
    @Override
    public void run() {
        String clientMessage = "";
        while(clientSocket.isConnected() && clientMessage != null){
            try {
                clientMessage = bufferedReader.readLine();
                if(clientMessage != null){
                    System.out.println(clientMessage);

                    // DO LOGIC STUFF
                    //receive message from auction house in format
                    //balance;id;bidAmount
                    if(clientMessage.contains("balance;") && this.isAuctionHouse) {
                        String[] words = clientMessage.split(";");
                        for (BankProxy bankProxy:bankAccounts) {
                            if (Integer.parseInt(words[1]) == bankProxy.getID()) {
                                if(bankProxy.checkBalance(Integer.parseInt(words[2]))) {
                                    //accept bid
                                    //lock balance
                                    printWriter.println("Bid accepted");
                                    printWriter.flush();
                                }
                                else{
                                    printWriter.println("Insufficient funds");
                                    printWriter.flush();
                                }
                            }
                        }
                    }
                    //balance inquiry
                    //received from either
                    else if(clientMessage.equalsIgnoreCase("balance")) {
                        if (this.isAuctionHouse) {
                            printWriter.println("Available Balance: " + this.getBalance());
                        }
                        else {
                            int totalBalance = this.getBalance()+this.lockedBalance;
                            printWriter.println("Available Balance: " + this.getBalance() + ", "+
                                    "Total Balance: " + totalBalance);
                        }
                    }
                    //auction house inquiry
                    //received from agent
                    else if(clientMessage.equalsIgnoreCase("auction houses")) {
                        printWriter.println(getActiveAuctionHouses());
                    }
                    //finalize bid payment format
                    //finalize;auctionhouseid;amount
                    //received from agent
                    else if(clientMessage.contains("finalize;")) {
                        String[] words = clientMessage.split(";");
                        BankProxy auctionHouse;
                        for(BankProxy bankProxy:bankAccounts) {
                            if(Integer.parseInt(words[1]) == bankProxy.getID()) {
                                sendPayment(Integer.parseInt(words[2]), bankProxy);
                            }
                        }
                    }
                    else {
                        printWriter.println("Incorrect message");
                        printWriter.flush();
                    }

                    // Send message back.
                    printWriter.println("Thanks for responding.");
                    printWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //check if balance is enough for bid
    public synchronized boolean checkBalance(int bidAmount) {
        if(bidAmount<=this.balance) {
            this.balance-=bidAmount;
            this.lockedBalance+=bidAmount;
            return true;
        }
        else {
            return false;
        }
    }

    //send a payment to specified account
    public synchronized void sendPayment(int amount,BankProxy account) {
        this.lockedBalance -= amount;
        account.receivePayment(amount);
    }

    //add to balance when receiving a payment
    public synchronized void receivePayment(int amount) {
        this.balance+=amount;
    }

    public int getID() {
        return ID;
    }

    public int getBalance() {
        return balance;
    }
}
