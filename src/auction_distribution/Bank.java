package auction_distribution;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

/**
 * Is a Server.
 */
public class Bank {
    ServerSocket serverSocket;
    private ArrayList<String> bankAccounts = new ArrayList<>();

    public Bank(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }


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

    //Generate a unique bank account number
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

    public static void main(String[] args) throws IOException {
        Bank bank = new Bank(4999);
        bank.startServer();
    }
}
