package ru.hse.java;

import ru.hse.java.client.Client;
import ru.hse.java.server.BlockingServer;
import ru.hse.java.server.NonBlockingServer;
import ru.hse.java.server.Server;
import ru.hse.java.utils.Params;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Application {
    private static final Scanner scanner = new Scanner(System.in);
    private static int min;
    private static int max;
    private static int step;
    private static String dynamicParam;

    public static void main(String[] args) {
        Params.SERVER_ARCHITECTURE = getArchitecture();
        Params.NUM_REQUESTS_PER_CLIENT = getNumRequestsPerClient();
        setDynamicParams();

        System.out.println("Parameters configured.");
        System.out.println("Server architecture = " + Params.SERVER_ARCHITECTURE);
        if (!dynamicParam.equals("N")) System.out.println("Array length = " + Params.ARRAY_LENGTH);
        if (!dynamicParam.equals("M")) System.out.println("Number of clients = " + Params.NUM_CLIENTS);
        if (!dynamicParam.equals("DELTA")) System.out.println("Client delta = " + Params.CLIENT_DELTA);
        System.out.println("Number of requests per client = " + Params.NUM_REQUESTS_PER_CLIENT);
        System.out.println("Dynamic param = " + dynamicParam + " with range [" + min + ", " + max + "] and step " + step);
        System.out.println("Metric: client wait time");
        System.out.println("Results:");

        Server server;
        switch (Params.SERVER_ARCHITECTURE) {
            case BLOCKING:
                server = new BlockingServer();
                break;
            case NON_BLOCKING:
                server = new NonBlockingServer();
                break;
            default:
                throw new AssertionError();
        }
        server.start();

        for (int param = min; param <= max; param += step) {
            switch (dynamicParam) {
                case "N":
                    Params.ARRAY_LENGTH = param;
                    break;
                case "M":
                    Params.NUM_CLIENTS = param;
                    break;
                case "DELTA":
                    Params.CLIENT_DELTA = param;
                    break;
                default:
                    throw new AssertionError();
            }
            System.out.println(dynamicParam + " = " + param);
            System.out.println("time = " + run() + "ms");
        }

        server.shutdown();
    }

    private static long run() {
        long sumTime = 0;

        try {
            ExecutorService threadPool = Executors.newCachedThreadPool();
            List<Future<Long>> futures = threadPool.invokeAll(
                    IntStream.range(0, Params.NUM_CLIENTS).mapToObj(Client::new).collect(Collectors.toList())
            );
            for (Future<Long> future : futures) {
                sumTime += future.get();
            }
            threadPool.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return sumTime / Params.NUM_CLIENTS;
    }

    private static void setDynamicParams() {
        while (true) {
            System.out.println("Choose param you want to be observed:");
            System.out.println("Type \"N\" or \"array length\" to choose array length");
            System.out.println("Type \"M\" or \"num clients\" to choose number of clients");
            System.out.println("Type \"DELTA\" or \"client delta\" to choose delta between client requests");
            System.out.print(">> ");
            String line = scanner.nextLine();
            switch (line) {
                case "N":
                case "array length":
                    inputDynamicArrayLength();
                    break;
                case "M":
                case "num clients":
                    inputDynamicNumClients();
                    break;
                case "DELTA":
                case "client delta":
                    inputDynamicClientDelta();
                    break;
                default:
                    System.out.println("Invalid parameter: " + line + ". Please try again");
                    continue;
            }
            break;
        }
    }

    private static void inputDynamicArrayLength() {
        dynamicParam = "N";
        setMinMaxStep("array length", Params.ARRAY_LENGTH_MIN_BOUND, Params.ARRAY_LENGTH_MAX_BOUND);
        Params.NUM_CLIENTS = getNumClients();
        Params.CLIENT_DELTA = getClientDelta();
    }

    private static void inputDynamicNumClients() {
        dynamicParam = "M";
        setMinMaxStep("number of clients", Params.NUM_CLIENTS_MIN_BOUND, Params.NUM_CLIENTS_MAX_BOUND);
        Params.ARRAY_LENGTH = getArrayLength();
        Params.CLIENT_DELTA = getClientDelta();
    }

    private static void inputDynamicClientDelta() {
        dynamicParam = "DELTA";
        setMinMaxStep("delta between client requests", Params.CLIENT_DELTA_MIN_BOUND, Params.CLIENT_DELTA_MAX_BOUND);
        Params.ARRAY_LENGTH = getArrayLength();
        Params.NUM_CLIENTS = getNumClients();
    }

    private static void setMinMaxStep(String paramName, int minBound, int maxBound) {
        min = getIntegerInRange("min for " + paramName, minBound, maxBound);
        max = getIntegerInRange("max for " + paramName, min, maxBound);
        step = getIntegerInRange("step for " + paramName, 1, max - min);
    }

    private static Params.ServerArchitecture getArchitecture() {
        while (true) {
            System.out.println("Choose server architecture: blocking or nonblocking.");
            System.out.print(">> ");
            String line = scanner.nextLine();
            line = line.strip();
            switch (line) {
                case "blocking":
                    return Params.ServerArchitecture.BLOCKING;
                case "nonblocking":
                    return Params.ServerArchitecture.NON_BLOCKING;
                default:
                    System.out.println("Invalid server architecture: " + line + ". Please try again.");
            }
        }
    }

    private static int getIntegerInRange(String paramName, int min, int max) {
        while (true) {
            System.out.println("Choose " + paramName + ": at least " + min + " and no more than " + max + ".");
            System.out.print(">> ");
            String line = scanner.nextLine();
            line = line.strip();
            int result;
            try {
                result = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println(line + " is not an 32-bit integer. Please try again.");
                continue;
            }
            if (result < min || result > max) {
                System.out.println(line + " is not from range [" + min + ", " + max + "]");
                continue;
            }
            return result;
        }
    }

    private static int getArrayLength() {
        return getIntegerInRange(
                "array length",
                Params.ARRAY_LENGTH_MIN_BOUND,
                Params.ARRAY_LENGTH_MAX_BOUND
        );
    }

    private static int getNumRequestsPerClient() {
        return getIntegerInRange(
                "number of requests per client",
                Params.NUM_REQUESTS_PER_CLIENT_MIN_BOUND,
                Params.NUM_REQUESTS_PER_CLIENT_MAX_BOUND
        );
    }

    private static int getNumClients() {
        return getIntegerInRange(
                "number of clients",
                Params.NUM_CLIENTS_MIN_BOUND,
                Params.NUM_CLIENTS_MAX_BOUND
        );
    }

    private static int getClientDelta() {
        return getIntegerInRange(
                "delta between client requests",
                Params.CLIENT_DELTA_MIN_BOUND,
                Params.CLIENT_DELTA_MAX_BOUND
        );
    }
}
