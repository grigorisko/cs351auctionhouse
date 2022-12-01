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
    private static ArrayList<BankProxy> clients = new ArrayList();
    private Socket clientSocket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String clientName;
    private int ID;

    public BankProxy(Socket clientSocket, int id) throws IOException {
        this.clientSocket = clientSocket;
        this.ID = id;
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        printWriter = new PrintWriter(clientSocket.getOutputStream());

        System.out.println("New Client Connected.");

        // Add to our clients list
        clients.add(this);
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


                    // Send message back.
                    printWriter.println("Thanks for responding.");
                    printWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
