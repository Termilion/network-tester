package general;

import java.io.Serializable;

public class IoTMessage extends Message implements Serializable {

    public IoTMessage(byte[] payload, String name, NTPClient client) {
        super("iot", name, payload, client);
    }
}
