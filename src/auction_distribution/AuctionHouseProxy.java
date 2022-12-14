/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The AuctionHouseProxy.java responsible for listening and sending messages
 * from it clients counterpart, and execute different tasks upon requested.
 */
package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AuctionHouseProxy implements Runnable{
    private Socket clientSocket;
    private BufferedReader bufferedReader;
    private PrintWriter printerWriter;
    // Holds a reference to the AuctionHouse (used for our inventory)
    private AuctionHouse auctionHouse;

    public AuctionHouseProxy(Socket clientSocket, AuctionHouse auctionHouse) throws IOException {
        this.auctionHouse = auctionHouse;
        this.clientSocket = clientSocket;
        //add to list of connected agents
        auctionHouse.addClient(this);
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        printerWriter = new PrintWriter(clientSocket.getOutputStream());

        System.out.println("New Bidder Connected.");
    }

    /**
     * Sends a message from Auction House to Agent
     */
    public void sendAgentMsg(String message){
        printerWriter.println(message);
        printerWriter.flush();
    }

    /**
     * Listen for Incoming Messages from an Agent
     */
    @Override
    public void run() {
        String clientMessage = "";
        while(clientSocket.isConnected() && clientMessage != null){
            try{
                clientMessage = bufferedReader.readLine();
                if(clientMessage != null){
                    System.out.println(clientMessage);
                    //return list of items
                    String itemMsg = "Items/";
                    if(clientMessage.equals("items")) {
                        List<Item> itemsOnSale = auctionHouse.getItemsOnSale();
                        if(itemsOnSale.size() > 0){
                            System.out.print("Pushing Items for Sale:");
                            for(Item thingy: auctionHouse.getItemsOnSale()){
                                System.out.print(" " + thingy.getItemName());
                            }
                            System.out.println();
                            for(Item i:itemsOnSale) {
                                itemMsg+=i.getItemName();
                                itemMsg+= ":";
                                itemMsg+=i.getItemID();
                                itemMsg+= ":";
                                itemMsg+=i.getDescription();
                                itemMsg+= ":";
                                itemMsg+=i.getCurrentBid();
                                itemMsg+= ":";
                                itemMsg+=i.getMinimumBid();
                                itemMsg+= ":";
                                itemMsg+=i.getTimeLeft();
                                itemMsg+= ";";
                            }
                        }else{
                            itemMsg += "null";
                        }

                        printerWriter.println(itemMsg);
                        printerWriter.flush();
                    }
                    else if (clientMessage.contains("autoBidder")) {
                        List<Item> itemsOnSale = auctionHouse.getItemsOnSale();
                        itemMsg = "Items/";
                        if(itemsOnSale.size() > 0) {
                            for (Item i : itemsOnSale) {
                                itemMsg += auctionHouse.getCompanyName();
                                itemMsg += ":";
                                itemMsg += i.getItemID();
                                itemMsg += ":";
                                itemMsg += i.getMinimumBid();
                                itemMsg += ":";
                                itemMsg += i.getCurrentWinnerAccount();
                                itemMsg += ";";
                            }
                        }
                        else {
                            itemMsg+="null";
                        }
                        printerWriter.println(itemMsg);
                        printerWriter.flush();
                    }
                    else if(clientMessage.contains("trybid:")) {
                        //startbid
                        String[] bidRequest = clientMessage.split(":");
                        int itemID = Integer.parseInt(bidRequest[1]);
                        double bid = Double.parseDouble(bidRequest[2]);
                        String bankAccount = bidRequest[3];
                        System.out.println("Processing New $" + bid + " bid on ItemID " + itemID);

                        auctionHouse.processBid(this, bid, bankAccount, itemID);
                    }else if(clientMessage.contains("name")){
                        printerWriter.println("name:" + auctionHouse.getCompanyName());
                        printerWriter.flush();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    //get the client socket
    public Socket getClientSocket() {
        return clientSocket;
    }
}
