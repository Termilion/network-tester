package client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import general.BulkMessage;
import general.Utility;

public class BulkClient extends Client {

    public BulkClient(String address, int port, String name, int numberOfTransmissions, int sendBufferSize) throws IOException {
        super(address, port, name, 5000, numberOfTransmissions, sendBufferSize);
    }

    public BulkClient(String address, int port, String name, int numberOfTransmissions) throws IOException {
        super(address, port, name, 5000, numberOfTransmissions, 1000);
    }

    @Override
    public void execute() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        int numberSend = 0;
        this.socket.setSendBufferSize(super.sendBufferSize);
        for (int i = 0; i < this.numberOfMBytes; i++) {
            byte[] mByte = Utility.generateBytes(1000000);
            BulkMessage message = new BulkMessage(mByte, this.numberOfMBytes, this.name, this.ntp);
            out.writeUnshared(message);
            out.reset();
            numberSend++;
            if ((numberSend % 100) == 0) {
                System.out.printf("%s: Send a hundred Mbyte! Current Number of Mbytes: %d\n", this.name, numberSend);
            }
        }
        out.flush();
        out.close();
    }
}
