package systemy.node;

import systemy.common.HashingUtil; // IMPORTING ROLE A's CODE!

import java.net.InetAddress;
import java.util.Scanner;

public class NodeApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RestClient restClient = new RestClient();

        System.out.println("=========================================");
        System.out.println("      System Y - Node Simulator          ");
        System.out.println("=========================================");

        System.out.print("Enter a unique name for this node (e.g., Node-1): ");
        String nodeName = scanner.nextLine();

        // NEW: We must hash our own name to get the ID for Role B's server!
        int myNodeId = HashingUtil.hash(nodeName);
        System.out.println("--> Calculated Node ID: " + myNodeId);

        String myIp = "127.0.0.1";
        try {
            myIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.out.println("⚠️ Could not determine local IP. Defaulting to 127.0.0.1.");
        }

        // Updated to send the Integer ID, not the String name
        System.out.println("Attempting to register [" + myNodeId + "] at IP [" + myIp + "]...");
        boolean registered = restClient.registerNode(myNodeId, myIp);

        if (!registered) {
            System.out.println("❌ Registration failed. Exiting...");
            return;
        }

        System.out.println("✅ Successfully registered with the Naming Server!");

        // The Graceful Shutdown Hook
        String finalMyIp = myIp;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[System] Intercepted shutdown signal. Cleaning up...");
            // Updated to send both ID and IP to match Role B's DELETE mapping
            restClient.removeNode(myNodeId, finalMyIp);
            System.out.println("[System] Node safely removed. Goodbye!");
        }));

        // The Interactive Command Line Loop
        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Find a file location");
            System.out.println("2. Exit and remove node");
            System.out.print("Choose an option (1 or 2): ");

            String choice = scanner.nextLine();

            if ("1".equals(choice)) {
                System.out.print("Enter the filename (e.g., doc1.txt): ");
                String filename = scanner.nextLine();

                System.out.println("🔍 Asking Naming Server for location...");
                String resultIp = restClient.getFileLocation(filename);

                System.out.println("--> Result: " + resultIp);
            }
            else if ("2".equals(choice)) {
                System.exit(0);
            } else {
                System.out.println("⚠️ Invalid option. Please type 1 or 2.");
            }
        }
    }
}