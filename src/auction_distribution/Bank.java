/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The Bank.java is Server that responsible for creating connection between
 * itself to the AuctionHouse and the Agent. Generate an account number
 * for each Agent whenever an Agent connect to the Bank
 */
package auction_distribution;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;


/**
 * Is a Server.
 */
public class Bank {
    ServerSocket serverSocket;
    private ArrayList<String> bankAccounts = new ArrayList<>();

    /**
     * This is the Bank constructor that will take in the port number
     * and initiate the connection
     * @param port port number
     * @throws IOException
     */
    public Bank(int port) throws IOException {
        serverSocket = new ServerSocket(port, 20, InetAddress.getLocalHost());
        System.out.println(serverSocket.getInetAddress());
        System.out.println(InetAddress.getLocalHost().getHostAddress()+":"+serverSocket.getLocalPort());
    }

    /**
     * Start the server whenever a connection is being made
     * between the Bank and the Clients
     * @throws IOException
     */
    private void startServer() throws IOException {
        while(!serverSocket.isClosed()){
            // Connect Ports, Readers, Writers
            Socket clientSocket = serverSocket.accept();
            // Assign account number to client
            String accountNumber = generateAccountNumber();
            bankAccounts.add(accountNumber);
            Thread clientThread = new Thread(new BankProxy(clientSocket, accountNumber));
            clientThread.start();
        }
    }

    /**
     * Generating an account number for
     * each Agent when a connection is being made
     * @return
     */
    private String generateAccountNumber() {
        String accountNumber = "";
        Random random = new Random();
        for (int i=0;i<8;i++) {
            int id = random.nextInt(0, 10);
            accountNumber += id;
        }
        if(bankAccounts.contains(accountNumber)) {
            accountNumber = generateAccountNumber();
        }
        return accountNumber;
    }

    /**
     * Initialize the Bank and start the server
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Bank bank = new Bank(0);
        bank.startServer();
    }
}
