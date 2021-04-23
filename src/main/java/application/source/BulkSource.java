package application.source;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import general.BulkPayload;
import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

public class BulkSource extends Source {

    public BulkSource(NTPClient ntp, String address, int port, int numberOfTransmissions, int sendBufferSize) throws IOException {
        super(ntp, address, port, 5000, numberOfTransmissions, sendBufferSize);
    }

    public BulkSource(NTPClient ntp, String address, int port, int numberOfTransmissions) throws IOException {
        super(ntp, address, port, 5000, numberOfTransmissions, 1000);
    }

    @Override
    public void execute() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        int numberSend = 0;
        this.socket.setSendBufferSize(super.sendBufferSize);
        for (int i = 0; i < this.numberOfMBytes; i++) {
            byte[] mByte = Utility.generateBytes(1000000);
            BulkPayload message = new BulkPayload(mByte, this.numberOfMBytes, this.ntp);
            out.writeUnshared(message);
            out.reset();
            numberSend++;
            if ((numberSend % 100) == 0) {
                ConsoleLogger.log(
                        String.format(
                                "Bulk-Source: Send a hundred Mbyte! Current Number of Mbytes: %d",
                                numberSend
                        )
                );
            }
        }
        out.flush();
        out.close();
    }
}
