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

public class AgentAHProxy implements Runnable{
    private Socket agentToAHSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String returnMessage;
    private AgentProxy agentProxy;
    private boolean ahMessageParsed;

    private static ArrayList<AgentAHProxy> agentAHProxies = new ArrayList<>();

    private String ahName;


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
                        agentProxy.decreaseActiveBids();
                    }
                    else if(message.contains("WINNER")) {
                        System.out.println(message);
                    }
                    //receive message from AH that bid was won
                    //format finalize;ahBankAccount;amount
                    else if(message.contains("finalize")) {
                        System.out.println(message);
                        agentProxy.sendBankMsg(message);
                        agentProxy.decreaseActiveBids();
                    }
                    //handle auction house exiting
                    //close socket and remove from AH list
                    else if(message.contains("exiting")) {
                        agentToAHSocket.close();
                        agentProxy.removeAuctionHouse(message.split(" ")[0]);
                    }
                    //System.out.println(message);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    public Socket getAgentToAHSocket() {
        return agentToAHSocket;
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