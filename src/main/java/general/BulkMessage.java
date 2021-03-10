package general;

import java.io.Serializable;

public class BulkMessage extends Message implements Serializable {
    int maxSize;

    public BulkMessage(byte[] payload, int maxSize, String name) {
        super("bulk", name, payload);
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
