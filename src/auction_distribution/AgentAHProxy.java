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

    public AgentAHProxy(Socket auctionHouseSocket) throws IOException {
        this.agentToAHSocket = auctionHouseSocket;
        in = new BufferedReader(new InputStreamReader(agentToAHSocket.getInputStream()));
        out = new PrintWriter(agentToAHSocket.getOutputStream(), true);
    }

    /**
     * Send a message to the auction house
     */
    public String sendAHMsg(String message) throws IOException {
        out.println(message);
        out.flush();
        String response = in.readLine();
        return response;
    }


    /**
     * Listen for Incoming Messages from Auction House
     */
    @Override
    public void run() {
        String clientMessage = "";
        while(true){
            try {
                while(agentToAHSocket.isConnected()){
                    // Read income message
                    String message = in.readLine();
                    System.out.println(message);

                    //System.out.println(message);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

}