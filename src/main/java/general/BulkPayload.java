package general;

import java.io.Serializable;

public class BulkMessage extends Message implements Serializable {
    int maxSize;

    public BulkMessage(byte[] payload, int maxSize, String name, NTPClient client) {
        super("bulk", name, payload, client);
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
