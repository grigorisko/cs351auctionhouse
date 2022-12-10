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
                    //System.out.println(message);
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
                        //System.out.println(message);
                        autoBidderProxy.sendBankMsg(message);
                        autoBidderProxy.decreaseActiveBids();
                    }
                    //handle auction house exiting
                    //close socket and remove from AH list
                    else if(message.contains("exiting")) {
                        autoBidderToAHSocket.close();
                        autoBidderProxy.removeAuctionHouse(message.split(" ")[0]);
                    }
                    //System.out.println(message);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    public Socket getAutoBidderToAHSocket() {
        return autoBidderToAHSocket;
    }

    public void setAhMessageParsed(boolean ahMessageParsed) {
        this.ahMessageParsed = ahMessageParsed;
    }

    public boolean isAhMessageParsed() {
        return ahMessageParsed;
    }

    public String getReturnMessage() {
        return returnMessage;
    }
}
