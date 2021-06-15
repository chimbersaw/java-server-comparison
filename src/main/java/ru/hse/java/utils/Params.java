package ru.hse.java.utils;

public class Params {
    private Params() {}

    public enum Metric {
        CLIENT_TIME_DELAY,
        SERVER_TIME_DELAY
    }

    public enum ServerArchitecture {
        BLOCKING,
        NON_BLOCKING
    }

    public final static int PORT = 1234;
    public final static Metric METRIC = Metric.CLIENT_TIME_DELAY;
    public final static int NUM_THREADS = Runtime.getRuntime().availableProcessors() - 2;

    public static ServerArchitecture SERVER_ARCHITECTURE = ServerArchitecture.NON_BLOCKING;

    public final static int ARRAY_LENGTH_MIN_BOUND = 1;
    public static int ARRAY_LENGTH = 1000;
    public final static int ARRAY_LENGTH_MAX_BOUND = 100000;

    public final static int NUM_CLIENTS_MIN_BOUND = 1;
    public static int NUM_CLIENTS = 30;
    public final static int NUM_CLIENTS_MAX_BOUND = 10000;

    public static int CLIENT_DELTA_MIN_BOUND = 0;
    public static int CLIENT_DELTA = 30;
    public static int CLIENT_DELTA_MAX_BOUND = 2000;

    public static int NUM_REQUESTS_PER_CLIENT_MIN_BOUND = 1;
    public static int NUM_REQUESTS_PER_CLIENT = 30;
    public static int NUM_REQUESTS_PER_CLIENT_MAX_BOUND = 10000;
}
