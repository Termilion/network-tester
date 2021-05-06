package application.source;

import java.io.*;
import java.nio.ByteBuffer;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

public class BulkSource extends Source {

    public BulkSource(NTPClient ntp, String address, int port, int waitTime, double numBytesToSend, int bufferSize) throws IOException {
        super(ntp, address, port, numBytesToSend, bufferSize, waitTime);
    }

    @Override
    public void execute() throws IOException {
        OutputStream out = socket.getOutputStream();
        int numberSend = 0;
        this.socket.setSendBufferSize(super.sendBufferSize);
        double maxNumberOfPackets = Math.ceil((double) this.numberOfBytesToSend / 1000);
        for (int j = 0; j < maxNumberOfPackets; j++) {
            byte[] kByte = new byte[1000];
            long time = this.ntp.getCurrentTimeNormalized();

            try {
                out.write(Utility.encodeTime(kByte, time));
                out.flush();
                numberSend++;
                if (numberSend % 1000 == 0) {
                    ConsoleLogger.log("Send an MByte!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        out.close();
    }
}
