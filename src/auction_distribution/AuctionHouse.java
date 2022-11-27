package auction_distribution;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Is a Client (to Bank) and Server (to Agent)
 */
public class AuctionHouse {
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String companyName;

    public AuctionHouse(String address, int port) throws IOException {
        // Gets company name
        System.out.println("Company Name:");
        Scanner scanner = new Scanner(System.in);
        companyName = scanner.nextLine();

        socket = new Socket(address, port);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter = new PrintWriter(socket.getOutputStream());
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
     * Sends a message
     */
    private void message(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);

                while(socket.isConnected()){
                    String message = scanner.nextLine();
                    printWriter.println(companyName + ": " + message);
                    printWriter.flush();
                }
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        AuctionHouse auctionHouse = new AuctionHouse("localhost", 4999);
        auctionHouse.listen();
        auctionHouse.message();
    }

}