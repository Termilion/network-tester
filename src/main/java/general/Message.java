package general;

import java.io.Serializable;
import java.sql.Timestamp;

public abstract class Message implements Serializable {
    protected String type;
    protected String name;
    protected byte[] payload;
    protected Timestamp timestamp;

    public Message(String type, String name, byte[] payload) {
        this.type = type;
        this.name = name;
        this.payload = payload;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public byte[] getPayload() {
        return payload;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
