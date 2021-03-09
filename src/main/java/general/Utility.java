package general;

import java.util.Random;

public class Utility {
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
}
