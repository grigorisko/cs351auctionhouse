package auction_distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Agent {
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String username;

    public Agent() throws IOException {
        // Create Agent Proxy for Communication to Bank & AHs
        AgentProxy agentProxy = new AgentProxy(new Socket("localhost", 4999), this);
        agentProxy.run();
    }

    public static void main(String[] args) throws IOException {
        Agent agent = new Agent();
    }

    public static void exit() {
        System.exit(0);
    }
}
