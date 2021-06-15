package ru.hse.java.server;

import ru.hse.java.utils.Constants;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Server {
    private volatile boolean isWorking = true;
    private ServerSocket serverSocket;
    private ExecutorService serverSocketService;

    public void start() throws IOException {
        serverSocketService = Executors.newSingleThreadExecutor();
        serverSocket = new ServerSocket(Constants.PORT);

        serverSocketService.submit(() -> {
            try (ServerSocket ignored = serverSocket) {
                while (isWorking) {
                    acceptClients(serverSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void shutdown() throws IOException {
        isWorking = false;
        serverSocket.close();
        serverSocketService.shutdown();
        closeServer();
    }

    abstract protected void acceptClients(ServerSocket serverSocket);

    abstract protected void closeServer();
}
