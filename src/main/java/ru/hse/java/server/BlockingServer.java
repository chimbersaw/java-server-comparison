package ru.hse.java.server;

import ru.hse.java.utils.Params;
import ru.hse.java.utils.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.hse.java.utils.Params.NUM_THREADS;

public class BlockingServer extends Server {
    private ServerSocket serverSocket;
    private final ExecutorService workerThreadPool = Executors.newFixedThreadPool(NUM_THREADS);
    private final ConcurrentHashMap.KeySetView<ClientData, Boolean> clients = ConcurrentHashMap.newKeySet();

    @Override
    protected void acceptClients() throws IOException {
        serverSocket = new ServerSocket(Params.PORT);
        while (isWorking.get()) {
            Socket socket = serverSocket.accept();
            ClientData clientData = new ClientData(socket);
            clients.add(clientData);
            clientData.processClient();
        }
    }

    @Override
    protected void closeServer() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        workerThreadPool.shutdown();
        clients.forEach(ClientData::close);
    }

    private class ClientData {
        private final Socket socket;
        private final ExecutorService responseWriter = Executors.newSingleThreadExecutor();
        private final ExecutorService requestReader = Executors.newSingleThreadExecutor();

        private final DataInputStream inputStream;
        private final DataOutputStream outputStream;

        private ClientData(Socket socket) throws IOException {
            this.socket = socket;
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
        }

        public void sendResponse(int[] data) {
            responseWriter.submit(() -> {
                try {
                    Utils.writeArray(outputStream, data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        public void processClient() {
            requestReader.submit(() -> {
                try {
                    while (isWorking.get() && socket.isConnected()) {
                        int[] data = Utils.readArray(inputStream);
                        workerThreadPool.submit(() -> {
                            Utils.bubbleSort(data);
                            sendResponse(data);
                        });
                    }
                } catch (IOException ignored) {
                } finally {
                    close();
                }
            });
        }

        public void close() {
            responseWriter.shutdown();
            requestReader.shutdown();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
