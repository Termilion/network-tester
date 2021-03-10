package client;

import general.IoTMessage;
import general.Utility;

import java.io.IOException;
import java.io.ObjectOutputStream;

public class IoTClient extends Client {

    public IoTClient(String address, int port, String name, int numberOfTransmissions) {
        super(address, port, name, 1, numberOfTransmissions);
    }

    @Override
    public void execute() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        byte[] mByte = Utility.generateBytes(1000000);
        IoTMessage message = new IoTMessage(mByte, this.name);
        out.writeObject(message);
        System.out.printf("%s: Send an IoTMessage!", this.name);
        out.flush();
        out.close();
    }
}