package LocSu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientHandler implements Runnable{

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ArrayList<ClientHandler> clients;
    private String clientName;
    private int bankBalance;
    private HashMap<String, Integer> items;
    public ClientHandler(Socket clientSocket, ArrayList<ClientHandler> clients) throws IOException {
        this.client = clientSocket;
        this.clients = clients;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(),true);
        this.items = new HashMap<>();
        items.put("Iphone", 3000);
        items.put("Chair", 500);
        items.put("Table", 300);
        items.put("Laptop", 4000);


    }
    @Override
    public void run() {
        try {
            while (true) {
                String request = in.readLine();
                if (request.equals("tell me a name")) {
                    out.println(DateServer.getRandomName());
                } else if (request.startsWith("say")) {
                    int firstSpace = request.indexOf(" ");
                    if (firstSpace != -1) {
                        outToAll(request.substring(firstSpace + 1));
                    }
                } else if (request.startsWith("set name")) {
                    int firstPart = request.indexOf("e");
                    int secPart = request.indexOf("e", firstPart + 1);
                    if (secPart != -1) {
                        clientName = request.substring(secPart + 1);
                        outToAll(request.substring(secPart + 1));
                    }
                } else if (request.equals("What is my name?")) {
                    printName(getClientName());
                } else if (request.contains("Set bank balance ")) {
                    int firstSpace = request.indexOf(" ");
                    int secSpace = request.indexOf(" ", firstSpace + 1);
                    int thirdSpace = request.indexOf(" ", secSpace + 1);
                    if (firstSpace != -1) {
//                        System.out.println(Integer.parseInt(request.substring(firstSpace + 1)));
                        setBankBalance(Integer.parseInt(request.substring(thirdSpace + 1)));
                        //out.println(Integer.parseInt(request.substring(thirdSpace + 1)));
                    }
                } else if (request.equals("What's my bank balance?")) {
                    printBalance(getBankBalance());
                } else {
                    out.println("Type 'tell me a name' to get a random name");
                }
            }
        } catch (IOException e) {
            System.err.println("IO exception in client handler");
            System.err.println(e.getStackTrace());
        } finally {
            out.close();
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public void outToAll(String msg) {
        for (ClientHandler aClient : clients) {
            aClient.out.println(msg);
        }
    }

    public void printName(String name) {
        out.println("This client name is: " + name);
    }

    public void printBalance(int balance) {
        out.println( getClientName() + " have " + balance + " dollars.");
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public int getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(int bankBalance) {
        this.bankBalance = bankBalance;
    }
}
