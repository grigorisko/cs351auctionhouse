/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The Agent.java is Client that responsible for creating connection between
 * itself to the AuctionHouse and the Bank. Requesting access to the list of AuctionHouses
 * , sending different commands using the AgentProxy
 */
package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Agent {
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String username;

    /**
     * Agent constructor
     * @throws IOException
     */
    public Agent() throws IOException {
        System.out.println("Enter Bank Address/Hostname");
        Scanner scanner = new Scanner(System.in);
        String bankAddress = scanner.nextLine();
        System.out.println("Enter Bank port");
        String portString = scanner.nextLine();
        int port = Integer.parseInt(portString);
        // Create Agent Proxy for Communication to Bank & AHs
        AgentProxy agentProxy = new AgentProxy(new Socket(bankAddress, port), this);
        agentProxy.run();
    }

    public static void main(String[] args) throws IOException {
        Agent agent = new Agent();
    }

    public static void exit() {
        System.exit(0);
    }
}
