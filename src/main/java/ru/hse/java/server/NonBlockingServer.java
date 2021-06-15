package ru.hse.java.server;

import com.google.common.primitives.Ints;
import ru.hse.java.proto.ArrayProto.Array;
import ru.hse.java.utils.Params;
import ru.hse.java.utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

import static ru.hse.java.utils.Params.NUM_THREADS;

public class NonBlockingServer extends Server {
    private final ExecutorService workerThreadPool = Executors.newFixedThreadPool(NUM_THREADS);
    private ServerSocketChannel serverSocketChannel;
    private Selector readSelector;
    private Selector writeSelector;
    private final ExecutorService readSelectorThreadPool = Executors.newSingleThreadExecutor();
    private final ExecutorService writeSelectorThreadPool = Executors.newSingleThreadExecutor();
    private final Queue<ClientData> readSelectorQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ClientData> writeSelectorQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap.KeySetView<ClientData, Boolean> clients = ConcurrentHashMap.newKeySet();

    private void readRequests() throws IOException {
        while (isWorking.get()) {
            if (readSelector.select() > 0) {
                Iterator<SelectionKey> iterator = readSelector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    ClientData client = (ClientData) key.attachment();
                    client.read();
                    iterator.remove();
                }
            }
            while (!readSelectorQueue.isEmpty()) {
                ClientData newClient = readSelectorQueue.poll();
                newClient.channel.register(readSelector, SelectionKey.OP_READ, newClient);
            }
        }
    }

    private void writeResponses() throws IOException {
        while (isWorking.get()) {
            if (writeSelector.select() > 0) {
                Iterator<SelectionKey> iterator = writeSelector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    ClientData client = (ClientData) key.attachment();
                    if (client.write()) {
                        key.cancel();
                    }
                    iterator.remove();
                }
            }
            while (!writeSelectorQueue.isEmpty()) {
                ClientData newClient = writeSelectorQueue.poll();
                newClient.channel.register(writeSelector, SelectionKey.OP_WRITE, newClient);
            }
        }
    }

    @Override
    protected void acceptClients() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(Params.PORT));

        readSelector = Selector.open();
        readSelectorThreadPool.submit(() -> {
            try {
                readRequests();
            } catch (IOException e) {
                e.printStackTrace();
                shutdown();
            }
        });

        writeSelector = Selector.open();
        writeSelectorThreadPool.submit(() -> {
            try {
                writeResponses();
            } catch (IOException e) {
                e.printStackTrace();
                shutdown();
            }
        });

        while (isWorking.get()) {
            SocketChannel channel = serverSocketChannel.accept();
            channel.configureBlocking(false);
            ClientData client = new ClientData(channel);
            clients.add(client);
            readSelectorQueue.add(client);
            readSelector.wakeup();
        }
    }

    @Override
    protected void closeServer() {
        try {
            serverSocketChannel.close();
            readSelector.close();
            writeSelector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        readSelectorThreadPool.shutdown();
        writeSelectorThreadPool.shutdown();
        workerThreadPool.shutdown();
        clients.forEach(ClientData::close);
    }

    private class ClientData {
        private final SocketChannel channel;
        private final Queue<ByteBuffer> writeBuffers = new ConcurrentLinkedQueue<>();
        private ByteBuffer bodyBuffer;
        private final ByteBuffer headerBuffer = ByteBuffer.allocate(Integer.BYTES);
        boolean headerRead = false;
        int headerBytesRead = 0;
        int bodyBytesRead = 0;

        public ClientData(SocketChannel channel) {
            this.channel = channel;
        }

        public void processRequest(ByteBuffer inputBuffer) {
            try {
                int[] data = Utils.readArray(inputBuffer);
                Utils.bubbleSort(data);
                byte[] array = Array.newBuilder().setSize(data.length).addAllElem(Ints.asList(data)).build().toByteArray();
                ByteBuffer outputBuffer = ByteBuffer.allocate(Integer.BYTES + array.length).putInt(array.length).put(array);
                outputBuffer.flip();
                writeBuffers.add(outputBuffer);
                writeSelectorQueue.add(this);
                writeSelector.wakeup();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void read() {
            if (!headerRead) {
                try {
                    headerBytesRead += channel.read(headerBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if (headerBytesRead == Integer.BYTES) {
                    headerBytesRead = 0;
                    headerRead = true;
                    headerBuffer.flip();
                    int size = headerBuffer.getInt();
                    bodyBuffer = ByteBuffer.allocate(size);
                    headerBuffer.clear();
                }
            } else {
                try {
                    bodyBytesRead += channel.read(bodyBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if (bodyBytesRead == bodyBuffer.capacity()) {
                    bodyBytesRead = 0;
                    headerRead = false;
                    bodyBuffer.flip();
                    workerThreadPool.submit(() -> processRequest(bodyBuffer));
                }
            }
        }

        public boolean write() {
            ByteBuffer buffer = writeBuffers.peek();
            if (buffer == null) return true;

            try {
                channel.write(buffer);
                if (!buffer.hasRemaining()) {
                    writeBuffers.poll();
                }
            } catch (IOException e) {
                writeBuffers.poll();
                e.printStackTrace();
            }

            return writeBuffers.isEmpty();
        }

        public void close() {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
