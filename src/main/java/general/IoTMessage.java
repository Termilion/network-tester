package general;

import java.io.Serializable;

public class IoTMessage extends Message implements Serializable {

    public IoTMessage(byte[] payload, String name) {
        super("iot", name, payload);
    }
}
