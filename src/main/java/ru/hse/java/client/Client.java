package ru.hse.java.client;

import ru.hse.java.utils.Constants;
import ru.hse.java.utils.Utils;

import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

public class Client implements Callable<Void> {
    private final int id;

    public Client(int id) {
        this.id = id;
    }

    @Override
    public Void call() throws Exception {
        int[] data = generateArray();
        try (Socket socket = new Socket("localhost", Constants.PORT)) {
            Utils.writeArray(socket.getOutputStream(), data);
            int[] sortedData = Utils.readArray(socket.getInputStream());
            checkData(data, sortedData);
        }
        return null;
    }

    private int[] generateArray() {
        Random random = new Random();
        return IntStream.generate(random::nextInt).limit(Constants.SIZE).toArray();
    }

    private void checkData(int[] data, int[] sortedData) {
        boolean isOk = data.length == sortedData.length;
        for (int i = 1; i < sortedData.length; i++) {
            if (sortedData[i - 1] > sortedData[i]) {
                isOk = false;
                break;
            }
        }
        if (isOk) {
            System.out.println("Client " + id + " is OK");
        } else {
            System.out.println("Client " + id + " fails");
        }
    }
}
