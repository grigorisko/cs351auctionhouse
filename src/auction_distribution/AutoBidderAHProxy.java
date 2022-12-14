/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The AutoBidderAHProxy.java is Client that responsible for creating connection between
 * the AutoBidder and the auction houses. It receives and parses messages from auction houses.
 */
package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class AutoBidderAHProxy implements Runnable{
    private Socket autoBidderToAHSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String returnMessage;
    private AutoBidderProxy autoBidderProxy;
    private boolean ahMessageParsed;
    private String ahName;

    /**
     * AutoBidderAHProxy's constructor
     * @param auctionHouseSocket
     * @param autoBidderProxy
     * @param name
     * @throws IOException
     */
    public AutoBidderAHProxy(Socket auctionHouseSocket, AutoBidderProxy autoBidderProxy, String name) throws IOException {
        this.autoBidderProxy = autoBidderProxy;
        this.autoBidderToAHSocket = auctionHouseSocket;
        this.ahName = name;
        autoBidderProxy.addAuctionHouse(this,ahName);
        in = new BufferedReader(new InputStreamReader(autoBidderToAHSocket.getInputStream()));
        out = new PrintWriter(autoBidderToAHSocket.getOutputStream(), true);
    }

    /**
     * Send a message to the auction house
     */
    public void sendAHMsg(String message) throws IOException {
        out.println(message);
        out.flush();
    }


    /**
     * Listen for Incoming Messages from Auction House
     */
    @Override
    public void run() {
        String clientMessage = "";
        while(true){
            try {
                while(autoBidderToAHSocket.isConnected() && !autoBidderToAHSocket.isClosed()){
                    // Read income message
                    String message = in.readLine();
                    if(message.contains("Items/")) {
                        returnMessage = message.split("/")[1];
                        ahMessageParsed = true;
                    }
                    else if(message.contains("ACCEPTED")) {
                        returnMessage = message;
                        ahMessageParsed = true;
                    }
                    else if(message.contains("REJECTED")) {
                        returnMessage = message;
                        ahMessageParsed = true;
                    }
                    else if(message.contains("OUTBID")) {
                        System.out.println(message);
                        autoBidderProxy.decreaseActiveBids();
                    }
                    else if(message.contains("WINNER")) {
                        System.out.println(message);
                    }
                    //receive message from AH that bid was won
                    //format finalize;ahBankAccount;amount
                    else if(message.contains("finalize")) {
                        autoBidderProxy.sendBankMsg(message);
                        autoBidderProxy.decreaseActiveBids();
                    }
                    //handle auction house exiting
                    //close socket and remove from AH list
                    else if(message.contains("exiting")) {
                        autoBidderToAHSocket.close();
                        autoBidderProxy.removeAuctionHouse(message.split("/")[1]);
                        System.out.println("Auction house " +
                                message.split("/")[1] + " exiting.");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }
    /**
     * Get AutoBidderToAHSocket
     * @return AutoBidderToAHSocket
     */
    public Socket getAutoBidderToAHSocket() {
        return autoBidderToAHSocket;
    }
    /**
     * Set ahMessageParsed
     * @param ahMessageParsed
     */
    public void setAhMessageParsed(boolean ahMessageParsed) {
        this.ahMessageParsed = ahMessageParsed;
    }
    /**
     * Check if isAhMessageParsed is true or false
     * @return true or false
     */
    public boolean isAhMessageParsed() {
        return ahMessageParsed;
    }
    /**
     * Get the returnMessage
     * @return returnMessage
     */
    public String getReturnMessage() {
        return returnMessage;
    }
}
