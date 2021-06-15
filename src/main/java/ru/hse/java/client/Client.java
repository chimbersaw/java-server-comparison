package ru.hse.java.client;

import ru.hse.java.utils.Params;
import ru.hse.java.utils.Utils;

import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static java.lang.Long.max;


public class Client implements Callable<Long> {
    private final int id;

    public Client(int id) {
        this.id = id;
    }

    @Override
    public Long call() throws Exception {
        long sumTime = 0;
        try (Socket socket = new Socket("localhost", Params.PORT)) {
            for (int i = 0; i < Params.NUM_REQUESTS_PER_CLIENT; i++) {
                int[] data = generateArray();
                long time = System.currentTimeMillis();

                Utils.writeArray(socket.getOutputStream(), data);
                Utils.readArray(socket.getInputStream());

                sumTime += System.currentTimeMillis() - time;

                Thread.sleep(max(0, Params.CLIENT_DELTA));
            }
        }
        return sumTime / Params.NUM_REQUESTS_PER_CLIENT;
    }

    private int[] generateArray() {
        Random random = new Random();
        return IntStream.generate(random::nextInt).limit(Params.ARRAY_LENGTH).toArray();
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
            System.out.println("Client " + id + " is OK.");
        } else {
            System.out.println("Client " + id + " fails.");
        }
    }
}
