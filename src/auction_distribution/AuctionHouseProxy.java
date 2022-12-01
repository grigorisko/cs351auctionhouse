package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AuctionHouseProxy implements Runnable{
    private Socket clientSocket;
    private BufferedReader bufferedReader;
    private PrintWriter printerWriter;

    public AuctionHouseProxy(Socket clientSocket) throws IOException {
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

                    // DO LOGIC STUFF


                    // Send message back.
                    printerWriter.println("Processing Bid.");
                    printerWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
