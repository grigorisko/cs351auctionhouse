/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The AutoBidder.java is Client that responsible for creating connection between
 * itself to the AuctionHouse and the Bank. It is the same with Agent.java, but automated
 */
package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class AutoBidder {
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String username;

    /**
     * AutoBidder's constructtor
     * @throws IOException
     */
    public AutoBidder() throws IOException {
        System.out.println("Enter Bank Address/Hostname");
        Scanner scanner = new Scanner(System.in);
        String bankAddress = scanner.nextLine();
        System.out.println("Enter Bank port");
        String portString = scanner.nextLine();
        int port = Integer.parseInt(portString);
        // Create AutoBidder Proxy for Communication to Bank & AHs
        AutoBidderProxy autoBidderProxyProxy = new AutoBidderProxy(new Socket(bankAddress, port), this);
        autoBidderProxyProxy.run();
    }

    public static void main(String[] args) throws IOException {
        AutoBidder autoBidder = new AutoBidder();
    }

    public static void exit() {
        System.exit(0);
    }
}