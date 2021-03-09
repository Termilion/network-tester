package general;

import java.io.Serializable;

public class BulkMessage extends Message implements Serializable {
    int maxSize;

    public BulkMessage(byte[] payload, int maxSize, String name) {
        this.type = "bulk";
        this.payload = payload;
        this.maxSize = maxSize;
        this.name = name;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
