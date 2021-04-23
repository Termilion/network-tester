package general;

import java.io.Serializable;

public class IoTPayload extends Payload implements Serializable {

    public IoTPayload(byte[] payload, NTPClient client) {
        super("iot", payload, client);
    }
}
