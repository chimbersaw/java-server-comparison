package ru.hse.java.server;

import java.util.Scanner;

public class ServerMain {
    public static void main(String[] args) {
        // only for testing purposes
        Server server = new NonBlockingServer();
        server.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(">> ");
            String line = scanner.nextLine();
            if (line.equals("exit")) {
                break;
            }
        }

        server.shutdown();
    }
}
