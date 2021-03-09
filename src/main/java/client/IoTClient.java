package client;

import general.BulkMessage;
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
        System.out.printf("%s:%d", this.name, this.numberOfMBytes);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        byte[] mByte = Utility.generateBytes(1000000);
        IoTMessage message = new IoTMessage(mByte, this.name);
        out.writeObject(message);
        System.out.println("Send an IoTMessage!\n");
        out.flush();
        out.close();
    }
}