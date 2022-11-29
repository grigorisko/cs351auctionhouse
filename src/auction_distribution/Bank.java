package auction_distribution;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Is a Server.
 */
public class Bank {
    ServerSocket serverSocket;

    public Bank(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }


    private void startServer() throws IOException {
        int id = 0;
        while(!serverSocket.isClosed()){
            // Connect Ports, Readers, Writers
            Socket clientSocket = serverSocket.accept();
            Thread clientThread = new Thread(new BankProxy(clientSocket, id));
            clientThread.start();
            id++;
        }
    }

    public static void main(String[] args) throws IOException {
        Bank bank = new Bank(4999);
        bank.startServer();
    }
}
