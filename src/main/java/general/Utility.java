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
        /**
         * Number of Bytes to encode/decode into/from the payload
         * Boolean = 1 Byte
         */
        public static final int PAYLOAD_BYTES = Integer.BYTES + Long.BYTES + Integer.BYTES + 1 + 1;

        int id;
        long time;
        int round;
        boolean firstPacketOfRound;
        boolean lastPacketOfRound;

        public static TransmissionPayload decode(byte[] payload) throws Exception {
            if (payload.length < PAYLOAD_BYTES) {
                throw new Exception("payload too small");
            }
            // convert from bytes
            ByteBuffer buffer = ByteBuffer.allocate(PAYLOAD_BYTES);
            buffer.put(payload, 0, PAYLOAD_BYTES);
            buffer.flip();

            int id = buffer.getInt();
            long time = buffer.getLong();
            int round = buffer.getInt();
            boolean firstPacketOfRound = buffer.get() == 1;
            boolean lastPacketOfRound = buffer.get() == 1;

            return new TransmissionPayload(id, time, round, firstPacketOfRound, lastPacketOfRound);
        }

        public TransmissionPayload(int id, long time, int round, boolean firstPacketOfRound, boolean lastPacketOfRound) {
            this.id = id;
            this.time = time;
            this.round = round;
            this.firstPacketOfRound = firstPacketOfRound;
            this.lastPacketOfRound = lastPacketOfRound;
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

        public boolean isFirstPacketOfRound() {
            return firstPacketOfRound;
        }

        public boolean isLastPacketOfRound() {
            return lastPacketOfRound;
        }

        public byte[] encode(byte[] payload) throws Exception {
            if (payload.length < PAYLOAD_BYTES) {
                throw new Exception("payload too small");
            }
            // convert to bytes
            ByteBuffer buffer = ByteBuffer.allocate(PAYLOAD_BYTES);
            buffer.putInt(id);
            buffer.putLong(time);
            buffer.putInt(round);
            buffer.put((byte) (firstPacketOfRound ? 1 : 0));
            buffer.put((byte) (lastPacketOfRound ? 1 : 0));
            buffer.rewind();

            // stash bytes in payload
            buffer.get(payload, 0, PAYLOAD_BYTES);
            return payload;
        }
    }
}
