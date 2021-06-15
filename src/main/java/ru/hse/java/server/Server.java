package ru.hse.java.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Server {
    protected volatile AtomicBoolean isWorking = new AtomicBoolean(true);
    private final ExecutorService serverSocketService = Executors.newSingleThreadExecutor();

    public void start() {
        serverSocketService.submit(() -> {
            try {
                acceptClients();
            } catch (IOException ignored) {
                shutdown();
            }
        });
    }

    public void shutdown() {
        if (isWorking.compareAndSet(true, false)) {
            serverSocketService.shutdown();
            closeServer();
        }
    }

    abstract protected void acceptClients() throws IOException;

    abstract protected void closeServer();
}
