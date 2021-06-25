package application.source;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

public class BulkSource extends Source {

    public BulkSource(NTPClient ntp, String address, int port, int resetTime, Date stopTime, int bufferSize) throws IOException {
        super(ntp, address, port, bufferSize, resetTime, stopTime);
    }

    @Override
    public void execute() throws IOException {
        OutputStream out = socket.getOutputStream();
        int numberSend = 0;
        if (super.sendBufferSize > 0) {
            this.socket.setSendBufferSize(super.sendBufferSize);
        }
        while (isRunning) {
            byte[] kByte = new byte[1000];
            long time = this.ntp.getCurrentTimeNormalized();

            try {
                out.write(Utility.encodeTime(kByte, time));
                out.flush();
                numberSend++;
                if (numberSend % 100000 == 0) {
                    ConsoleLogger.log("%s | send 100 MByte! [%s]", socket.getInetAddress().getHostAddress(), numberSend);
                }
            } catch (Exception e) {
                // if source isn't running anymore and the socket fails, there is no need to print the stacktrace
                if (isRunning) {
                    e.printStackTrace();
                }
                break;
            }
        }
        ConsoleLogger.log("%s | stopped transmission. %s bytes send", socket.getInetAddress().getHostAddress(), numberSend);
        out.close();
    }
}
