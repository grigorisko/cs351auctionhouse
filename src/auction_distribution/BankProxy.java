/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The BankProxy.java responsible for listening and sending messages
 * from it clients counterpart, and execute different tasks upon requested.
 */
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
    private BankAccount bankAccount;
    private boolean isAuctionHouse = false;
    private static ArrayList<BankAccount> bankAccounts = new ArrayList<>();
    private static ArrayList<BankProxy> bankProxies = new ArrayList<>();

    /**
     * The BankProxy constructor
     * @param clientSocket the client socket
     * @param accountNumber the client's account number
     * @throws IOException
     */
    public BankProxy(Socket clientSocket, String accountNumber) throws IOException {
        // Socket connection w/ Client
        this.clientSocket = clientSocket;
        this.bankAccount = new BankAccount(accountNumber, 0);
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        printWriter = new PrintWriter(clientSocket.getOutputStream());
    }

    /**
     * Performs the initial setup on client connection.
     * Gets names from AH,agents and initial balance from agents
     * and sends them their bank account number
     * If client is an auction house, notifies any connected agents
     * that a new auction house has connected
     * @throws IOException
     */
    private void initialSetup() throws IOException {
        // Get Initial Client Data (format: name;clientType;address;port)
        String clientInfo = bufferedReader.readLine();


        // Parse initial client data based what client connected (AH or Agent)
        if(clientInfo.toLowerCase().contains("server")){  // Client = AH
            String companyName = clientInfo.split(";")[0];

            // Infinite While Loop until given a Unique Name :)
            while(getActiveAuctionHouses().toLowerCase().contains(companyName.toLowerCase())){
                printWriter.println("Bank Requires Unique Auction-House Name.");
                printWriter.flush();

                companyName = bufferedReader.readLine().split(";")[0];
            }

            System.out.println("New <AuctionHouse> Connected w/ Server details: " + clientInfo);
            this.isAuctionHouse = true;
            // Add AuctionHouse to list of active AuctionHouses
            String auctionHouseServer = clientInfo.split("server;")[1];
            String port = auctionHouseServer.split(";")[1];
            String IPAddress = clientSocket.getRemoteSocketAddress().toString().split("/")[1].split(":")[0];
            auctionHouseServer = IPAddress + ";" + port;
            addAuctionHouse(companyName + ";" +auctionHouseServer);
            bankProxies.add(this);
            bankAccounts.add(this.bankAccount);
            System.out.println(auctionHouseServer);
            //send agents new AH info
            for(BankProxy bankProxy: bankProxies) {
                if (!bankProxy.isAuctionHouse) {
                    bankProxy.printWriter.println("newAH/"+companyName+";"+auctionHouseServer);
                    bankProxy.printWriter.flush();
                }
            }

        }else{  // Client = Agent
            System.out.println("New <Agent> Connected w/ details: " + clientInfo.split(";")[1]);
            String balanceInfo = bufferedReader.readLine();


            //this.bankAccount = new BankAccount(accountNumber, 1000);
            bankProxies.add(this);
            bankAccounts.add(this.bankAccount);
            if(!isAuctionHouse) {
                this.bankAccount.setStartingBalance(Double.parseDouble(balanceInfo.split(":")[1]));
            }
            //send clients their account number
            printWriter.println("accountnumber:" + bankAccount.getAccountNumber());
            // Send client list of active AuctionHouses
            System.out.println(getActiveAuctionHouses());
            printWriter.println(getActiveAuctionHouses());
            printWriter.flush();
        }

        printWriter.println("Bank Connection Successful.");
        printWriter.flush();

        if(isAuctionHouse) {
            printWriter.println("accountNumber:"+bankAccount.getAccountNumber());
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
     * Remove an auction house to our list of active auction houses.
     * Method used for synchronization purposes.
     * @param auctionHouse
     */
    private synchronized void removeAuctionHouse(String auctionHouse){
        activeAuctionHouses.remove(auctionHouse);
    }

    /**
     * Returns a String of all address & ports of our active auction houses.
     * @return string formatted as: address1;port address2;port
     */
    private synchronized String getActiveAuctionHouses(){
        String listAsString = "";
        for(String auctionServer: activeAuctionHouses){
            listAsString += auctionServer +"/";
        }

        return listAsString;
    }


    /**
     * Listen for Incoming Messages
     */
    @Override
    public void run() {
        try {
            initialSetup();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String clientMessage = "";
        while(clientSocket.isConnected() && clientMessage != null){
            try {
                clientMessage = bufferedReader.readLine();
                if(clientMessage != null){
                    //System.out.println(clientMessage);

                    // DO LOGIC STUFF
                    //receive message from auction house in format
                    //balance;accountNumber;bidAmount
                    if(clientMessage.contains("balance;") && this.isAuctionHouse) {
                        String[] words = clientMessage.split(";");
                        System.out.println("Checking balance of account "+words[1]);
                        for (BankAccount bankAccount:bankAccounts) {
                            if (words[1].equals(bankAccount.getAccountNumber())) {
                                if(bankAccount.checkBalance(Double.parseDouble(words[2]))) {
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
                            printWriter.println("Available Balance: " + this.bankAccount.getBalance());
                            printWriter.flush();
                        }
                        else {
                            double totalBalance = bankAccount.getBalance()+bankAccount.getLockedBalance();
                            printWriter.println("Available Balance: " + bankAccount.getBalance() + ", "+
                                    "Total Balance: " + totalBalance +"\n");
                            printWriter.flush();

                        }
                    }
                    else if(clientMessage.equals("AutoBidderAvailableBalance")) {
                        printWriter.println("Available Balance:"+this.bankAccount.getBalance());
                        printWriter.flush();
                    }
                    //auction house inquiry
                    //received from agent
                    else if(clientMessage.equalsIgnoreCase("auction houses")) {
                        printWriter.println(getActiveAuctionHouses());
                        printWriter.flush();
                    }
                    //finalize bid payment format
                    //finalize;auctionhouseid;amount
                    //received from agent
                    else if(clientMessage.contains("finalize;")) {
                        System.out.println("Finalizing bid");
                        String[] words = clientMessage.split(";");
                        System.out.println("Transferring funds from "+
                                this.bankAccount.getAccountNumber() +
                                " to " + words[1]);
                        for(BankAccount bankAccount:bankAccounts) {
                            if(words[1].equals(bankAccount.getAccountNumber())) {
                                this.bankAccount.sendPayment(Double.parseDouble(words[2]), bankAccount);
                            }
                        }
                        //notify auction house about payment
                        for(BankProxy bankProxy: bankProxies) {
                            if(bankProxy.bankAccount.getAccountNumber().equals(words[1])) {
                                bankProxy.printWriter.println("Received $" + Double.parseDouble(words[2]) + " payment");
                                bankProxy.printWriter.flush();
                            }
                        }
                    }
                    //unblock agent balance when outbid
                    else if(clientMessage.contains("unblock;")) {
                        //System.out.println(clientMessage);
                        String[] words = clientMessage.split(";");
                        for(BankAccount bankAccount:bankAccounts) {
                            if(words[1].equals(bankAccount.getAccountNumber())) {
                               bankAccount.unblockBalance(Double.parseDouble(words[2]));
                            }
                        }
                    }
                    //auction house deregistering
                    else if(clientMessage.contains("exiting")) {
                        if(isAuctionHouse) {
                            System.out.println("Auction House " + clientMessage.split("/")[0] + " exiting");
                            //remove auction house from list
                            for (String s : activeAuctionHouses) {
                                if (s.split(";")[0].equalsIgnoreCase(clientMessage.split("/")[0])) {
                                    removeAuctionHouse(s);
                                    break;
                                }
                            }
                            printWriter.println("exit acknowledged");
                            printWriter.flush();
                        }
                        else {
                            printWriter.println("exit acknowledged");
                            printWriter.flush();
                            System.out.println(clientMessage);
                        }
                        //remove this bankproxy
                        bankProxies.remove(this);
                    }
                    else {
                        printWriter.println("Incorrect message");
                        printWriter.flush();
                    }

                    // Send message back.
                    //printWriter.println("Thanks for responding.");
                    //printWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
