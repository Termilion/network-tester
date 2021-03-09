package general;

import java.io.Serializable;

public abstract class Message implements Serializable {
    protected String type;
    protected String name;
    protected byte[] payload;

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public byte[] getPayload() {
        return payload;
    }
}
