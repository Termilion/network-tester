package general;

import java.io.Serializable;

public class BulkPayload extends Payload implements Serializable {
    int maxSize;

    public BulkPayload(byte[] payload, int maxSize, NTPClient client) {
        super("bulk", payload, client);
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
