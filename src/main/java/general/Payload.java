package general;

import java.io.Serializable;
import java.sql.Timestamp;

public abstract class Payload implements Serializable {
    protected String type;
    protected byte[] payload;
    protected Timestamp timestamp;

    public Payload(String type, byte[] payload, NTPClient client) {
        this.type = type;
        this.payload = payload;
        this.timestamp = new Timestamp(client.getCurrentTimeNormalized());
    }

    public String getType() {
        return type;
    }


    public byte[] getPayload() {
        return payload;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
