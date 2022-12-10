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

    public AutoBidder() throws IOException {
        System.out.println("Enter Bank Address");
        Scanner scanner = new Scanner(System.in);
        String bankAddress = scanner.nextLine();
        // Create AutoBidder Proxy for Communication to Bank & AHs
        AutoBidderProxy autoBidderProxyProxy = new AutoBidderProxy(new Socket(bankAddress, 4999), this);
        autoBidderProxyProxy.run();
    }

    public static void main(String[] args) throws IOException {
        AutoBidder autoBidder = new AutoBidder();
    }

    public static void exit() {
        System.exit(0);
    }
}