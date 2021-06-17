package application.source;

import java.io.*;
import java.nio.ByteBuffer;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

public class BulkSource extends Source {

    public BulkSource(NTPClient ntp, String address, int port, int resetTime, double numBytesToSend, int bufferSize) throws IOException {
        super(ntp, address, port, numBytesToSend, bufferSize, resetTime);
    }

    @Override
    public void execute() throws IOException {
        OutputStream out = socket.getOutputStream();
        int numberSend = 0;
        if (super.sendBufferSize > 0) {
            this.socket.setSendBufferSize(super.sendBufferSize);
        }
        double maxNumberOfPackets = Math.ceil(this.numberOfBytesToSend / 1000);
        for (int j = 0; j < maxNumberOfPackets; j++) {
            byte[] kByte = new byte[1000];
            long time = this.ntp.getCurrentTimeNormalized();

            try {
                out.write(Utility.encodeTime(kByte, time));
                out.flush();
                numberSend++;
                if (numberSend % 100000 == 0) {
                    ConsoleLogger.log("%s | send 100 MByte! [%s / %s packets]", socket.getInetAddress().getHostAddress(), j+1, (long) maxNumberOfPackets);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        ConsoleLogger.log("%s | finished transmission of %s bytes", socket.getInetAddress().getHostAddress(), (long) this.numberOfBytesToSend);
        out.close();
    }
}
