package LocSu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DateServer {
    static final int PORT = 4000;
    static String[] names = {"Wily", "Felix", "Carlsbad", "Hobob"};
    static String[] adjs = {"the gentle", "the un-gentle", "the overwrought", "the urbane"};

    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(4);
    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(PORT);

        while (true) {
            System.out.println("[SERVER] Waiting for client connection. . .");
            Socket client = listener.accept();
            System.out.println("[SERVER] Connected to client!");
            ClientHandler clientThread = new ClientHandler(client, clients);
            clients.add(clientThread);

            pool.execute(clientThread);
        }

//        PrintWriter out = new PrintWriter(client.getOutputStream(),true);
//        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
//        try {
//            while (true) {
//                String request = in.readLine();
//                if (request.contains("name")) {
//                    out.println(getRandomName());
//                } else {
//                    out.println("Type 'tell me a name' to get a random name");
//                }
//            }
//        } finally {
//            out.close();
//            in.close();
//        }

        //System.out.println("[SERVER] Name sent. Closing.");

//        client.close();
//        listener.close();
    }
    public static String getRandomName() {
        String name = names[(int) (Math.random()*names.length)];
        String adj = adjs[(int) (Math.random()*adjs.length)];
        return name + " " + adj;
    }
}
