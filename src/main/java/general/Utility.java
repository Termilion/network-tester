package general;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;

public class Utility {
    public static class NotImplementedException extends IOException {
        public NotImplementedException() {}
    }

    public static class InterfaceNotFoundException extends IOException {
        public InterfaceNotFoundException(String interfaceName) {
            super("Specified network interface not found! " + interfaceName);
            printAvailableInterfaces();
        }

        private void printAvailableInterfaces() {
            try {
                System.err.print("Available Interfaces:\n\n");
                for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    System.err.printf("Display name: %s\n", ni.getDisplayName());
                    System.err.printf("Name: %s\n", ni.getName());
                    Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                    for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                        System.err.printf("InetAddress: %s\n", inetAddress);
                    }
                    System.err.print("\n");
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Pair<K, V> {
        K key;
        V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

    public static byte[] generateBytes(int number) {
        byte[] bytes = new byte[number];
        new Random().nextBytes(bytes);
        return bytes;
    }

    public static byte[][] generateMbytes(int number) {
        byte[][] mBytes = new byte[1000000][number];
        for (int i = 0; i < number; i++) {
            mBytes[i] = generateBytes(1000000);
        }
        return mBytes;
    }

    public static class TransmissionPayload {
        int id;
        long time;
        int round;

        public static TransmissionPayload decode(byte[] payload) throws Exception {
            int bytesToDecode = Integer.BYTES + Long.BYTES + Integer.BYTES;
            if (payload.length < bytesToDecode) {
                throw new Exception("payload too small");
            }
            // convert from bytes
            ByteBuffer buffer = ByteBuffer.allocate(bytesToDecode);
            buffer.put(payload, 0, bytesToDecode);
            buffer.flip();

            int id = buffer.getInt();
            long time = buffer.getLong();
            int round = buffer.getInt();

            return new TransmissionPayload(id, time, round);
        }

        public TransmissionPayload(int id, long time, int round) {
            this.id = id;
            this.time = time;
            this.round = round;
        }

        public int getId() {
            return id;
        }

        public long getTime() {
            return time;
        }

        public int getRound() {
            return round;
        }

        public byte[] encode(byte[] payload) throws Exception {
            int bytesToEncode = Integer.BYTES + Long.BYTES + Integer.BYTES;
            if (payload.length < bytesToEncode) {
                throw new Exception("payload too small");
            }
            // convert to bytes
            ByteBuffer buffer = ByteBuffer.allocate(bytesToEncode);
            buffer.putInt(id);
            buffer.putLong(time);
            buffer.putInt(round);
            buffer.rewind();

            // stash bytes in payload
            buffer.get(payload, 0, bytesToEncode);
            return payload;
        }
    }
}
