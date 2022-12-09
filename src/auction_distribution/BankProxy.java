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

    public BankProxy(Socket clientSocket, String accountNumber) throws IOException {
        // Socket connection w/ Client
        this.clientSocket = clientSocket;

        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        printWriter = new PrintWriter(clientSocket.getOutputStream());

        // Get Initial Client Data (format: name;clientType;address;port)
        String clientInfo = bufferedReader.readLine();


        // Parse initial client data based what client connected (AH or Agent)
        if(clientInfo.toLowerCase().contains("server")){  // Client = AH
            System.out.println("New <AuctionHouse> Connected w/ Server details: " + clientInfo);
            this.bankAccount = new BankAccount(accountNumber, 0);
            this.isAuctionHouse = true;
            // Add AuctionHouse to list of active AuctionHouses
            String companyName = clientInfo.split(";")[0];
            String auctionHouseServer = clientInfo.split("server;")[1];
            addAuctionHouse(companyName + ";" +auctionHouseServer);
            bankProxies.add(this);
            bankAccounts.add(this.bankAccount);
            //send agents new AH info
            for(BankProxy bankProxy: bankProxies) {
                if (!bankProxy.isAuctionHouse) {
                    bankProxy.printWriter.println("newAH"+getActiveAuctionHouses());
                    bankProxy.printWriter.flush();
                }
            }

        }else{  // Client = Agent
            System.out.println("New <Agent> Connected w/ details: " + clientInfo);
            String balanceInfo = bufferedReader.readLine();


            this.bankAccount = new BankAccount(accountNumber, 1000);
            bankProxies.add(this);
            bankAccounts.add(this.bankAccount);
            if(!isAuctionHouse) {
                this.bankAccount.setStartingBalance(Double.parseDouble(balanceInfo.split(":")[1]));
            }
            //send clients their account number
            printWriter.println("accountnumber:" + bankAccount.getAccountNumber());
            // Send client list of active AuctionHouses
            printWriter.println(getActiveAuctionHouses());
            printWriter.flush();
        }

        printWriter.println("Bank Connection Successful.");
        printWriter.flush();

        if(isAuctionHouse) {
            printWriter.println("accountNumber:"+bankAccount.getAccountNumber());
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
                    //balance;accountNumber;bidAmount
                    if(clientMessage.contains("balance;") && this.isAuctionHouse) {
                        System.out.println(clientMessage);
                        String[] words = clientMessage.split(";");
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
                        System.out.println(clientMessage);
                        String[] words = clientMessage.split(";");
                        for(BankAccount bankAccount:bankAccounts) {
                            if(words[1].equals(bankAccount.getAccountNumber())) {
                                System.out.println("hello");
                                this.bankAccount.sendPayment(Double.parseDouble(words[2]), bankAccount);
                            }
                        }
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
