package general;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

public class Utility {
    public static class InterfaceNotFoundException extends IOException {
        public InterfaceNotFoundException(String interfaceName) {
            super("Specified network interface not found! " + interfaceName);
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

    public static byte[] encodeTime(byte[] payload, long time) throws Exception {
        if (payload.length < Long.BYTES) {
            throw new Exception("payload too small");
        } else {
            // convert long -> bytes
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(time);
            byte[] tsArray = buffer.array();

            // stash bytes in payload
            for (int k = 0; k < tsArray.length; k++) {
                payload[k] = tsArray[k];
            }
            return payload;
        }
    }

    public static long decodeTime(byte[] payload) {
        if (payload.length < Long.BYTES) {
            return -1;
        } else {
            // extract first bytes
            byte[] bytes = new byte[Long.BYTES];
            for (int i = 0; i < Long.BYTES; i++) {
                bytes[i] = payload[i];
            }

            // convert bytes -> long
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.put(bytes);
            buffer.flip();
            return buffer.getLong();
        }
    }
}
