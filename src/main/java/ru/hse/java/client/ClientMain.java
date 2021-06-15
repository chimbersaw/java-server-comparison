package ru.hse.java.client;

import ru.hse.java.utils.Params;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClientMain {
    public static void main(String[] args) {
        // only for testing purposes
        try {
            ExecutorService threadPool = Executors.newCachedThreadPool();
            List<Future<Long>> futures = threadPool.invokeAll(
                    IntStream.range(0, Params.NUM_CLIENTS).mapToObj(Client::new).collect(Collectors.toList())
            );
            for (Future<Long> future : futures) {
                future.get();
            }
            threadPool.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
