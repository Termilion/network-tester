package application.source;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

public class IoTSource extends Source {

    public static final double IOT_DATA_SIZE = 1e6;

    public IoTSource(NTPClient ntp, String address, int port, int resetTime, Date stopTime, int bufferSize) throws IOException {
        super(ntp, address, port, bufferSize, resetTime, stopTime);
    }

    @Override
    public void execute() throws IOException {
        OutputStream out = socket.getOutputStream();
        int numberSend = 0;
        if (super.sendBufferSize > 0) {
            this.socket.setSendBufferSize(super.sendBufferSize);
        }

        double maxNumberOfPackets = Math.ceil(IOT_DATA_SIZE / 1000);
        for (int j = 0; j < maxNumberOfPackets; j++) {
            byte[] kByte = new byte[1000];
            long time = this.ntp.getCurrentTimeNormalized();

            if (isRunning) {
                try {
                    out.write(Utility.encodeTime(kByte, time));
                    out.flush();
                    numberSend++;
                    if (numberSend % 1000 == 0) {
                        ConsoleLogger.log("%s | send one MByte! [%s]", socket.getInetAddress().getHostAddress(), numberSend);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            } else {
                break;
            }
        }
        ConsoleLogger.log("%s | stopped transmission. %s bytes send", socket.getInetAddress().getHostAddress(), numberSend);
        out.close();
    }
}