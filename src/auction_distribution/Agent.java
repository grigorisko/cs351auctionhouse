package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Agent {
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String username;
    private ArrayList<Socket> connectedAHs;  //List of Available Auction Houses

    public Agent() throws IOException {
        // Gets username
        System.out.println("Bidding Username:");
        Scanner scanner = new Scanner(System.in);
        username = scanner.nextLine();

        // Create Agent-Bank Connection
        socket = new Socket("localhost", 4999);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter = new PrintWriter(socket.getOutputStream());

        // Send Initial Client Information
        sendBankMsg(username + ";agent");

        // Get List of Active Auction Houses
        connectedAHs = new ArrayList<>();
        String activeAHs = bufferedReader.readLine();
        System.out.println("Active Auction Houses = " + activeAHs);

        // Connect to ALL Active Auction Houses

    }

    private void sendBankMsg(String message){
        printWriter.println(message);
        printWriter.flush();
    }


    /**
     * Listens for incoming messages
     */
    private void listen(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        while(socket.isConnected()){
                            String message = bufferedReader.readLine();
                            System.out.println(message);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }).start();
    }


    /**
     * Sends a message, when we type something into Console.
     */
    private void sendConsoleInput(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);

                while(socket.isConnected()){
                    String message = scanner.nextLine();
                    printWriter.println(username + ": " + message);
                    printWriter.flush();
                }
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        Agent agent = new Agent();
        agent.listen();
        agent.sendConsoleInput();
    }
}
