package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        printerWriter = new PrintWriter(clientSocket.getOutputStream());

        System.out.println("New Bidder Connected.");
    }

    /**
     * Listen for Incoming Messages
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
                    if(clientMessage.equals("items")) {
                        List<Item> itemsOnSale = auctionHouse.getItemsOnSale();
                        String itemMsg = "";
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
                        printerWriter.println(itemMsg);
                        printerWriter.flush();
                    }
                    if(clientMessage.contains("trybid:")) {
                        //startbid
                        String[] bidRequest = clientMessage.split(":");
                        int itemID = Integer.parseInt(bidRequest[1]);
                        double bid = Double.parseDouble(bidRequest[2]);
                        System.out.println("Processing New $" + bid + " bid on ItemID " + itemID);
                        boolean bidValid;  // true = accepted client bid
                        switch(itemID){
                            case 1:
                                bidValid = auctionHouse.processBid_ItemID_0(bid);
                                break;
                            case 2:
                                bidValid = auctionHouse.processBid_ItemID_1(bid);
                                break;
                            default:
                                bidValid = auctionHouse.processBid_ItemID_2(bid);
                                break;
                        }

                        if(bidValid){
                            printerWriter.println("Bid Accepted.");
                        }else{
                            printerWriter.println("Bid Rejected.");
                        }
                        printerWriter.flush();
                    }

                    // DO LOGIC STUFF


                    // Send message back.
                    //printerWriter.println("Processing Bid.");
                    //printerWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
