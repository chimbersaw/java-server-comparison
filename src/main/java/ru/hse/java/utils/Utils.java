package ru.hse.java.utils;

import com.google.common.primitives.Ints;
import ru.hse.java.proto.ArrayProto.Array;

import java.io.*;
import java.nio.ByteBuffer;

public class Utils {
    public static void bubbleSort(int[] array) {
        boolean sorted = false;
        int temp;
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < array.length - 1; i++) {
                if (array[i] > array[i + 1]) {
                    temp = array[i];
                    array[i] = array[i + 1];
                    array[i + 1] = temp;
                    sorted = false;
                }
            }
        }
    }

    public static int[] readArray(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);

        int msgSize = dataInputStream.readInt();
        Array data = Array.parseFrom(inputStream.readNBytes(msgSize));

        return data.getElemList().stream().mapToInt(x -> x).toArray();
    }

    public static int[] readArray(ByteBuffer buffer) throws IOException {
        Array data = Array.parseFrom(buffer);

        return data.getElemList().stream().mapToInt(x -> x).toArray();
    }

    public static void writeArray(OutputStream outputStream, int[] data) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        byte[] array = Array.newBuilder().setSize(data.length).addAllElem(Ints.asList(data)).build().toByteArray();

        dataOutputStream.writeInt(array.length);
        dataOutputStream.write(array);
        dataOutputStream.flush();
    }

    private Utils() {}
}
