/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The AgentAHProxy.java responsible for listening and sending messages
 * from it clients counterpart, and execute different tasks upon requested.
 * This is mainly focus on the interaction with the AuctionHouse
 */
package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class AgentAHProxy implements Runnable{
    private Socket agentToAHSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String returnMessage;
    private AgentProxy agentProxy;
    private boolean ahMessageParsed;
    private String ahName;

    /**
     * AgentAHProxy's constructor
     * @param auctionHouseSocket
     * @param agentProxy
     * @param name
     * @throws IOException
     */
    public AgentAHProxy(Socket auctionHouseSocket, AgentProxy agentProxy, String name) throws IOException {
        this.agentProxy = agentProxy;
        this.agentToAHSocket = auctionHouseSocket;
        this.ahName = name;
        agentProxy.addAuctionHouse(this,ahName);
        in = new BufferedReader(new InputStreamReader(agentToAHSocket.getInputStream()));
        out = new PrintWriter(agentToAHSocket.getOutputStream(), true);
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
                while(agentToAHSocket.isConnected() && !agentToAHSocket.isClosed()){
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
                        agentProxy.decreaseActiveBids();
                    }
                    else if(message.contains("WINNER")) {
                        System.out.println(message);
                    }
                    //receive message from AH that bid was won
                    //format finalize;ahBankAccount;amount
                    else if(message.contains("finalize")) {
                        agentProxy.sendBankMsg(message);
                        agentProxy.decreaseActiveBids();
                    }else if(message.contains("name")){
                        returnMessage = message.split(":")[1];
                        ahMessageParsed = true;
                    }
                    //handle auction house exiting
                    //close socket and remove from AH list
                    else if(message.contains("exiting")) {
                        agentToAHSocket.close();
                        String companyName = message.split("/")[1];
                        agentProxy.removeAuctionHouse(companyName);
                        System.out.println("Auction house " +
                                companyName + " exiting.");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    /**
     * Get agentToAHSocket
     * @return agentToAHSocket
     */
    public Socket getAgentToAHSocket() {
        return agentToAHSocket;
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