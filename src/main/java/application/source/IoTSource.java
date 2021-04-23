package application.source;

import general.ConsoleLogger;
import general.IoTPayload;
import general.NTPClient;
import general.Utility;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Random;

public class IoTSource extends Source {

    public IoTSource(NTPClient ntp, String address, int port, int numberOfTransmissions, int sendBufferSize) {
        super(ntp, address, port, 1, numberOfTransmissions, sendBufferSize, 100 * new Random().nextInt(10));
    }

    public IoTSource(NTPClient ntp, String address, int port, int numberOfTransmissions) {
        super(ntp, address, port, 1, numberOfTransmissions, 1000, 100 * new Random().nextInt(10));
    }

    public IoTSource(NTPClient ntp, String address, int port) {
        super(ntp, address, port, 1, 100, 1000, 100 * new Random().nextInt(10));
    }

    @Override
    public void execute() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        byte[] mByte = Utility.generateBytes(1000000);
        IoTPayload message = new IoTPayload(mByte, this.ntp);
        out.writeObject(message);
        ConsoleLogger.log(
                String.format(
                        "IoT-Client: Send an IoTMessage!"
                )
        );
        out.flush();
        out.close();
    }
}