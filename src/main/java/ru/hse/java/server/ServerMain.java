package ru.hse.java.server;

import java.io.IOException;
import java.util.Scanner;

public class ServerMain {
    public static void main(String[] args) {
        try {
            Server server = new BlockingServer();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
