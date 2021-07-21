package application.source;

import general.TimeProvider;
import general.Utility;
import general.logger.ConsoleLogger;

import java.io.IOException;
import java.io.OutputStream;

public class IoTSource extends Source {

    public static final double IOT_DATA_SIZE = 1e6;

    public IoTSource(TimeProvider timeProvider, String address, int port, int resetTime, int bufferSize) {
        super(timeProvider, address, port, bufferSize, resetTime);
    }

    @Override
    protected void execute() throws IOException {
        OutputStream out = socket.getOutputStream();
        int numberSend = 0;

        double maxNumberOfPackets = Math.ceil(IOT_DATA_SIZE / 1000);
        for (int j = 0; j < maxNumberOfPackets; j++) {
            byte[] kByte = new byte[1000];
            long time = this.timeProvider.getAdjustedTime();

            if (isRunning) {
                try {
                    byte[] bytes = Utility.encodeTime(kByte, time);
                    out.write(bytes);
                    out.flush();
                    numberSend++;
                    if (numberSend % 1000 == 0) {
                        ConsoleLogger.log("%s | send one MByte! [%s]", socket.getInetAddress().getHostAddress(), numberSend);
                    }
                } catch (Exception e) {
                    double simTime = (time - beginTime.getTime()) / 1000.0;
                    ConsoleLogger.log("Source: Exception at simTime " + simTime);
                    e.printStackTrace();
                    break;
                }
            } else {
                break;
            }
        }
        ConsoleLogger.log("%s | stopped transmission. %s packets send", socket.getInetAddress().getHostAddress(), numberSend);
        out.close();
        socket.close();
    }
}
