package client;

import general.IoTMessage;
import general.Utility;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Random;

public class IoTClient extends Client {

    public IoTClient(String address, int port, String name, int numberOfTransmissions, int sendBufferSize) throws IOException {
        super(address, port, name, 1, numberOfTransmissions, sendBufferSize, 100 * new Random().nextInt(10));
    }

    public IoTClient(String address, int port, String name, int numberOfTransmissions) throws IOException {
        super(address, port, name, 1, numberOfTransmissions, 1000, 100 * new Random().nextInt(10));
    }

    public IoTClient(String address, int port, String name) throws IOException {
        super(address, port, name, 1, 100, 1000, 100 * new Random().nextInt(10));
    }

    @Override
    public void execute() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        byte[] mByte = Utility.generateBytes(1000000);
        IoTMessage message = new IoTMessage(mByte, this.name, this.ntp);
        out.writeObject(message);
        System.out.printf("%s: Send an IoTMessage!", this.name);
        out.flush();
        out.close();
    }
}