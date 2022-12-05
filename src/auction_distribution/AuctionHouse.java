package auction_distribution;
/** TODO
 *  Create the hashmap for easier access with the Host and the port information
 *  Maybe a function that give out the port and host information through the name of the company (auction house)
 *  Communicate to the bank?????????
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Is a Client (to Bank) and Server (to Agent)Bid
 */
public class AuctionHouse {
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String companyName;
    private List<Item> inventoryList = new ArrayList<Item>();
    private static List<Item> itemsOnSale = new ArrayList<Item>();
    private int itemID = 0;

    public AuctionHouse() throws IOException {

        // Gets company name
        System.out.println("Company Name:");
        Scanner scanner = new Scanner(System.in);
        companyName = scanner.nextLine();

        // Create Bank-AuctionHouse Connection
        socket = new Socket("localhost", 4999);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter = new PrintWriter(socket.getOutputStream());

        // Create AuctionHouse Server
        int random = new Random().nextInt(1000,5000);
        serverSocket = new ServerSocket(random);  // TODO: change this to be actual address & port

        // Send Server Data to bank
        int serverPort = serverSocket.getLocalPort();
        sendBankMsg("server;localhost;"+serverPort);

        initializeInventory();
        System.out.println(itemsOnSale.toString());
    }

    /**
     * Sends a message to Bank
     * @param message
     */
    private void sendBankMsg(String message){
        printWriter.println(companyName + ";" + message);
        printWriter.flush();
    }

    /**
     * Starts AuctionHouse Server
     * @throws IOException
     */
    private void startServer() throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!serverSocket.isClosed()){
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Thread thread = new Thread(new AuctionHouseProxy(clientSocket));
                        thread.start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
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
                    printWriter.println(companyName + ": " + message);
                    printWriter.flush();
                }
            }
        }).start();
    }

    private Item sellNewItem() {
        Random rand = new Random();
        Item itemToSell = inventoryList.get(rand.nextInt(inventoryList.size()));
        itemToSell.setItemID(itemID);
        itemID++;
        return itemToSell;
    }

    private void initializeInventory() {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader
                             (getClass().getClassLoader().getResourceAsStream("inventory")))) {
            String line;
            String[] words;
            while ((line = reader.readLine()) != null) {
                words = line.split(" ");
                String itemName = words[0];
                String description="";
                for (int i=1;i<words.length;i++) {
                    if(i==words.length-1) {
                        description += words[i];
                    }
                    else {
                        description += words[i] + " ";
                    }
                }
                Item newItem = new Item(itemName,description);
                inventoryList.add(newItem);
            }
            for (int i=0;i<3;i++) {
                itemsOnSale.add(sellNewItem());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        AuctionHouse auctionHouse = new AuctionHouse();
        auctionHouse.listen();
        auctionHouse.sendConsoleInput();
        auctionHouse.startServer();
    }
    public static List<Item> getItemsOnSale() {
        return itemsOnSale;
    }

    public List<Item> getInventoryList(){
        return inventoryList;
    }


}