package application.source;

import general.ConsoleLogger;
import general.TimeProvider;
import general.Utility;

import java.io.IOException;
import java.io.OutputStream;

public class BulkSource extends Source {

    public BulkSource(TimeProvider timeProvider, String address, int port, int resetTime, int bufferSize) {
        super(timeProvider, address, port, bufferSize, resetTime);
    }

    @Override
    protected void execute() throws IOException {
        OutputStream out = socket.getOutputStream();
        int numberSend = 0;

        while (isRunning) {
            byte[] kByte = new byte[1000];
            long time = this.timeProvider.getAdjustedTime();

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
                    double simTime = (time - beginTime.getTime()) / 1000.0;
                    ConsoleLogger.log("Source: Exception at simTime " + simTime);
                    e.printStackTrace();
                }
                break;
            }
        }
        ConsoleLogger.log("%s | stopped transmission. %s bytes send", socket.getInetAddress().getHostAddress(), numberSend);
        out.close();
    }
}
