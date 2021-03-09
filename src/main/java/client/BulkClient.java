package client;

import java.io.IOException;
import java.io.ObjectOutputStream;

import general.BulkMessage;
import general.Utility;

public class BulkClient extends Client {

    public BulkClient(String address, int port, String name, int numberOfTransmissions) {
        super(address, port, name, 5000, numberOfTransmissions);
    }

    @Override
    public void execute() throws IOException {
        System.out.printf("%s:%d", this.name, this.numberOfMBytes);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        int numberSend = 0;
        for (int i = 0; i < this.numberOfMBytes; i++) {
            byte[] mByte = Utility.generateBytes(1000000);
            BulkMessage message = new BulkMessage(mByte, this.numberOfMBytes, this.name);
            out.writeObject(message);
            numberSend++;
            if ((numberSend % 100) == 0) {
                System.out.printf("Send a hundred Mbyte! Current Number of Mbytes: %d\n", numberSend);
            }
        }
        out.flush();
        out.close();
    }
}
