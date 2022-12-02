package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class AgentProxy implements Runnable{
    private Socket agentToBankSocket;
    private BufferedReader in;
    private PrintWriter out;
    private ArrayList<AgentProxy> clients;
    private int bankBalance;
    private int trueBalance;
    private String clientName;
    private ArrayList<Socket> connectedAHs;  //List of Available Auction Houses

    public AgentProxy(Socket agentSocket, ArrayList<AgentProxy> clients) throws IOException {
        this.agentToBankSocket = agentSocket;
        this.clients = clients;
        in = new BufferedReader(new InputStreamReader(agentSocket.getInputStream()));
        out = new PrintWriter(agentSocket.getOutputStream(), true);
        this.bankBalance = 0;
        this.trueBalance = 0;
        System.out.println("New Bidder Connected.");
    }

    public AgentProxy(Socket socket) throws IOException {
        // Gets username
        System.out.println("Bidding Username:");
        Scanner scanner = new Scanner(System.in);
        clientName = scanner.nextLine();

        // Create Agent-Bank Connection
        this.agentToBankSocket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        this.bankBalance = 0;
        this.trueBalance = 0;
        System.out.println("Connected to a new Server.");

        // Send Initial Client Information to Bank
        sendBankMsg("agent;");

        // Get List of Active Auction Houses from Bank
        connectedAHs = new ArrayList<>();
        String[] activeAHs = in.readLine().strip().split(" ");
        System.out.println("Active Auction Houses = " + Arrays.toString(activeAHs));

        // Connect to ALL Active Auction Houses
        for(String ahServer : activeAHs){
            String address = ahServer.split(";")[0];
            int port = Integer.parseInt(ahServer.split(";")[1]);
            System.out.println("Trying to Connect to " + address + " w/ port " + port);
            Socket newConnectedServer = new Socket(address, port);
            connectedAHs.add(newConnectedServer);

        }


        // Start Independent Threads
        consoleInput();
    }


    private void sendBankMsg(String message){
        out.println(message);
        out.flush();
    }


    /**
     * Gets Console Input
     */
    private void consoleInput(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);
                while(agentToBankSocket.isConnected()){
                    String input = scanner.nextLine();

                    // Find Keywords & Process input
                    // bid 3000 to ah1
                    if(input.contains("bid")){
                        System.out.println("You want to bid...");


                    }
                    if(input.contains("balance")){
                        System.out.println("You want to check balance...");
                    }

                }
            }
        }).start();
    }


    /**
     * Listen for Incoming Messages (Bank or Auction Houses)
     */
    @Override
    public void run() {
        String clientMessage = "";
        while(true){
            try {
                while(agentToBankSocket.isConnected()){
                    // Read income message
                    String message = in.readLine();

                    // Figure out who it's from

                    // Do logic, based on who it's from


                    System.out.println(message);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }
    // For testing purposes
    public void outToAll(String msg) {
        for (AgentProxy aClient : clients) {
            aClient.out.println(msg);
        }
    }
}